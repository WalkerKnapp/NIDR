package gq.luma.render.renderer;

import com.walker.pipeline.PipelineDatatype;
import com.walker.pipeline.PipelineSource;
import gq.luma.render.CompletedRender;
import gq.luma.render.RenderRequest;
import gq.luma.render.engine.SrcGame;
import gq.luma.render.engine.SrcGameConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public abstract class DemoSource extends PipelineSource {
    protected ExecutorService threadPool = Executors.newFixedThreadPool(Integer.MAX_VALUE);
    protected AtomicReference<RenderRequest> currentRender = new AtomicReference<>(null);

    protected HashMap<SrcGame, SrcGameConfiguration> gameConfigurations;
    protected CopyOnWriteArrayList<SrcGame> runningGames = new CopyOnWriteArrayList<>();

    public DemoSource() {
        super(PipelineDatatype.SuperType.RAW_VIDEO, PipelineDatatype.RAW_BGR24);
    }

    public abstract CompletableFuture<CompletedRender> renderDemo(RenderRequest renderRequest);

    private void setupGame(SrcGame game){
        if(runningGames.contains(game)){
            return;
        }

        if(!gameConfigurations.containsKey(game)){
            throw new ConfigurationException(game.name() + " configuration not set for " + getClass().getSimpleName());
        }
        SrcGameConfiguration configuration = gameConfigurations.get(game);

        try {
            Files.deleteIfExists(configuration.getLogPath());
        } catch (IOException e) {
            throw new ConfigurationException(getClass().getSimpleName() + " failed to delete log file. Check permissions.", e);
        }
    }

    private void updateStatus(String status){
        RenderRequest request = currentRender.get();
        if(request != null){
            request.setStatus(status);
        }
    }
}
