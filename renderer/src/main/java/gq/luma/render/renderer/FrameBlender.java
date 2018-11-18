package gq.luma.render.renderer;

import com.walker.pipeline.PipelineDatatype;
import com.walker.pipeline.PipelineJoint;

import java.nio.ByteBuffer;

public class FrameBlender extends PipelineJoint {
    protected FrameBlender() {
        super(PipelineDatatype.RAW_BGR24, PipelineDatatype.RAW_BGR24);
    }

    @Override
    protected void consumeBuffer(ByteBuffer buffer) {

    }

    @Override
    protected void flush() {

    }
}
