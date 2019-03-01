package gq.luma.render.renderer.configuration;

import gq.luma.render.engine.SrcGame;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Optional;

public class Source2013Configuration extends SrcGameConfiguration {
    public Source2013Configuration(SrcGame game, String mainDir) {
        super(game, Paths.get(mainDir).resolve(game.getDirectoryName()).toString(),
                Paths.get(mainDir).resolve(game.getDirectoryName()).resolve("cfg").toString(),
                Paths.get(mainDir).resolve(game.getDirectoryName()).resolve("console.log").toString(),
                Paths.get(mainDir).resolve("portal2.exe").toString(), "portal2.exe");
    }

    @Override
    public Optional<ProcessHandle> launchGame() throws URISyntaxException, IOException {
        Desktop.getDesktop().browse(new URI("steam://rungameid/" + getGame().getAppCode()));
        return Optional.empty();
    }
}
