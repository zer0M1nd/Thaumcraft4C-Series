package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ExcavationDefinitionServerTests {
    @GameTest(template="wand_test_empty")
    public static void excavationRangeAndAcquisitionMatchLegacy(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ExcavationRange"));
        try {
            level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
            for(double distance:new double[]{9.9,10.1}){
                player.moveTo(pos.getX()-distance,pos.getY()+.5-player.getEyeHeight(),pos.getZ()+.5,-90,0);
                var hit=ExcavationFocusItem.target(player);
                test.assertTrue((hit.getType()==HitResult.Type.BLOCK)==(distance<10),"Ten-block ray from eye");
            }
            player.moveTo(pos.getX()-4,pos.getY()+.5-player.getEyeHeight(),pos.getZ()+.5,-90,0);
            for(var obstacle:new net.minecraft.world.level.block.Block[]{Blocks.AIR,Blocks.WATER,Blocks.LAVA,Blocks.GLASS}){
                level.setBlock(pos.west(2),obstacle.defaultBlockState(),2);
                var hit=ExcavationFocusItem.target(player);
                test.assertTrue(hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(obstacle==Blocks.GLASS?pos.west(2):pos),"Fluid ignored, solid collision intercepts");
            }
            var grid=new net.minecraft.world.inventory.TransientCraftingContainer(player.inventoryMenu,3,3);
            for(int slot=0;slot<9;slot++)grid.setItem(slot,new ItemStack(slot==4?Items.EMERALD:slot%2==0?TCItems.shard(ShardType.EARTH):Items.QUARTZ));
            var recipe=level.getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()).stream().filter(r->r.getId().getPath().endsWith("focus_excavation")).findFirst().orElseThrow();
            test.assertTrue(recipe.matches(grid,level)&&recipe.assemble(grid,level.registryAccess()).is(TCItems.EXCAVATION_FOCUS.get()),"Actual old four earth shards/four quartz/emerald recipe");
            test.assertTrue(recipe.visCost(grid).equals(java.util.Map.of(TCAspects.catalog().require("terra"),20,TCAspects.catalog().require("perditio"),5,TCAspects.catalog().require("ordo"),5)),"Exact acquisition vis cost");
            test.assertTrue(recipe.unlocked(grid,key->key.equals("FOCUSEXCAVATION"))&&!recipe.unlocked(grid,key->false),"Exact research gate");
            for(int slot=0;slot<9;slot++){var held=grid.getItem(slot);grid.setItem(slot,ItemStack.EMPTY);test.assertTrue(!recipe.matches(grid,level),"All nine ingredients required");grid.setItem(slot,held);}
        } finally {level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);level.setBlock(pos.west(2),Blocks.AIR.defaultBlockState(),2);}
        tc4revival.TC4Revival.LOGGER.info("PASS excavationRangeAndAcquisitionMatchLegacy: eye range/fluid/obstruction and loaded acquisition recipe/cost/research/nine missing-slot negatives");test.succeed();
    }
}
