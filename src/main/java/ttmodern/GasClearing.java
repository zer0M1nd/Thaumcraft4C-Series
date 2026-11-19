package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public final class GasClearing {
    public static int clear(ServerLevel world,BlockPos origin,Player owner){int cleared=0;
        for(var pos:BlockPos.betweenClosed(origin.offset(-3,-3,-3),origin.offset(2,2,2))){
            if(!world.hasChunkAt(pos)||!world.mayInteract(owner,pos))continue;var state=world.getBlockState(pos);if(!(state.getBlock() instanceof GasBlock))continue;
            var event=new net.minecraftforge.event.level.BlockEvent.BreakEvent(world,pos.immutable(),state,owner);if(net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event))continue;
            if(world.getBlockState(pos)!=state)continue;if(world.removeBlock(pos,false)){cleared++;}
        }return cleared;
    }
    private GasClearing(){}
}
