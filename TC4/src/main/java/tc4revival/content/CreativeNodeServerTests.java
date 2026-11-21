package tc4revival.content;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class CreativeNodeServerTests {
    @GameTest(template="wand_test_empty")
    public static void creativeNodeItemPlacesRandomAuraAndAppearsInTab(GameTestHelper test) {
        var level=test.getLevel();var target=test.absolutePos(new net.minecraft.core.BlockPos(3,3,3));
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"CreativeNodeAudit"));
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        var tab=TCCreativeTabs.THAUMCRAFT.get();tab.buildContents(new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(),true,level.registryAccess()));
        test.assertTrue(tab.getDisplayItems().stream().filter(stack->stack.is(TCItems.AURA_NODE.get())).count()==1,"The random node appears once in the actual creative tab");
        var seen=new java.util.HashSet<String>();
        for(int seed=0;seed<16;seed++) {
            level.setBlock(target,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            level.setBlock(target.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            var stack=new net.minecraft.world.item.ItemStack(TCItems.AURA_NODE.get());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
            player.setPos(target.getX()+.5,target.getY(),target.getZ()+3);
            level.random.setSeed(seed);
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atBottomCenterOf(target),net.minecraft.core.Direction.UP,target.below(),false);
            var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            var result=stack.useOn(context);
            test.assertTrue(result.consumesAction() && level.getBlockEntity(target) instanceof AuraNodeBlockEntity,"Actual item use places a real node");
            var node=(AuraNodeBlockEntity)level.getBlockEntity(target);
            test.assertTrue(node.aspects().total()>0 && stack.getCount()==1,"Generated aura is nonempty and creative placement does not consume the item");
            var saved=node.saveWithoutMetadata();var restored=new AuraNodeBlockEntity(target,node.getBlockState());restored.load(saved);
            test.assertTrue(restored.aspects().view().equals(node.aspects().view()) && restored.nodeType()==node.nodeType() && restored.nodeModifier()==node.nodeModifier(),"Generated aura and node properties survive save/load");
            seen.add(node.aspects().view().toString());
        }
        test.assertTrue(seen.size()>1,"Different random seeds do not all produce a fixed placeholder node");
        tc4revival.TC4Revival.LOGGER.info("PASS creativeNodeItemPlacesRandomAuraAndAppearsInTab: 16 actual placements");test.succeed();
    }
}
