package gadomancymodern.familiar;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

/** Twenty data-pack templates expand against the live aspect registry, including other addons' aspects. */
@Mod.EventBusSubscriber(modid="gadomancy")
public class FamiliarRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> TYPES=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,"gadomancy");
    public static final RegistryObject<RecipeSerializer<FamiliarInfusion>> SERIALIZER=TYPES.register("familiar_infusion",FamiliarInfusion.Serializer::new);
    public static void register(IEventBus bus){TYPES.register(bus);}
    public static int expand(RecipeManager manager){
        var all=new ArrayList<Recipe<?>>(manager.getRecipes());var templates=all.stream().filter(r->r instanceof FamiliarInfusion f&&f.template()).map(r->(FamiliarInfusion)r).toList();if(templates.isEmpty())return 0;
        for(var template:templates){String prefix=template.getId().getPath()+"/";all.removeIf(r->r.getId().getNamespace().equals(template.getId().getNamespace())&&r.getId().getPath().startsWith(prefix));for(var aspect:tc4revival.aspect.TCAspects.catalog().all()){
            var json=template.templateDefinition();json.addProperty("familiar_aspect",aspect.tag());all.add(new FamiliarInfusion(new ResourceLocation(template.getId().getNamespace(),prefix+aspect.tag().replace(':','/')),json));
        }}manager.replaceRecipes(all);return templates.size()*tc4revival.aspect.TCAspects.catalog().all().size();
    }
    @SubscribeEvent public static void starting(ServerAboutToStartEvent event){expand(event.getServer().getRecipeManager());}
    @SubscribeEvent(priority=EventPriority.HIGH) public static void reload(OnDatapackSyncEvent event){if(event.getPlayer()==null)expand(event.getPlayerList().getServer().getRecipeManager());}
}
