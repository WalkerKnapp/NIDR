package gq.luma.render;

import com.walker.pipeline.longtype.LongPipeline;
import gq.luma.render.renderer.FrameBlender;
import gq.luma.render.renderer.HumbleVideoOutput;
import gq.luma.render.renderer.source.Source2013DemoSource;
import io.wkna.sdp.SourceDemo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PipelineTasRenderer {
    public static void main(String[] args) throws IOException, InterruptedException {


        RenderSettings blueCrosshairSettings = generateSettings();
        blueCrosshairSettings.getAdditionalCommands().add("ss_pip_bottom_offset 100000");
        blueCrosshairSettings.getAdditionalCommands().add("crosshair 1");
        RenderSettings orangeCrosshairSettings = generateSettings();
        orangeCrosshairSettings.getAdditionalCommands().add("ss_pip_bottom_offset 0");
        orangeCrosshairSettings.getAdditionalCommands().add("crosshair 1");
        RenderSettings blueNoCHSettings = generateSettings();
        blueNoCHSettings.getAdditionalCommands().add("ss_pip_bottom_offset 100000");
        blueNoCHSettings.getAdditionalCommands().add("crosshair 0");
        RenderSettings orangeNoCHSettings = generateSettings();
        orangeNoCHSettings.getAdditionalCommands().add("ss_pip_bottom_offset 0");
        orangeNoCHSettings.getAdditionalCommands().add("crosshair 0");

        Path folderPath = Paths.get("H:\\Portal 2\\Rendering\\tas\\coop\\rerender2");

        /*HashMap<String, AtomicInteger> countsPerName = new HashMap<>();

        System.out.println(Stream.of(Objects.requireNonNull(new File(folderPath).listFiles()))
                .filter(file -> file.getName().endsWith("dem"))
                .map(file -> {
                    try {
                        String name = file.getName().split("_")[2].split("\\.")[0];
                        countsPerName.computeIfAbsent(name, (str) -> new AtomicInteger(0)).incrementAndGet();
                        return SrcDemo.of(file, true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).map(demo -> demo != null ? demo.getPlaybackTime() : 0).mapToDouble(Float::floatValue).sum());

        countsPerName.forEach((str, atm) -> System.out.println(str + " - " + atm.toString()));*/

        HashMap<Path, SourceDemo> demos = new HashMap<>();
        Files.list(folderPath)
                .filter(file -> file.getFileName().toString().endsWith("dem"))
                .forEach(path -> {
                    try {
                        demos.put(path, SourceDemo.parse(path, false));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        //System.out.println(demos.size());

        ArrayList<RenderRequest> renderRequests = new ArrayList<>();
        demos.forEach((path, demo) -> {
            // Blue's POV w/ crosshair
            /*renderRequests.add(new RenderRequest(Collections.singletonList(demo),
                    blueCrosshairSettings,
                    folderPath.resolve(path.getFileName().toString() +
                            "_blue_ch." +
                            blueCrosshairSettings.getOutputFormat().getOutputContainer())));*/
            // Orange's POV w/ crosshair
            renderRequests.add(new RenderRequest(Collections.singletonList(demo),
                    orangeCrosshairSettings,
                    folderPath.resolve(path.getFileName().toString() +
                            "_orange_ch." +
                            orangeCrosshairSettings.getOutputFormat().getOutputContainer())));
            // Blue's POV w/o crosshair
            /*renderRequests.add(new RenderRequest(Collections.singletonList(demo),
                    blueNoCHSettings,
                    folderPath.resolve(path.getFileName().toString() +
                            "_blue_no_ch." +
                            blueNoCHSettings.getOutputFormat().getOutputContainer())));
            // Orange's POV w/o crosshair
            renderRequests.add(new RenderRequest(Collections.singletonList(demo),
                    orangeNoCHSettings,
                    folderPath.resolve(path.getFileName().toString() +
                            "_orange_no_ch." +
                            orangeNoCHSettings.getOutputFormat().getOutputContainer())));*/
        });

        for(RenderRequest request : renderRequests) {

            if(Files.exists(request.getOutputPath()) && Files.size(request.getOutputPath()) > 300) {
                continue;
            }

            Source2013DemoSource demoSource = new Source2013DemoSource();
            HumbleVideoOutput humbleVideoOutput = new HumbleVideoOutput(request.getSettings(), request.getOutputPath());

            /** Video pipeline **/
            LongPipeline.Builder pipelineBuilder = LongPipeline.builder().joint(demoSource.getDemoVideoSource());
            if(request.getSettings().getFrameblend() > 1) {
                FrameBlender blender = new FrameBlender(request.getSettings());
                pipelineBuilder.joint(blender);
            }
            LongPipeline pipeline = pipelineBuilder.joint(humbleVideoOutput.getVideoOutupt()).build();

            /** Audio pipeline **/
            LongPipeline.Builder audioPipelineBuilder = LongPipeline.builder().joint(demoSource.getDemoAudioSource());
            LongPipeline audioPipeline = audioPipelineBuilder.joint(humbleVideoOutput.getAudioOutput()).build();

            demoSource.renderDemo(request).join();

            pipeline.flush();
            audioPipeline.flush();

            humbleVideoOutput.finish();
        }

        Thread.sleep(5 * 1000);

        System.exit(0);
    }

    private static RenderSettings generateSettings() throws IOException {
        RenderSettings settings = new RenderSettings();
        settings.setWidth(1920);
        settings.setHeight(1080);
        settings.setFps(60);
        settings.setFrameblend(1);
        settings.setOutputFormat(OutputFormat.H264);
        settings.setTwoPass(false);
        settings.setCrf(0);
        settings.setDemoInterpolate(true);
        settings.setRemoveBrokenFrames(true);
        settings.setSplitScreenWorkaround(true);
        settings.setStartOddSpecified(true);
        settings.setForceStartOdd(false);
        settings.setAdditionalCommands(new ArrayList<>());
        settings.setSkipFrames(1);
        //settings.setAdditionalCommands(Files.readAllLines(Paths.get("H:\\Portal 2\\Portal 2old\\portal2\\cfg\\render.cfg")));

        settings.getAdditionalCommands().add("r_portal_use_dlights 1");
        settings.getAdditionalCommands().add("plugin_load sar");
        //settings.getAdditionalCommands().add("cl_enable_remote_splitscreen 1");
        settings.getAdditionalCommands().add("sar_disable_challenge_stats_hud 1");
        settings.getAdditionalCommands().add("sar_disable_no_focus_sleep 1");
        //settings.getAdditionalCommands().add("r_portal_use_pvs_optimization 0");

        settings.getAdditionalCommands().add("ss_pipsplit 3");
        settings.getAdditionalCommands().add("ss_pipscale 1");
        settings.getAdditionalCommands().add("ss_pip_right_offset 0");

        return settings;
    }
}

