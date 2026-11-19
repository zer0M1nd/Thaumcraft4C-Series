package emtmodern.test;

import emtmodern.ElectroMagicTools;
import emtmodern.item.*;
import ic2.api.recipe.Recipes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class MaterialServerTests {
    public static ItemStack stack(String id){return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)));}
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void originalOreProcessingCountsMetadataAndUuRecipes(GameTestHelper test){
        var level=test.getLevel();
        var macerate=Recipes.macerator.get(level).apply(stack("thaumcraft:amber_ore"),false);test.assertTrue(macerate!=null&&macerate.getOutput().stream().anyMatch(s->s.is(stack("emt:crushed_amber").getItem())&&s.getCount()==2),"Real macerator doubles amber ore");
        var wash=Recipes.oreWashing.get(level).apply(stack("emt:crushed_cinnabar"),false);test.assertTrue(wash!=null&&wash.getOutput().stream().anyMatch(s->s.is(stack("ic2:small_tin_dust").getItem())&&s.getCount()==1)&&wash.recipe().getMetaData().getInt("amount")==1000,"Original cinnabar typo yields one small tin dust at 1000 mB");
        var centrifuge=Recipes.centrifuge.get(level).apply(stack("emt:purified_amber"),false);test.assertTrue(centrifuge!=null&&centrifuge.recipe().getMetaData().getInt("minHeat")==1000&&centrifuge.getOutput().stream().anyMatch(s->s.is(stack("thaumcraft:amber").getItem())),"Real centrifuge uses original heat/output");
        var cluster=Recipes.macerator.get(level).apply(stack("thaumcraft:iron_cluster"),false);test.assertTrue(cluster!=null&&cluster.getOutput().stream().anyMatch(s->s.is(stack("ic2:small_iron_dust").getItem())&&s.getCount()==22),"Native cluster becomes 22 small dust");
        for(String id:java.util.List.of("uu_matter_drop","uu_uranium_to_iridium","uu_amber_bricks_to_uranium_ore","uu_shard_to_resin","uu_shard_to_redstone","uu_shard_to_lapis"))test.assertTrue(level.getRecipeManager().byKey(new ResourceLocation("emt",id)).isPresent(),"Complete original UU recipe present: "+id);
        test.assertTrue(level.getRecipeManager().byKey(new ResourceLocation("emt","uu_amber_bricks_to_uranium_ore")).orElseThrow().getResultItem(level.registryAccess()).getCount()==8,"Amber bricks conversion preserves original 8 uranium ores");test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void taintedFeatherRetainsProbabilityAndUndeadImmunity(GameTestHelper test){
        var player=GogglesServerTests.player(test);var feather=stack("emt:tainted_feather");feather.setCount(4);player.getInventory().items.set(0,feather);
        test.assertTrue(TaintedFeather.shouldPoison(player,feather,4)&&!TaintedFeather.shouldPoison(player,feather,5),"Original <= stack count boundary");var zombie=EntityType.ZOMBIE.create(test.getLevel());test.assertTrue(!TaintedFeather.shouldPoison(zombie,feather,0),"Undead are immune");
        ((TaintedFeather)feather.getItem()).poison(player);test.assertTrue(feather.getCount()==3&&player.getEffect(tc4revival.content.TCEffects.FLUX_TAINT.get()).getDuration()==120&&!TaintedFeather.shouldPoison(player,feather,0),"Poisons for 120 ticks, consumes one, does not reapply while affected");test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void lightningSummonerChargesFortyTicksAndCreatesActualLightning(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));var player=GogglesServerTests.player(test);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);player.setXRot(0);player.setYRot(0);
        var target=BlockPos.containing(player.getEyePosition()).south(3);level.setBlock(target,Blocks.STONE.defaultBlockState(),3);
        var stack=stack("emt:lightning_summoner");player.getInventory().items.set(0,stack);player.getInventory().selected=0;player.setItemInHand(InteractionHand.MAIN_HAND,stack);var item=(LightningSummoner)stack.getItem();item.use(level,player,InteractionHand.MAIN_HAND);
        item.onUseTick(level,player,stack,72000-39);test.assertTrue(stack.getCount()==1&&player.isUsingItem(),"Does not fire before 40 ticks");test.assertTrue(LightningSummoner.trace(player).getBlockPos().equals(target),"Ray selects the real hit block");
        item.onUseTick(level,player,stack,72000-40);test.assertTrue(stack.isEmpty()&&!player.isUsingItem(),"Consumes one and stops using after strike");test.assertTrue(!level.getEntities(EntityType.LIGHTNING_BOLT,new AABB(target).inflate(1),e->true).isEmpty(),"Actual lightning entity is spawned at target");test.succeed();
    }
}
