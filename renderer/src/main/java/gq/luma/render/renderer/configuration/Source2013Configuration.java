package gq.luma.render.renderer.configuration;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Optional;

public class Source2013Configuration extends SrcGameConfiguration {
    public Source2013Configuration(String directoryName, int appCode, String mainDir) {
        super(directoryName, appCode,
                Paths.get(mainDir).resolve(directoryName).toString(),
                Paths.get(mainDir).resolve(directoryName).resolve("cfg").toString(),
                Paths.get(mainDir).resolve(directoryName).resolve("console.log").toString(),
                Paths.get(mainDir).resolve("portal2.exe").toString(), "portal2.exe");
    }

    @Override
    public Optional<ProcessHandle> launchGame() throws URISyntaxException, IOException {
        Desktop.getDesktop().browse(new URI("steam://rungameid/" + getAppCode()
                //+ "//-condebug"
        ));
        return Optional.empty();
    }
}
