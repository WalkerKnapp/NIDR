package gq.luma.render.engine.filesystem;

import gq.luma.render.renderer.configuration.SrcGameConfiguration;
import io.wkna.sdp.DemoWriterChannel;
import jnr.ffi.Platform;
import jnr.ffi.Pointer;
import jnr.ffi.types.mode_t;
import jnr.ffi.types.off_t;
import jnr.ffi.types.size_t;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.FuseStubFS;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.FuseFileInfo;
import ru.serce.jnrfuse.struct.Statvfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static jnr.ffi.Platform.OS.WINDOWS;

public class SrcGameWatcher extends FuseStubFS {

    private DemoWriterChannel demoWriter;
    private SeekableByteChannel backupChannel = FileChannel.open(Paths.get("H:\\Portal 2\\Rendering\\tas\\coop\\rerender\\mp_coop_lobby_3_5.dem"));

    private FSAudioHandler audioHandler;
    private FSVideoHandler videoHandler;

    private CopyOnWriteArrayList<LogMonitor> activeMonitors;

    private class LogMonitor {
        private CompletableFuture<String> future;
        private String[] matches;

        private LogMonitor(CompletableFuture<String> cf, String[] matches){
            this.future = cf;
            this.matches = matches;
        }
    }

    public SrcGameWatcher(SrcGameConfiguration config, FSVideoHandler videoHandler, FSAudioHandler audioHandler) throws IOException {
        // Creates the sub-directory "nidr" if it doesn't already exist
        Path nidrPath = config.getGamePath().resolve("nidr");
        Files.deleteIfExists(nidrPath);

        // Symbolic link "console.log" to the "nidr" directory
        Files.deleteIfExists(config.getLogPath());

        activeMonitors = new CopyOnWriteArrayList<>();
        this.audioHandler = audioHandler;
        this.videoHandler = videoHandler;

        Path hardLinkPath = Paths.get("V:/");

        // Start FUSE filesystem
        this.mount(hardLinkPath, false, false, new String[]{"-o", "big_writes", "-o", "direct_io", "-o", "max_write=104857600"});

        Files.createSymbolicLink(config.getLogPath(), nidrPath.resolve("console.log"));

        Files.createSymbolicLink(nidrPath, hardLinkPath);
    }

    public SrcGameWatcher(String consoleLogDirPath, String startmoviePath, FSVideoHandler videoHandler, FSAudioHandler audioHandler) throws IOException {
        Path nidrPath = Paths.get(startmoviePath).resolve("nidr");
        Path consoleLogPath = Paths.get(consoleLogDirPath).resolve("console.log");
        Files.deleteIfExists(nidrPath);

        Files.deleteIfExists(consoleLogPath);

        activeMonitors = new CopyOnWriteArrayList<>();
        this.audioHandler = audioHandler;
        this.videoHandler = videoHandler;

        Path hardLinkPath = Paths.get("V:/");

        // Start FUSE filesystem
        this.mount(hardLinkPath, false, false, new String[]{"-o", "big_writes", "-o", "direct_io", "-o", "max_write=104857600"});

        Files.createSymbolicLink(consoleLogPath, nidrPath.resolve("console.log"));

        Files.createSymbolicLink(nidrPath, hardLinkPath);
    }

    public void provideDemo(DemoWriterChannel demoChannel){
        if(this.demoWriter != null) demoWriter.close();
        this.demoWriter = demoChannel;
    }

    public CompletableFuture<String> watch(String... contains){
        CompletableFuture<String> cf = new CompletableFuture<>();
        activeMonitors.add(new LogMonitor(cf, contains));
        return cf;
    }

    public void close(){
        this.umount();
        if(this.demoWriter != null) demoWriter.close();
        activeMonitors.forEach(logMonitor -> logMonitor.future.completeExceptionally(new IllegalStateException("The game has been closed.")));
    }

    @Override
    public int statfs(String path, Statvfs stbuf) {
        if (Platform.getNativePlatform().getOS() == WINDOWS && "/".equals(path)) {
            stbuf.f_blocks.set(1024 * 1024); // total data blocks in file system
            stbuf.f_frsize.set(1024 * 10);        // fs block size
            stbuf.f_bfree.set(1024 * 1024);  // free blocks in fs
        }
        return 0;
    }

    @Override
    public int open(String path, FuseFileInfo fi) {
        //System.out.println("Open call to: " + path);

        final int pathLength = path.length();
        char lastChar = path.charAt(pathLength - 1);
        if(lastChar == 'g'){
            fi.fh.set(Integer.MIN_VALUE);
        }


        return 0;
    }

    private String latestCreated = "";

    @Override
    public int create(String path, @mode_t long mode, FuseFileInfo fi) {
        //System.out.println("Create call to " + path + ": " + fi.toString());
        latestCreated = path;

        //fi.flags.set(fi.flags.get() | OpenFlags.O_ASYNC.intValue());

        final int pathLength = path.length();
        char lastChar = path.charAt(pathLength - 1);

        switch (lastChar){
            case '/':
                return -ErrorCodes.EEXIST(); // Path "/" already exists
            case 'g':
            case 'G':
                fi.fh.set(Integer.MIN_VALUE);
                return 0;
                //return -ErrorCodes.EEXIST(); // Path "something.log" already exists
            case 'v':
            case 'V':
                fi.fh.set(Integer.MAX_VALUE);
                return 0; // Path "something.wav"
            case 'a':
            case 'A':

                // Find the frame index from the path name
                int total = 0;
                int j = 1;
                for(int i = pathLength - 5; i >= 0; i--){
                    if(path.charAt(i) == '_')
                        break;
                    total += (path.charAt(i) - '0') * j;
                    j *= 10;
                }
                fi.fh.set(total);
                return 0; // Path "something.tga"
            default:
                return 0;
        }
    }

