package gq.luma.render.renderer;

import com.walker.pipeline.PipelineDatatype;
import com.walker.pipeline.PipelineOutput;
import gq.luma.render.RenderSettings;
import io.humble.ferry.Buffer;
import io.humble.video.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public class HumbleVideoOutput extends PipelineOutput {
    private static final Logger logger = LogManager.getLogger();

    private Muxer muxer;

    private Encoder videoEncoder;
    private MediaPictureResampler videoResampler;
    private MediaPacket videoPacket;
    private MediaPicture rawFrame;
    private MediaPicture resampledFrame;
    private ByteBuffer rawFrameBuffer;
    private int frameIndex;

    public HumbleVideoOutput(RenderSettings settings, Path finalFile) throws IOException, InterruptedException {
        super(PipelineDatatype.SuperType.RAW_VIDEO, PipelineDatatype.RAW_BGR24);

        muxer = Muxer.make(finalFile.toAbsolutePath().toString(), null, null);

        Codec codec = Codec.findEncodingCodecByIntID(settings.getOutputFormat().getVideoCodec() + 1);
        logger.debug("Video codec: " + codec.getName());
        videoEncoder = Encoder.make(codec);
        videoEncoder.setWidth(settings.getWidth());
        videoEncoder.setHeight(settings.getHeight());
        videoEncoder.setTimeBase(Rational.make(1, settings.getFps()));

        switch (settings.getOutputFormat()){
            case H264:
                videoEncoder.setPixelFormat(PixelFormat.Type.PIX_FMT_YUV420P);
                videoEncoder.setProperty("preset", "fast");
                videoEncoder.setProperty("crf", settings.getCrf());
                break;
            case DNXHD:
                videoEncoder.setPixelFormat(PixelFormat.Type.PIX_FMT_YUVJ422P);
                videoEncoder.setProperty("b", "185M");
                break;
            case HUFFYUV:
                videoEncoder.setPixelFormat(PixelFormat.Type.PIX_FMT_YUVJ422P);
                break;
            case GIF:
                break;
            case WAV:
                break;
            case RAW:
                break;
        }

        //TODO: Setup Audio

        if(muxer.getFormat().getFlag(MuxerFormat.Flag.GLOBAL_HEADER)){
            videoEncoder.setFlag(Encoder.Flag.FLAG_GLOBAL_HEADER, true);
        }

        videoEncoder.open(null, null);
        muxer.addNewStream(videoEncoder);
        muxer.open(null, null);

        videoResampler = MediaPictureResampler.make(videoEncoder.getWidth(), videoEncoder.getHeight(), videoEncoder.getPixelFormat(),
                videoEncoder.getWidth(), videoEncoder.getHeight(), PixelFormat.Type.PIX_FMT_BGR24, 0);
        videoResampler.open();

        videoPacket = MediaPacket.make();

        rawFrame = MediaPicture.make(videoEncoder.getWidth(), videoEncoder.getHeight(), PixelFormat.Type.PIX_FMT_BGR24);
        resampledFrame = MediaPicture.make(videoEncoder.getWidth(), videoEncoder.getHeight(), videoEncoder.getPixelFormat());

        Buffer rawBuffer = rawFrame.getData(0);
        int size = rawFrame.getDataPlaneSize(0);
        rawFrameBuffer = rawBuffer.getByteBuffer(0, size);
        rawBuffer.delete();
    }

    public void finish() {
        logger.debug("Waiting for video stream to finish...");
        //TODO: Add audio support
        do {
            videoEncoder.encode(videoPacket, null);
            if(videoPacket.isComplete()){
                muxer.write(videoPacket, true);
            }
        } while (videoPacket.isComplete());

        logger.debug("All streams closed!");
        muxer.close();
    }

    @Override
    protected void consumeBuffer(ByteBuffer buffer) {
        System.out.println("Encoding frame: " + frameIndex);

        buffer.position(0);
        rawFrameBuffer.position(0);
        rawFrameBuffer.put(buffer);

        rawFrame.setTimeStamp(frameIndex++);
        rawFrame.setComplete(true);

        videoResampler.resample(resampledFrame, rawFrame);

        do {
            videoEncoder.encode(videoPacket, resampledFrame);
            if(videoPacket.isComplete()){
                muxer.write(videoPacket, true);
            }
        } while (videoPacket.isComplete());
    }

    @Override
    protected void flush() {

    }
}
