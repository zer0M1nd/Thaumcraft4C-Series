package ttmodern;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class MobilizerRelayEntity extends BlockEntity {
    private BlockPos partner;
    public MobilizerRelayEntity(BlockPos pos,BlockState state) { super(Tinkerer.MOBILIZER_RELAY_ENTITY.get(),pos,state); }
    public BlockPos partner() { return partner; }
    static boolean validTrack(BlockPos a,BlockPos b) { return a.getY()==b.getY()&&!a.equals(b)&&(a.getX()==b.getX()||a.getZ()==b.getZ())&&a.distManhattan(b)<=32; }
    public boolean verify() {
        if(partner==null||level==null)return false;
        if(!validTrack(worldPosition,partner)){partner=null;setChanged();return false;}
        if(!level.hasChunkAt(partner))return false;
        if(!(level.getBlockEntity(partner) instanceof MobilizerRelayEntity other)||!worldPosition.equals(other.partner)){partner=null;setChanged();return false;}
        return true;
    }
    public boolean pair(MobilizerRelayEntity other) {
        if(level==null||level!=other.level||!validTrack(worldPosition,other.worldPosition)||partner!=null||other.partner!=null)return false;
        partner=other.worldPosition.immutable();other.partner=worldPosition.immutable();setChanged();other.setChanged();return true;
    }
    public void tick() {
        if(!(level instanceof ServerLevel server))return;
        boolean linked=verify();if(server.getGameTime()%40!=0)return;
        if(!linked&&partner==null)outer:for(int distance=1;distance<=32;distance++)for(var direction:Direction.Plane.HORIZONTAL) {
            var pos=worldPosition.relative(direction,distance);if(!server.hasChunkAt(pos))continue;
            if(server.getBlockEntity(pos) instanceof MobilizerRelayEntity other){other.verify();if(pair(other)){linked=true;break outer;}}
        }
        if(!linked||!verify())return;
        int dx=Integer.signum(partner.getX()-worldPosition.getX()),dz=Integer.signum(partner.getZ()-worldPosition.getZ());
        for(int step=1;step<worldPosition.distManhattan(partner);step++) {
            var pos=worldPosition.offset(dx*step,0,dz*step);if(!server.hasChunkAt(pos))continue;
            if(server.getBlockEntity(pos) instanceof MobilizerEntity mover)mover.link(worldPosition,partner);
            thaumcraft.api.fx.LegacyEffects.sparkle(server,pos.getCenter(),0x5555ee,1.5f,1,0);
        }
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag);if(partner!=null)tag.putLong("Partner",partner.asLong()); }
    @Override public void load(CompoundTag tag) { super.load(tag);partner=tag.contains("Partner",Tag.TAG_LONG)?BlockPos.of(tag.getLong("Partner")):null;if(partner!=null&&!validTrack(worldPosition,partner))partner=null; }
}
