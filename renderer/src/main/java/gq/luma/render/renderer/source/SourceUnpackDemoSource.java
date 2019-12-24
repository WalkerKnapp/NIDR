package gq.luma.render.renderer.source;

import gq.luma.render.RenderRequest;
import gq.luma.render.RenderSettings;
import gq.luma.render.engine.SrcGameInstance;
import gq.luma.render.renderer.configuration.SourceUnpackConfiguration;
import gq.luma.render.renderer.configuration.SrcGameConfiguration;
import io.wkna.sdp.SourceDemo;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class SourceUnpackDemoSource extends DemoSource{

    public SourceUnpackDemoSource(){
        gameConfigurations.put("portal",
                new SourceUnpackConfiguration("portal", 400,"H:\\Portal 2\\Unpack", null));
    }

    @Override
    public void runSyncRender(RenderRequest renderRequest) throws Exception {
        for (SourceDemo demo : renderRequest.getDemos()) {

            // Start the game if not started
            SrcGameConfiguration configuration = getConfiguration(demo.getGameDirectory());
            SrcGameInstance gameInstance = setupGame(configuration);

            //TODO: Send info packet downstream in the pipeline

            gameInstance.getWatcher().provideDemo(demo.createWriter());

            // Write start script
            writeCfg(configuration, renderRequest.getSettings());

            // Start the render
            gameInstance.sendCommand("exec nidr.run.cfg");
            updateStatus("Rendering");

            // Wait for dem_stop
            long startTime = System.currentTimeMillis();
            gameInstance.getWatcher().watch("dem_stop").join();
            long endTime = System.currentTimeMillis();
            System.out.println("Time spent rendering: " + ((endTime - startTime)/1000f));

        }
    }

    private void writeCfg(SrcGameConfiguration configuration, RenderSettings settings) throws IOException {
        List<String> configLines = new ArrayList<>();
        configLines.add("sv_cheats 1");
        configLines.addAll(settings.getAdditionalCommands());
        if(settings.isDemoInterpolate()) configLines.add("demo_interpolateview 1"); else configLines.add("demo_interpolateview 0");
        configLines.add("host_framerate " + (settings.getFps() * settings.getFrameblend()));
        configLines.add("mat_setvideomode " + settings.getWidth() + " " + settings.getHeight() + " 1");
        configLines.add("demo_debug 1");

        configLines.add("startmovie nidr\\tga_ raw");

        configLines.add("playdemo nidr/dem");
        configLines.add("hud_reloadscheme");

        Files.write(configuration.getConfigPath().resolve("nidr.run.cfg"), configLines);
    }
}
