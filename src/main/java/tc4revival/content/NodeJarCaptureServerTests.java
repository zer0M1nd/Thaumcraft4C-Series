package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;

/** Capture transaction and portable-node lifecycle; authored for the deferred server batch. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class NodeJarCaptureServerTests {
    private NodeJarCaptureServerTests() {}

    @GameTest(template="wand_test_empty")
    public static void everyFailedWriteRestoresShellAndFullNodeData(GameTestHelper test) {
        var level=test.getLevel(); var origin=test.absolutePos(new BlockPos(2,2,2));
        var center=origin.offset(1,0,1); build(test,origin);
        var node=(AuraNodeBlockEntity)level.getBlockEntity(center);
        var original=node.saveWithoutMetadata();
        var result=new NodeJarData(node.aspects(),node.nodeType(),AuraNodeModifier.PALE,node.nodeId());
        int[] charges={0};
        // Include a writer that changes the world and then reports failure.
        for (int stop=1;stop<=36;stop++) {
            int failAt=stop; int[] writes={0};
            test.assertTrue(!NodeJarCapture.convert(level,origin,result,()->{ charges[0]++; return true; },(pos,state)->{
                boolean changed=level.setBlock(pos,state,18);
                return ++writes[0]!=failAt && changed;
            }),"Capture must reject failed write "+stop);
            assertRestored(test,origin,original);
        }
        test.assertTrue(charges[0]==0,"Failed shell writes must never charge vis");
        test.assertTrue(!NodeJarCapture.convert(level,origin,result,()->false,(pos,state)->level.setBlock(pos,state,18)),
                "A failed final payment must roll back the complete conversion");
        assertRestored(test,origin,original);
        boolean threw=false;
        try {
            NodeJarCapture.convert(level,origin,result,()->true,(pos,state)->{
                level.setBlock(pos,state,18);
                throw new IllegalArgumentException("injected capture interruption");
            });
        } catch (IllegalArgumentException expected) { threw=true; }
        test.assertTrue(threw,"Unexpected exceptions must remain visible after rollback");
        assertRestored(test,origin,original);
        test.assertTrue(NodeJarCapture.convert(level,origin,result,()->{ charges[0]++; return true; },
                (pos,state)->level.setBlock(pos,state,18)),"A restored structure must accept a subsequent capture");
        test.assertTrue(charges[0]==1,"A successful conversion must charge once");
        var jar=(NodeJarBlockEntity)level.getBlockEntity(center); var air=TCAspects.catalog().require("aer");
        test.assertTrue(jar.aspects().amount(air)==31 && jar.base().amount(air)==31
                && jar.contents().modifier()==AuraNodeModifier.PALE && jar.nodeId().equals("capture-identity"),
                "Capture must preserve current vis and identity, using the selected degraded modifier");
        for (int row=0;row<4;row++) for (int x=0;x<3;x++) for (int z=0;z<3;z++) {
            var pos=origin.offset(x,2-row,z);
            if (!pos.equals(center)) test.assertTrue(level.getBlockState(pos).isAir(),"Successful capture must consume all 35 shell blocks");
        }
        test.succeed();
    }

    @GameTest(template="wand_test_empty")
    public static void capturedNodeSurvivesDropPlacementAndWandRelease(GameTestHelper test) {
        var level=test.getLevel(); var origin=test.absolutePos(new BlockPos(2,2,2));
        var center=origin.offset(1,0,1); build(test,origin);
        var node=(AuraNodeBlockEntity)level.getBlockEntity(center);
        var result=new NodeJarData(node.aspects(),node.nodeType(),node.nodeModifier(),node.nodeId());
        test.assertTrue(NodeJarCapture.convert(level,origin,result,()->true,(pos,state)->level.setBlock(pos,state,18)),"Capture must succeed");
        var jar=(NodeJarBlockEntity)level.getBlockEntity(center);
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"NodeJarTest"));
        var drops=net.minecraft.world.level.block.Block.getDrops(jar.getBlockState(),level,center,jar);
        test.assertTrue(drops.size()==1 && drops.get(0).is(TCItems.NODE_JAR.get()),"Breaking a captured node must yield one portable jar");
        var stack=drops.get(0); var air=TCAspects.catalog().require("aer");
        level.setBlock(center,Blocks.AIR.defaultBlockState(),18);
        level.setBlock(center.below(),Blocks.STONE.defaultBlockState(),18);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(center.below()),
                net.minecraft.core.Direction.UP,center.below(),false);
        var placement=new net.minecraft.world.item.context.BlockPlaceContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,stack,hit);
        test.assertTrue(TCItems.NODE_JAR.get().place(placement).consumesAction(),"Portable jar must place through the actual item path");
        test.assertTrue(level.getBlockEntity(center) instanceof NodeJarBlockEntity,"Placement must create the jar entity");
        var placed=(NodeJarBlockEntity)level.getBlockEntity(center);
        test.assertTrue(placed.aspects().amount(air)==31 && placed.nodeId().equals("capture-identity"),"Placement must load the portable payload");
        var wand=new net.minecraft.world.item.ItemStack(TCItems.CASTING_WAND.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wand);
        var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(center),net.minecraft.core.Direction.UP,center,false));
        test.assertTrue(TCItems.CASTING_WAND.get().onItemUseFirst(wand,context).consumesAction(),"Wand dispatch must release a placed node jar");
        var released=(AuraNodeBlockEntity)level.getBlockEntity(center);
        test.assertTrue(released.base().amount(air)==31 && released.aspects().amount(air)==31
                && released.nodeModifier()==AuraNodeModifier.BRIGHT && released.nodeId().equals("capture-identity"),
                "Full transport must retain identity and current vis, not restore the old 40-vis capacity");
        test.succeed();
    }

    private static void build(GameTestHelper test,BlockPos origin) {
        var level=test.getLevel();
        for (int row=0;row<4;row++) for (int x=0;x<3;x++) for (int z=0;z<3;z++)
            level.setBlock(origin.offset(x,2-row,z),row==0
                    ? Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,x%2==0 ? SlabType.TOP : SlabType.BOTTOM)
                    : Blocks.GLASS.defaultBlockState(),18);
        var center=origin.offset(1,0,1); level.setBlock(center,TCArcane.AURA_NODE.get().defaultBlockState(),18);
        var node=(AuraNodeBlockEntity)level.getBlockEntity(center); var air=TCAspects.catalog().require("aer");
        node.configure(new AspectAmounts().add(air,40),AuraNodeType.PURE,AuraNodeModifier.BRIGHT,"capture-identity",false);
        node.take(air,9);
    }

    private static void assertRestored(GameTestHelper test,BlockPos origin,net.minecraft.nbt.CompoundTag original) {
        test.assertTrue(NodeJarCapture.matches(test.getLevel(),origin),"Failure must leave a complete capturable structure");
        for (int x=0;x<3;x++) for (int z=0;z<3;z++)
            test.assertTrue(test.getLevel().getBlockState(origin.offset(x,2,z)).getValue(SlabBlock.TYPE)==(x%2==0 ? SlabType.TOP : SlabType.BOTTOM),
                    "Rollback must retain exact slab state");
        test.assertTrue(test.getLevel().getBlockEntity(origin.offset(1,0,1)).saveWithoutMetadata().equals(original),
                "Rollback must restore the complete node NBT, including its original capacity");
    }
}
