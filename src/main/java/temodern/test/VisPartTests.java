package temodern.test;

import java.util.*;
import appeng.api.parts.IPartItem;
import appeng.api.util.AEColor;
import appeng.core.definitions.*;
import appeng.blockentity.networking.CableBusBlockEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.*;
import temodern.machine.*;
import thaumcraft.api.visnet.VisNetHandler;

@GameTestHolder(temodern.TE.ID) @PrefixGameTestTemplate(false)
public final class VisPartTests {
    private static CableBusBlockEntity cable(GameTestHelper t,BlockPos pos){
        t.setBlock(pos,AEBlocks.CABLE_BUS.block());t.setBlock(pos.west(),AEBlocks.CREATIVE_ENERGY_CELL.block());
        var host=(CableBusBlockEntity)t.getBlockEntity(pos);host.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT),null,null);return host;
    }
    @SuppressWarnings("unchecked") private static VisInterfacePart part(CableBusBlockEntity host,Direction face,boolean creative){
        var item=(IPartItem<VisInterfacePart>)net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumicenergistics:"+(creative?"creative_":"")+"vis_interface"));
        return host.addPart(item,face,null);
    }
    @GameTest(template="test_empty",timeoutTicks=200) public static void multiFaceBindingSuppliesBlocksAndAssembler(GameTestHelper t){
        var pos=new BlockPos(2,2,2);var host=cable(t,pos);var source=part(host,Direction.UP,true);var output=part(host,Direction.SOUTH,false);
        t.setBlock(pos.east(),Machines.ALL.get("arcane_assembler").get());var assembler=(NetworkMachine)t.getBlockEntity(pos.east());
        assembler.workstation.inventory.setItem(9,((tc4revival.content.CastingWandItem)tc4revival.content.TCItems.CASTING_WAND.get()).create(tc4revival.content.WandParts.Rod.WOOD,tc4revival.content.WandParts.Cap.IRON,false,false));
        t.setBlock(pos.north(),Machines.ALL.get("vis_interface").get());var block=(NetworkMachine)t.getBlockEntity(pos.north());
        t.runAfterDelay(100,()->{
            t.assertTrue(source.node().isActive()&&output.node().isActive(),"Both faces have active AE nodes");
            t.assertTrue(!source.key().equals(output.key()),"Same host has distinct TC endpoint keys");
            var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(t.getLevel());var card=AEItems.MEMORY_CARD.stack();
            player.setShiftKeyDown(true);source.memoryCard(player,card);player.setShiftKeyDown(false);
            output.memoryCard(player,card);block.memoryCard(player,card);assembler.memoryCard(player,card);
            t.assertTrue(output.source()&&VisNetHandler.consume(output,thaumcraft.api.aspects.Aspect.AIR,250)==250,"Bound output part drains creative input buffer");
            t.assertTrue(block.source()&&block.consumeVis(tc4revival.aspect.TCAspects.catalog().require("aer"),250)==250,"Legacy output block accepts part UUID");
            var saved=new net.minecraft.nbt.CompoundTag();source.writeToNBT(saved);t.assertTrue(saved.getUUID("identity").equals(source.visIdentity()),"Part saves its stable UUID");
            var outputTag=new net.minecraft.nbt.CompoundTag();output.writeToNBT(outputTag);t.assertTrue(outputTag.getBoolean("output")&&outputTag.getUUID("linked").equals(source.visIdentity()),"Output persists mode and bound source");
        });
        t.runAfterDelay(120,()->{
            var air=tc4revival.aspect.TCAspects.catalog().require("aer");
            t.assertTrue(assembler.consumeNetworkVis(Map.of(air,100),true),"Bound cable part charges the actual assembler wand");
            t.assertTrue(!assembler.saveWithFullMetadata().contains("vis"),"Assembler has no internal vis buffer");
            host.removePart(source);
            t.assertTrue(!source.valid()&&VisNetHandler.root(source)==null,"Removed endpoint is invalid and detached");
            t.assertTrue(output.drain(air,1)==0&&block.consumeVis(air,1)==0,"Removing bound source stops both output forms");t.succeed();
        });
    }
    @GameTest(template="test_empty",timeoutTicks=200) public static void inputConsumesExternalNetworkAndRejectsFeedback(GameTestHelper t){
        var sourceHost=cable(t,new BlockPos(2,2,2));var source=part(sourceHost,Direction.UP,true);
        var inputHost=cable(t,new BlockPos(2,2,5));var input=part(inputHost,Direction.UP,false);
        var feedback=part(sourceHost,Direction.SOUTH,false);
        t.runAfterDelay(110,()->{
            var air=tc4revival.aspect.TCAspects.catalog().require("aer");
            t.assertTrue(input.node().getGrid()!=source.node().getGrid(),"Independent AE grids");
            t.assertTrue(input.takeVis(air,100)==100,"Input part receives real TC vis from external grid");
            t.assertTrue(feedback.takeVis(air,1)==0,"Same AE grid cannot feed vis back into its input");
            var tag=inputHost.saveWithFullMetadata();var id=input.visIdentity();
            inputHost.removePart(input);inputHost.load(tag);
            var restored=(VisInterfacePart)inputHost.getPart(Direction.UP);
            t.assertTrue(restored!=null&&restored.visIdentity().equals(id),"Host reload restores UUID");t.succeed();
        });
    }
}
