package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tc4revival.content.*;

public final class MaterialPeeler extends Entity {
    private BlockPos center;
    private final int initialRotation=random.nextInt(100000);
    public int innerRotation(){return initialRotation+tickCount;}
    public MaterialPeeler(EntityType<? extends MaterialPeeler> type,Level level){super(type,level);noPhysics=true;}
    public void bind(BlockPos pos){center=pos.immutable();}
    @Override protected void defineSynchedData(){}
    @Override public boolean isPickable(){return !isRemoved();}
    @Override public boolean isPushable(){return false;}
    @Override public void move(MoverType type,Vec3 delta){}
    @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel server))return;
        if(tickCount%300==0)playSound(TCSounds.EVIL_PORTAL.get(),Math.max(.01f,(float)(.15+random.nextGaussian()*.066)),(float)(.75+random.nextGaussian()*.1));
        if(tickCount%5==0 && (center==null || server.hasChunkAt(center) && (!(server.getBlockEntity(center) instanceof PredictabilityBlockEntity tile) || !tile.isMaster())))collapse();
    }
    @Override public void playerTouch(Player player){
        if(tickCount%20!=0 || !(player instanceof ServerPlayer server) || EtherealState.robe(player))return;
        EtherealState.toggle(server);player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION,200,10));
        tc4revival.network.FocusEffectPacket.burst(level(),player.position(),2);playSound(TCSounds.WAND_FAIL.get(),1,1);
    }
    public void collapse(){
        if(isRemoved() || !(level() instanceof ServerLevel server))return;
        tc4revival.network.FocusEffectPacket.burst(level(),position(),2);playSound(TCSounds.CRAFT_FAIL.get(),1,1);discard();
        if(!net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(server,this))return;
        var origin=BlockPos.containing(getX()+random.nextGaussian()*1.5,getY(),getZ()+random.nextGaussian()*1.5);
        for(int i=0;i<10;i++){var pos=origin.offset((int)((random.nextFloat()-random.nextFloat())*5),0,(int)((random.nextFloat()-random.nextFloat())*5));
            if(!random.nextBoolean() || !canChange(server,pos) || server.getBiome(pos).is(TCBiomes.TAINTED_LAND))continue;
            if(!WorldBiomeChanges.changeColumn(server,pos,TCBiomes.TAINTED_LAND))continue;
            if(server.getBlockState(pos.below()).isRedstoneConductor(server,pos.below()) && server.getBlockState(pos).canBeReplaced())server.setBlock(pos,TaintFibreBlock.faces(TCArcane.TAINT_FIBRES.get().defaultBlockState(),server,pos),3);
            if(canChange(server,pos.above()) && server.isEmptyBlock(pos.above()))server.setBlock(pos.above(),(random.nextBoolean()?TCArcane.FLUX_GAS.get():TCArcane.FLUX_GOO.get()).defaultBlockState(),3);
        }
    }
    private boolean canChange(ServerLevel level,BlockPos pos){return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos)
        && !net.minecraftforge.event.ForgeEventFactory.onBlockPlace(this,net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos),net.minecraft.core.Direction.UP);}
    @Override protected void addAdditionalSaveData(CompoundTag tag){if(center!=null)tag.putLong("BoundCenter",center.asLong());}
    @Override protected void readAdditionalSaveData(CompoundTag tag){center=tag.contains("BoundCenter")?BlockPos.of(tag.getLong("BoundCenter")):null;}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
