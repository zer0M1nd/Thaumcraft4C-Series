package tc4revival.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import tc4revival.research.*;

/** Recipe/tag reloads invalidate previews. No recipe search or JSON decoding during every frame. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public class ClientLiveBookRecipes {
    private static final Map<ResearchPage,List<LiveBookRecipes.View>> CACHE=new HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel world;
    private static long generation;
    public static long generation(){return generation;}
    public static List<LiveBookRecipes.View> get(ResearchPage page) {
        var level=Minecraft.getInstance().level;
        if(world!=level) { CACHE.clear();world=level;generation++; }
        return level==null?List.of():CACHE.computeIfAbsent(page,p->LiveBookRecipes.resolve(level,p));
    }
    @SubscribeEvent public static void recipes(net.minecraftforge.client.event.RecipesUpdatedEvent event){CACHE.clear();generation++;}
    @SubscribeEvent public static void tags(net.minecraftforge.event.TagsUpdatedEvent event){CACHE.clear();generation++;}
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){CACHE.clear();world=null;generation++;}
}
