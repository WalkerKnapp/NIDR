package gq.luma.render.renderer.configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

public class SourceUnpackConfiguration extends SrcGameConfiguration {

    private Path unpackDir;
    private String batchFile;

    public SourceUnpackConfiguration(String gameDirectoryName, int appCode, String unpackDir, String batchFileName) {
        super(gameDirectoryName, appCode, Paths.get(unpackDir).resolve(gameDirectoryName).toString(),
                Paths.get(unpackDir).resolve(gameDirectoryName).resolve("cfg").toString(),
                Paths.get(unpackDir).resolve(gameDirectoryName).resolve("console.log").toString(),
                Paths.get(unpackDir).resolve("hl2.exe").toString(),
                "hl2.exe");

        this.unpackDir = Paths.get(unpackDir);
        batchFile = Objects.requireNonNullElseGet(batchFileName,
                () -> (gameDirectoryName.equals("portal") ? "Portal.bat" : "Half Life 2.bat"));
    }

    @Override
    public Optional<ProcessHandle> launchGame() throws IOException {
        return Optional.of(new ProcessBuilder("cmd.exe", "/C", batchFile).directory(unpackDir.toFile()).start().toHandle());
    }
}
