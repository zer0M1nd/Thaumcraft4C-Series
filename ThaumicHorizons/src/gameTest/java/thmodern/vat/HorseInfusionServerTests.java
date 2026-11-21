package thmodern.vat;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class HorseInfusionServerTests {
    @GameTest(template="empty")
    public static void undeadHorsesRejectFoodBreedingAndNewArmour(GameTestHelper test){
        var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"UndeadHorse"));
        var food=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE,2);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,food);
        for(var type:java.util.List.of(Horizons.UNDEAD_HORSE,Horizons.SKELETAL_HORSE)){
            var horse=type.get().create(test.getLevel());horse.setHealth(5);horse.mobInteract(player,net.minecraft.world.InteractionHand.MAIN_HAND);
            test.assertTrue(player.getVehicle()==null&&food.getCount()==2&&!horse.isFood(food),"Untamed undead horse neither accepts food nor mounts");
            horse.setTamed(true);horse.mobInteract(player,net.minecraft.world.InteractionHand.MAIN_HAND);player.stopRiding();
            test.assertTrue(horse.getHealth()==5&&!horse.isInLove()&&!horse.canMate(net.minecraft.world.entity.EntityType.HORSE.create(test.getLevel())),"Feeding cannot heal or enable breeding");
            test.assertTrue(!horse.canWearArmor()&&!horse.isArmor(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_HORSE_ARMOR)),"Undead form rejects new horse armour");
        }
        var ender=Horizons.ENDERSTEED.get().create(test.getLevel());test.assertTrue(ender.isFood(food)&&ender.canWearArmor(),"Living endersteed retains ordinary horse food and armour support");test.succeed();
    }
    @GameTest(template="empty")
    public static void horseGeneticsAndBoostsSurviveConversionAndReload(GameTestHelper test){
        var source=EntityType.HORSE.create(test.getLevel());source.getAttribute(Attributes.MAX_HEALTH).setBaseValue(22);source.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.23);source.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(.6);
        var recipe=VatInfusionRecipes.all().stream().filter(r->r.id.getPath().equals("endersteed")).findFirst().orElseThrow();var target=CreatureTransformation.prepare(source,recipe);
        test.assertTrue(Math.abs(target.getMaxHealth()-110)<.001&&Math.abs(target.getAttributeValue(Attributes.MOVEMENT_SPEED)-.253)<.00001&&Math.abs(target.getAttributeValue(Attributes.JUMP_STRENGTH)-.75)<.00001,"Ender multipliers apply to source genetic bases");
        var tag=new CompoundTag();target.save(tag);var loaded=Horizons.ENDERSTEED.get().create(test.getLevel());loaded.load(tag);loaded.refreshStats();test.assertTrue(loaded.getMaxHealth()==target.getMaxHealth(),"Reload does not stack boosts");test.succeed();
    }
    @GameTest(template="empty")
    public static void blinkRequiresSafeGroundAndDoesNotMoveOnFailure(GameTestHelper test){
        var level=test.getLevel();var start=test.absolutePos(new BlockPos(3,3,3));var horse=Horizons.ENDERSTEED.get().create(level);horse.moveTo(start.getCenter());horse.setYRot(0);
        for(int x=-1;x<=1;x++)for(int z=0;z<=7;z++)level.setBlock(start.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);
        test.assertTrue(horse.blink(28)&&Math.abs(horse.getZ()-(start.getZ()+4.5))<.01,"Charge 28 moves four blocks forward onto safe ground");
        var before=horse.position();for(int x=-2;x<=2;x++)for(int z=2;z<=6;z++)for(int y=-3;y<=7;y++)level.setBlock(horse.blockPosition().offset(x,y,z),Blocks.STONE.defaultBlockState(),3);
        test.assertTrue(!horse.blink(28)&&horse.position().equals(before),"Blocked destination keeps original position");test.succeed();
    }
}
