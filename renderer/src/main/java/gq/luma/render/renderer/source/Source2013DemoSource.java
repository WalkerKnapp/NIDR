package gq.luma.render.renderer.source;

import gq.luma.render.RenderRequest;
import gq.luma.render.RenderSettings;
import gq.luma.render.engine.DemoUtils;
import gq.luma.render.engine.SrcGameInstance;
import gq.luma.render.plugins.Plugin;
import gq.luma.render.renderer.configuration.Source2013Configuration;
import gq.luma.render.renderer.configuration.SrcGameConfiguration;
import io.wkna.sdp.SourceDemo;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class Source2013DemoSource extends DemoSource {

    private Plugin[] plugins;

    public Source2013DemoSource(Plugin... plugins){
        this.plugins = plugins;

        gameConfigurations.put("portal2",
                new Source2013Configuration("portal2", 620, "B:\\Programs\\Steam\\steamapps\\common\\Portal 2"));
        gameConfigurations.put("portal_stories",
                new Source2013Configuration("portal_stories", 317400, "F:\\SteamLibrary\\steamapps\\common\\Portal Stories Mel"));
    }

    @Override
    public void runSyncRender(RenderRequest renderRequest) throws Exception {
        for (SourceDemo demo : renderRequest.getDemos()) {

            // Start the game if not started
            SrcGameConfiguration configuration = getConfiguration(demo.getGameDirectory());
            SrcGameInstance gameInstance = setupGame(configuration);

            //TODO: Send info packet downstream in the pipeline

            gameInstance.getWatcher().provideDemo(demo.createWriter());

            for(Plugin p : plugins) {
                p.onDemoLoad(configuration, demo);
            }

            // Write start script
            boolean shouldStartOnOddTick = ((renderRequest.getSettings().isStartOddSpecified() && renderRequest.getSettings().isForceStartOdd())
                    || (!renderRequest.getSettings().isStartOddSpecified() && DemoUtils.getFirstPlaybackTick(demo) % 2 == 0))
                    && !demo.getMapName().contains("mp_");
            System.err.println("Starting demo on odd tick?: " + shouldStartOnOddTick);
            writeCfg(configuration, renderRequest.getSettings(), shouldStartOnOddTick);

            if(renderRequest.getSettings().isSplitScreenWorkaround()) {
                //runningGames.get(demo.getGame()).sendCommand("ss_splitmode 0");
                gameInstance.sendCommand("mat_setvideomode " + renderRequest.getSettings().getWidth() + " " + renderRequest.getSettings().getHeight() + " 1");

                Thread.sleep(8000);

                gameInstance.sendCommand("ss_map mp_coop_doors");
                gameInstance.getWatcher().watch("Redownloading all lightmaps").join();
                Thread.sleep(700);
            }

            // Start the render
            gameInstance.sendCommand("exec nidr.run.cfg");
            if(shouldStartOnOddTick){
                if(!demo.getMapName().contains("mp_")) {
                    gameInstance.getWatcher().watch("Redownloading all lightmaps").join();
                    //new Scanner(System.in).nextLine();
                    Thread.sleep(700);

                } else {
                    new Scanner(System.in).nextLine();
                    gameInstance.getWatcher().watch("Demo message, tick 0").join();
                    Thread.sleep(1000);
                }
                //runningGames.get(demo.getGame()).sendCommand("exec cameramove");
                Thread.sleep(3000);
                gameInstance.sendCommand("demo_pauseatservertick 1;demo_resume");
                Thread.sleep(3000);
                gameInstance.sendCommand("sv_alternateticks 1");
                Thread.sleep(3000);
                gameInstance.sendCommand("exec nidr.restart.cfg");
            }
            updateStatus("Rendering");

            // Wait for dem_stop
            long startTime = System.currentTimeMillis();
            CompletableFuture<String> watcherCf = gameInstance.getWatcher().watch("dem_stop", "leaderboard_open", "playvideo_end_level_transition");
            CompletableFuture<String> onEnter = CompletableFuture.supplyAsync(() -> {
                System.out.println("Press enter to end the render early.");
                return new Scanner(System.in).nextLine();
            });
            System.out.println("Ending due to finding: " + CompletableFuture.anyOf(watcherCf, onEnter).join());
            onEnter.cancel(true);
            watcherCf.cancel(true);
            //new Scanner(System.in).nextLine();
            long endTime = System.currentTimeMillis();
            gameInstance.sendCommand("endmovie;stopdemo").join();
            System.out.println("Time spent rendering: " + ((endTime - startTime)/1000f));
            Thread.sleep(1000);

        }

        runningGames.forEach((game, instance) -> instance.close());
    }

    private void writeCfg(SrcGameConfiguration configuration, RenderSettings settings, boolean shouldStartOdd) throws IOException {
        List<String> configLines = new ArrayList<>();
        configLines.add("sv_cheats 1");
        configLines.addAll(settings.getAdditionalCommands());
        if(settings.isDemoInterpolate()) configLines.add("demo_interpolateview 1"); else configLines.add("demo_interpolateview 0");
        configLines.add("host_framerate " + (settings.getFps() * settings.getFrameblend()));
        configLines.add("mat_setvideomode " + settings.getWidth() + " " + settings.getHeight() + " 1");
        configLines.add("demo_debug 1");

        if(shouldStartOdd){
            configLines.add("sv_alternateticks 0");
            configLines.add("demo_pauseatservertick 120");
            Files.write(configuration.getConfigPath().resolve("nidr.restart.cfg"), Arrays.asList("startmovie nidr\\tga_ raw", "demo_resume"));
        } else {
            configLines.add("startmovie nidr\\tga_ raw");
        }

        /*if(!demo.getDemoPath().toString().contains("overwrite_autoload")) {

        } else {
            configLines.add("playdemo " + demo.getDemoPath().getFileName().toString());
        }*/
        configLines.add("playdemo nidr/dem");

        configLines.add("hud_reloadscheme");

        Files.write(configuration.getConfigPath().resolve("nidr.run.cfg"), configLines);
    }

    public static void main(String[] args){
        Source2013DemoSource source = new Source2013DemoSource();
        source.renderDemo(null);
        new Scanner(System.in);
    }
}
