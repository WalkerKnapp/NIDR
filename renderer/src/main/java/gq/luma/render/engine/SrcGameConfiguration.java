package gq.luma.render.engine;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class SrcGameConfiguration {
    private SrcGame game;

    private String gameDir;
    private String configDir;
    private String logPath;
    private String executablePath;
    private String executableName;

    private HashMap<String, String> attributes;

    public Path getGamePath(){
        return Paths.get(gameDir);
    }

    public File getConfigDir(){
        return new File(configDir);
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

    private String getGameAttribute(String key){
        return attributes.get(key);
    }
}
