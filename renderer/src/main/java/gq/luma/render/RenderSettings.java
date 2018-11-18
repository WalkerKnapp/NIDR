package gq.luma.render;

import java.util.List;

public class RenderSettings {

    private int width;
    private int height;
    private int fps;
    private int frameblend;

    private OutputFormat outputFormat;
    private boolean twoPass;
    private int crf;

    private boolean demoInterpolate;
    private boolean startOddSpecified;
    private boolean forceStartOdd;
    private boolean removeBrokenFrames;

    private List<String> additionalCommands;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public int getFrameblend() {
        return frameblend;
    }

    public void setFrameblend(int frameblend) {
        this.frameblend = frameblend;
    }

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
    }

    public boolean isTwoPass() {
        return twoPass;
    }

    public void setTwoPass(boolean twoPass) {
        this.twoPass = twoPass;
    }

    public int getCrf() {
        return crf;
    }

    public void setCrf(int crf) {
        this.crf = crf;
    }

    public boolean isDemoInterpolate() {
        return demoInterpolate;
    }

    public void setDemoInterpolate(boolean demoInterpolate) {
        this.demoInterpolate = demoInterpolate;
    }

    public boolean isRemoveBrokenFrames() {
        return removeBrokenFrames;
    }

    public void setRemoveBrokenFrames(boolean removeBrokenFrames) {
        this.removeBrokenFrames = removeBrokenFrames;
    }

    public List<String> getAdditionalCommands() {
        return additionalCommands;
    }

    public void setAdditionalCommands(List<String> additionalCommands) {
        this.additionalCommands = additionalCommands;
    }

    public boolean isForceStartOdd() {
        return forceStartOdd;
    }

    public void setForceStartOdd(boolean forceStartOdd) {
        this.forceStartOdd = forceStartOdd;
    }

    public boolean isStartOddSpecified() {
        return startOddSpecified;
    }

    public void setStartOddSpecified(boolean startOddSpecified) {
        this.startOddSpecified = startOddSpecified;
    }
}
