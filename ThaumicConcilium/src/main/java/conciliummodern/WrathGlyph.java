package conciliummodern;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

/** Original flame glyph, emitted on simulation ticks instead of once per rendered frame. */
public final class WrathGlyph extends Entity {
    private static final int[][] POINTS={{0,5},{1,4},{2,3},{3,3},{4,3},{5,3},{6,3},{7,3},{8,3},{9,3},{10,3},{11,3},{12,4},{13,4},{14,1},{14,5},{14,10},{14,12},{15,2},{15,12},{16,3},{16,13},{17,1},{17,4},{17,9},{17,14},{18,2},{18,5},{18,7},{18,9},{18,13},{18,16},{19,3},{19,11},{19,14},{20,3},{20,12},{20,14},{21,3},{21,12},{21,14},{22,3},{22,12},{22,14},{23,3},{25,2},{26,1},{24,3},{23,2},{22,1}};
    private static final EntityDataAccessor<Boolean> BURNOUT=SynchedEntityData.defineId(WrathGlyph.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(WrathGlyph.class,EntityDataSerializers.INT);
    public WrathGlyph(EntityType<? extends WrathGlyph> type,Level level){super(type,level);noPhysics=true;}
    public static void spawn(Level level,Entity actor,boolean burnout){
        var glyph=new WrathGlyph(ConciliumEntities.WRATH_GLYPH.get(),level);glyph.setPos(actor.position());glyph.setYRot(actor.getYRot());glyph.entityData.set(BURNOUT,burnout);level.addFreshEntity(glyph);
    }
    @Override protected void defineSynchedData(){entityData.define(BURNOUT,false);entityData.define(AGE,0);}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
    @Override public void tick(){
        super.tick();int age=entityData.get(AGE);
        if(!level().isClientSide){if(age>=POINTS.length){discard();return;}entityData.set(AGE,age+2);return;}
        for(int i=age;i<Math.min(age+2,POINTS.length);i++)for(int side:new int[]{-1,1}){
            double yaw=Math.toRadians(getYRot()),offset=POINTS[i][1]*.3*side;
            double x=getX()-Math.cos(yaw)*offset,y=getY()+POINTS[i][0]*.2,z=getZ()-Math.sin(yaw)*offset;
            level().addParticle(ParticleTypes.FLAME,x,y,z,0,0,0);
            if(age>20 && entityData.get(BURNOUT))level().addParticle(ParticleTypes.LAVA,x,y,z,0,0,0);
        }
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){tag.putBoolean("Burnout",entityData.get(BURNOUT));tag.putInt("Age",entityData.get(AGE));}
    @Override protected void readAdditionalSaveData(CompoundTag tag){entityData.set(BURNOUT,tag.getBoolean("Burnout"));entityData.set(AGE,Math.max(0,tag.getInt("Age")));}
}
