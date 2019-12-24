package gq.luma.render.renderer;

import com.walker.pipeline.PipelineDatatype;
import com.walker.pipeline.longtype.LongPipelineJoint;
import gq.luma.render.RenderSettings;
import jnr.ffi.NativeType;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Type;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

public class FrameBlender extends LongPipelineJoint {

    private static final Type UCHAR_TYPE = Runtime.getSystemRuntime().findType(NativeType.UCHAR);

    private int frameblend;
    private int bufferSize;
    //private float multiplier;

    private long totalBufferPointer;
    private int[] totalData;
    private int framesAccumulated;

    private AtomicBoolean shutdown = new AtomicBoolean(false);
    private Thread blenderThread;

    //private Pointer totalAccumulator;

    //private int[] accumulatedData;
    //private int pointerIndex = 0;

    //private int accumulatedFrames = 0;

    private long[] frameCache;
    private Object[] frameCacheLocks;
    private final Object frameCacheFull;
    private int frameIndex;

    private Unsafe unsafe;

    public FrameBlender(RenderSettings settings) {
        super(PipelineDatatype.RAW_BGR24, PipelineDatatype.RAW_BGR24);

        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            this.unsafe = (Unsafe) f.get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }

        frameblend = settings.getFrameblend();
        //multiplier = 1f / settings.getFrameblend();
        bufferSize = settings.getWidth() * settings.getHeight() * 3;
        //accumulatedData = new int[bufferSize];
        //wrappedData = new byte[bufferSize];
        totalData = new int[bufferSize];
        //storedBuffer = ByteBuffer.wrap(wrappedData);
        totalBufferPointer = unsafe.allocateMemory(bufferSize);

        frameCache = new long[frameblend];
        frameCacheLocks = new Object[frameblend];
        for(int i = 0; i < frameblend; i++) {
            frameCacheLocks[i] = new Object();
        }
        frameCacheFull = new Object();
        frameIndex = 0;

        blenderThread = new Thread(() -> {
            while(!shutdown.get()) {
                //System.err.println("Processing buffers====================");
                processBuffers();
            }
        }, "Blender-Thread");
        blenderThread.start();
    }

    @Override
    public void ready() {
        super.ready();
        //totalAccumulator = requestZCBuffer();
    }

    /*protected void consumeBuffer(ByteBuffer buffer) {
        buffer.position(0);
        int j = 0;
        while(buffer.hasRemaining()){
            totalData[j++] += (buffer.get() & 0xFF);
            //int sub = ((int) (( * multiplier) + (wrappedData[j] & 0xFF)));
            //if(sub > 0xFF) System.err.println("Pixel " + j + " is evil.");
            //wrappedData[j] = (byte) (sub);
            //j++;
        }

        if(framesAccumulated % frameblend == frameblend - 1){
            for(int i = 0; i < totalData.length; i++){
                wrappedData[i] = (byte) (totalData[i] / frameblend);
            }

            pushBuffer(storedBuffer);
            totalData = new int[bufferSize];
        }

        framesAccumulated++;
    }*/

    protected void consumeBuffer(long buffer) {
        synchronized (frameCacheLocks[frameIndex]) {
            if(0L != frameCache[frameIndex]) {

                try {
                    frameCacheLocks[frameIndex].wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            frameCache[frameIndex] = buffer;
            frameCacheLocks[frameIndex].notify();
        }

        frameIndex++;

        if(frameIndex == frameblend) {
            //synchronized (frameCacheFull) {
            //    frameCacheFull.notify();
            //}
            //System.err.println("Processing buffers====================");
            //new Thread(this::processBuffers).start();

            frameIndex = 0;
        }
    }

    private void processBuffers() {
        final Unsafe unsafe = this.unsafe;
        final int frameblend = this.frameblend;
        final int bufferSize = this.bufferSize;

        int j = 0;

        for(int i = 0; i < frameblend; i++) {
            synchronized (frameCacheLocks[i]) {
                long thisFrame = frameCache[i];

                if(0L == thisFrame) {
                    try {
                        frameCacheLocks[i].wait(0);
                        thisFrame = frameCache[i];
                    } catch (InterruptedException e) {
                        return;
                    }
                }

                j = 0;

                while (j < bufferSize) {
                    totalData[j] += unsafe.getByte(null, thisFrame + j) & 0xFF;
                    j++;
                }

                unsafe.freeMemory(frameCache[i]);

                frameCache[i] = 0L;
                //System.err.println("notifying framecachelocks[" + i + "]");
                frameCacheLocks[i].notify();
            }
        }

        for(int i = 0; i < totalData.length; i++){
            unsafe.putByte(totalBufferPointer + i, (byte) (totalData[i] / frameblend));
        }

        System.err.println("pushing the buffer");

        pushBuffer(totalBufferPointer);
        this.totalData = new int[bufferSize];
    }

    /*protected void consumeBuffer(Pointer buffer) {
        //System.out.println(buffer.size());
        //if(buffer.size() == Long.MAX_VALUE) throw new IllegalStateException("mmmmax out");
        for(int i = 0; i < buffer.size(); i++) {
            accumulatedData[pointerIndex++] += buffer.getByte(i) & 0xFF;
        }

        //System.out.println("did it?");

        if(pointerIndex == bufferSize) {
            pointerIndex = 0;
            accumulatedFrames++;
            if(accumulatedFrames == frameblend) {
                for(int i = 0; i < bufferSize; i++) {
                    totalAccumulator.putInt(UCHAR_TYPE, i, accumulatedData[i] / frameblend);
                }

                pushZCBuffer();

                accumulatedFrames = 0;
            }
        }
    }*/

    @Override
    protected long provideZCBuffer() {
        return -1L;
    }

    @Override
    protected void consumeZC() {

    }

    @Override
    protected void flush() {
        shutdown.set(true);
        try {
            blenderThread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (blenderThread.isAlive()) {
            Thread.onSpinWait();
            blenderThread.interrupt();
        }
    }
}
