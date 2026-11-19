package thmodern.creature;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public final class OrePig extends Pig {
    private int nuggetProgress;
    public OrePig(EntityType<? extends Pig> type,Level level){super(type,level);goalSelector.addGoal(5,new EatStoneGoal(this));}
    public int nuggetProgress(){return nuggetProgress;}
    public boolean eatStone(ItemEntity item){
        if(level().isClientSide||!item.isAlive()||item.level()!=level()||item.hasPickUpDelay()||!item.getItem().is(Items.COBBLESTONE)||distanceToSqr(item)>2)return false;
        var stack=item.getItem().copy();stack.shrink(1);if(stack.isEmpty())item.discard();else item.setItem(stack);
        nuggetProgress+=1+random.nextInt(15);
        if(nuggetProgress>=100){nuggetProgress-=100;var output=chooseNugget();if(!output.isEmpty())spawnAtLocation(output,.3f);}
        playSound(net.minecraft.sounds.SoundEvents.PLAYER_BURP,.2f,((random.nextFloat()-random.nextFloat())*.7f+1)*2);return true;
    }
    private ItemStack chooseNugget(){
        var items=new ArrayList<Item>();var weights=new ArrayList<Integer>();
        items.add(Items.GOLD_NUGGET);weights.add(6);
        for(var row:List.of(new String[]{"silver","6"},new String[]{"copper","8"},new String[]{"tin","10"},new String[]{"lead","10"})){
            var ingots=ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(ResourceLocation.parse("forge:ingots/"+row[0])));
            var nuggets=ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(ResourceLocation.parse("forge:nuggets/"+row[0])));
            if(!ingots.isEmpty()&&!nuggets.isEmpty()){items.add(nuggets.stream().sorted(Comparator.comparing(i->ForgeRegistries.ITEMS.getKey(i).toString())).findFirst().orElseThrow());weights.add(Integer.parseInt(row[1]));}
        }
        var mercury=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:quicksilver_drop"));
        if(mercury!=null&&mercury!=Items.AIR){items.add(mercury);weights.add(10);}
        items.add(Items.IRON_NUGGET);weights.add(25);
        int roll=random.nextInt(weights.stream().mapToInt(Integer::intValue).sum());
        for(int i=0;i<items.size();i++){roll-=weights.get(i);if(roll<0)return new ItemStack(items.get(i));}return ItemStack.EMPTY;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("NuggetProgress",nuggetProgress);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);nuggetProgress=Math.max(0,Math.min(99,tag.getInt("NuggetProgress")));}
    public static final class EatStoneGoal extends Goal {
        private final OrePig pig;private ItemEntity target;private int remaining;
        public EatStoneGoal(OrePig pig){this.pig=pig;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){
            target=pig.level().getEntitiesOfClass(ItemEntity.class,pig.getBoundingBox().inflate(16),item->item.isAlive()&&!item.hasPickUpDelay()&&item.getItem().is(Items.COBBLESTONE)&&pig.distanceToSqr(item)<128).stream().min(Comparator.comparingDouble(pig::distanceToSqr)).orElse(null);
            return target!=null;
        }
        @Override public void start(){remaining=500;pig.getNavigation().moveTo(target,1.25);}
        @Override public boolean canContinueToUse(){return remaining>0&&target!=null&&target.isAlive()&&target.getItem().is(Items.COBBLESTONE)&&pig.distanceToSqr(target)<256;}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void tick(){remaining--;pig.getLookControl().setLookAt(target,30,30);if(pig.distanceToSqr(target)<=2)pig.eatStone(target);else if(remaining%10==0)pig.getNavigation().moveTo(target,1.25);}
        @Override public void stop(){target=null;remaining=0;pig.getNavigation().stop();}
    }
}
