package gq.luma.render.engine;

import gq.luma.render.engine.filesystem.SrcGameWatcher;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SrcGameInstance {
    private SrcGame game;
    private SrcGameConfiguration configuration;
    private BiConsumer<Throwable, Boolean> errorHandler;

    private SrcGameWatcher watcher;

    private ProcessHandle gameProcessHandle;
    private AtomicBoolean instanceClosed = new AtomicBoolean(false);

    public SrcGameInstance(){
        try {
            watcher = new SrcGameWatcher(configuration);
            addConfigEcho();

            Desktop.getDesktop().browse(new URI("steam://rungameid/" + game.getAppCode()));

            watcher.watch("NIDR.Ready").join();

            ProcessHandle.allProcesses()
                    .filter(ph -> ph.info().command()
                            .map(command -> command.contains(configuration.getExecutableName()))
                            .orElse(false))
                    .findAny()
                    .ifPresentOrElse(handle -> {
                        gameProcessHandle = handle;
                        handle.onExit().thenRun(() -> {
                            if(!instanceClosed.get()) {
                                errorHandler.accept(new IllegalStateException("Game has closed at an unexpected time. Was there a crash?"), true);
                            }
                        });
                    }, () -> errorHandler.accept(new IllegalStateException("Failed to find game executable when launched. Is the configuration correct?"), false));



        } catch (IOException | URISyntaxException e) {
            errorHandler.accept(new IllegalStateException("Error encountered while trying to launch the source engine. Is the configuration correct?", e), false);
        }
    }

    private void addConfigEcho() throws IOException {
        Path configFile = configuration.getLogPath().resolve("config.cfg");
        Path logBackup = configuration.getLogPath().resolve("config.cfg_nidr_backup");
        if(Files.exists(configFile)) {
            List<String> preExistingConfig = Files.readAllLines(configFile);
            if(preExistingConfig.stream().anyMatch(str -> str.contains("echo NIDR.Ready"))){
                return;
            }

            Files.deleteIfExists(logBackup);
            Files.write(logBackup, preExistingConfig);
        }
        Files.write(configFile, Collections.singleton("echo NIDR.Ready"), StandardOpenOption.APPEND);
    }
}
