package gq.luma.render.renderer.source;

import com.walker.pipeline.PipelineDatatype;
import com.walker.pipeline.PipelineSource;
import com.walker.pipeline.longtype.LongPipelineSource;
import gq.luma.render.CompletedRender;
import gq.luma.render.RenderRequest;
import gq.luma.render.RenderSettings;
import gq.luma.render.renderer.configuration.ConfigurationException;
import gq.luma.render.renderer.configuration.SrcGameConfiguration;
import gq.luma.render.engine.SrcGameInstance;
import gq.luma.render.engine.filesystem.FSAudioHandler;
import gq.luma.render.engine.filesystem.FSVideoHandler;
import io.humble.video.AudioChannel;
import io.humble.video.AudioFormat;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class DemoSource implements FSVideoHandler, FSAudioHandler {
    protected ExecutorService threadPool = Executors.newFixedThreadPool(Integer.MAX_VALUE);
    protected AtomicReference<RenderRequest> currentRender = new AtomicReference<>(null);

    protected HashMap<String, SrcGameConfiguration> gameConfigurations = new HashMap<>();
    protected ConcurrentHashMap<SrcGameConfiguration, SrcGameInstance> runningGames = new ConcurrentHashMap<>();

    protected DemoVideoSource demoVideoSource;
    protected DemoAudioSource demoAudioSource;

    // Framebuffer

    private Unsafe unsafe;
    //private ByteBuffer frameBuffer;
    //private Pointer frameBufferPointer;
    private long frameBufferBasePointer;
    ///private Field bAddr;
    //private long addressBufferOffset;

    private long frameSize;
    private int bufferSize;

    private int skipFrames;

    //private long[] circularFrameBuffers;


    /** Audio **/

    public static final AudioFormat.Type audioFormatType = AudioFormat.Type.SAMPLE_FMT_S16;
    public static final AudioChannel.Layout audioChannelLayout = AudioChannel.Layout.CH_LAYOUT_STEREO;
    public static final long sampleRate = 44100;
    public static final int channels = 2;
    public static final int samplesPerFrame = 1024;

    private static final int bytesPerSample = 2;

    private int audioBufferSize = bytesPerSample * samplesPerFrame * channels;

    private boolean headered = false;
    private long audioBufferBasePointer;
    private long audioPointer = 0;
    private CompletableFuture<Void> audioFinished = new CompletableFuture<>();


    public class DemoVideoSource extends LongPipelineSource {

        protected DemoVideoSource() {
            super(PipelineDatatype.SuperType.RAW_VIDEO, PipelineDatatype.RAW_BGR24);
        }

        @Override
        protected void flush() {

        }

        public void sendBuffer(long buffer) {
            pushBuffer(buffer);
        }
    }

    public class DemoAudioSource extends LongPipelineSource {
        protected DemoAudioSource() {
            super(PipelineDatatype.SuperType.RAW_VIDEO, PipelineDatatype.RAW_BGR24);
        }

        @Override
        protected void flush() {

        }

        public void sendBuffer(long buffer) {
            pushBuffer(buffer);
        }
    }

    public DemoSource() {
        super();

        this.demoVideoSource = new DemoVideoSource();
        this.demoAudioSource = new DemoAudioSource();

        try {
            // Setup unsafe
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            this.unsafe = (Unsafe) f.get(null);
            //this.unsafe = (Unsafe) Class.forName("jdk.internal.misc.Unsafe")
             //       .getMethod("getUnsafe")
            //        .invoke(null);

            //bAddr = java.nio.Buffer.class.getDeclaredField("address");
            //bAddr.setAccessible(true);

            //this.addressBufferOffset = unsafe.objectFieldOffset(bAddr);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<CompletedRender> renderDemo(RenderRequest renderRequest) {
        if(currentRender.get() != null){
            throw new IllegalStateException("Multiple demos cannot be rendered at once with the " + getClass().getSimpleName());
        }
        return CompletableFuture.supplyAsync(() -> {
            currentRender.set(renderRequest);

            try {
                // Setup buffers
                bufferSize = (renderRequest.getSettings().getWidth() * renderRequest.getSettings().getHeight() * 3);
                frameSize = bufferSize + 18;
                //frameBuffer = ByteBuffer.allocateDirect(bufferSize);

                //this.frameBufferBasePointer = (long) bAddr.get(frameBuffer);
                //this.frameBufferPointer = Pointer.wrap(Runtime.getSystemRuntime(), frameBuffer);

                /*if(circularFrameBuffers != null) {
                    for (int i = 0; i < frameblend; i++) {
                        unsafe.freeMemory(circularFrameBuffers[i]);
                    }
                }
                circularFrameBuffers = new long[frameblend];
                for(int i = 0; i < frameblend; i++) {
                    circularFrameBuffers[i] = unsafe.allocateMemory(bufferSize);
                }*/

                this.frameBufferBasePointer = unsafe.allocateMemory(bufferSize);
                this.audioBufferBasePointer = unsafe.allocateMemory(audioBufferSize);

                this.skipFrames = renderRequest.getSettings().getSkipFrames();

                runSyncRender(renderRequest);

                System.out.println("Waiting 10 seconds for audio to end...");
                try {
                    audioFinished.get(10, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    System.out.println("Audio failed to finish");
                }

            } catch (Throwable t){
                t.printStackTrace();
            }

            currentRender.set(null);
            return new CompletedRender();
        }, threadPool);
    };

    public abstract void runSyncRender(RenderRequest renderRequest) throws Exception;

    protected void updateStatus(String status){
        RenderRequest request = currentRender.get();
        if(request != null){
            request.setStatus(status);
        }
    }

    protected SrcGameConfiguration getConfiguration(String gameDirectory){
        if(!gameConfigurations.containsKey(gameDirectory)){
            throw new ConfigurationException(gameDirectory + " configuration not set for " + getClass().getSimpleName());
        }
        return gameConfigurations.get(gameDirectory);
    }

    protected SrcGameInstance setupGame(SrcGameConfiguration game){
        if(runningGames.containsKey(game)){
            return runningGames.get(game);
        }

        SrcGameInstance instance = new SrcGameInstance(game, new BiConsumer<Throwable, Boolean>() {
            @Override
            public void accept(Throwable throwable, Boolean aBoolean) {
                throwable.printStackTrace();
            }
        });
        runningGames.put(game, instance);
        instance.start(this, this);
        return instance;
    }

    @Override
    public void handleAudioData(Pointer buf, long offset, long size) {
        if(offset == 0 && headered){
            addAudioBufferAndFlush(buf, (int) size);
        }
        else{
            this.headered = true;
            if(offset == 40) {
                if(audioPointer != 0){

                } //flush(audioConsumer);
                audioFinished.complete(null);
            }
            audioPointer = 0;
        }
    }

    private void addAudioBufferAndFlush(Pointer data, int writeLength){
        int inputIndex = 0;
        while (inputIndex < writeLength){
            if((writeLength - inputIndex) < (audioBufferSize - audioPointer)){
                unsafe.copyMemory(data.address() + inputIndex, audioBufferBasePointer + audioPointer, writeLength - inputIndex);
                audioPointer += writeLength - inputIndex;
                break;
            }
            else{
                unsafe.copyMemory(data.address() + inputIndex, audioBufferBasePointer + audioPointer, audioBufferSize - audioPointer);
                inputIndex += audioBufferSize - audioPointer;
                flush();
                audioPointer = 0;
            }
        }
    }

    private void flush(){
        //int sampleCount = (int) (audioPointer / (bytesPerSample * channels));
        demoAudioSource.sendBuffer(audioBufferBasePointer);
        unsafe.setMemory(audioBufferBasePointer, audioBufferSize, (byte) 0);
    }

    /*@Override
    public void handleVideoData(int index, Pointer buf, long offset, long writeLength) {
        int frameOffset = 0;
        long destOffset = 0;
        if(offset == 0){
            frameOffset = 18;
        } else {
            destOffset = offset - 18;
        }
        unsafe.copyMemory(buf.address() + frameOffset, frameBufferBasePointer + destOffset, writeLength - frameOffset);

        //Pointer p = Runtime.getSystemRuntime().getMemoryManager().allocate((int) (writeLength - frameOffset));
        //buf.transferTo(frameOffset, p, 0, writeLength - frameOffset);
        //unsafe.copyMemory(buf.address() + frameOffset, p.address(), writeLength - frameOffset);

        //pushBuffer(p);
        //pushBuffer(Runtime.getSystemRuntime().getMemoryManager().newPointer(buf.address() + frameOffset, writeLength - frameOffset));

        if(offset + writeLength == frameSize){
            System.out.println("Pushing frame");
            pushBuffer(frameBuffer);
        }
    }*/

    @Override
    public void handleVideoData(int index, Pointer buf, long offset, long writeLength) {
        int frameOffset = 0;
        long destOffset = 0;
        if(offset == 0){
            frameOffset = 18;
        } else {
            destOffset = offset - 18;
        }

        //System.out.println("Copy length: " + (writeLength - frameOffset) + ", basePointer: " + frameBufferBasePointer + ", destOffset: " + destOffset + ", fromAddress: " + buf.address() + ", fromSize: " + buf.size() + ", toSize: " + bufferSize);

        //buf.transferTo(frameOffset, frameBufferPointer, destOffset, writeLength - frameOffset);
        unsafe.copyMemory(buf.address() + frameOffset, frameBufferBasePointer + destOffset, writeLength - frameOffset);

        //Pointer p = Runtime.getSystemRuntime().getMemoryManager().allocate((int) (writeLength - frameOffset));
        //buf.transferTo(frameOffset, p, 0, writeLength - frameOffset);
        //unsafe.copyMemory(buf.address() + frameOffset, p.address(), writeLength - frameOffset);

        //pushBuffer(p);
        //pushBuffer(Runtime.getSystemRuntime().getMemoryManager().newPointer(buf.address() + frameOffset, writeLength - frameOffset));

        if(offset + writeLength == frameSize){

            if (skipFrames == 0) {
                System.out.println("Pushing frame");
                demoVideoSource.sendBuffer(frameBufferBasePointer);
            } else {
                System.out.println("Skipping frame");
                skipFrames--;
            }

            //currentFrame++;

            //frameBuffer = ByteBuffer.allocateDirect(bufferSize);
            //frameBufferPointer = Pointer.wrap(Runtime.getSystemRuntime(), frameBuffer);
            //frameBufferBasePointer = unsafe.getLong(frameBuffer, addressBufferOffset);
            /*try {
                frameBufferBasePointer = (long) bAddr.get(frameBuffer);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }*/

            //System.out.println("Allocating new frame. Old pointer: " + frameBufferBasePointer);

            frameBufferBasePointer = unsafe.allocateMemory(bufferSize);

            //System.out.println("Allocating new frame. New pointer: " + frameBufferBasePointer);
        }
    }

    public DemoVideoSource getDemoVideoSource() {
        return demoVideoSource;
    }

    public DemoAudioSource getDemoAudioSource() {
        return demoAudioSource;
    }
}
