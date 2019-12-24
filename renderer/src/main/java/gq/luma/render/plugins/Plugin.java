package gq.luma.render.plugins;

import gq.luma.render.renderer.configuration.SrcGameConfiguration;
import io.wkna.sdp.SourceDemo;

public interface Plugin {
    void onDemoLoad(SrcGameConfiguration gameConfig, SourceDemo demo);
}
