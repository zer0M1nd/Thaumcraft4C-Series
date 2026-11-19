package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class ImbuedFireServerTests {
    @GameTest(template="wand_test_empty") public static void tinkererFireTransformsAndOrderUsesTags(GameTestHelper test) {
        var world=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));
        world.setBlock(pos,Blocks.GRASS_BLOCK.defaultBlockState(),3);test.assertTrue(ImbuedFireRules.transform("ignis",world,pos).is(Blocks.NETHERRACK),"Ignis changes grass to netherrack");
        world.setBlock(pos,Blocks.LAVA.defaultBlockState(),3);test.assertTrue(ImbuedFireRules.transform("aqua",world,pos).is(Blocks.OBSIDIAN),"Aqua cools lava");
        world.setBlock(pos,Blocks.SPAWNER.defaultBlockState(),3);test.assertTrue(ImbuedFireRules.transform("terra",world,pos).is(Blocks.IRON_BLOCK),"Terra explicitly transforms spawner");test.assertTrue(ImbuedFireRules.transform("ordo",world,pos)==null,"Order never processes block entity inventories");
        world.setBlock(pos,Blocks.DIAMOND_ORE.defaultBlockState(),3);test.assertTrue(ImbuedFireRules.transform("ordo",world,pos).is(Blocks.DIAMOND_BLOCK),"Order resolves ore to its storage block");
        world.setBlock(pos,Tinkerer.FIRES.get("aer").get().defaultBlockState(),3);test.assertTrue(ImbuedFireRules.transform("perditio",world,pos).is(Blocks.FIRE),"Perditio dispels other elemental fires");
        world.setBlock(pos,Blocks.BEDROCK.defaultBlockState(),3);test.assertTrue(ImbuedFireRules.transform("ordo",world,pos)==null,"Unbreakable terrain is never a conversion target");world.removeBlock(pos,false);test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererFirePauseAndSpentSpreadCleanup(GameTestHelper test) {
        var world=test.getLevel();var pos=test.absolutePos(new BlockPos(2,3,2));var fire=Tinkerer.FIRES.get("aer").get();var old=world.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK);
        try {
            world.setBlock(pos,fire.defaultBlockState().setValue(ImbuedFireBlock.AGE,1),3);
            world.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(false,world.getServer());fire.tick(world.getBlockState(pos),world,pos,world.random);test.assertTrue(world.getBlockState(pos).is(fire),"Gamerule pauses decay as well as spreading");
            world.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(true,world.getServer());fire.tick(world.getBlockState(pos),world,pos,world.random);test.assertTrue(world.isEmptyBlock(pos),"Spread flame without transmutation fuel expires");
        } finally {world.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(old,world.getServer());world.removeBlock(pos,false);}
        test.succeed();
    }
}
