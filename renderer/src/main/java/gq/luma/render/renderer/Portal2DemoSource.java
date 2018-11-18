package gq.luma.render.renderer;

import gq.luma.render.CompletedRender;
import gq.luma.render.RenderRequest;
import gq.luma.render.RenderSettings;
import gq.luma.render.engine.SrcDemo;
import gq.luma.render.engine.SrcGame;
import gq.luma.render.engine.SrcGameConfiguration;
import jnr.ffi.Pointer;
import sun.misc.Unsafe;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class Portal2DemoSource extends DemoSource {

    private Unsafe unsafe;
    private ByteBuffer frameBuffer;
    private long frameBufferBasePointer;

    private long frameSize;

    @Override
    public CompletableFuture<CompletedRender> renderDemo(RenderRequest renderRequest) {
        if(currentRender.get() != null){
            throw new IllegalStateException("Multiple demos cannot be rendered at once with the " + getClass().getSimpleName());
        }
        return CompletableFuture.supplyAsync(() -> {
            currentRender.set(renderRequest);

            try {
                // Setup unsafe
                Field f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                this.unsafe = (Unsafe) f.get(null);

                // Setup buffers
                frameSize = (renderRequest.getSettings().getWidth() * renderRequest.getSettings().getHeight() * 3) + 18;
                frameBuffer = ByteBuffer.allocateDirect(renderRequest.getSettings().getWidth() * renderRequest.getSettings().getHeight() * 3);
                Field bAddr = java.nio.Buffer.class.getDeclaredField("address");
                bAddr.setAccessible(true);
                this.frameBufferBasePointer = (long) bAddr.get(frameBuffer);

                // Start the game if not started
                SrcGameConfiguration configuration = getConfiguration(SrcGame.PORTAL2);
                setupGame(SrcGame.PORTAL2);

                for (SrcDemo demo : renderRequest.getDemos()) {

                    //TODO: Send info packet downstream in the pipeline

                    // Write start script
                    boolean shouldStartOnOddTick = ((renderRequest.getSettings().isStartOddSpecified() && renderRequest.getSettings().isForceStartOdd())
                            || (!renderRequest.getSettings().isStartOddSpecified() && demo.getFirstPlaybackTick() % 2 == 0))
                            && !demo.getMapName().contains("mp_");
                    writeCfg(configuration, renderRequest.getSettings(), demo, shouldStartOnOddTick);

                    // Start the render
                    runningGames.get(SrcGame.PORTAL2).sendCommand("exec nidr.run");
                    if(shouldStartOnOddTick){
                        if(!demo.getMapName().contains("mp_")) {
                            runningGames.get(SrcGame.PORTAL2).getWatcher().watch("Redownloading all lightmaps").join();
                            Thread.sleep(700);
                        } else {
                            runningGames.get(SrcGame.PORTAL2).getWatcher().watch("Demo message, tick 0").join();
                            Thread.sleep(1000);
                        }
                        runningGames.get(SrcGame.PORTAL2).sendCommand("demo_pauseatservertick 1;demo_resume");
                        Thread.sleep(3000);
                        runningGames.get(SrcGame.PORTAL2).sendCommand("sv_alternateticks 1");
                        Thread.sleep(3000);
                        runningGames.get(SrcGame.PORTAL2).sendCommand("exec nidr.restart");
                    }
                    updateStatus("Rendering");

                    // Wait for dem_stop
                    long startTime = System.currentTimeMillis();
                    runningGames.get(SrcGame.PORTAL2).getWatcher().watch("dem_stop", "leaderboard_open").join();
                    long endTime = System.currentTimeMillis();
                    System.out.println("Time spent rendering: " + ((endTime - startTime)/1000f));

                }

            } catch (Throwable t){
                t.printStackTrace();
            }

            currentRender.set(null);
            return new CompletedRender();
        }, threadPool);
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

        configLines.add("playdemo " + demo.getDemoPath().toAbsolutePath().toString());
        configLines.add("hud_reloadscheme");

        Files.write(configuration.getConfigPath().resolve("nidr.run.cfg"), configLines);
    }

    public static void main(String[] args){
        Portal2DemoSource source = new Portal2DemoSource();
        source.gameConfigurations.put(SrcGame.PORTAL2, new SrcGameConfiguration(SrcGame.PORTAL2,
                "F:\\SteamLibrary\\steamapps\\common\\Portal 2\\portal2",
                "F:\\SteamLibrary\\steamapps\\common\\Portal 2\\portal2\\cfg",
                "F:\\SteamLibrary\\steamapps\\common\\Portal 2\\portal2\\console.log",
                "F:\\SteamLibrary\\steamapps\\common\\Portal 2\\portal2.exe",
                "portal2.exe"));
        source.renderDemo(null);
        new Scanner(System.in);
    }

    @Override
    protected void flush() {
        // Source will never need to be flushed
    }

    @Override
    public void handleAudioData(Pointer buf, long offset, long size) {
        // TODO: Handle Audio
    }

    @Override
    public void handleVideoData(int index, Pointer buf, long offset, long writeLength) {
        int frameOffset = 0;
        long destOffset = 0;
        if(offset == 0){
            frameOffset = 18;
        } else {
            destOffset = offset - 18;
        }
        unsafe.copyMemory(buf.address() + frameOffset, frameBufferBasePointer + destOffset, writeLength - frameOffset);

        if(offset + writeLength == frameSize){
            pushBuffer(frameBuffer);
        }
    }
}
