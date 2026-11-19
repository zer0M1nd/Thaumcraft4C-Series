package thmodern.vat;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class FarmCreatureServerTests {
    @GameTest(template="empty")
    public static void chocolateMilkExchangesBucketCuresAndReturnsContainer(GameTestHelper test){
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"ChocolateTest"));player.getAbilities().instabuild=false;
        var cow=Horizons.CHOCOLATE_COW.get().create(test.getLevel());player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));cow.mobInteract(player,InteractionHand.MAIN_HAND);
        test.assertTrue(player.getMainHandItem().is(Horizons.CHOCOLATE_MILK.get())&&player.getMainHandItem().getCount()==1,"One empty bucket becomes one chocolate milk bucket");
        player.addEffect(new MobEffectInstance(MobEffects.POISON,200));var result=Horizons.CHOCOLATE_MILK.get().finishUsingItem(player.getMainHandItem(),test.getLevel(),player);
        test.assertTrue(!player.hasEffect(MobEffects.POISON)&&result.is(Items.BUCKET),"Drinking uses ordinary milk cures and returns bucket");
        test.assertTrue(new ItemStack(Horizons.CHOCOLATE_MILK.get()).getCraftingRemainingItem().is(Items.BUCKET),"Crafting also returns the empty bucket");test.succeed();
    }
    @GameTest(template="empty")
    public static void chromaticSheepCyclesAndSavesColour(GameTestHelper test){
        var sheep=Horizons.CHROMATIC_SHEEP.get().create(test.getLevel());sheep.setColor(DyeColor.BLACK);sheep.tickCount=29;sheep.aiStep();test.assertTrue(sheep.getColor()==DyeColor.BLACK,"No premature colour change");
        sheep.tickCount=30;sheep.aiStep();test.assertTrue(sheep.getColor()==DyeColor.WHITE,"Black wraps to white at thirty ticks");
        sheep.tickCount=60;sheep.aiStep();var tag=new CompoundTag();sheep.save(tag);var restored=Horizons.CHROMATIC_SHEEP.get().create(test.getLevel());restored.load(tag);
        test.assertTrue(restored.getColor()==DyeColor.ORANGE,"Colour is synchronized through native sheep data and saved");test.succeed();
    }
    @GameTest(template="empty")
    public static void selfShearingRequiresRegrownAdultWool(GameTestHelper test){
        var sheep=Horizons.SELF_SHEARING_SHEEP.get().create(test.getLevel());sheep.moveTo(test.absolutePos(new BlockPos(3,3,3)).getCenter());sheep.setColor(DyeColor.RED);sheep.tickCount=100;sheep.setAge(-1000);sheep.aiStep();
        test.assertTrue(!sheep.isSheared(),"Babies cannot manufacture wool");sheep.setAge(0);sheep.aiStep();
        var box=sheep.getBoundingBox().inflate(2);var drops=test.getLevel().getEntitiesOfClass(ItemEntity.class,box);int count=drops.stream().filter(e->e.getItem().is(Items.RED_WOOL)).mapToInt(e->e.getItem().getCount()).sum();
        test.assertTrue(sheep.isSheared()&&count>=1&&count<=3,"Adult self-shears one to three wool of its actual colour");
        sheep.tickCount=200;sheep.aiStep();test.assertTrue(test.getLevel().getEntitiesOfClass(ItemEntity.class,box).stream().mapToInt(e->e.getItem().getCount()).sum()==count,"No additional wool before regrowth");
        sheep.ate();test.assertTrue(!sheep.isSheared(),"Eating regrows wool through vanilla rules");drops.forEach(ItemEntity::discard);test.succeed();
    }
}
