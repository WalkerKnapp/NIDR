package gq.luma.render.renderer.source;

import gq.luma.render.RenderRequest;
import gq.luma.render.RenderSettings;
import gq.luma.render.engine.SrcDemo;
import gq.luma.render.engine.SrcGame;
import gq.luma.render.renderer.configuration.SourceUnpackConfiguration;
import gq.luma.render.renderer.configuration.SrcGameConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class SourceUnpackDemoSource extends DemoSource{

    public SourceUnpackDemoSource(){
        gameConfigurations.put(SrcGame.PORTAL, new SourceUnpackConfiguration(SrcGame.PORTAL,"H:\\Portal 2\\Unpack", null));
    }

    @Override
    public void runSyncRender(RenderRequest renderRequest) throws Exception {
        for (SrcDemo demo : renderRequest.getDemos()) {

            // Start the game if not started
            SrcGameConfiguration configuration = getConfiguration(demo.getGame());
            setupGame(demo.getGame());

            //TODO: Send info packet downstream in the pipeline

            // Write start script
            writeCfg(configuration, renderRequest.getSettings(), demo);

            // Start the render
            runningGames.get(demo.getGame()).sendCommand("exec nidr.run.cfg");
            updateStatus("Rendering");

            // Wait for dem_stop
            long startTime = System.currentTimeMillis();
            runningGames.get(demo.getGame()).getWatcher().watch("dem_stop").join();
            long endTime = System.currentTimeMillis();
            System.out.println("Time spent rendering: " + ((endTime - startTime)/1000f));

        }
    }

    private void writeCfg(SrcGameConfiguration configuration, RenderSettings settings, SrcDemo demo) throws IOException {
        List<String> configLines = new ArrayList<>();
        configLines.add("sv_cheats 1");
        configLines.addAll(settings.getAdditionalCommands());
        if(settings.isDemoInterpolate()) configLines.add("demo_interpolateview 1"); else configLines.add("demo_interpolateview 0");
        configLines.add("host_framerate " + (settings.getFps() * settings.getFrameblend()));
        configLines.add("mat_setvideomode " + settings.getWidth() + " " + settings.getHeight() + " 1");
        configLines.add("demo_debug 1");

        configLines.add("startmovie nidr\\tga_ raw");

        configLines.add("playdemo \"" + demo.getDemoPath().toString() + "\"");
        configLines.add("hud_reloadscheme");

        Files.write(configuration.getConfigPath().resolve("nidr.run.cfg"), configLines);
    }
}
