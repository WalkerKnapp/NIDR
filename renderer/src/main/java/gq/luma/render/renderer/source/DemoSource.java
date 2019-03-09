package gq.luma.render.renderer.source;

import com.walker.pipeline.PipelineDatatype;
import com.walker.pipeline.PipelineSource;
import gq.luma.render.CompletedRender;
import gq.luma.render.RenderRequest;
import gq.luma.render.RenderSettings;
import gq.luma.render.engine.SrcGame;
import gq.luma.render.renderer.configuration.ConfigurationException;
import gq.luma.render.renderer.configuration.SrcGameConfiguration;
import gq.luma.render.engine.SrcGameInstance;
import gq.luma.render.engine.filesystem.FSAudioHandler;
import gq.luma.render.engine.filesystem.FSVideoHandler;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public abstract class DemoSource extends PipelineSource<ByteBuffer> implements FSVideoHandler, FSAudioHandler {
    protected ExecutorService threadPool = Executors.newFixedThreadPool(Integer.MAX_VALUE);
    protected AtomicReference<RenderRequest> currentRender = new AtomicReference<>(null);

    protected HashMap<SrcGame, SrcGameConfiguration> gameConfigurations = new HashMap<>();
    protected ConcurrentHashMap<SrcGame, SrcGameInstance> runningGames = new ConcurrentHashMap<>();

    // Framebuffer

    private Unsafe unsafe;
    private ByteBuffer frameBuffer;
    private Pointer frameBufferPointer;
    private long frameBufferBasePointer;
    private Field bAddr;
    private long addressBufferOffset;

    private long frameSize;
    private int bufferSize;

    public DemoSource() {
        super(PipelineDatatype.SuperType.RAW_VIDEO, PipelineDatatype.RAW_BGR24);
    }

    public CompletableFuture<CompletedRender> renderDemo(RenderRequest renderRequest) {
        if(currentRender.get() != null){
            throw new IllegalStateException("Multiple demos cannot be rendered at once with the " + getClass().getSimpleName());
        }
        return CompletableFuture.supplyAsync(() -> {
            currentRender.set(renderRequest);

            try {
                // Setup unsafe
                Field f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                this.unsafe = (Unsafe) f.get(null);

                // Setup buffers
                bufferSize = (renderRequest.getSettings().getWidth() * renderRequest.getSettings().getHeight() * 3);
                frameSize = bufferSize + 18;
                frameBuffer = ByteBuffer.allocateDirect(bufferSize);
                bAddr = java.nio.Buffer.class.getDeclaredField("address");
                bAddr.setAccessible(true);
                this.frameBufferBasePointer = (long) bAddr.get(frameBuffer);
                this.frameBufferPointer = Pointer.wrap(Runtime.getSystemRuntime(), frameBuffer);
                this.addressBufferOffset = unsafe.objectFieldOffset(bAddr);

                runSyncRender(renderRequest);

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

    protected SrcGameConfiguration getConfiguration(SrcGame game){
        if(!gameConfigurations.containsKey(game)){
            throw new ConfigurationException(game.name() + " configuration not set for " + getClass().getSimpleName());
        }
        return gameConfigurations.get(game);
    }

    protected void setupGame(SrcGame game){
        if(runningGames.containsKey(game)){
            return;
        }
        SrcGameConfiguration configuration = getConfiguration(game);

        SrcGameInstance instance = new SrcGameInstance(game, configuration, (t, bool) -> {
            t.printStackTrace();
        });
        runningGames.put(game, instance);
        instance.start(this, this);
    }

    @Override
    protected void flush() {
        // Source will never need to be flushed
    }

    @Override
    public void handleAudioData(Pointer buf, long offset, long size) {
        // TODO: Handle Audio
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

        //buf.transferTo(frameOffset, frameBufferPointer, destOffset, writeLength - frameOffset);
        unsafe.copyMemory(buf.address() + frameOffset, frameBufferBasePointer + destOffset, writeLength - frameOffset);

        //Pointer p = Runtime.getSystemRuntime().getMemoryManager().allocate((int) (writeLength - frameOffset));
        //buf.transferTo(frameOffset, p, 0, writeLength - frameOffset);
        //unsafe.copyMemory(buf.address() + frameOffset, p.address(), writeLength - frameOffset);

        //pushBuffer(p);
        //pushBuffer(Runtime.getSystemRuntime().getMemoryManager().newPointer(buf.address() + frameOffset, writeLength - frameOffset));

        if(offset + writeLength == frameSize){
            System.out.println("Pushing frame");
            pushBuffer(frameBuffer);

            frameBuffer = ByteBuffer.allocateDirect(bufferSize);
            //frameBufferPointer = Pointer.wrap(Runtime.getSystemRuntime(), frameBuffer);
            frameBufferBasePointer = unsafe.getLong(frameBuffer, addressBufferOffset);
            /*try {
                frameBufferBasePointer = (long) bAddr.get(frameBuffer);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }*/
        }
    }
}
