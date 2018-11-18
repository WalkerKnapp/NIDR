package gq.luma.render.engine;

import java.nio.file.Path;

public class SrcDemo {
    private String filestamp;
    private int protocol;
    private int networkProtocol;
    private SrcGame game;
    private String mapName;
    private String serverName;
    private String clientName;
    private float playbackTime;
    private int playbackTicks;
    private int playbackFrames;
    private int signOnLength;

    private String signOnString;
    private int firstRealTick;

    private Path demoPath;

    public String getFilestamp() {
        return filestamp;
    }

    public int getProtocol() {
        return protocol;
    }

    public int getNetworkProtocol() {
        return networkProtocol;
    }

    public SrcGame getGame() {
        return game;
    }

    public String getMapName() {
        return mapName;
    }

    public String getServerName() {
        return serverName;
    }

    public String getClientName() {
        return clientName;
    }

    public float getPlaybackTime() {
        return playbackTime;
    }

    public int getPlaybackTicks() {
        return playbackTicks;
    }

    public int getPlaybackFrames() {
        return playbackFrames;
    }

    public int getSignOnLength() {
        return signOnLength;
    }

    public String getSignOnString() {
        return signOnString;
    }

    public int getFirstPlaybackTick() {
        return firstRealTick;
    }

    public Path getDemoPath() {
        return demoPath;
    }
}
