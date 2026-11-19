package automagy;

import automagy.content.Content;
import automagy.content.EnchantedIngredient;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.resources.ResourceLocation;

@Mod(Automagy.ID)
public final class Automagy {
    public static final String ID = "automagy";
    public Automagy() {
        RevivalFeatures.register();
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,
                new thaumcraft.api.config.MigratingConfigSpec(AutomagyConfig.SPEC)
                        .existingDefault("nethermind.ignoreSection",false,"nethermind")
                        .existingDefault("compatibility.botaniaBlacklistPhialXP",true,"netherHeart"));
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,LootConfig.SPEC);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,CreativeToolsConfig.SPEC,"automagy-creative-tools.toml");
        var bus=FMLJavaModLoadingContext.get().getModEventBus();
        Content.register(bus);
        bus.addListener(this::setup);
        automagy.content.FilterNetwork.register();
    }
    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(()->{
            automagy.content.AdvancedNodeJarring.register();
            thaumcraft.api.entities.PechTradeValues.register(ResourceLocation.parse("automagy:mercatus_amalgam"),0,Content.MERCATUS_AMALGAM.get(),7);
            thaumcraft.api.crafting.ArcaneResultTransforms.register(ResourceLocation.parse("automagy:preserve_filter"),(inputs,width,height,registries,base,config)->{
                var filters=inputs.stream().filter(stack->stack.getItem() instanceof automagy.content.FilterItem).toList();
                if(filters.isEmpty())return net.minecraft.world.item.ItemStack.EMPTY;
                var result=config.has("copy")&&config.get("copy").getAsBoolean()&&filters.size()>1?new net.minecraft.world.item.ItemStack(filters.get(1).getItem(),base.getCount()):base;
                result.getOrCreateTag().put("Filter",automagy.content.ItemFilter.load(filters.get(0)).save());return result;
            });
            var pearls=new net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior(){
                @Override protected net.minecraft.world.entity.projectile.Projectile getProjectile(net.minecraft.world.level.Level level,net.minecraft.core.Position position,net.minecraft.world.item.ItemStack stack){return automagy.content.AvaricePearlEntity.dispensed(level,position,stack);}
            };
            net.minecraft.world.level.block.DispenserBlock.registerBehavior(Content.AVARICE_PEARL.get(),pearls);
            net.minecraft.world.level.block.DispenserBlock.registerBehavior(Content.MISERS_PEARL.get(),pearls);
            net.minecraftforge.common.crafting.CraftingHelper.register(ResourceLocation.fromNamespaceAndPath(ID,"enchanted"),EnchantedIngredient.SERIALIZER);
            thaumcraft.api.research.ScanRegistry.register(ResourceLocation.fromNamespaceAndPath(ID,"nether_wisp"),new thaumcraft.api.research.ScanRegistry.Handler(){
                @Override public java.util.Optional<tc4revival.research.ScanTarget> entity(net.minecraft.world.entity.Entity entity){
                    if(!(entity instanceof automagy.content.NetherWispEntity))return java.util.Optional.empty();
                    var extra=OptionalAspects.nether();if(extra.isEmpty())return java.util.Optional.empty();
                    return tc4revival.aspect.entity.EntityAspects.resolve(entity).map(base->new tc4revival.research.ScanTarget(
                        new tc4revival.research.ScanTarget.Entity(entity.getUUID()),tc4revival.research.PlayerKnowledge.ScanKind.ENTITY,
                        java.util.Optional.of(base.legacyIdentity()),base.aspects().copy().add(extra.get(),1)));
                }
            });
            thaumcraft.api.ThaumcraftApi.addWarpToItem(ResourceLocation.fromNamespaceAndPath(ID,"nethermind"),2);
            thaumcraft.api.ThaumcraftApi.addWarpToItem(ResourceLocation.fromNamespaceAndPath(ID,"mob_lure"),2);
            thaumcraft.api.ThaumcraftApi.addWarpToItem(ResourceLocation.fromNamespaceAndPath(ID,"experience_stone"),1);
            thaumcraft.api.ThaumcraftApi.addWarpToItem(ResourceLocation.fromNamespaceAndPath(ID,"dimension_lure"),2);
        });
    }
}
