package gq.luma.example;

import com.walker.pipeline.longtype.LongPipeline;
import gq.luma.plugins.WorkshopPlugin;
import gq.luma.render.OutputFormat;
import gq.luma.render.RenderRequest;
import gq.luma.render.RenderSettings;
import gq.luma.render.engine.DemoUtils;
import gq.luma.render.renderer.FrameBlender;
import gq.luma.render.renderer.HumbleVideoOutput;
import gq.luma.render.renderer.source.Source2013DemoSource;
import io.wkna.sdp.SourceDemo;
import io.wkna.sdp.messages.DemoMessage;
import io.wkna.sdp.messages.SignOnMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorkshopExample {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println(System.class.getResource("/log4j2.xml"));

        RenderSettings settings = new RenderSettings();
        settings.setWidth(1920);
        settings.setHeight(1080);
        settings.setFps(60);
        settings.setFrameblend(32);
        settings.setOutputFormat(OutputFormat.HUFFYUV);
        settings.setTwoPass(false);
        settings.setCrf(10);
        settings.setDemoInterpolate(true);
        //settings.setSplitScreenWorkaround(true);
        //settings.setStartOddSpecified(true);
        //settings.setForceStartOdd(true);
        settings.setInitialPauseTiming(16);
        //settings.setRemoveBrokenFrames(true);
        settings.setAdditionalCommands(Files.readAllLines(Paths.get("H:\\Portal 2\\Portal 2old\\portal2\\cfg\\render.cfg")));
        //settings.setAdditionalCommands(Files.readAllLines(Paths.get("H:\\Portal 2\\Rendering\\Henus\\render.cfg")));
        //settings.setAdditionalCommands(new ArrayList<>());
        settings.getAdditionalCommands().add("r_portal_use_pvs_optimization 0");
        //settings.getAdditionalCommands().add("mat_ambient_light_b -0.01\n" + "mat_ambient_light_g -0.01\n" + "mat_ambient_light_r -0.01");

        //settings.getAdditionalCommands().add("r_portal_use_dlights 1");
        //settings.getAdditionalCommands().add("plugin_load sar");
        //settings.getAdditionalCommands().add("net_graph 0");
        settings.getAdditionalCommands().add("cl_enable_remote_splitscreen 1");
        //settings.getAdditionalCommands().add("cl_showpos 1");
        //settings.getAdditionalCommands().add("sar_hud_position 1");
        //settings.getAdditionalCommands().add("sar_hud_session 1");
        //settings.getAdditionalCommands().add("sar_disable_challenge_stats_hud 1");
        //settings.getAdditionalCommands().add("sar_disable_no_focus_sleep 1");
        //settings.getAdditionalCommands().add("crosshair 0");
        //settings.getAdditionalCommands().add("r_portal_use_pvs_optimization 1");

        //SrcDemo srcDemo = SrcDemo.of(new File("PitFlings_1575_Zypeh.dem"), true);

        Path folderPath = Paths.get("H:\\Portal 2\\Rendering\\spidda");

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
                .filter(file -> {
                    try {
                        return (!Files.exists(folderPath.resolve(file.getFileName() + "."  + settings.getOutputFormat().getOutputContainer())))
                                || Files.size(folderPath.resolve(file.getFileName() + "."  + settings.getOutputFormat().getOutputContainer())) < 300;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                })
                .filter(file -> !Files.exists(folderPath.resolve(file.getFileName() + ".mp4")))
                .forEach(path -> {
                    System.out.println("Parsing demo: " + path);
                    try {
                        demos.put(path, SourceDemo.parse(path, false));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });





        demos.forEach((path, demo) -> {

            int firstRealPacket = DemoUtils.getFirstPlaybackTick(demo);
            for(DemoMessage message : demo.getMessages()) {
                if(message instanceof SignOnMessage) {
                    if(message.getTick() % 2 != firstRealPacket % 2) {
                        message.setTick(message.getTick() + 1);
                    }
                }
            }

            try {
                RenderRequest request = new RenderRequest(new ArrayList<>(Collections.singletonList(demo)), settings);
                Path outPath = folderPath.resolve(path.getFileName().toString() + "." + settings.getOutputFormat().getOutputContainer());
                //RenderRequest request = new RenderRequest(new ArrayList<>(List.of(demo)), settings);
                //Path outPath = Paths.get("H:\\Portal 2\\Rendering\\happy.dem.mov");

                Source2013DemoSource demoSource = new Source2013DemoSource(new WorkshopPlugin());
                HumbleVideoOutput humbleVideoOutput = new HumbleVideoOutput(settings, outPath);

                /** Video pipeline **/
                LongPipeline.Builder pipelineBuilder = LongPipeline.builder().joint(demoSource.getDemoVideoSource());
                if (settings.getFrameblend() > 1) {
                    FrameBlender blender = new FrameBlender(settings);
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
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });

        Thread.sleep(5 * 1000);

        System.exit(0);
    }
}
