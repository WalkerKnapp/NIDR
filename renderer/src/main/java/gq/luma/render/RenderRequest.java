package gq.luma.render;

import io.wkna.sdp.SourceDemo;

import java.nio.file.Path;
import java.util.List;

public class RenderRequest {
    private List<SourceDemo> demos;
    private RenderSettings settings;
    private Path outputPath;
    private String status;

    public RenderRequest(List<SourceDemo> demos, RenderSettings settings){
        this.demos = demos;
        this.settings = settings;
    }

    public RenderRequest(List<SourceDemo> demos, RenderSettings settings, Path outputPath){
        this.demos = demos;
        this.settings = settings;
        this.outputPath = outputPath;
    }

    public List<SourceDemo> getDemos() {
        return demos;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public RenderSettings getSettings() {
        return settings;
    }

    public Path getOutputPath() {
        return outputPath;
    }
}
