package emtmodern.item;

import net.minecraft.core.*;
import net.minecraft.sounds.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

public class LegacyHoeTilling {
    public static boolean till(UseOnContext context,BlockPos pos,boolean mycelium){
        var player=context.getPlayer();var level=context.getLevel();if(player==null||level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos)||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand()))return false;
        var local=new UseOnContext(player,context.getHand(),new BlockHitResult(Vec3.atCenterOf(pos),context.getClickedFace(),pos,false));
        var state=level.getBlockState(pos);
        var eventState=net.minecraftforge.event.ForgeEventFactory.onToolUse(state,local,net.minecraftforge.common.ToolActions.HOE_TILL,false);
        if(eventState==null)return false;
        if(eventState!=state){if(!level.isClientSide)level.setBlock(pos,eventState,3);return true;}
        if(context.getClickedFace()==Direction.DOWN||!level.isEmptyBlock(pos.above()))return false;
        if(!state.is(Blocks.DIRT)&&!state.is(Blocks.GRASS_BLOCK)&&!state.is(Blocks.COARSE_DIRT)&&!state.is(Blocks.PODZOL)&&!(mycelium&&state.is(Blocks.MYCELIUM)))return false;
        if(!level.isClientSide){level.playSound(null,pos,SoundEvents.GRAVEL_PLACE,SoundSource.BLOCKS,1,.8f);level.setBlock(pos,Blocks.FARMLAND.defaultBlockState(),3);}
        return true;
    }
}
