package conciliummodern;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thaumcraft.api.fx.LegacyEffects;

public final class VisCondenserBlockEntity extends BlockEntity {
    private int cooldown;
    public VisCondenserBlockEntity(BlockPos pos,BlockState state){super(ConciliumBlocks.CONDENSER_TILE.get(),pos,state);}
    public int cooldown(){return cooldown;}
    public static void tick(Level level,BlockPos pos,BlockState state,VisCondenserBlockEntity tile){
        if(tile.cooldown>0){tile.cooldown--;if(!level.isClientSide){tile.setChanged();if(tile.cooldown==0)tile.changed();}}
        if(!level.isClientSide && level.random.nextInt(20)==0 && level.getBlockEntity(pos.above()) instanceof DestabilizedCrystalBlockEntity)
            level.playSound(null,pos.above(),TCSounds.JACOBS.get(),SoundSource.BLOCKS,.01f,1);
    }
    public void activate(){
        if(level==null || level.isClientSide || cooldown>0 || !(level.getBlockEntity(worldPosition.above()) instanceof DestabilizedCrystalBlockEntity crystal) || crystal.amount()<=0)return;
        for(var input:new Direction[]{Direction.EAST,Direction.WEST,Direction.SOUTH,Direction.NORTH}) {
            var neighbor=worldPosition.relative(input);if(!level.hasChunkAt(neighbor))continue;
            int power=level.getSignal(neighbor,input);if(power<=0)continue;
            shoot(crystal,input.getOpposite(),Math.min(15,power));break;
        }
        if(crystal.amount()>0 && level.getSignal(worldPosition.below(),Direction.DOWN)>0)sendEssentia(crystal);
    }
    private void shoot(DestabilizedCrystalBlockEntity crystal,Direction direction,int power){
        var start=Vec3.atCenterOf(crystal.getBlockPos());var end=start.add(Vec3.atLowerCornerOf(direction.getNormal()).scale(power));
        // A straight 15-block beam must not load adjacent chunks just to resolve its hit.
        for(int i=1;i<=power;i++)if(!level.hasChunkAt(crystal.getBlockPos().relative(direction,i))){end=start.add(Vec3.atLowerCornerOf(direction.getNormal()).scale(i-1));break;}
        if(start.distanceToSqr(end)<.26)return;
        var hit=level.clip(new ClipContext(start.add(Vec3.atLowerCornerOf(direction.getNormal()).scale(.50001)),end,ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,null));
        if(hit.getType()!=HitResult.Type.MISS)end=hit.getLocation();
        int spent=crystal.drain(crystal.aspectKey(),Math.min(16,crystal.amount()));if(spent<=0)return;
        int damage=net.minecraft.util.Mth.clamp(spent+1-power,0,16);
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(start,end).inflate(.6,.3,.6))) {
            if(!target.isAlive())continue;
            // Broad-phase inflation must not let the beam damage creatures behind its stopping wall.
            var sight=level.clip(new ClipContext(start.add(Vec3.atLowerCornerOf(direction.getNormal()).scale(.50001)),target.getBoundingBox().getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,target));
            if(sight.getType()!=HitResult.Type.MISS)continue;
            boolean hurt=target.hurt(level.damageSources().magic(),damage);
            if(hurt && target.getHealth()<=0)tc4revival.aspect.entity.EntityAspects.resolve(target).ifPresent(result->{
                var aspects=result.aspects().aspects();if(aspects.isEmpty())return;
                var essence=new ItemStack(TCItems.WISP_ESSENCE.get());var amount=new AspectAmounts();amount.add(aspects.get(level.random.nextInt(aspects.size())),2);
                ((StoredAspectItem)essence.getItem()).setStoredAspects(essence,amount);target.spawnAtLocation(essence,.2f);
            });
        }
        flash(start,end,crystal.aspectKey());cooldown=40;changed();
    }
    private void sendEssentia(DestabilizedCrystalBlockEntity crystal){
        for(var pos:BlockPos.betweenClosed(crystal.getBlockPos().offset(-8,-8,-8),crystal.getBlockPos().offset(8,8,8))) {
            if(!level.hasChunkAt(pos))continue;
            if(level.getBlockEntity(pos) instanceof CondenserReceiver receiver && receiver.receiveCrystal(crystal)) {
                flash(Vec3.atCenterOf(crystal.getBlockPos()),Vec3.atCenterOf(pos).add(0,1,0),crystal.aspectKey());return;
            }
        }
    }
    private void flash(Vec3 start,Vec3 end,String aspect){
        level.playSound(null,worldPosition,TCSounds.SHOCK.get(),SoundSource.BLOCKS,.8f,.9f+level.random.nextFloat()*.1f);
        ConciliumArcNetwork.emit(level,start,end,TCAspects.catalog().find(aspect).map(Aspect::color).orElse(0xffffff),.01f);
    }
    private void changed(){setChanged();if(level!=null && !level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("cooldown",cooldown);}
    @Override public void load(CompoundTag tag){super.load(tag);cooldown=Math.max(0,Math.min(40,tag.getInt("cooldown")));}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void onDataPacket(Connection connection,ClientboundBlockEntityDataPacket packet){if(packet.getTag()!=null)load(packet.getTag());}
}
