package gq.luma.render.renderer.source;

import gq.luma.render.RenderRequest;
import gq.luma.render.RenderSettings;
import gq.luma.render.engine.SrcDemo;
import gq.luma.render.engine.SrcGame;
import gq.luma.render.renderer.configuration.Source2013Configuration;
import gq.luma.render.renderer.configuration.SrcGameConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Source2013DemoSource extends DemoSource {

    public Source2013DemoSource(){
        gameConfigurations.put(SrcGame.PORTAL2, new Source2013Configuration(SrcGame.PORTAL2, "F:\\SteamLibrary\\steamapps\\common\\Portal 2"));
        gameConfigurations.put(SrcGame.PORTAL_STORIES_MEL, new Source2013Configuration(SrcGame.PORTAL_STORIES_MEL, "F:\\SteamLibrary\\steamapps\\common\\Portal Stories Mel"));
    }

    @Override
    public void runSyncRender(RenderRequest renderRequest) throws Exception {
        for (SrcDemo demo : renderRequest.getDemos()) {

            // Start the game if not started
            SrcGameConfiguration configuration = getConfiguration(demo.getGame());
            setupGame(demo.getGame());

            //TODO: Send info packet downstream in the pipeline

            runningGames.get(demo.getGame()).getWatcher().provideDemo(Files.newByteChannel(demo.getDemoPath()));

            // Write start script
            boolean shouldStartOnOddTick = ((renderRequest.getSettings().isStartOddSpecified() && renderRequest.getSettings().isForceStartOdd())
                    || (!renderRequest.getSettings().isStartOddSpecified() && demo.getFirstPlaybackTick() % 2 == 0))
                    && !demo.getMapName().contains("mp_");
            writeCfg(configuration, renderRequest.getSettings(), demo, shouldStartOnOddTick);

            // Start the render
            runningGames.get(demo.getGame()).sendCommand("exec nidr.run.cfg");
            if(shouldStartOnOddTick){
                if(!demo.getMapName().contains("mp_")) {
                    runningGames.get(demo.getGame()).getWatcher().watch("Redownloading all lightmaps").join();
                    //new Scanner(System.in).nextLine();
                    Thread.sleep(700);
                } else {
                    new Scanner(System.in).nextLine();
                    runningGames.get(demo.getGame()).getWatcher().watch("Demo message, tick 0").join();
                    Thread.sleep(1000);
                }
                runningGames.get(demo.getGame()).sendCommand("demo_pauseatservertick 1;demo_resume");
                Thread.sleep(3000);
                runningGames.get(demo.getGame()).sendCommand("sv_alternateticks 1");
                Thread.sleep(3000);
                runningGames.get(demo.getGame()).sendCommand("exec nidr.restart.cfg");
            }
            updateStatus("Rendering");

            // Wait for dem_stop
            long startTime = System.currentTimeMillis();
            runningGames.get(demo.getGame()).getWatcher().watch("dem_stop", "leaderboard_open").join();
            //new Scanner(System.in).nextLine();
            long endTime = System.currentTimeMillis();
            System.out.println("Time spent rendering: " + ((endTime - startTime)/1000f));

        }

        runningGames.forEach((game, instance) -> instance.close());
    }

    private void writeCfg(SrcGameConfiguration configuration, RenderSettings settings, SrcDemo demo, boolean shouldStartOdd) throws IOException {
        List<String> configLines = new ArrayList<>();
        configLines.add("sv_cheats 1");
        configLines.addAll(settings.getAdditionalCommands());
        if(settings.isDemoInterpolate()) configLines.add("demo_interpolateview 1"); else configLines.add("demo_interpolateview 0");
        configLines.add("host_framerate " + (settings.getFps() * settings.getFrameblend()));
        configLines.add("mat_setvideomode " + settings.getWidth() + " " + settings.getHeight() + " 1");
        configLines.add("demo_debug 1");

        if(shouldStartOdd){
            configLines.add("sv_alternateticks 0");
            configLines.add("demo_pauseatservertick 1");
            Files.write(configuration.getConfigPath().resolve("nidr.restart.cfg"), Arrays.asList("startmovie nidr\\tga_ raw", "demo_resume"));
        } else {
            configLines.add("startmovie nidr\\tga_ raw");
        }

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
