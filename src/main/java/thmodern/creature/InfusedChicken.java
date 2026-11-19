package thmodern.creature;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import thmodern.Horizons;

/** Keeps vanilla movement/animation, replacing only the adult production schedule. */
public final class InfusedChicken extends Chicken {
    private int featherTime;
    public InfusedChicken(EntityType<? extends Chicken> type,Level level){super(type,level);featherTime=4000+random.nextInt(4000);}
    public boolean golden(){return getType()==Horizons.GOLD_CHICKEN.get();}
    @Override public void aiStep(){
        int remaining=eggTime;eggTime=Integer.MAX_VALUE;super.aiStep();eggTime=remaining;
        if(level().isClientSide){
            if(golden()&&random.nextFloat()>.95f)thaumcraft.api.fx.LegacyEffects.sparkle(level(),new net.minecraft.world.phys.Vec3(getRandomX(.6),getRandomY(),getRandomZ(.6)),0xbaba33,2,1,0);
            return;
        }
        if(!isAlive()||isBaby()||isChickenJockey())return;
        if(--eggTime<=0){
            plop();spawnAtLocation(golden()?Horizons.GOLD_EGG.get():Horizons.INK_EGG.get());
            int interval=golden()?8000:6000;eggTime=interval+random.nextInt(interval);
        }
        if(!golden()&&--featherTime<=0){plop();spawnAtLocation(Items.FEATHER);featherTime=4000+random.nextInt(4000);}
    }
    private void plop(){playSound(SoundEvents.CHICKEN_EGG,1,(random.nextFloat()-random.nextFloat())*.2f+1);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("FeatherTime",featherTime);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);if(tag.contains("FeatherTime"))featherTime=Math.max(1,Math.min(8000,tag.getInt("FeatherTime")));}
}
