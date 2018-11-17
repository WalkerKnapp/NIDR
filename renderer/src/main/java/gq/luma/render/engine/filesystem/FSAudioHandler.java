package gq.luma.render.engine.filesystem;

import jnr.ffi.Pointer;

public interface FSAudioHandler {
    void handleAudioData(Pointer buf, long offset, long size);
}
