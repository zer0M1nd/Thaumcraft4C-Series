package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

/** Water bursts use the actual impact location, including entity hits and negative coordinates. */
public final class ClearWaterProjectile extends ThrowableItemProjectile {
    public ClearWaterProjectile(EntityType<? extends ClearWaterProjectile> type,Level level) { super(type,level); }
    @Override protected Item getDefaultItem() { return ConciliumItems.CLEAR_WATER.get(); }
    @Override protected float getGravity() { return .05f; }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override protected void onHit(HitResult hit) {
        super.onHit(hit);
        if(level().isClientSide)return;
        for(var entity:level().getEntities(this,getBoundingBox().inflate(5))) {
            entity.clearFire();
            if(entity instanceof Blaze || entity instanceof MagmaCube)entity.hurt(damageSources().thrown(this,getOwner()),10);
        }
        var impact=BlockPos.containing(hit.getLocation());
        for(int x=-3;x<3;x++)for(int z=-3;z<3;z++)for(int y=0;y<=1;y++) {
            var pos=impact.offset(x,y,z);
            if(!level().hasChunkAt(pos) || !(level().getBlockState(pos).getBlock() instanceof BaseFireBlock))continue;
            if(getOwner() instanceof ServerPlayer player) {
                if(!level().mayInteract(player,pos) || !player.mayUseItemAt(pos,Direction.UP,getItem())
                        || net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level(),player.gameMode.getGameModeForPlayer(),player,pos)==-1)continue;
            } else if(!net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level(),getOwner()==null?this:getOwner()))continue;
            level().removeBlock(pos,false);
        }
        level().levelEvent(2002,impact,0x385dc6);
        discard();
    }
}
