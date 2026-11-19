package thmodern.creature;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.IForgeShearable;

/** Passive spider-shaped grazer; its fleece is string rather than wool. */
public final class Sheeder extends Spider implements IForgeShearable {
    private static final EntityDataAccessor<Boolean> SHEARED=SynchedEntityData.defineId(Sheeder.class,EntityDataSerializers.BOOLEAN);
    public Sheeder(EntityType<? extends Spider> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(SHEARED,false);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new PanicGoal(this,.5));
        goalSelector.addGoal(3,new TemptGoal(this,.44,Ingredient.of(Items.WHEAT),false));
        goalSelector.addGoal(5,new EatBlockGoal(this));goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,.4));
        goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,6));goalSelector.addGoal(8,new RandomLookAroundGoal(this));
    }
    @Override protected boolean shouldDespawnInPeaceful(){return false;}
    @Override protected SoundEvent getAmbientSound(){return SoundEvents.SHEEP_AMBIENT;}
    public boolean sheared(){return entityData.get(SHEARED);}
    @Override public boolean isShearable(ItemStack tool,Level level,BlockPos pos){return isAlive()&&!sheared()&&!isBaby();}
    @Override public List<ItemStack> onSheared(Player player,ItemStack tool,Level level,BlockPos pos,int fortune){
        if(level.isClientSide||!isShearable(tool,level,pos))return List.of();
        entityData.set(SHEARED,true);playSound(SoundEvents.SHEEP_SHEAR,1,1);
        var drops=new ArrayList<ItemStack>();for(int i=0,n=2+random.nextInt(4);i<n;i++)drops.add(new ItemStack(Items.STRING));return drops;
    }
    @Override public void ate(){entityData.set(SHEARED,false);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("Sheared",sheared());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(SHEARED,tag.getBoolean("Sheared"));}
}
