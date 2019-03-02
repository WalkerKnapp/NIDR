package gq.luma.render.renderer;

import com.walker.pipeline.PipelineDatatype;
import com.walker.pipeline.PipelineJoint;
import gq.luma.render.RenderSettings;
import jnr.ffi.NativeType;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Type;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

public class FrameBlender extends PipelineJoint<ByteBuffer> {

    private static final Type UCHAR_TYPE = Runtime.getSystemRuntime().findType(NativeType.UCHAR);

    private int frameblend;
    private int bufferSize;
    //private float multiplier;

    private ByteBuffer storedBuffer;
    private int[] totalData;
    private byte[] wrappedData;
    private int framesAccumulated;

    //private Pointer totalAccumulator;

    //private int[] accumulatedData;
    //private int pointerIndex = 0;

    //private int accumulatedFrames = 0;

    private ByteBuffer[] frameCache;
    private Object[] frameCacheLocks;
    private final Object frameCacheFull;
    private int frameIndex;

    public FrameBlender(RenderSettings settings) {
        super(PipelineDatatype.RAW_BGR24, PipelineDatatype.RAW_BGR24);
        frameblend = settings.getFrameblend();
        //multiplier = 1f / settings.getFrameblend();
        bufferSize = settings.getWidth() * settings.getHeight() * 3;
        //accumulatedData = new int[bufferSize];
        wrappedData = new byte[bufferSize];
        totalData = new int[bufferSize];
        storedBuffer = ByteBuffer.wrap(wrappedData);

        frameCache = new ByteBuffer[frameblend];
        frameCacheLocks = new Object[frameblend];
        for(int i = 0; i < frameblend; i++) {
            frameCacheLocks[i] = new Object();
        }
        frameCacheFull = new Object();
        frameIndex = 0;

        new Thread(() -> {
            while(true) {
                //System.err.println("Processing buffers====================");
                processBuffers();
            }
        }).start();
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

    protected void consumeBuffer(ByteBuffer buffer) {
        synchronized (frameCacheLocks[frameIndex]) {
            if(frameCache[frameIndex] != null) {

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
        /*synchronized (frameCacheFull) {
            try {
                System.err.println("waiting for framecachefull");
                frameCacheFull.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/

        for(int i = 0; i < frameblend; i++) {
            synchronized (frameCacheLocks[i]) {
                if(frameCache[i] == null) {
                    try {
                        //System.err.println("waiting for framecachelocks[" + i + "]");
                        frameCacheLocks[i].wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }

                frameCache[i].position(0);
                int j = 0;
                while(frameCache[i].hasRemaining()){
                //for(int j = 0; j < bufferSize; j++) {
                    totalData[j++] += (frameCache[i].get() & 0xFF);
                    //int sub = ((int) (( * multiplier) + (wrappedData[j] & 0xFF)));
                    //if(sub > 0xFF) System.err.println("Pixel " + j + " is evil.");
                    //wrappedData[j] = (byte) (sub);
                    //j++;
                }

                frameCache[i] = null;
                //System.err.println("notifying framecachelocks[" + i + "]");
                frameCacheLocks[i].notify();
            }
        }

        for(int i = 0; i < totalData.length; i++){
            wrappedData[i] = (byte) (totalData[i] / frameblend);
        }

        System.err.println("pushing the buffer");

        pushBuffer(storedBuffer);
        totalData = new int[bufferSize];
    }

    protected void consumeBuffer(Pointer buffer) {
        /*//System.out.println(buffer.size());
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
        }*/
    }

    @Override
    protected ByteBuffer provideZCBuffer() {
        return null;
    }

    @Override
    protected void consumeZC() {

    }

    @Override
    protected void flush() {

    }
}
