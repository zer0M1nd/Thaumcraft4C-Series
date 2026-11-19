package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class TaintAppearanceServerTests {
    @GameTest(template="wand_test_empty")
    public static void taintSurfaceVariantsAndSupports(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        for(var side:Direction.values())level.setBlockAndUpdate(pos.relative(side),Blocks.STONE.defaultBlockState());
        for(int variant=0;variant<=4;variant++) {
            var state=TaintFibreBlock.faces(TCArcane.TAINT_FIBRES.get().defaultBlockState().setValue(TaintFibreBlock.VARIANT,variant),level,pos);
            for(var side:Direction.values())test.assertTrue(state.getValue(MultifaceBlock.getFaceProperty(side)),"All variants keep attached fibres");
        }
        for(var side:Direction.values()) {
            level.setBlockAndUpdate(pos.relative(side),TCArcane.TAINT_CRUST.get().defaultBlockState());
            var state=TaintFibreBlock.faces(TCArcane.TAINT_FIBRES.get().defaultBlockState(),level,pos);
            test.assertTrue(!state.getValue(MultifaceBlock.getFaceProperty(side)),"No fibre face over old solid taint material");
            level.setBlockAndUpdate(pos.relative(side),Blocks.STONE.defaultBlockState());
        }
        test.assertTrue(!TaintFibreAppearance.hasCross(0,true)&&!TaintFibreAppearance.hasCross(1,false)&&!TaintFibreAppearance.hasCross(2,false),"Bare and unsupported grass do not grow crosses");
        test.assertTrue(TaintFibreAppearance.hasCross(1,true)&&TaintFibreAppearance.hasCross(2,true)&&TaintFibreAppearance.hasCross(3,false)&&TaintFibreAppearance.hasCross(4,false),"Floor grass and both independent stalks");
        int[] choices=new int[4];
        for(int x=-64;x<64;x++)for(int z=-32;z<32;z++)for(int side=0;side<6;side++) {
            int value=TaintFibreAppearance.overlay(x,71,z,side);choices[value]++;
            test.assertTrue(value==TaintFibreAppearance.overlay(x,71,z,side),"Overlay stable without world RNG");
            var offset=TaintFibreAppearance.plantOffset(x,71,z);
            test.assertTrue(Math.abs(offset[0])<=.25&&Math.abs(offset[1])<=.25,"Grass displacement stays in old quarter-block bounds");
        }
        int visible=choices[1]+choices[2]+choices[3];
        test.assertTrue(visible>1800&&visible<3100&&choices[1]>0&&choices[2]>0&&choices[3]>0,"Rare overlays retain all three variants: "+java.util.Arrays.toString(choices));
        for(int value:new int[]{Integer.MIN_VALUE,Integer.MAX_VALUE})test.assertTrue(TaintFibreAppearance.overlay(value,value,value,5)>=0,"Extreme positions preserve bounded int seed behavior");
        tc4revival.TC4Revival.LOGGER.info("Taint appearance rules passed: five variants, six faces, growth support, overlay counts {}",java.util.Arrays.toString(choices));test.succeed();
    }
}
