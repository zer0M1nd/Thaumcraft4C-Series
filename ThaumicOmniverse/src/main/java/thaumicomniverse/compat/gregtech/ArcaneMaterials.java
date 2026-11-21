package thaumicomniverse.compat.gregtech;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.event.*;
import com.gregtechceu.gtceu.api.data.chemical.material.info.*;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.*;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import net.minecraftforge.eventbus.api.IEventBus;
import tc4revival.content.*;
import java.util.*;
import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.*;

/** Native GT forms use the original Thaumcraft ingots and shards where those already exist. */
public final class ArcaneMaterials {
    public static Material THAUMIUM, VOID, AMBER, BRASS, ICHORIUM, THAUMINITE;
    public static final Map<ShardType,Material> INFUSED=new LinkedHashMap<>();
    private static final Map<net.minecraft.resources.ResourceLocation,java.util.function.Supplier<? extends net.minecraft.world.level.ItemLike>> REUSED=new LinkedHashMap<>();
    public static void register(IEventBus bus) {
        bus.addListener((MaterialRegistryEvent event)->GTCEuAPI.materialManager.createRegistry("thaumicomniverse"));
        bus.addListener((MaterialEvent event)->create());
    }
    private static void create() {
        THAUMIUM=new Material.Builder(ArcaneGT.id("thaumium")).langValue("Thaumium").ingot(3).liquid(1200).color(0x8062B0)
                .iconSet(MaterialIconSet.METALLIC).flags(GENERATE_PLATE,GENERATE_ROD,GENERATE_BOLT_SCREW,GENERATE_FRAME,GENERATE_GEAR,GENERATE_LONG_ROD)
                .toolStats(ToolProperty.Builder.of(8,3,768,3).enchantability(25).build()).buildAndRegister();
        VOID=new Material.Builder(ArcaneGT.id("void")).langValue("Void Metal").ingot(4).liquid(1800).color(0x44335F)
                .iconSet(MaterialIconSet.METALLIC).flags(GENERATE_PLATE,GENERATE_ROD,GENERATE_BOLT_SCREW,GENERATE_FRAME,GENERATE_GEAR,GENERATE_LONG_ROD)
                .toolStats(ToolProperty.Builder.of(10,4,1536,4).enchantability(25).build()).buildAndRegister();
        // Railcraft supplies these two IDs with its native cart linking/rotation behaviour.
        if(net.minecraftforge.fml.ModList.get().isLoaded("railcraft") && net.minecraftforge.fml.ModList.get().isLoaded("thaumicfutures")) {
            THAUMIUM.getProperty(PropertyKey.TOOL).removeTypes(GTToolType.CROWBAR);
            VOID.getProperty(PropertyKey.TOOL).removeTypes(GTToolType.CROWBAR);
        }
        // Farmer's Delight already owns these IDs and supplies its cutting-board tool behaviour.
        if(net.minecraftforge.fml.ModList.get().isLoaded("farmersdelight")) {
            THAUMIUM.getProperty(PropertyKey.TOOL).removeTypes(GTToolType.KNIFE);
            VOID.getProperty(PropertyKey.TOOL).removeTypes(GTToolType.KNIFE);
        }
        reuse(TagPrefix.ingot,THAUMIUM,()->TCItems.material(MaterialType.THAUMIUM));
        reuse(TagPrefix.ingot,VOID,()->TCItems.material(MaterialType.VOID));
        reuse(TagPrefix.nugget,THAUMIUM,()->TCItems.nugget(NuggetType.THAUMIUM));
        reuse(TagPrefix.nugget,VOID,()->TCItems.nugget(NuggetType.VOID));
        reuse(TagPrefix.block,THAUMIUM,()->TCItems.THAUMIUM_BLOCK.get());
        if(net.minecraftforge.fml.ModList.get().isLoaded("thaumicbases"))
            reuse(TagPrefix.block,VOID,"thaumicbases:void_block");
        else if(net.minecraftforge.fml.ModList.get().isLoaded("witchinggadgets"))
            reuse(TagPrefix.block,VOID,"witchinggadgets:void_metal_block");
        else if(net.minecraftforge.fml.ModList.get().isLoaded("allthemodium"))
            reuse(TagPrefix.block,VOID,()->thaumicomniverse.compat.AtmCompat.baseBlock("void"));
        if(net.minecraftforge.fml.ModList.get().isLoaded("emt"))reuse(TagPrefix.plate,THAUMIUM,"emt:thaumium_plate");
        AMBER=new Material.Builder(ArcaneGT.id("amber")).langValue("Amber").gem(1).ore().color(0xDC9B32)
                .iconSet(MaterialIconSet.GEM_HORIZONTAL).flags(GENERATE_PLATE,GENERATE_LENS,DISABLE_DECOMPOSITION)
                .addOreByproducts(GTMaterials.Gold,GTMaterials.Sulfur,GTMaterials.Coal).buildAndRegister();
        reuse(TagPrefix.gem,AMBER,()->TCItems.material(MaterialType.AMBER));
        reuse(TagPrefix.block,AMBER,()->TCItems.AMBER_BLOCK.get());
        reuse(TagPrefix.ore,AMBER,()->TCArcane.ore(7));
        reuse(TagPrefix.oreDeepslate,AMBER,()->TCArcane.deepslateOre(7));
        Material[] byproducts={GTMaterials.Quartzite,GTMaterials.Sulfur,GTMaterials.Sapphire,GTMaterials.Olivine,GTMaterials.CertusQuartz,GTMaterials.Coal};
        int[] durability={256,384,320,512,768,192};
        float[] speed={12,9,8,6,10,14};
        for(int index=0;index<6;index++) {
            var shard=ShardType.values()[index];
            var material=new Material.Builder(ArcaneGT.id("infused_"+shard.material())).langValue("Infused "+shard.material().substring(0,1).toUpperCase(Locale.ROOT)+shard.material().substring(1))
                    .gem(3).ore(true).color(shard.tint()&0xffffff).iconSet(MaterialIconSet.QUARTZ)
                    .flags(GENERATE_PLATE,GENERATE_ROD,GENERATE_LENS,GENERATE_BOLT_SCREW,DISABLE_DECOMPOSITION)
                    .addOreByproducts(byproducts[index],AMBER,GTMaterials.Redstone)
                    .toolStats(ToolProperty.Builder.of(speed[index],3+index%3,durability[index],3,
                            GTToolType.PICKAXE,GTToolType.AXE,GTToolType.SHOVEL,GTToolType.SWORD,GTToolType.HOE,
                            GTToolType.MINING_HAMMER,GTToolType.HARD_HAMMER,GTToolType.SAW,GTToolType.FILE,
                            GTToolType.SCYTHE,GTToolType.KNIFE,GTToolType.CROWBAR).enchantability(25).build()).buildAndRegister();
            INFUSED.put(shard,material);reuse(TagPrefix.gem,material,()->TCItems.shard(shard));
            reuse(TagPrefix.ore,material,()->TCArcane.ore(shard.metadata()+1));
            reuse(TagPrefix.oreDeepslate,material,()->TCArcane.deepslateOre(shard.metadata()+1));
        }
        if(net.minecraftforge.fml.ModList.get().isLoaded("thaumic_tinkerer")) {
            ICHORIUM=metal("ichorium","Ichorium",0xEAD280,"thaumic_tinkerer:ichorium");
            reuse(TagPrefix.nugget,ICHORIUM,"thaumic_tinkerer:ichor_nugget");
        }
        if(net.minecraftforge.fml.ModList.get().isLoaded("thaumicbases")) {
            THAUMINITE=metal("thauminite","Thauminite",0x7DCEB5,"thaumicbases:thauminite_ingot");
            reuse(TagPrefix.nugget,THAUMINITE,"thaumicbases:thauminite_nugget");
            reuse(TagPrefix.block,THAUMINITE,"thaumicbases:thauminite_block");
        }
    }
    /** GT creates addon tag prefixes after material events, before it generates their items. */
    static void reuseClusters() {
        // A prefix can reuse an existing item while still participating in GT tags and processing.
        reuse(ArcaneGT.CLUSTER,GTMaterials.Iron,()->TCItems.cluster(ClusterType.IRON));
        reuse(ArcaneGT.CLUSTER,GTMaterials.Gold,()->TCItems.cluster(ClusterType.GOLD));
        reuse(ArcaneGT.CLUSTER,GTMaterials.Copper,()->TCItems.cluster(ClusterType.COPPER));
        if(net.minecraftforge.fml.ModList.get().isLoaded("witchinggadgets")) {
            // Only equivalent ingot-producing clusters; cinnabar has a different TC smelting product.
            for(String name:List.of("cobalt","nickel","manganese","zinc","platinum")) {
                var material=GTCEuAPI.materialManager.getMaterial("gtceu:"+name);
                if(material!=null)reuse(ArcaneGT.CLUSTER,material,"witchinggadgets:"+name+"_cluster");
            }
        }
    }
    private static void reuse(TagPrefix prefix,Material material,String id) {
        reuse(prefix,material,()->java.util.Objects.requireNonNull(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse(id)),id));
    }
    private static void reuse(TagPrefix prefix,Material material,java.util.function.Supplier<? extends net.minecraft.world.level.ItemLike> item) {
        // GT's early loot/worldgen lookup identifies block suppliers before item tags exist.
        // generateBlock is configured later than MaterialEvent for ore prefixes.
        if(prefix==TagPrefix.block||prefix==TagPrefix.ore||prefix==TagPrefix.oreDeepslate)prefix.setIgnored(material,com.gregtechceu.gtceu.utils.memoization.GTMemoizer.memoizeBlockSupplier(
                ()->net.minecraft.world.level.block.Block.byItem(item.get().asItem())));
        else prefix.setIgnored(material,item);
        REUSED.put(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(material.getModid(),prefix.idPattern().formatted(material.getName())),item);
    }
    public static void missingMappings(net.minecraftforge.registries.MissingMappingsEvent event) {
        for(String namespace:List.of("thaumicomniverse","gtceu")) {
            for(var mapping:event.getMappings(net.minecraftforge.registries.ForgeRegistries.Keys.ITEMS,namespace)) {
                var replacement=REUSED.get(mapping.getKey());if(replacement!=null)mapping.remap(replacement.get().asItem());
            }
            for(var mapping:event.getMappings(net.minecraftforge.registries.ForgeRegistries.Keys.BLOCKS,namespace)) {
                var replacement=REUSED.get(mapping.getKey());if(replacement==null)continue;
                var block=net.minecraft.world.level.block.Block.byItem(replacement.get().asItem());
                if(block!=net.minecraft.world.level.block.Blocks.AIR)mapping.remap(block);
            }
        }
    }
    private static Material metal(String id,String name,int color,String ingot) {
        var material=new Material.Builder(ArcaneGT.id(id)).langValue(name).ingot().color(color).iconSet(MaterialIconSet.METALLIC)
                .flags(GENERATE_PLATE,GENERATE_ROD,GENERATE_BOLT_SCREW,GENERATE_GEAR,DISABLE_DECOMPOSITION).buildAndRegister();
        reuse(TagPrefix.ingot,material,ingot);
        return material;
    }
    private ArcaneMaterials() {}
}
