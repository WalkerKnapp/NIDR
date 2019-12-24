package gq.luma.render.renderer;

import com.walker.pipeline.PipelineDatatype;
import com.walker.pipeline.PipelineOutput;
import com.walker.pipeline.longtype.LongPipelineOutput;
import gq.luma.render.OutputFormat;
import gq.luma.render.RenderSettings;
import gq.luma.render.renderer.source.DemoSource;
import io.humble.ferry.Buffer;
import io.humble.video.*;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public class HumbleVideoOutput {
    private static final Logger logger = LogManager.getLogger();

    private Muxer muxer;

    private RenderSettings renderSettings;

    private Encoder videoEncoder;
    private MediaPictureResampler videoResampler;
    private MediaPacket videoPacket;
    private MediaPicture rawFrame;
    private MediaPicture resampledFrame;
    private ByteBuffer rawFrameBuffer;
    private long rawFrameBufferPointer;
    private int frameIndex;

    private Unsafe unsafe;
    private long bufferSize;

    private Encoder audioEncoder;
    private MediaPacket audioPacket;
    private MediaAudio audioFrame;
    private ByteBuffer rawAudioBuffer;
    private long rawAudioBufferPointer;
    private int rawAudioBufferSize;
    private int sampleIndex;

    private HumbleVideoVideoOutupt videoOutupt;
    private HumbleVideoAudioOutput audioOutput;

    public class HumbleVideoVideoOutupt extends LongPipelineOutput {
        protected HumbleVideoVideoOutupt() {
            super(PipelineDatatype.SuperType.RAW_VIDEO, PipelineDatatype.RAW_BGR24);
        }

        @Override
        protected void consumeBuffer(long buffer) {
            HumbleVideoOutput.this.consumeVideoBuffer(buffer);
        }

        @Override
        protected long provideZCBuffer() {
            return -1L;
        }

        @Override
        protected void consumeZC() {

        }

        @Override
        protected void flush() {

        }
    }

    public class HumbleVideoAudioOutput extends LongPipelineOutput {

        protected HumbleVideoAudioOutput() {
            super(PipelineDatatype.SuperType.RAW_VIDEO, PipelineDatatype.RAW_BGR24);
        }

        @Override
        protected void consumeBuffer(long buffer) {
            HumbleVideoOutput.this.consumeAudioBuffer(buffer);
        }

        @Override
        protected long provideZCBuffer() {
            return -1L;
        }

        @Override
        protected void consumeZC() {

        }

        @Override
        protected void flush() {

        }
    }

    public HumbleVideoOutput(RenderSettings settings, Path finalFile) throws IOException, InterruptedException {

        this.videoOutupt = new HumbleVideoVideoOutupt();
        this.audioOutput = new HumbleVideoAudioOutput();

        renderSettings = settings;

        muxer = Muxer.make(finalFile.toAbsolutePath().toString(), null, null);

        Codec codec = Codec.findEncodingCodecByIntID(settings.getOutputFormat().getVideoCodec() + 1);
        /*if(settings.getOutputFormat() == OutputFormat.H264
                //&& settings.getCrf() == 0
            ) {
            //Codec.getInstalledCodecs().forEach(codec1 -> System.out.println(codec1.getName() + " " + codec1.canEncode()));
            codec = Codec.findEncodingCodecByName("libx264rgb");
        }*/
        Codec audioCodec = findBestAudioCodec(codec);
        logger.debug("Video codec: " + codec.getName());
        logger.debug("Audio codec: " + audioCodec.getName());

        videoEncoder = Encoder.make(codec);
        videoEncoder.setWidth(settings.getWidth());
        videoEncoder.setHeight(settings.getHeight());
        videoEncoder.setTimeBase(Rational.make(1, settings.getFps()));

        sampleIndex = (int)(DemoSource.sampleRate * (0.1f + (settings.getSkipFrames()/((float)settings.getFps()))));

        switch (settings.getOutputFormat()){
            case H264:
                videoEncoder.setPixelFormat(PixelFormat.Type.PIX_FMT_YUV420P);
                /*codec.getSupportedVideoPixelFormats().forEach(type -> {
                    try {
                        System.out.println(type.toString());
                    } catch (Throwable throwable) {
                        System.out.println(type.swigValue());
                    }
                });*/
                //videoEncoder.setPixelFormat(PixelFormat.Type.PIX_FMT_BGR24);
                videoEncoder.setProperty("preset", "veryslow");
                videoEncoder.setProperty("crf", settings.getCrf());
                break;
            case DNXHD:
                videoEncoder.setPixelFormat(PixelFormat.Type.PIX_FMT_YUV422P);
                videoEncoder.setProperty("b", "185M");
                break;
            case HUFFYUV:
                videoEncoder.setPixelFormat(PixelFormat.Type.PIX_FMT_RGB24);
                break;
            case GIF:
                break;
            case WAV:
                break;
            case RAW:
                break;
        }

        audioEncoder = Encoder.make(audioCodec);
        audioEncoder.setSampleFormat(DemoSource.audioFormatType);
        audioEncoder.setSampleRate((int) DemoSource.sampleRate);
        audioEncoder.setTimeBase(Rational.make(1, (int) DemoSource.sampleRate));
        audioEncoder.setChannels(DemoSource.channels);
        audioEncoder.setChannelLayout(DemoSource.audioChannelLayout);

        if(muxer.getFormat().getFlag(MuxerFormat.Flag.GLOBAL_HEADER)){
            videoEncoder.setFlag(Encoder.Flag.FLAG_GLOBAL_HEADER, true);
            audioEncoder.setFlag(Encoder.Flag.FLAG_GLOBAL_HEADER, true);
        }

        videoEncoder.open(null, null);
        audioEncoder.open(null, null);
        muxer.addNewStream(videoEncoder);
        muxer.addNewStream(audioEncoder);
        muxer.open(null, null);

        videoResampler = MediaPictureResampler.make(videoEncoder.getWidth(), videoEncoder.getHeight(), videoEncoder.getPixelFormat(),
                videoEncoder.getWidth(), videoEncoder.getHeight(), PixelFormat.Type.PIX_FMT_BGR24, 0);
        videoResampler.open();

        videoPacket = MediaPacket.make();

        rawFrame = MediaPicture.make(videoEncoder.getWidth(), videoEncoder.getHeight(), PixelFormat.Type.PIX_FMT_BGR24);
        resampledFrame = MediaPicture.make(videoEncoder.getWidth(), videoEncoder.getHeight(), videoEncoder.getPixelFormat());

        bufferSize = settings.getWidth() * settings.getHeight() * 3;

        Buffer rawBuffer = rawFrame.getData(0);
        int size = rawFrame.getDataPlaneSize(0);
        rawFrameBuffer = rawBuffer.getByteBuffer(0, size);
        rawBuffer.delete();

        this.audioPacket = MediaPacket.make();
        this.audioFrame = MediaAudio.make(
                DemoSource.samplesPerFrame,
                (int) DemoSource.sampleRate,
                DemoSource.channels,
                DemoSource.audioChannelLayout,
                DemoSource.audioFormatType);
        this.audioFrame.setTimeBase(Rational.make(1, (int) DemoSource.sampleRate));

        Buffer audioBuffer = this.audioFrame.getData(0);
        rawAudioBufferSize = this.audioFrame.getDataPlaneSize(0);
        this.rawAudioBuffer = audioBuffer.getByteBuffer(0, rawAudioBufferSize);
        audioBuffer.delete();

        try {
            // Setup unsafe
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            this.unsafe = (Unsafe) f.get(null);

            Field bAddr = java.nio.Buffer.class.getDeclaredField("address");
            bAddr.setAccessible(true);

            rawFrameBufferPointer = (long) bAddr.get(rawFrameBuffer);
            rawAudioBufferPointer = (long) bAddr.get(rawAudioBuffer);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private Codec findBestAudioCodec(Codec videoCodec) {
        return Codec.findEncodingCodec(Codec.ID.CODEC_ID_AAC);
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

    private void consumeVideoBuffer(long buffer) {

        System.out.println("Encoding frame: " + frameIndex);

        unsafe.copyMemory(buffer, rawFrameBufferPointer, bufferSize);

        //TODO: Make this optional, needs to be removed when frameblending
        if(renderSettings.getFrameblend() <= 1) {
            unsafe.freeMemory(buffer);
        }

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

    private void consumeAudioBuffer(long buffer) {
        System.out.println("Encoding samples: " + sampleIndex);

        unsafe.copyMemory(buffer, rawAudioBufferPointer, rawAudioBufferSize);

        audioFrame.setTimeStamp(sampleIndex);
        audioFrame.setComplete(true);

        do {
            audioEncoder.encodeAudio(audioPacket, audioFrame);
            if (audioPacket.isComplete()) {
                muxer.write(audioPacket, true);
            }
        } while (audioPacket.isComplete());

        sampleIndex += DemoSource.samplesPerFrame;
    }

    /*@Override
    protected long provideZCBuffer() {
        //return Pointer.wrap(Runtime.getSystemRuntime(), rawFrameBuffer);
        return -1L;
    }

    @Override
    protected void consumeZC() {

        System.out.println("Encoding frame: " + frameIndex);


        rawFrame.setTimeStamp(frameIndex++);
        rawFrame.setComplete(true);

        videoResampler.resample(resampledFrame, rawFrame);

        do {
            videoEncoder.encode(videoPacket, resampledFrame);
            if(videoPacket.isComplete()){
                muxer.write(videoPacket, true);
            }
        } while (videoPacket.isComplete());
    }*/

    public HumbleVideoVideoOutupt getVideoOutupt() {
        return videoOutupt;
    }

    public HumbleVideoAudioOutput getAudioOutput() {
        return audioOutput;
    }
}
