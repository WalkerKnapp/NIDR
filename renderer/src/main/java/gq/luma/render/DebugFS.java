package gq.luma.render;

import com.kenai.jffi.MemoryIO;
import jnr.ffi.Platform;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.types.*;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.FuseStubFS;
import ru.serce.jnrfuse.NotImplemented;
import ru.serce.jnrfuse.flags.FuseBufFlags;
import ru.serce.jnrfuse.struct.*;

import java.nio.file.Paths;
import java.util.Scanner;

import static jnr.ffi.Platform.OS.WINDOWS;

public class DebugFS extends FuseStubFS {

    /*@Override
    public int readlink(String path, Pointer buf, @size_t long size) {
        return 0;
    }

    @Override
    public int mkdir(String path, @mode_t long mode) {
        return 0;
    }

    @Override
    public int unlink(String path) {
        return 0;
    }

    @Override
    public int rmdir(String path) {
        return 0;
    }

    @Override
    public int symlink(String oldpath, String newpath) {
        return 0;
    }

    @Override
    public int rename(String oldpath, String newpath) {
        return 0;
    }

    @Override
    public int link(String oldpath, String newpath) {
        return 0;
    }

    @Override
    public int chmod(String path, @mode_t long mode) {
        return 0;
    }

    @Override
    public int chown(String path, @uid_t long uid, @gid_t long gid) {
        return 0;
    }

    @Override
    public int truncate(String path, @off_t long size) {
        return 0;
    }

    @Override
    public int read(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
        return 0;
    }*/

    @Override
    public int statfs(String path, Statvfs stbuf) {
        if (Platform.getNativePlatform().getOS() == WINDOWS) {
            if ("/".equals(path)) {
                stbuf.f_blocks.set(1024 * 1024); // total data blocks in file system
                stbuf.f_frsize.set(1024);        // fs block size
                stbuf.f_bfree.set(1024 * 1024);  // free blocks in fs
            }
        }
        return 0;
    }

    /*@Override
    public int flush(String path, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int release(String path, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int fsync(String path, int isdatasync, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int setxattr(String path, String name, Pointer value, @size_t long size, int flags) {
        return 0;
    }

    @Override
    public int listxattr(String path, Pointer list, @size_t long size) {
        return 0;
    }

    @Override
    public int removexattr(String path, String name) {
        return 0;
    }*/

    /*@Override
    public int opendir(String path, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int readdir(String path, Pointer buf, FuseFillDir filter, @off_t long offset, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int releasedir(String path, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int fsyncdir(String path, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public Pointer init(Pointer conn) {
        return null;
    }

    @Override
    public void destroy(Pointer initResult) {
    }

    @Override
    public int ftruncate(String path, @off_t long size, FuseFileInfo fi) {
        return truncate(path, size);
    }

    @Override
    public int lock(String path, FuseFileInfo fi, int cmd, Flock flock) {
        return -ErrorCodes.ENOSYS();
    }*/

    /*@Override
    public int utimens(String path, Timespec[] timespec) {
        return -ErrorCodes.ENOSYS();
    }

    @Override
    public int bmap(String path, @size_t long blocksize, long idx) {
        return 0;
    }

    @Override
    public int ioctl(String path, int cmd, Pointer arg, FuseFileInfo fi, @u_int32_t long flags, Pointer data) {
        return -ErrorCodes.ENOSYS();
    }

    @Override
    public int poll(String path, FuseFileInfo fi, FusePollhandle ph, Pointer reventsp) {
        return -ErrorCodes.ENOSYS();
    }

    @Override
    public int flock(String path, FuseFileInfo fi, int op) {
        return -ErrorCodes.ENOSYS();
    }

    @Override
    public int fallocate(String path, int mode, @off_t long off, @off_t long length, FuseFileInfo fi) {
        return -ErrorCodes.ENOSYS();
    }*/

    @Override
    public int open(String path, FuseFileInfo fi) {
        //System.out.println("Open call to " + path + ": " + fi.toString());
        return 0;
    }

    @Override
    public int create(String path, @mode_t long mode, FuseFileInfo fi) {
        //System.out.println("Create call to " + path + ": " + fi.toString());
        if(path.equals("/console.log")){
            return -ErrorCodes.EEXIST();
        } else if(path.equals("/")){
            return -ErrorCodes.EEXIST();
        }
        return 0;
    }

