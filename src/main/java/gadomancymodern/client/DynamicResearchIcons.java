package gadomancymodern.client;
import gadomancymodern.Gadomancy;
import net.minecraft.world.item.*;
import tc4revival.research.ResearchDefinition;
/** Original second-based changing research examples, without changing gameplay item registrations. */
public class DynamicResearchIcons {
    public static void setup(){
        thaumcraft.api.client.ResearchPresentationClient.registerIcon("gadomancy:examples",0,(context,current)->{
            var overlays=new java.util.ArrayList<>(current.overlays());
            if(context.research().equals("gadomancy:golem_runic_shield"))overlays.add(net.minecraft.resources.ResourceLocation.parse("gadomancy:textures/items/r_runicupg.png"));
            return new thaumcraft.api.client.ResearchPresentationClient.Icon(select(context.research(),current.stack(),context.milliseconds()),overlays);
        });
    }
    protected static java.util.List<ItemStack> armor;
    public static java.util.List<ItemStack> armor(){if(armor==null)armor=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues().stream().map(Item::getDefaultInstance).filter(gadomancymodern.disguise.ArmorDisguise::armor).toList();return armor;}
    public static ItemStack disguise(long second){var choices=armor();return choices.isEmpty()?new ItemStack(Items.IRON_CHESTPLATE):choices.get(new java.util.Random(second).nextInt(choices.size())).copy();}
    public static ResearchDefinition.Stack select(String research,ResearchDefinition.Stack fallback,long milliseconds){long second=milliseconds/1000;java.util.List<ItemStack> icons=new java.util.ArrayList<>();switch(research){
        case "gadomancy:aura_core"->{for(var type:Gadomancy.AURA_CORES.values())icons.add(type.get().getDefaultInstance());}
        case "gadomancy:armor_disguise"->{icons.add(disguise(second));}
        case "gadomancy:ancient_stones"->{icons.add(tc4revival.content.TCEldritch.STONE.get().asItem().getDefaultInstance());icons.add(tc4revival.content.TCEldritch.PEDESTAL_STONE.get().asItem().getDefaultInstance());}
        case "gadomancy:sticky_jar"->{icons.add(tc4revival.content.TCItems.ESSENTIA_JAR.get().getDefaultInstance());var knowledge=tc4revival.client.ClientKnowledge.snapshot();if(knowledge.isPresent()&&knowledge.get().researchMarkers().contains("JARVOID"))icons.add(tc4revival.content.TCItems.VOID_JAR.get().getDefaultInstance());if(knowledge.isPresent()&&knowledge.get().researchMarkers().contains("gadomancy:remote_jar"))icons.add(Gadomancy.REMOTE_JAR_ITEM.get().getDefaultInstance());for(var stack:icons)stack.getOrCreateTag().putBoolean("isStickyJar",true);}
        case "gadomancy:golem_runic_shield"->{for(var type:tc4revival.content.GolemMaterial.values())icons.add(tc4revival.content.TCItems.golem(type).getDefaultInstance());icons.add(Gadomancy.SILVERWOOD_GOLEM.get().getDefaultInstance());}
        default->{return fallback;}
    }if(icons.isEmpty())return fallback;var stack=icons.get((int)Math.floorMod(second,icons.size()));return new ResearchDefinition.Stack(new tc4revival.aspect.recipe.RecipeAspectData.Key(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString(),0),1,stack.isDamageableItem(),stack.hasTag()?java.util.Optional.of(stack.getTag().toString()):java.util.Optional.empty(),java.util.Optional.empty());}
}
