package tc4revival.content;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class FluxScrubberServerTests {
    private static FluxScrubberBlockEntity machine(GameTestHelper test) { var pos=test.absolutePos(new BlockPos(4,4,4));test.getLevel().setBlockAndUpdate(pos,TCArcane.FLUX_SCRUBBER.get().defaultBlockState());return (FluxScrubberBlockEntity)test.getLevel().getBlockEntity(pos); }
    private static CompoundTag stored(int power,int charges,int essentia) { var tag=new CompoundTag();tag.putInt("power",power);tag.putInt("charges",charges);tag.putInt("essentia",essentia);return tag; }
    @GameTest(template="wand_test_empty")
    public static void fluxScrubberScanQuantaAndRadius(GameTestHelper test) {
        var tile=machine(test);var level=test.getLevel();var pos=tile.getBlockPos();tile.load(stored(100,0,0));tile.tick();
        test.assertTrue(tile.uncheckedCount()==35937-16&&tile.power()==100,"Empty search checks sixteen cells of the shuffled cube without spending power");
        tile.load(stored(100,0,0));var goo=pos.east();var gas=pos.above(15);var outside=pos.above(16);
        level.setBlockAndUpdate(goo,TCArcane.FLUX_GOO.get().defaultBlockState().setValue(FiniteFluidBlock.QUANTA,2));level.setBlockAndUpdate(gas,TCArcane.FLUX_GAS.get().defaultBlockState().setValue(FiniteFluidBlock.QUANTA,1));level.setBlockAndUpdate(outside,TCArcane.FLUX_GOO.get().defaultBlockState().setValue(FiniteFluidBlock.QUANTA,3));
        for(int i=0;i<7000;i++) { int before=tile.power();tile.tick();test.assertTrue(before-tile.power()<=5,"At most one quantum is removed per tick"); }
        test.assertTrue(level.isEmptyBlock(goo)&&level.isEmptyBlock(gas)&&tile.power()==85&&tile.charges()==3,"Three quanta of both flux forms cost fifteen CV and yield three charges");
        test.assertTrue(level.getBlockState(outside).getValue(FiniteFluidBlock.QUANTA)==3,"Strict radius excludes exactly sixteen blocks");
        var tag=tile.saveWithoutMetadata();var restored=new FluxScrubberBlockEntity(pos,tile.getBlockState());restored.load(tag);
        test.assertTrue(restored.power()==85&&restored.charges()==3&&restored.uncheckedCount()==0,"Energy and charges persist, randomized search queue does not");
        level.setBlockAndUpdate(outside,Blocks.AIR.defaultBlockState());pass(test,"scan-quanta-radius-save");
    }
    @GameTest(template="wand_test_empty")
    public static void fluxScrubberConversionAndTransport(GameTestHelper test) {
        var tile=machine(test);var level=test.getLevel();boolean success=false,failure=false;
        for(int sample=0;sample<32;sample++) {
            tile.load(stored(0,4,3));long seed=sample*104729L;var random=net.minecraft.util.RandomSource.create(seed);random.nextInt(1000);boolean expected=random.nextInt(4)==0;
            level.random.setSeed(seed);tile.tick();test.assertTrue(tile.charges()==0&&tile.essentiaAmount(Direction.UP)==(expected?4:3),"Each four charges independently roll one-quarter chance of one magic essentia");if(expected)success=true;else failure=true;
        }
        test.assertTrue(success&&failure,"Fixed seeds cover both outcomes");
        tile.load(stored(0,1000,4));for(int i=0;i<250;i++)tile.tick();test.assertTrue(tile.charges()==0&&tile.essentiaAmount(Direction.NORTH)==4,"Full output still consumes charges and discards overflow");
        var magic=TCAspects.catalog().require("praecantatio");
        for(var face:Direction.values()) test.assertTrue(tile.connectable(face)==(face==Direction.DOWN)&&tile.canOutput(face)==(face==Direction.DOWN)&&!tile.canInput(face)&&tile.suction(face)==0&&tile.essentiaType(face).equals(magic),"Only the placement face is connected, output-only, without suction");
        test.assertTrue(tile.takeEssentia(magic,-5,Direction.DOWN)==0&&tile.essentiaAmount(Direction.DOWN)==4,"Negative withdrawal cannot create essentia");
        test.assertTrue(tile.takeEssentia(TCAspects.catalog().require("aer"),30,Direction.UP)==0&&tile.essentiaAmount(Direction.DOWN)==4,"Wrong face and aspect cannot withdraw");
        test.assertTrue(tile.takeEssentia(magic,30,Direction.DOWN)==4&&tile.essentiaAmount(Direction.DOWN)==0,"Correct port returns only the stored amount");
        test.assertTrue(tile.addEssentia(magic,1,Direction.DOWN)==0,"Direct insertion is rejected");pass(test,"conversion-capacity-transport");
    }
    @GameTest(template="wand_test_empty")
    public static void fluxScrubberVisRecipeAndPlacement(GameTestHelper test) {
        var tile=machine(test);var level=test.getLevel();var pos=tile.getBlockPos();tile.tick();test.assertTrue(tile.power()==0&&tile.uncheckedCount()==0,"No wind CV means no scanning");
        var air=TCAspects.catalog().require("aer");var nodePos=pos.east(3);level.setBlockAndUpdate(nodePos,TCArcane.ENERGIZED_NODE.get().defaultBlockState());var node=(EnergizedNodeBlockEntity)level.getBlockEntity(nodePos);node.configure(new AspectAmounts().add(air,100),AuraNodeType.NORMAL,null,"scrubber-air");node.tick();
        tile.load(stored(4,0,0));tile.tick();test.assertTrue(tile.power()==14&&tile.uncheckedCount()>0,"Below-five reserve draws up to ten CV, retaining a fourteen-point reserve");
        tile.tick();test.assertTrue(tile.power()==14,"Reserve at least five prevents further CV requests");level.setBlockAndUpdate(nodePos,Blocks.AIR.defaultBlockState());
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ScrubberTest"));
        for(var face:Direction.values()) {
            var context=new net.minecraft.world.item.context.BlockPlaceContext(level,player,net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(TCItems.FLUX_SCRUBBER.get()),new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),face,pos,false));
            test.assertTrue(TCArcane.FLUX_SCRUBBER.get().getStateForPlacement(context).getValue(FluxScrubberBlock.FACING)==face.getOpposite(),"Six-way placement output faces the supporting block");
        }
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int slot){return ItemStack.EMPTY;}};
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);grid.setItem(1,new ItemStack(TCItems.BELLOWS.get()));grid.setItem(3,new ItemStack(Items.IRON_BARS));grid.setItem(4,new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:vis_filter"))));grid.setItem(5,new ItemStack(Items.IRON_BARS));grid.setItem(6,new ItemStack(TCItems.ARCANE_STONE_BRICKS.get()));grid.setItem(7,new ItemStack(TCArcane.ESSENTIA_TUBE.get()));grid.setItem(8,new ItemStack(TCItems.ARCANE_STONE_BRICKS.get()));
        var recipe=level.getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()).stream().filter(r->r.getId().getPath().endsWith("flux_scrubber")).findFirst().orElseThrow();
        test.assertTrue(recipe.matches(grid,level)&&recipe.assemble(grid,level.registryAccess()).is(TCItems.FLUX_SCRUBBER.get())&&recipe.visCost(grid).get(air)==8&&recipe.visCost(grid).values().stream().mapToInt(Integer::intValue).sum()==40&&recipe.unlocked(grid,k->k.equals("FLUXSCRUB"))&&!recipe.unlocked(grid,k->false),"Actual recipe has forty total vis and FLUXSCRUB research gate");
        test.assertTrue(tile.getBlockState().getCollisionShape(level,pos).bounds().getXsize()==1,"Legacy full-block collision is retained");
        var drops=net.minecraft.world.level.block.Block.getDrops(tile.getBlockState(),level,pos,tile);test.assertTrue(drops.stream().anyMatch(s->s.is(TCItems.FLUX_SCRUBBER.get())&&!s.hasTag()),"Drop is the uncharged device");pass(test,"vis-placement-recipe-drop");
    }
    private static void pass(GameTestHelper test,String name) { com.mojang.logging.LogUtils.getLogger().info("Flux scrubber test passed: {}",name);test.succeed(); }
}