    @Override
    public int getattr(String path, FileStat stat) {
        //System.out.println("Getattr call to " + path);

        final int pathLength = path.length();
        char lastChar = path.charAt(pathLength - 1);

        switch (lastChar){
            case '/':
                stat.st_mode.set(FileStat.S_IFDIR | 0777);
                return 0;
            case 'g':
                // File path is "something.log"
                stat.st_mode.set(FileStat.S_IFREG | 0777);
                stat.st_uid.set(getContext().uid.get());
                stat.st_gid.set(getContext().gid.get());
                stat.st_size.set(0);
                return 0;
            case 'a':
            case 'A':
                // File path is "something.tga"
                if(path.equals(latestCreated)) {
                    return 0;
                } else {
                    return -ErrorCodes.ENOENT();
                }
            case 'v':
            case 'V':
                // File path is "something.wav"
                //System.out.println("GETATTR CALL TO WAV FILE");
                if(!latestCreated.isEmpty()) {
                    //System.out.println("RETURNING AS EXISTS");
                    stat.st_mode.set(FileStat.S_IFREG | 0777);
                    stat.st_uid.set(getContext().uid.get());
                    stat.st_gid.set(getContext().pid.get());
                    stat.st_blksize.set(4096);
                    //stat.st_size.set(0);
                    return 0;
                } else {
                    //System.out.println("RETURNING NOT AS EXISTS");
                    return -ErrorCodes.ENOENT();
                }
            case 'm':
                if(path.charAt(pathLength - 2) == 'e') {
                    stat.st_mode.set(FileStat.S_IFREG | 0777);
                    stat.st_uid.set(getContext().uid.get());
                    stat.st_gid.set(getContext().gid.get());
                    System.out.println("Demo size: " + demoWriter.size());
                    stat.st_size.set(demoWriter.size());
                    return 0;
                } else {
                    return -ErrorCodes.ENOENT();
                }
            default:
                return -ErrorCodes.ENOENT();
        }
    }

    @Override
    public int read(String path, Pointer buf, long size, long offset, FuseFileInfo fi) {
        //System.out.println("Read to path: " + path + " size: " + size + " offset: " + offset);
        final int pathLength = path.length();
        char lastChar = path.charAt(pathLength - 1);

        if(lastChar == 'm'){
            //System.out.println("Reading demo, " + size + ", offset=" + offset);
            try {
                byte[] raw = new byte[(int) size];
                ByteBuffer buffer = ByteBuffer.wrap(raw);
                demoWriter.position(offset);
                int read = demoWriter.read(buffer);
                if(read == -1) {
                    return -ErrorCodes.EIO();
                }
                if(size != read) System.err.println("Read asked for " + size + " but actually returned " + read);

                /*byte[] backupBytes = new byte[(int) size];
                ByteBuffer backupBuffer = ByteBuffer.wrap(backupBytes);
                backupChannel.position(offset);
                int backupRead = backupChannel.read(backupBuffer);

                if(read != backupRead) {
                    System.err.println("Backupread =/= read, " + backupRead + "=/=" + read);
                }
                boolean ident = true;
                for(int i = 0; i < size; i++) {
                    if(buffer.get(i) != backupBuffer.get(i)) {
                        ident = false;
                    }
                }
                if(!ident) {
                    System.err.println("ReadBuffer =/= BackupBuffer");
                    System.err.println(bytesToHex(raw));
                    System.err.println(bytesToHex(backupBytes));
                }*/

                buf.put(0, raw, 0, read);
                return read;
            } catch (Exception e){
                e.printStackTrace();
                //throw new IllegalStateException(e);
                return 0;
            }
        }
        return (int) size;
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public int write(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi){
        //System.out.println("Write to path: " + path + " size: " + size + " offset: " + offset);
        int fileHandle = (int) fi.fh.get();
        switch (fileHandle){
            case Integer.MAX_VALUE: // Audio Data
                audioHandler.handleAudioData(buf, offset, size);
                //System.out.println("=========================Handled audio write: offset=" + offset + ",size=" + size);
                break;
            case Integer.MIN_VALUE: // Log Data
                byte[] lineData = new byte[(int) size];
                buf.get(0, lineData, 0, (int) size);
                handleLogEntry(new String(lineData));
                break;
            default: // Video Data
                if(path.endsWith("wav")) {
                    //System.err.println("wav fell through.");
                    audioHandler.handleAudioData(buf, offset, size);
                    //System.out.println("=========================Handled audio write: offset=" + offset + ",size=" + size);
                    break;
                } else if (path.endsWith("tga")) {
                    videoHandler.handleVideoData(fileHandle, buf, offset, size);
                } else {
                    System.err.println("Bad write to path: " + path);
                }

        }
        return (int) size;
    }

    private void handleLogEntry(String entry){
        //System.out.println("Handling log entry: " + entry);
        for(LogMonitor monitor : activeMonitors){
            for(String match : monitor.matches){
                if(entry.contains(match) && !entry.contains("coop_bluebot_load")){
                    monitor.future.complete(match);
                    activeMonitors.remove(monitor);
                    break;
                }
            }
        }
    }
}
