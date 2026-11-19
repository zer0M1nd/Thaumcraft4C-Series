package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class TooltipServerTests {
    @GameTest(template="wand_test_empty") public static void tinkererMicroblockMaterialsResolveFrozenRegistry(GameTestHelper test) throws Exception {
        var materials=MicroblockIntegration.materials();test.assertTrue(materials.size()==20&&new java.util.HashSet<>(materials).size()==20,"All original material families resolve distinct registered blocks");test.assertTrue(materials.contains(Tinkerer.PILLAR.get()),"Last dark-quartz variant is not lost to legacy exclusive metadata bound");
        if(net.minecraftforge.fml.ModList.get().isLoaded("cb_microblock")){
            var key=Class.forName("codechicken.microblock.api.BlockMicroMaterial").getMethod("makeMaterialKey",net.minecraft.world.level.block.state.BlockState.class);
            var lookup=Class.forName("codechicken.microblock.util.MicroMaterialRegistry").getMethod("getMaterial",net.minecraft.resources.ResourceLocation.class);
            for(var block:materials)test.assertTrue(lookup.invoke(null,key.invoke(null,block.defaultBlockState()))!=null,"Actual microblock registry contains "+net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block));
        }
        test.succeed();
    }
    private static String key(Component line){return ((TranslatableContents)line.getContents()).getKey();}
    @GameTest(template="wand_test_empty") public static void tinkererServerTooltipReflectsStateWithoutInventoryData(GameTestHelper test){
        var world=test.getLevel();var pos=test.absolutePos(new BlockPos(2,1,2));world.setBlockAndUpdate(pos,Tinkerer.TABLET.get().defaultBlockState());var tablet=(TabletEntity)world.getBlockEntity(pos);
        test.assertTrue(MachineTooltip.lines(tablet).size()==1,"Empty tablet only shows tool status");
        var item=new ItemStack(Items.STICK);item.getOrCreateTag().putString("PrivateTestData","do-not-export");tablet.items.setStackInSlot(0,item);tablet.configure(false,true);
        var snapshot=MachineTooltip.snapshot(tablet);var lines=MachineTooltip.read(snapshot);
        test.assertTrue(lines.size()==3&&key(lines.get(1)).equals("ttwaila.rightClick")&&key(lines.get(2)).equals("ttwaila.redstone"),"Snapshot carries current authoritative mode");
        test.assertTrue(!snapshot.toString().contains("do-not-export"),"Tooltip does not serialize inventory NBT");
        world.setBlockAndUpdate(pos,Tinkerer.WARP_GATE.get().defaultBlockState());var gate=(WarpGateEntity)world.getBlockEntity(pos);gate.locked(true);test.assertTrue(key(MachineTooltip.lines(gate).get(0)).equals("ttwaila.disallowIncoming"),"Locked gate accurately reports incoming travel disabled");gate.locked(false);test.assertTrue(key(MachineTooltip.lines(gate).get(0)).equals("ttwaila.allowIncoming"),"Unlocked gate allows incoming travel");
        var malformed=new CompoundTag();var bad=new ListTag();bad.add(StringTag.valueOf("not-json"));bad.add(StringTag.valueOf("x".repeat(8193)));bad.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("valid"))));malformed.put("Lines",bad);test.assertTrue(MachineTooltip.read(malformed).size()==1,"Malformed or excessive entries do not break valid status lines");world.removeBlock(pos,false);test.succeed();
    }
}
