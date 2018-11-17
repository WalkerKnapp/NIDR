package gq.luma.render.engine.filesystem;

import gq.luma.render.engine.SrcGameConfiguration;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static jnr.ffi.Platform.OS.WINDOWS;

public class SrcGameWatcher extends FuseStubFS {

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

    public SrcGameWatcher(SrcGameConfiguration config) throws IOException {
        // Creates the sub-directory "nidr" if it doesn't already exist
        Path nidrPath = config.getGamePath().resolve("nidr");
        Files.createDirectories(nidrPath);

        // Symbolic link "console.log" to the "nidr" directory
        Files.deleteIfExists(config.getLogPath());
        Files.createSymbolicLink(config.getLogPath(), nidrPath.resolve("console.log"));

        activeMonitors = new CopyOnWriteArrayList<>();
    }

    public CompletableFuture<String> watch(String... contains){
        CompletableFuture<String> cf = new CompletableFuture<>();
        activeMonitors.add(new LogMonitor(cf, contains));
        return cf;
    }

    @Override
    public int statfs(String path, Statvfs stbuf) {
        if (Platform.getNativePlatform().getOS() == WINDOWS && "/".equals(path)) {
            stbuf.f_blocks.set(1024 * 1024); // total data blocks in file system
            stbuf.f_frsize.set(1024);        // fs block size
            stbuf.f_bfree.set(1024 * 1024);  // free blocks in fs
        }
        return 0;
    }

    @Override
    public int open(String path, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int create(String path, @mode_t long mode, FuseFileInfo fi) {
        //System.out.println("Create call to " + path + ": " + fi.toString());

        final int pathLength = path.length();
        char lastChar = path.charAt(pathLength - 1);

        switch (lastChar){
            case '/':
                return -ErrorCodes.EEXIST(); // Path "/" already exists
            case 'g':
            case 'G':
                fi.fh.set(Integer.MIN_VALUE);
                return -ErrorCodes.EEXIST(); // Path "something.log" already exists
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
                //TODO: Handle TGAs
                return 0;
            case 'w':
            case 'W':
                // File path is "something.wav"
                //TODO: Check that the latest frame exists
                stat.st_mode.set(FileStat.S_IFREG | 0777);
                return 0;
            default:
                return -ErrorCodes.ENOENT();
        }
    }

    @Override
    public int write(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi){
        int fileHandle = (int) fi.fh.get();
        switch (fileHandle){
            case Integer.MAX_VALUE: // Audio Data
                audioHandler.handleAudioData(buf, offset, size);
                break;
            case Integer.MIN_VALUE: // Log Data
                byte[] lineData = new byte[(int) size];
                buf.get(0, lineData, 0, (int) size);
                handleLogEntry(new String(lineData));
                break;
            default: // Video Data
                videoHandler.handleVideoData(fileHandle, buf, offset, size);

        }
        return (int) size;
    }

    private void handleLogEntry(String entry){
        for(LogMonitor monitor : activeMonitors){
            for(String match : monitor.matches){
                if(entry.contains(match)){
                    monitor.future.complete(match);
                    activeMonitors.remove(monitor);
                    break;
                }
            }
        }
    }
}