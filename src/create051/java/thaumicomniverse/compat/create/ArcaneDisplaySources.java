package thaumicomniverse.compat.create;

import com.simibubi.create.content.redstone.displayLink.source.DisplaySource;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraftforge.registries.*;
import tc4revival.content.*;
import tc4revival.aspect.AspectAmounts;
import thaumcraft.api.aspects.EssentiaStorageApi;

public class ArcaneDisplaySources extends DisplaySource {
    public static final java.util.function.Supplier<ArcaneDisplaySources> ESSENTIA=com.google.common.base.Suppliers.memoize(ArcaneDisplaySources::new);
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) { /* 0.5 registers during common setup below. */ }
    public static void setup(){
        com.simibubi.create.content.redstone.displayLink.AllDisplayBehaviours.register(net.minecraft.resources.ResourceLocation.parse("thaumicomniverse:essentia"),ESSENTIA.get());
        for(var block:ForgeRegistries.BLOCKS)if(ArcaneContraptions.thaumic(block.defaultBlockState())&&block instanceof EntityBlock factory){
            var tile=factory.newBlockEntity(BlockPos.ZERO,block.defaultBlockState());
            if(tile instanceof RevealedAspectContainer||tile instanceof WandCharger.Tile||tile instanceof EssentiaSwitch.Tile||EssentiaStorageApi.supports(tile,null))com.simibubi.create.content.redstone.displayLink.AllDisplayBehaviours.assignBlock(ESSENTIA.get(),block);
        }
    }
    @Override public java.util.List<MutableComponent> provideText(DisplayLinkContext context,DisplayTargetStats stats){
        if(!ArcaneContraptions.enabled())return EMPTY;
        var tile=context.getSourceBlockEntity();AspectAmounts aspects=new AspectAmounts();
        if(tile instanceof RevealedAspectContainer container)aspects=container.revealedAspects();
        else if(tile instanceof WandCharger.Tile charger){var stack=charger.inventory.getStackInSlot(0);if(stack.getItem() instanceof CastingWandItem wand)for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())aspects.add(aspect,wand.vis(stack,aspect)/100);}
        else if(tile instanceof EssentiaSwitch.Tile detector)return java.util.List.of(detector.format(Math.max(0,detector.currentLevel),false));
        else if(tile!=null&&tile.getLevel() instanceof net.minecraft.server.level.ServerLevel server)aspects=EssentiaStorageApi.atInventory(server,tile.getBlockPos()).contents();
        return aspects.view().entrySet().stream().sorted(java.util.Comparator.comparing(e->e.getKey().tag())).limit(stats.maxRows())
            .map(e->Component.literal(e.getKey().displayName()+" "+e.getValue())).toList();
    }
    @Override public int getPassiveRefreshTicks(){return 20;}
}
