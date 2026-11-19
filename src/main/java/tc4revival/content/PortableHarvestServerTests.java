package tc4revival.content;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class PortableHarvestServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void portableNodeAndMirrorPlayerHarvest(GameTestHelper test) {
        var level=test.getLevel();
        var player=FakePlayerFactory.get(level,new GameProfile(java.util.UUID.randomUUID(),"PortableHarvest"));
        var pos=test.absolutePos(new BlockPos(3,3,3));
        var otherPos=test.absolutePos(new BlockPos(8,3,3));
        player.setPos(pos.getCenter());
        var values=new AspectAmounts().add(TCAspects.catalog().require("aer"),17).add(TCAspects.catalog().require("ignis"),9);
        var payload=new NodeJarData(values,AuraNodeType.UNSTABLE,AuraNodeModifier.BRIGHT,"harvest-node");
        for(var mode:new GameType[]{GameType.CREATIVE,GameType.SURVIVAL}) {
            player.setGameMode(mode);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
            for(boolean allow:new boolean[]{true,false}) {
                level.setBlockAndUpdate(pos,TCArcane.NODE_JAR.get().defaultBlockState());
                var node=(NodeJarBlockEntity)level.getBlockEntity(pos);node.configure(payload);node.drop=allow;
                test.assertTrue(player.gameMode.destroyBlock(pos)&&level.isEmptyBlock(pos),"Node removed by actual player harvest: "+mode);
                var expected=new ItemStack(TCItems.NODE_JAR.get());expected.setTag(payload.write());
                checkDrop(test,pos,allow&&mode!=GameType.CREATIVE?expected:ItemStack.EMPTY,"Node contents and native creative harvest: "+mode);
            }
            for(boolean essentia:new boolean[]{false,true}) for(var facing:Direction.values()) for(boolean linked:new boolean[]{false,true}) {
                var block=essentia?TCArcane.ESSENTIA_MIRROR.get():TCArcane.ITEM_MIRROR.get();
                var state=essentia?block.defaultBlockState().setValue(EssentiaMirrorBlock.FACING,facing)
                        :block.defaultBlockState().setValue(ItemMirrorBlock.FACING,facing);
                level.setBlockAndUpdate(pos.relative(facing.getOpposite()),Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(otherPos.relative(facing.getOpposite()),Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(pos,state);level.setBlockAndUpdate(otherPos,state);
                var target=GlobalPos.of(level.dimension(),otherPos);
                if(linked) {
                    if(essentia) {
                        var mirror=(EssentiaMirrorBlockEntity)level.getBlockEntity(pos);mirror.remember(target);
                        test.assertTrue(mirror.restoreLink(),"Essentia pair established before breaking");
                    } else {
                        var mirror=(ItemMirrorBlockEntity)level.getBlockEntity(pos);mirror.remember(target);
                        test.assertTrue(mirror.restoreLink(),"Item pair established before breaking");
                    }
                }
                test.assertTrue(player.gameMode.destroyBlock(pos)&&level.isEmptyBlock(pos),"Mirror removed by actual player harvest");
                var expected=linked?(essentia?EssentiaMirrorItem.linkedStack(target,level.dimension().location().toString())
                        :ItemMirrorItem.linkedStack(target,level.dimension().location().toString()))
                        :new ItemStack(essentia?TCItems.ESSENTIA_MIRROR.get():TCItems.ITEM_MIRROR.get());
                checkDrop(test,pos,mode==GameType.CREATIVE?ItemStack.EMPTY:expected,"Mirror drop with complete link: "+mode+"/"+essentia+"/"+facing+"/"+linked);
                boolean stillLinked=essentia?((EssentiaMirrorBlockEntity)level.getBlockEntity(otherPos)).linked()
                        :((ItemMirrorBlockEntity)level.getBlockEntity(otherPos)).linked();
                test.assertTrue(!stillLinked,"Harvest invalidates the surviving partner after saving the portable link");
                level.setBlockAndUpdate(otherPos,Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(pos.relative(facing.getOpposite()),Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(otherPos.relative(facing.getOpposite()),Blocks.AIR.defaultBlockState());
            }
        }
        tc4revival.TC4Revival.LOGGER.info("portableNodeAndMirrorPlayerHarvest PASS: survival/creative, node payload/suppression, both mirrors/all faces/link states/partner invalidation");
        test.succeed();
    }
    private static void checkDrop(GameTestHelper test,BlockPos pos,ItemStack expected,String label) {
        var drops=test.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1));
        test.assertTrue(expected.isEmpty()?drops.isEmpty():drops.size()==1&&drops.get(0).getItem().getCount()==1
                &&ItemStack.isSameItemSameTags(drops.get(0).getItem(),expected),label);
        for(var drop:drops)drop.discard();
    }
}
