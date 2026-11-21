package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ExcavationSpeedServerTests {
    @GameTest(template="wand_test_empty")
    public static void excavationLegacyMaterialSpeedAndObsidianMultiplier(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));var focus=TCItems.EXCAVATION_FOCUS.get();
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ExcavationSpeed"));
        player.moveTo(pos.getX()+.5,pos.getY()+2,pos.getZ()+.5,0,90);
        Block[] blocks={Blocks.STONE,Blocks.DIRT,Blocks.GRASS_BLOCK,Blocks.SAND,Blocks.HAY_BLOCK,Blocks.OBSIDIAN,Blocks.IRON_BLOCK,Blocks.GOLD_BLOCK,Blocks.CLAY,Blocks.SNOW_BLOCK,Blocks.OAK_PLANKS,Blocks.GLASS,Blocks.LAPIS_BLOCK,TCArcane.THAUMIUM_BLOCK.get(),TCArcane.ARCANE_STONE.get()};
        int cases=0;
        try {
            level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);
            for(int index=0;index<blocks.length;index++)for(int potency:new int[]{0,1,3}){
                var block=blocks[index];var state=block.defaultBlockState();boolean fast=index<6||index>=13;
                test.assertTrue(state.is(ExcavationFocusItem.FAST_MATERIAL)==fast,"Observed legacy material tag "+block);
                level.setBlock(pos,state,2);var core=new ItemStack(focus);for(int rank=1;rank<=potency;rank++)focus.applyUpgrade(core,FocusUpgrade.POTENCY,rank);
                var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true);WandFocusItem.attach(wand,core);player.setItemInHand(InteractionHand.MAIN_HAND,wand);
                float speed=fast?.25f+potency*.25f:.05f+potency*.1f;if(block==Blocks.OBSIDIAN)speed*=3;
                float work=0;int additions=0;while(work<state.getDestroySpeed(level,pos)){work+=speed;additions++;}
                focus.cast(wand,level,player,InteractionHand.MAIN_HAND,null);
                for(int tick=0;tick<1+additions;tick++)focus.usingTick(wand,player,10000-tick);
                test.assertTrue(level.getBlockState(pos).is(block),"No early completion for material and potency "+block+" "+potency);
                focus.usingTick(wand,player,9000);test.assertTrue(level.isEmptyBlock(pos),"Completes at exact legacy progress boundary "+block+" "+potency);
                focus.release(wand,level,player,0);cases++;
            }
        } finally {
            focus.release(player.getMainHandItem(),level,player,0);level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),2);
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(3)).forEach(net.minecraft.world.entity.Entity::discard);
        }
        tc4revival.TC4Revival.LOGGER.info("PASS excavationLegacyMaterialSpeedAndObsidianMultiplier: {} actual progress boundaries",cases);test.succeed();
    }
}