   /* @Override
    public int fgetattr(String path, FileStat stbuf, FuseFileInfo fi) {
        return super.fgetattr(path, stbuf, fi);
    }*/

    @Override
    public int write_buf(String path, FuseBufvec buf, long off, FuseFileInfo fi) {
        //System.out.println("WriteBuf to path: " + path);
        return super.write_buf(path, buf, off, fi);
    }

    @Override
    public int getattr(String path, FileStat stat) {
        char[] pathChars = path.toCharArray();
        int length = pathChars.length;
        if(path.equals("/")){
            stat.st_mode.set(FileStat.S_IFDIR | 0777);
            //stat.st_uid.set(uid);
            //stat.st_gid.set(pid);
            return 0;
        } else if(path.equals("/console.log")) {
            stat.st_mode.set(FileStat.S_IFREG | 0777);
            stat.st_uid.set(getContext().uid.get());
            stat.st_gid.set(getContext().gid.get());
            stat.st_size.set(0);
            //System.out.println("Getattr: " + path + " exists! Returning 0 due to game log");
            //return -ErrorCodes.EEXIST();
            return 0;
        } else if(path.equals("")){
            //TODO: Set to latestFrameCreated
            //FileStat.S_IFREG | 0777
            stat.st_mode.set(FileStat.S_IXUGO);
            //stat.st_size.set(0);
            //stat.st_uid.set(uid);
            //stat.st_gid.set(pid);
            //System.out.println("Getattr: " + path + " exists! Returning 0 due to equalling last created");
            return 0;
        } else if(!"".isEmpty() && (length > 2 && (pathChars[length - 1] == 'V' || pathChars[length - 1] == 'v') &&
                (pathChars[length - 2] == 'A' || pathChars[length - 2] == 'a') &&
                (pathChars[length - 3] == 'W' || pathChars[length - 3] == 'w'))){
            //TODO: Set to latestFrameCreated
            stat.st_mode.set(FileStat.S_IXUGO);
            //`stat.st_size.set(0);
            //stat.st_uid.set(uid);
            //stat.st_gid.set(pid);
            //stat.st_blksize.set(4096);
            //System.out.println("Getattr: " + path + " exists! Returning 0 due to lastcreated being present and audio");
            return 0;
        } else  {
            //System.out.println("Getattr: " + path + " does not exist! Returning " + -ErrorCodes.ENOENT());
            return -ErrorCodes.ENOENT();
        }
    }

    /*@Override
    public int mknod(String path, long mode, long rdev) {
        return super.mknod(path, mode, rdev);
    }*/

    @Override
    public int write(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi){
        /*if(offset == 0){
            byte[] header = new byte[44];
            buf.get(0, header, 0, 44);
            System.out.println("Header=" + new String(Hex.encodeHex(header)));
        }*/
        System.out.println("Write=" + path + ",Size=" + size + ",fh=" + fi.fh.get() + ",flags=" + fi.flags.get());
        byte[] bufA = new byte[(int) size];
        buf.get(0, bufA, 0, (int) size);
        System.out.println(new String(bufA));
        return (int) size;
    }

    /*@Override
    public int getxattr(String path, String name, Pointer value, long size) {
        return super.getxattr(path, name, value, size);
    }*/

    /*@Override
    public int access(String path, int mask) {
        return super.access(path, mask);
    }*/

    public static void main(String[] args){
        DebugFS debugFS = new DebugFS();
        // new String[]{"-o", "splice_read,splice_write,splice_move"}
        debugFS.mount(Paths.get("mountPoint"), false, false);

        DebugFS debugFS2 = new DebugFS();
        debugFS2.mount(Paths.get("mountPoint2"), false, false);

        new Scanner(System.in).nextLine();

        debugFS.umount();

        new Scanner(System.in).nextLine();

        DebugFS debugFS3 = new DebugFS();
        debugFS3.mount(Paths.get("mountPoint3"), false, false);

        new Scanner(System.in).nextLine();

        debugFS2.umount();
        debugFS3.umount();

        debugFS.umount();
    }
}
