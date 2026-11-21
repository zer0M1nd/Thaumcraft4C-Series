package ttmodern;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Tinkerer.ID)
public final class KamiContent {
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> ITEMS=net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Tinkerer.ID,"kami"));
    public static boolean visible(net.minecraft.world.item.Item item){return TinkererConfig.KAMI.get()||!item.builtInRegistryHolder().is(ITEMS);}
    public static final java.util.List<String> RESEARCH=java.util.List.of("DIMENSION_SHARDS","ICHOR","ICHOR_CLOTH","ICHORIUM","CAP_ICHOR","ROD_ICHORCLOTH","ICHOR_TOOLS","ICHORCLOTH_ARMOR","ICHOR_PICK_GEM","ICHOR_SHOVEL_GEM","ICHOR_AXE_GEM","ICHOR_SWORD_GEM","ICHORCLOTH_HELM_GEM","ICHORCLOTH_CHEST_GEM","ICHORCLOTH_LEGS_GEM","ICHORCLOTH_BOOTS_GEM","PROTOCLAY","CAT_AMULET","ICHOR_POUCH","BLOCK_TALISMAN","WARP_GATE","PLACEMENT_MIRROR","FOCUS_RECALL","FOCUS_XP_DRAIN","FOCUS_SHADOWBEAM");
    public static void setup(){for(String key:RESEARCH)thaumcraft.api.research.ResearchCategories.registerEnablement(Tinkerer.ID+":"+key,()->TinkererConfig.KAMI.get()&&(!(key.equals("WARP_GATE")||key.equals("FOCUS_RECALL"))||tc4revival.config.TCConfig.SERVER.allowMirrors.get()));}
    @SubscribeEvent public static void drops(LivingDropsEvent event){
        var entity=event.getEntity();if(entity.level().isClientSide||!TinkererConfig.KAMI.get()||!(event.getSource().getEntity() instanceof Player))return;
        String dimension=entity.level().dimension().location().toString();String material=null;int chance=1;
        if(entity.getType()==EntityType.ENDERMAN&&dimension.equals(TinkererConfig.KAMI_END.get())){material="ender_shard";chance=32;}
        else if(entity.getType()==EntityType.ZOMBIFIED_PIGLIN&&dimension.equals(TinkererConfig.KAMI_NETHER.get())){material="nether_shard";chance=16;}
        if(material!=null&&entity.getRandom().nextInt(chance)==0)event.getDrops().add(new ItemEntity(entity.level(),entity.getX(),entity.getY(),entity.getZ(),new ItemStack(Tinkerer.KAMI_MATERIALS.get(material).get())));
    }
    private KamiContent(){}
}
