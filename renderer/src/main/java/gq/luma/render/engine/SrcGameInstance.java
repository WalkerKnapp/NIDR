package gq.luma.render.engine;

import gq.luma.render.engine.filesystem.FSAudioHandler;
import gq.luma.render.engine.filesystem.FSVideoHandler;
import gq.luma.render.engine.filesystem.SrcGameWatcher;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class SrcGameInstance {
    private SrcGame game;
    private SrcGameConfiguration configuration;
    private BiConsumer<Throwable, Boolean> errorHandler;

    private SrcGameWatcher watcher;

    private ProcessHandle gameProcessHandle;
    private AtomicBoolean instanceClosed = new AtomicBoolean(false);

    public SrcGameInstance(SrcGame game, SrcGameConfiguration configuration, BiConsumer<Throwable, Boolean> errorHandler){
        this.game = game;
        this.configuration = configuration;
        this.errorHandler = errorHandler;
    }

    public void start(FSVideoHandler videoHandler, FSAudioHandler audioHandler){
        if(instanceClosed.get()){
            throw new IllegalStateException("SrcGameInstance cannot be reused. Please make a new one.");
        }

        try {
            watcher = new SrcGameWatcher(configuration, videoHandler, audioHandler);
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
                                instanceClosed.set(true);
                                errorHandler.accept(new IllegalStateException("Game has closed at an unexpected time. Was there a crash?"), true);
                                watcher.close();
                            }
                        });
                    }, () -> errorHandler.accept(new IllegalStateException("Failed to find game executable when launched. Is the configuration correct?"), false));



        } catch (IOException | URISyntaxException e) {
            errorHandler.accept(new IllegalStateException("Error encountered while trying to launch the source engine. Is the configuration correct?", e), false);
        }
    }

    public CompletableFuture<Void> sendCommand(String... command) throws IOException {
        if(!instanceClosed.get()){
            String[] args = new String[3 + command.length];
            args[0] = configuration.getExecutablePath();
            args[1] = "-hijack";
            args[2] = "-console";
            for(int i = 3; i < args.length; i++){
                args[i] = "+" + command[i - 3];
            }

            ProcessBuilder pb = new ProcessBuilder(args);
            return pb.start().onExit().thenApply(p -> null);
        }
        throw new IllegalStateException("Cannot send command to closed instance.");
    }

    public void close(){
        if(!instanceClosed.get()){
            instanceClosed.set(true);
            gameProcessHandle.destroy();
        }

        watcher.close();
    }

    private void addConfigEcho() throws IOException {
        Path configFile = configuration.getConfigPath().resolve("autoexec.cfg");
        Path logBackup = configuration.getConfigPath().resolve("autoexec.cfg_nidr_backup");
        if(Files.exists(configFile)) {
            List<String> preExistingConfig = Files.readAllLines(configFile);
            if(preExistingConfig.stream().anyMatch(str -> str.contains("echo NIDR.Ready"))){
                return;
            }

            Files.deleteIfExists(logBackup);
            Files.write(logBackup, preExistingConfig);
        } else {
            Files.createFile(configFile);
        }
        Files.write(configFile, Collections.singleton("echo NIDR.Ready"), StandardOpenOption.APPEND);
    }

    public SrcGame getGame() {
        return game;
    }

    public SrcGameWatcher getWatcher() {
        return watcher;
    }
}
