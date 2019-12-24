package gq.luma.render.renderer.source;

import com.walker.pipeline.PipelineDatatype;
import com.walker.pipeline.longtype.LongPipelineSource;
import gq.luma.render.RenderRequest;
import sun.misc.Unsafe;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSource extends LongPipelineSource {

    private Path dir;

    private Unsafe unsafe;
    private ByteBuffer frameBuffer;
    private long frameBufferBasePointer;

    private Field bufferAddress;
    private Field bufferCapacity;
    //private long addressBufferOffset;

    int bufferSize;

    public FileSource(RenderRequest renderRequest, Path directory) throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        super(PipelineDatatype.SuperType.RAW_VIDEO, PipelineDatatype.RAW_BGR24);

        this.dir = directory;

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        this.unsafe = (Unsafe) f.get(null);

        bufferAddress = java.nio.Buffer.class.getDeclaredField("address");
        bufferAddress.setAccessible(true);
        bufferCapacity = java.nio.Buffer.class.getDeclaredField("capacity");
        bufferCapacity.setAccessible(true);

        //this.addressBufferOffset = unsafe.objectFieldOffset(bufferAddress);

        bufferSize = (renderRequest.getSettings().getWidth() * renderRequest.getSettings().getHeight() * 3);
        int frameSize = bufferSize + 18;

        this.frameBufferBasePointer = unsafe.allocateMemory(bufferSize);

        //(long addr, int cap)
        Constructor<?> constructor = Class.forName("java.nio.DirectByteBuffer").getDeclaredConstructor(Long.TYPE, Integer.TYPE);
        constructor.setAccessible(true);
        this.frameBuffer = (ByteBuffer) constructor.newInstance(frameBufferBasePointer, bufferSize);

        bufferAddress.setLong(frameBuffer, frameBufferBasePointer);
        bufferCapacity.setInt(frameBuffer, bufferSize);

        //this.frameBufferBasePointer = (long) bufferAddress.get(frameBuffer);
    }

    public void run() throws IOException, IllegalAccessException {
        System.out.println("Running FileSource");
        Path nextImage;
        for(int i = 0; Files.exists(nextImage = dir.resolve("tga_" + composeToString(i) + ".tga")); i++) {
            System.out.println("File exists: " + nextImage.toString());
            handleTGAFile(nextImage);
        }
    }

    public String composeToString(int i) {
        StringBuilder ret = new StringBuilder(String.valueOf(i));
        while(ret.length() < 4) {
            ret.insert(0, '0');
        }
        return ret.toString();
    }

    @Override
    protected void flush() {
        // Source will never need to be flushed
    }

    private void handleTGAFile(Path file) throws IOException, IllegalAccessException {
        FileChannel sbc = FileChannel.open(file);
        frameBuffer.rewind();
        System.out.println(frameBuffer.capacity());
        System.out.println(sbc.read(frameBuffer, 18));
        sbc.close();

        System.out.println("Pushing frame");
        pushBuffer(frameBufferBasePointer);

        this.frameBufferBasePointer = unsafe.allocateMemory(bufferSize);
        bufferAddress.setLong(frameBuffer, frameBufferBasePointer);
    }
}
