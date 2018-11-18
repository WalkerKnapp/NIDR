package gq.luma.render.renderer;

import com.walker.pipeline.PipelineDatatype;
import com.walker.pipeline.PipelineJoint;
import gq.luma.render.RenderSettings;

import java.nio.ByteBuffer;

public class FrameBlender extends PipelineJoint {
    private int frameblend;
    private int bufferSize;
    private float multiplier;

    private ByteBuffer storedBuffer;
    private int[] totalData;
    private byte[] wrappedData;
    private int framesAccumulated;

    public FrameBlender(RenderSettings settings) {
        super(PipelineDatatype.RAW_BGR24, PipelineDatatype.RAW_BGR24);
        frameblend = settings.getFrameblend();
        multiplier = 1f / settings.getFrameblend();
        bufferSize = settings.getWidth() * settings.getHeight() * 3;
        wrappedData = new byte[bufferSize];
        totalData = new int[bufferSize];
        storedBuffer = ByteBuffer.wrap(wrappedData);
    }

    @Override
    protected void consumeBuffer(ByteBuffer buffer) {
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
    }

    @Override
    protected void flush() {

    }
}
