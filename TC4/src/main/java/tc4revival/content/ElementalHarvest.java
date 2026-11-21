package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;

/** Harvest without invoking the held tool again (which would recurse into area harvesting). */
public  class ElementalHarvest {
    private ElementalHarvest() {}
    public static boolean harvest(ServerPlayer player,BlockPos pos,int effect) {
        return harvest(player,pos,true,effect);
    }
    public static boolean harvest(ServerPlayer player,BlockPos pos,boolean followItems,int effect) {
        var level=player.serverLevel();
        if (!level.hasChunkAt(pos) || !level.mayInteract(player,pos)
                || player.blockActionRestricted(level,pos,player.gameMode.getGameModeForPlayer())) return false;
        var state=level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level,pos)<0
                || ForgeHooks.onBlockBreakEvent(level,player.gameMode.getGameModeForPlayer(),player,pos)<0) return false;
        var entity=level.getBlockEntity(pos); var tool=player.getMainHandItem().copy();
        boolean drops=!player.isCreative() && state.canHarvestBlock(level,pos,player);
        state.getBlock().playerWillDestroy(level,pos,state,player);
        boolean removed=state.onDestroyedByPlayer(level,pos,player,drops,level.getFluidState(pos));
        if (!removed) return false;
        state.getBlock().destroy(level,pos,state);
        level.levelEvent(2001,pos,Block.getId(state));
        if (drops) {
            state.getBlock().playerDestroy(level,player,pos,state,entity,tool);
            if (!followItems) return true;
            var center=Vec3.atCenterOf(pos);
            for (var item:level.getEntitiesOfClass(ItemEntity.class,new AABB(center,center).inflate(2),
                    item->item.isAlive() && item.tickCount==0 && !(item instanceof FollowingItemEntity))) {
                var following=TCEntities.FOLLOWING_ITEM.get().create(level);
                if (following==null) continue;
                following.setPos(item.position()); following.setItem(item.getItem().copy());
                following.setDeltaMovement(item.getDeltaMovement()); following.follow(player,effect);
                if (level.addFreshEntity(following)) item.discard();
            }
        }
        return true;
    }
}
