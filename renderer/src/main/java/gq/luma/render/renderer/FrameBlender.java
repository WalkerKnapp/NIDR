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
    private byte[] wrappedData;
    private int framesAccumulated;

    public FrameBlender(RenderSettings settings) {
        super(PipelineDatatype.RAW_BGR24, PipelineDatatype.RAW_BGR24);
        frameblend = settings.getFrameblend();
        multiplier = 1f / settings.getFrameblend();
        bufferSize = settings.getWidth() * settings.getHeight() * 3;
        wrappedData = new byte[bufferSize];
        storedBuffer = ByteBuffer.wrap(wrappedData);
    }

    @Override
    protected void consumeBuffer(ByteBuffer buffer) {
        buffer.position(0);
        int j = 0;
        while(buffer.hasRemaining()){
            wrappedData[j++] += (int)(buffer.get() * multiplier) & 0xFF;
        }

        if(framesAccumulated % frameblend == frameblend - 1){
            pushBuffer(storedBuffer);
            wrappedData = new byte[bufferSize];
            storedBuffer = ByteBuffer.wrap(wrappedData);
        }

        framesAccumulated++;
    }

    @Override
    protected void flush() {

    }
}
