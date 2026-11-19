package automagy.content;

import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.wands.WandTriggerRegistry;

public final class MawCrafting {
    public static void register(){WandTriggerRegistry.register(ResourceLocation.parse("automagy:hungry_maw"),s->s.is(Blocks.OBSIDIAN),context->{
        if(!(context.getPlayer() instanceof ServerPlayer player))return InteractionResult.PASS;
        var level=context.getLevel();var pos=context.getClickedPos().below();
        if(!player.mayBuild()||!level.hasChunkAt(pos)||!level.mayInteract(player,pos)||!ThaumcraftApiHelper.isResearchComplete(player,"automagy:HUNGRYMAW"))return InteractionResult.PASS;
        if(!(tc4revival.content.WandFocusItem.attached(context.getItemInHand()).getItem() instanceof tc4revival.content.PortableHoleFocusItem)
                ||!(level.getBlockState(pos).getBlock() instanceof tc4revival.content.HungryChestBlock))return InteractionResult.PASS;
        var event=new net.minecraftforge.event.level.BlockEvent.BreakEvent(level,pos,level.getBlockState(pos),player);
        if(net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event))return InteractionResult.PASS;
        level.setBlock(pos,Content.MAW_PROCESS.get().defaultBlockState(),3);
        if(level.getBlockEntity(pos) instanceof MawProcessBlockEntity process){process.owner=player.getUUID();process.setChanged();}
        return InteractionResult.PASS;
    });}
    private MawCrafting(){}
}
