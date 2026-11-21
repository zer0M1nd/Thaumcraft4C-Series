package infusionbrewing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;

public final class ReactionSpawns {
    private ReactionSpawns(){}
    public static void potionPair(ServerLevel level,BlockPos pos,int a,int b){
        for(int id=1;id<=23;id++)if(id==a||id==b){
            EntityType<? extends Mob> type=switch(id){
                case 5->EntityType.ENDERMAN;case 8->EntityType.BAT;case 10->EntityType.SLIME;case 13->EntityType.SQUID;case 14->EntityType.VILLAGER;case 16->EntityType.SPIDER;
                case 2,7->EntityType.WITCH;case 4->EntityType.CREEPER;case 9,17->level.dimensionType().ultraWarm()?EntityType.ZOMBIFIED_PIGLIN:EntityType.ZOMBIE;
                case 15->EntityType.BLAZE;case 18,20->EntityType.SKELETON;case 19->EntityType.CAVE_SPIDER;default->null;};
            if(type!=null)spawn(level,pos,type,BrewingRules.support(id)?.1:1);
        }
    }
    public static void spawn(ServerLevel level,BlockPos origin,EntityType<? extends Mob> type,double chance){
        if(!BrewingConfig.SPAWNS.get()||level.random.nextDouble()>chance)return;
        if(level.getEntitiesOfClass(Mob.class,new AABB(origin.offset(-8,-4,-8),origin.offset(9,5,9)),e->e.getType()==type).size()>=10)return;
        var mob=type.create(level);if(mob==null)return;
        for(int attempt=0;attempt<13;attempt++){
            var pos=origin.offset(level.random.nextInt(9)-5,level.random.nextInt(5)-3,level.random.nextInt(9)-5);
            if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)||pos.getY()<level.getMinBuildHeight())continue;
            mob.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
            if(!level.noCollision(mob)||!level.getBlockState(pos).isAir())continue;
            mob.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),MobSpawnType.TRIGGERED,null,null);
            if(mob instanceof Zombie zombie&&level.random.nextDouble()<.7)zombie.setBaby(true);
            if(mob instanceof Creeper&&level.random.nextDouble()<.7){var tag=mob.saveWithoutId(new net.minecraft.nbt.CompoundTag());tag.putBoolean("powered",true);mob.load(tag);}
            if(mob instanceof Zombie||mob instanceof AbstractSkeleton){
                mob.setItemSlot(EquipmentSlot.MAINHAND,mob instanceof AbstractSkeleton&&level.random.nextDouble()<.8?new ItemStack(Items.IRON_SWORD):tc("thaumium_sword"));
                double r=level.random.nextDouble();if(r<.05)mob.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));else if(r<.6)mob.setItemSlot(EquipmentSlot.HEAD,tc("thaumium_helmet"));
                if(level.random.nextDouble()<.6)mob.setItemSlot(EquipmentSlot.CHEST,tc("thaumium_chestplate"));
                if(level.random.nextDouble()<.6)mob.setItemSlot(EquipmentSlot.LEGS,tc("thaumium_leggings"));
                if(level.random.nextDouble()<.6)mob.setItemSlot(EquipmentSlot.FEET,tc("thaumium_boots"));
            }
            level.addFreshEntity(mob);mob.spawnAnim();return;
        }
    }
    private static ItemStack tc(String name){var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("thaumcraft",name));return item==null?ItemStack.EMPTY:new ItemStack(item);}
}
