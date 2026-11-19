package tc4revival.resources;

import java.util.concurrent.*;
import java.util.function.Consumer;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;

/** Downloads before pack discovery; failures are displayed after vanilla's first resource reload. */
public class OriginalDownloadBootstrap {
    private static boolean attempted;
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    public static String failures() { return ResourceLoadingState.details(); }
    public static void retry(Consumer<String> progress) {
        ResourceLoadingState.clear();
        try {
            var targets = OriginalDownloadTargets.installed();
            var owners = LegacyModLogo.primarySourceOwners();
            var logoSources = targets.stream().filter(target -> owners.contains(target.owner()))
                    .map(target -> ProcessedOriginalResources.directoryName(target.legacyModId(), target.minecraftVersion()))
                    .collect(java.util.stream.Collectors.toSet());
            var results=OriginalResourceDownloads.obtainMissing(FMLPaths.GAMEDIR.get().toAbsolutePath().normalize(),targets,logoSources,message->{LOGGER.info("{}",message);progress.accept(message);},CurseForgeDownload::fetch);
            for(var result:results) {
                if(!result.failure().isEmpty()){LOGGER.warn("Original resource download failed: {}",result.failure());ResourceLoadingState.failure(result.failure());}
                if(!result.warning().isEmpty())ResourceLoadingState.warning(result.owner()+": "+result.warning());
                if(result.downloaded()!=null)LOGGER.info("Prepared original {} resources in {}",result.owner(),result.downloaded());
            }
        }catch(Exception failure){LOGGER.warn("Original resource preparation failed",failure);ResourceLoadingState.failure(String.valueOf(failure.getMessage()));}
    }
    public static void beforeResourceFinders() {
        if(attempted)return;attempted=true;
        var task=CompletableFuture.runAsync(()->retry(net.minecraftforge.fml.loading.ImmediateWindowHandler::updateProgress));
        while(true)try{task.get(50,TimeUnit.MILLISECONDS);return;}
        catch(TimeoutException waiting){GLFW.glfwPollEvents();}
        catch(InterruptedException interrupted){Thread.currentThread().interrupt();task.cancel(true);ResourceLoadingState.failure("Resource download interrupted");return;}
        catch(ExecutionException failure){ResourceLoadingState.failure(String.valueOf(failure.getCause()));return;}
    }
}
