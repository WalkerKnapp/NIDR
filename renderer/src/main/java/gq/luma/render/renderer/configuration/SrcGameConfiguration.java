package gq.luma.render.renderer.configuration;

import gq.luma.render.engine.SrcGame;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Optional;

public abstract class SrcGameConfiguration {
    private SrcGame game;

    private String gameDir;
    private String configDir;
    private String logPath;
    private String executablePath;
    private String executableName;

    public SrcGameConfiguration(SrcGame game, String gameDir, String configDir, String logPath, String executablePath, String executableName){
        this.game = game;
        this.gameDir = gameDir;
        this.configDir = configDir;
        this.logPath = logPath;
        this.executableName = executableName;
        this.executablePath = executablePath;
    }

    public Path getGamePath(){
        return Paths.get(gameDir);
    }

    public Path getConfigPath(){
        return Paths.get(configDir);
    }

    public Path getLogPath() {
        return Paths.get(logPath);
    }

    public String getExecutablePath() {
        return executablePath;
    }

    public String getExecutableName() {
        return executableName;
    }

    public abstract Optional<ProcessHandle> launchGame() throws URISyntaxException, IOException;

    public SrcGame getGame() {
        return game;
    }
}
