package gq.luma.render.renderer;

import com.walker.pipeline.PipelineDatatype;
import com.walker.pipeline.PipelineSource;
import gq.luma.render.CompletedRender;
import gq.luma.render.RenderRequest;
import gq.luma.render.engine.SrcGame;
import gq.luma.render.engine.SrcGameConfiguration;
import gq.luma.render.engine.SrcGameInstance;
import gq.luma.render.engine.filesystem.FSAudioHandler;
import gq.luma.render.engine.filesystem.FSVideoHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public abstract class DemoSource extends PipelineSource implements FSVideoHandler, FSAudioHandler {
    protected ExecutorService threadPool = Executors.newFixedThreadPool(Integer.MAX_VALUE);
    protected AtomicReference<RenderRequest> currentRender = new AtomicReference<>(null);

    protected HashMap<SrcGame, SrcGameConfiguration> gameConfigurations = new HashMap<>();
    protected ConcurrentHashMap<SrcGame, SrcGameInstance> runningGames = new ConcurrentHashMap<>();

    public DemoSource() {
        super(PipelineDatatype.SuperType.RAW_VIDEO, PipelineDatatype.RAW_BGR24);
    }

    public abstract CompletableFuture<CompletedRender> renderDemo(RenderRequest renderRequest);

    public void setupGame(SrcGame game){
        if(runningGames.containsKey(game)){
            return;
        }
        SrcGameConfiguration configuration = getConfiguration(game);

        SrcGameInstance instance = new SrcGameInstance(game, configuration, (t, bool) -> {
            t.printStackTrace();
        });
        runningGames.put(game, instance);
        instance.start(this, this);
    }

    protected SrcGameConfiguration getConfiguration(SrcGame game){
        if(!gameConfigurations.containsKey(game)){
            throw new ConfigurationException(game.name() + " configuration not set for " + getClass().getSimpleName());
        }
        return gameConfigurations.get(game);
    }

    protected void updateStatus(String status){
        RenderRequest request = currentRender.get();
        if(request != null){
            request.setStatus(status);
        }
    }
}
