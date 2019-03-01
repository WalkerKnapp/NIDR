package gq.luma.render.renderer.configuration;

import gq.luma.render.engine.SrcGame;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class SourceUnpackConfiguration extends SrcGameConfiguration {

    private Path unpackDir;
    private String batchFile;

    public SourceUnpackConfiguration(SrcGame game, String unpackDir, String batchFileName) {
        super(game, Paths.get(unpackDir).resolve(game.getDirectoryName()).toString(),
                Paths.get(unpackDir).resolve(game.getDirectoryName()).resolve("cfg").toString(),
                Paths.get(unpackDir).resolve(game.getDirectoryName()).resolve("console.log").toString(),
                Paths.get(unpackDir).resolve("hl2.exe").toString(),
                "hl2.exe");

        this.unpackDir = Paths.get(unpackDir);
        if(batchFileName == null) {
            batchFile = game == SrcGame.PORTAL ? "Portal.bat" : "Half Life 2.bat";
        } else {
            batchFile = batchFileName;
        }
    }

    @Override
    public Optional<ProcessHandle> launchGame() throws IOException {
        return Optional.of(new ProcessBuilder("cmd.exe", "/C", batchFile).directory(unpackDir.toFile()).start().toHandle());
    }
}
