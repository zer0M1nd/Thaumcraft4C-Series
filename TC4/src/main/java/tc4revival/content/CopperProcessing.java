package tc4revival.content;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeHooks;

/** Native copper transitions retain slab/stair properties and any wax coating. */
public final class CopperProcessing {
    private CopperProcessing() {}
    public static Optional<BlockState> deoxidized(BlockState state) {
        var unwrapped=HoneycombItem.WAX_OFF_BY_BLOCK.get().get(state.getBlock());
        var bare=unwrapped==null?state:unwrapped.withPropertiesOf(state);
        return WeatheringCopper.getPrevious(bare).map(previous->{
            if(unwrapped==null)return previous;
            var waxed=HoneycombItem.WAXABLES.get().get(previous.getBlock());
            return waxed==null?state:waxed.withPropertiesOf(previous);
        });
    }
    public static boolean shock(ServerPlayer player,ItemStack wand,BlockHitResult hit) {
        var level=player.serverLevel();var pos=hit.getBlockPos();
        if(!level.hasChunkAt(pos)||!player.mayBuild()||!level.mayInteract(player,pos)
                ||!player.mayUseItemAt(pos,hit.getDirection(),wand)
                ||player.blockActionRestricted(level,pos,player.gameMode.getGameModeForPlayer()))return false;
        var before=level.getBlockState(pos);var after=deoxidized(before);
        if(after.isEmpty()||ForgeHooks.onBlockBreakEvent(level,player.gameMode.getGameModeForPlayer(),player,pos)<0
                ||!level.getBlockState(pos).equals(before))return false;
        if(!level.setBlock(pos,after.get(),3))return false;
        level.levelEvent(3005,pos,0);return true;
    }
}
