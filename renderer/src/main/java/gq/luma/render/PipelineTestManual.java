package gq.luma.render;

import com.walker.pipeline.longtype.LongPipeline;
import gq.luma.render.renderer.FrameBlender;
import gq.luma.render.renderer.HumbleVideoOutput;
import gq.luma.render.renderer.source.FileSource;
import io.wkna.sdp.SourceDemo;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PipelineTestManual {
    public static void main(String[] args) throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InstantiationException, InvocationTargetException {
        RenderSettings settings = new RenderSettings();
        settings.setWidth(1920);
        settings.setHeight(1080);
        settings.setFps(60);
        settings.setFrameblend(32);
        settings.setOutputFormat(OutputFormat.H264);
        settings.setTwoPass(false);
        settings.setCrf(13);
        settings.setDemoInterpolate(true);
        //settings.setStartOddSpecified(true);
        //settings.setForceStartOdd(false);
        settings.setRemoveBrokenFrames(true);
        settings.setAdditionalCommands(Files.readAllLines(Paths.get("H:\\Portal 2\\Portal 2old\\portal2\\cfg\\render.cfg")));

        Scanner scanner = new Scanner(System.in);


        Path demoPath = Paths.get("H:\\Portal 2\\Rendering\\baister\\zhouse_136.dem");
        Path outPath = Paths.get("H:\\Portal 2\\Rendering\\baister\\zhouse_136.dem.mp4");
        Path nidrPath = Paths.get("F:\\SteamLibrary\\steamapps\\SourceMods\\Memories\\nidr");

        Path hardLinkPath = Paths.get("H:\\banister");
        Files.deleteIfExists(nidrPath);
        Files.createSymbolicLink(nidrPath, hardLinkPath);

        SourceDemo demo = SourceDemo.parse(demoPath, false);

        RenderRequest request = new RenderRequest(null, settings);
        /*DemoSource demoSource = new DemoSource() {
            @Override
            public void runSyncRender(RenderRequest renderRequest) throws Exception {
                //SrcGameWatcher watcher = new SrcGameWatcher("F:\\SteamLibrary\\steamapps\\SourceMods\\Memories", "F:\\SteamLibrary\\steamapps\\SourceMods\\Memories",  this, this);
                //watcher.provideDemo(Files.newByteChannel(demo.getDemoPath()));

                System.out.println("Waiting for movie start. Run the following commands...");

                boolean shouldStartOnOddTick = ((renderRequest.getSettings().isStartOddSpecified() && renderRequest.getSettings().isForceStartOdd())
                        || (!renderRequest.getSettings().isStartOddSpecified() && demo.getFirstPlaybackTick() % 2 == 0))
                        && !demo.getMapName().contains("mp_");
                writeCfg("F:\\SteamLibrary\\steamapps\\common\\Portal 2\\portal2\\cfg", shouldStartOnOddTick);

                System.out.println("Run the command \"exec nidr.run.cfg\"");
                scanner.nextLine();
                if(shouldStartOnOddTick){
                    System.out.println("Run the command \"demo_pauseatservertick 1;demo_resume\"");
                    scanner.nextLine();
                    System.out.println("Run the command \"sv_alternateticks 1\"");
                    scanner.nextLine();
                    System.out.println("Run the command \"exec nidr.restart.cfg\"");
                    scanner.nextLine();
                }


                System.out.println("Waiting for manual stop. (Press enter)");
                scanner.nextLine();
                System.out.println();
            }

            private void writeCfg(String cfgPath, boolean shouldStartOdd) throws IOException {
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
                    Files.write(Paths.get(cfgPath).resolve("nidr.restart.cfg"), Arrays.asList("startmovie nidr\\tga_ raw", "demo_resume"));
                } else {
                    configLines.add("startmovie nidr\\tga_ raw");
                }

                configLines.add("playdemo " + demo.getDemoPath().getFileName().toString());
                configLines.add("hud_reloadscheme");

                Files.write(Paths.get(cfgPath).resolve("nidr.run.cfg"), configLines);
            }
        };*/
        HumbleVideoOutput humbleVideoOutput = new HumbleVideoOutput(settings, outPath);
        FileSource fileSource = new FileSource(request, nidrPath);
        LongPipeline.Builder pipelineBuilder = LongPipeline.builder()
                .joint(fileSource);
        if(settings.getFrameblend() > 1) {
            FrameBlender blender = new FrameBlender(settings);
            pipelineBuilder.joint(blender);
        }
        LongPipeline pipeline = pipelineBuilder.joint(humbleVideoOutput.getVideoOutupt()).build();

        //demoSource.renderDemo(request).join();

        fileSource.run();

        pipeline.flush();

        humbleVideoOutput.finish();
    }
}
