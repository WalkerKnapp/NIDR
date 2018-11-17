package gq.luma.render.renderer;

import gq.luma.render.CompletedRender;
import gq.luma.render.RenderRequest;

import java.util.concurrent.CompletableFuture;

public class Portal2DemoSource extends DemoSource {

    @Override
    public CompletableFuture<CompletedRender> renderDemo(RenderRequest renderRequest) {
        if(currentRender.get() != null){
            throw new IllegalStateException("Multiple demos cannot be rendered at once with the " + getClass().getSimpleName());
        }
        return CompletableFuture.supplyAsync(() -> {
            currentRender.set(renderRequest);

            // Start the game if not started

            // Write autorecord and send exec autorecord

            // Detect if the demo should be started on an odd tick

            // Send restarter

            // Wait for dem_stop

            //

            currentRender.set(null);
            return new CompletedRender();
        }, threadPool);
    }

    @Override
    protected void flush() {
        // Source will never need to be flushed
    }
}
