package thmodern.vat;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
import tc4revival.content.*;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class PigInfusionServerTests {
    @GameTest(template="empty")
    public static void orePigConsumesOneStoneAndPersistsProgress(GameTestHelper test){
        var pig=Horizons.ORE_PIG.get().create(test.getLevel());pig.moveTo(test.absolutePos(new BlockPos(3,3,3)).getCenter());
        var tag=new CompoundTag();pig.save(tag);tag.putInt("NuggetProgress",99);pig.load(tag);
        var stone=new ItemEntity(test.getLevel(),pig.getX(),pig.getY(),pig.getZ(),new ItemStack(Items.COBBLESTONE,2));stone.setNoPickUpDelay();
        test.assertTrue(pig.eatStone(stone)&&stone.getItem().getCount()==1&&pig.nuggetProgress()>=0&&pig.nuggetProgress()<15,"One stone pays progress and crosses threshold once");
        var drops=test.getLevel().getEntitiesOfClass(ItemEntity.class,pig.getBoundingBox().inflate(2));test.assertTrue(drops.size()==1&&drops.get(0).getItem().getCount()==1,"Exactly one nugget is emitted");
        var saved=new CompoundTag();pig.save(saved);var restored=Horizons.ORE_PIG.get().create(test.getLevel());restored.load(saved);test.assertTrue(restored.nuggetProgress()==pig.nuggetProgress(),"Fractional progress survives save");
        stone.setItem(new ItemStack(Items.DIRT));test.assertTrue(!pig.eatStone(stone),"Non-cobblestone cannot be consumed");drops.forEach(ItemEntity::discard);test.succeed();
    }
    @GameTest(template="empty")
    public static void taintEaterRemovesBlockHealsAndCleansBiome(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,2,3));level.setBlock(pos,TCArcane.TAINT_CRUST.get().defaultBlockState(),3);WorldBiomeChanges.changeColumn(level,pos,TCBiomes.TAINTED_LAND);
        var pig=Horizons.TAINT_EATER_PIG.get().create(level);pig.moveTo(pos.above().getCenter());pig.setHealth(5);
        test.assertTrue(pig.eatTaint(pos)&&level.isEmptyBlock(pos)&&pig.getHealth()==6,"Taint block consumed without drops and pig healed");
        test.assertTrue(EtherealBloomBlockEntity.storedBiome(level,pos).is(Biomes.PLAINS),"Biome cell column cleansed to plains");
        level.setBlock(pos,TCArcane.TAINT_CRUST.get().defaultBlockState(),3);test.assertTrue(!pig.eatTaint(pos),"Twenty-tick cooldown prevents immediate second consumption");
        var save=new CompoundTag();pig.save(save);test.assertTrue(save.getInt("EatingCooldown")==20,"Cooldown persists");test.succeed();
    }
    @GameTest(template="empty")
    public static void taintEaterRejectsOrdinaryTerrainAndClearsTaintEffects(GameTestHelper test){
        var pig=Horizons.TAINT_EATER_PIG.get().create(test.getLevel());var pos=test.absolutePos(new BlockPos(3,2,3));pig.moveTo(pos.above().getCenter());test.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),3);
        test.assertTrue(!pig.eatTaint(pos)&&test.getLevel().getBlockState(pos).is(Blocks.STONE),"Ordinary blocks are not food");
        for(var effect:java.util.List.of(TCEffects.FLUX_TAINT.get(),TCEffects.VIS_EXHAUST.get(),TCEffects.INFECTIOUS_VIS_EXHAUST.get()))pig.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect,100));pig.aiStep();
        test.assertTrue(!pig.hasEffect(TCEffects.FLUX_TAINT.get())&&!pig.hasEffect(TCEffects.VIS_EXHAUST.get())&&!pig.hasEffect(TCEffects.INFECTIOUS_VIS_EXHAUST.get()),"All three legacy taint/exhaustion effects removed");test.succeed();
    }
}
