package gq.luma.render.renderer.configuration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public abstract class SrcGameConfiguration {
    private String directoryName;
    private int appCode;

    private String gameDir;
    private String configDir;
    private String logPath;
    private String executablePath;
    private String executableName;

    public SrcGameConfiguration(String directoryName, int appCode,
                                String gameDir, String configDir, String logPath, String executablePath, String executableName){
        this.directoryName = directoryName;
        this.appCode = appCode;
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

    public String getDirectoryName() {
        return directoryName;
    }

    public int getAppCode() {
        return appCode;
    }

    public abstract Optional<ProcessHandle> launchGame() throws URISyntaxException, IOException;
}
