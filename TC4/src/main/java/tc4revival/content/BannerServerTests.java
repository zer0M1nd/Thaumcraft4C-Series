package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class BannerServerTests {
    @GameTest(template="wand_test_empty")
    public static void nativeCrimsonBannerPreservesPattern(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());CrimsonBanner.place(level,pos,8);
        test.assertTrue(level.getBlockState(pos).is(Blocks.RED_BANNER),"Generated banner is vanilla");
        var banner=(BannerBlockEntity)level.getBlockEntity(pos);
        test.assertTrue(ItemStack.isSameItemSameTags(banner.getItem(),CrimsonBanner.item()),"Crimson pattern assigned");
        var restored=new BannerBlockEntity(pos,banner.getBlockState());restored.load(banner.saveWithFullMetadata());
        test.assertTrue(ItemStack.isSameItemSameTags(restored.getItem(),CrimsonBanner.item()),"Native NBT survives save/load");
        var drops=Block.getDrops(banner.getBlockState(),level,pos,banner);
        test.assertTrue(drops.size()==1&&drops.get(0).is(net.minecraft.world.item.Items.RED_BANNER)&&java.util.Objects.equals(BannerBlockEntity.getItemPatterns(drops.get(0)),BannerBlockEntity.getItemPatterns(CrimsonBanner.item())),"Vanilla loot keeps pattern");
        test.succeed();
    }
}
