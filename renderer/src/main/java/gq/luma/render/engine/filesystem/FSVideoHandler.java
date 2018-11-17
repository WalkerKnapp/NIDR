package gq.luma.render.engine.filesystem;

import jnr.ffi.Pointer;

public interface FSVideoHandler {
    void handleVideoData(int index, Pointer buf, long offset, long writeLength);
}
