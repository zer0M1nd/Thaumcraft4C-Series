package tc4revival.content;

import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class InfernalFurnaceServerTests {
    private InfernalFurnaceServerTests() {}
    @GameTest(template="wand_test_empty")
    public static void countdownQueueReloadAndWasteRules(GameTestHelper test) {
        var host=new FurnaceHost(); var process=new InfernalFurnaceProcess();
        test.assertTrue(process.insert(new ItemStack(Items.COBBLESTONE,2),host),"Whole input must be accepted");
        process.tick(host); test.assertTrue(process.cookTime()==140,"The first tick starts a countdown without consuming an input");
        for (int i=0;i<139;i++) process.tick(host);
        test.assertTrue(host.output.isEmpty() && process.item(0).getCount()==2,"Input remains until the last countdown tick");
        process.tick(host);
        test.assertTrue(host.output.size()==1 && process.item(0).getCount()==1 && process.cookTime()==0,"Only one item finishes per cycle");
        process.tick(host); test.assertTrue(process.cookTime()==140,"The next cycle begins on a separate tick");
        process.addFireEssentia(host); host.bellows=3;
        var saved=new CompoundTag(); process.save(saved);
        var restored=new InfernalFurnaceProcess(); restored.load(saved); restored.tick(host);
        test.assertTrue(restored.cookTime()==139 && restored.speedyTime()==599,"Reload retains the pending countdown");
        for (int i=0;i<139;i++) restored.tick(host);
        test.assertTrue(restored.item(0).isEmpty() && host.output.size()==2,"The restored stack must finish exactly once");
        int speed=restored.speedyTime(); restored.tick(host);
        test.assertTrue(restored.speedyTime()==speed,"Idle furnaces retain accumulated fire essentia time");
        test.assertTrue(restored.insert(new ItemStack(Items.DIRT,64),host) && restored.item(0).isEmpty() && host.destroyed==1,
                "An accepted nonsmeltable entity must be destroyed, without occupying a slot");
        for (int slot=0;slot<32;slot++) test.assertTrue(restored.insert(new ItemStack(Items.COBBLESTONE,64),host),"Every internal slot must accept a full stack");
        test.assertTrue(!restored.insert(new ItemStack(Items.COBBLESTONE),host),"A full queue must leave an incoming item entity unconsumed");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void wandFormationFireInletBellowsAndDismantling(GameTestHelper test) {
        var level=test.getLevel(); var origin=test.absolutePos(new BlockPos(2,1,2)); var center=origin.offset(1,1,1);
        for (var outlet:Direction.Plane.HORIZONTAL) {
            build(test,origin,outlet);
            test.assertTrue(InfernalFurnaceStructure.match(level,origin).orElseThrow()==outlet,"Blueprint must find each of the four outlet orientations");
        }
        build(test,origin,Direction.EAST);
        level.setBlock(origin.offset(1,1,0),Blocks.IRON_BARS.defaultBlockState(),18);
        test.assertTrue(InfernalFurnaceStructure.match(level,origin).isEmpty(),"Two outlets must reject the blueprint");
        level.setBlock(origin.offset(1,1,0),Blocks.OBSIDIAN.defaultBlockState(),18);
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"FurnaceFormation"));
        player.setPos(origin.getCenter());
        tc4revival.research.KnowledgeSavedData.get(level.getServer()).update(player.getUUID(),player.getRandom()::nextInt,
                knowledge->knowledge.rememberResearch("INFERNALFURNACE"));
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wand);
        var clicked=center.east();
        var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(clicked),Direction.EAST,clicked,false));
        test.assertTrue(TCItems.CASTING_WAND.get().onItemUseFirst(wand,context).consumesAction(),"Actual wand entry must form the researched structure");
        var fire=tc4revival.aspect.TCAspects.catalog().require("ignis");
        test.assertTrue(TCItems.CASTING_WAND.get().vis(wand,fire)==5000,"Formation must charge 50 fire vis with a gold cap");
        var core=(InfernalFurnaceBlockEntity)level.getBlockEntity(center);
        test.assertTrue(core.core() && core.outlet()==Direction.EAST,"Formation must retain the outlet on its core");
        var inletPos=center.west(); var sourcePos=center.west(2);
        level.setBlock(sourcePos,TCArcane.ALEMBIC.get().defaultBlockState(),18);
        var alembic=(AlembicBlockEntity)level.getBlockEntity(sourcePos); alembic.addToContainer(fire,2);
        var inlet=(InfernalFurnaceBlockEntity)level.getBlockEntity(inletPos);
        for (int i=0;i<4;i++) inlet.tick();
        test.assertTrue(alembic.amount()==2,"The inlet must wait for its fifth draw tick");
        inlet.tick();
        test.assertTrue(alembic.amount()==1 && core.process().speedyTime()==600 && inlet.suction(Direction.WEST)==0,
                "One unit of fire essentia adds 600 ticks and disables suction above the threshold");
        level.setBlock(sourcePos,TCArcane.BELLOWS.get().defaultBlockState().setValue(BellowsBlock.FACING,Direction.EAST),18);
        test.assertTrue(core.bellows()==1,"A correctly oriented bellows two blocks from the core must count");
        level.setBlock(sourcePos,TCArcane.BELLOWS.get().defaultBlockState().setValue(BellowsBlock.FACING,Direction.WEST),18);
        test.assertTrue(core.bellows()==0,"An outward-facing bellows must not accelerate the furnace");
        var broken=origin; level.destroyBlock(broken,false);
        test.assertTrue(!level.getBlockState(center).is(TCArcane.INFERNAL_FURNACE.get())
                && level.getBlockState(clicked).is(Blocks.IRON_BARS),"Breaking a checked shell member must dismantle the core and restore the outlet");
        var blazes=level.getEntitiesOfClass(net.minecraft.world.entity.monster.Blaze.class,new net.minecraft.world.phys.AABB(center).inflate(2));
        test.assertTrue(blazes.size()==1 && blazes.get(0).hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION)
                && blazes.get(0).hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE),"Dismantling releases one empowered blaze");
        tc4revival.TC4Revival.LOGGER.info("wandFormationFireInletBellowsAndDismantling PASS");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void zeroBaseMetalAndMeatBonuses(GameTestHelper test) {
        var metal=InfernalBonusRecipe.find(test.getLevel(),new ItemStack(Items.IRON_ORE)).orElseThrow();
        var meat=InfernalBonusRecipe.find(test.getLevel(),new ItemStack(Items.BEEF)).orElseThrow();
        var random=net.minecraft.util.RandomSource.create(2718); boolean missing=false,awarded=false;
        for (int i=0;i<64;i++) {
            var rolled=metal.roll(0,random); missing|=rolled.isEmpty(); awarded|=!rolled.isEmpty();
            test.assertTrue(rolled.isEmpty() || rolled.is(Items.IRON_NUGGET) && rolled.getCount()==1,
                    "A zero-base metal bonus must use the vanilla iron nugget and never create a phantom zero-count item");
            var food=meat.roll(3,random);
            test.assertTrue(food.isEmpty() || food.is(TCItems.food(FoodType.BEEF)) && food.getCount()>=1 && food.getCount()<=3,
                    "Legacy registration resets meat base count to zero, with at most one bonus per bellows");
        }
        test.assertTrue(missing && awarded,"Metal bonus must allow both failed and successful rolls"); test.succeed();
    }
    private static void build(GameTestHelper test,BlockPos origin,Direction outlet) {
        for (int y=0;y<3;y++) for (int x=0;x<3;x++) for (int z=0;z<3;z++)
            test.getLevel().setBlock(origin.offset(x,y,z),InfernalFurnaceStructure.material(x,y,z).defaultBlockState(),18);
        test.getLevel().setBlock(origin.offset(1+outlet.getStepX(),1,1+outlet.getStepZ()),Blocks.IRON_BARS.defaultBlockState(),18);
    }
    private static  class FurnaceHost implements InfernalFurnaceProcess.Host {
        int bellows,destroyed;
        final ArrayList<ItemStack> output=new ArrayList<>();
        @Override public Optional<InfernalFurnaceProcess.Smelting> recipe(ItemStack input) {
            return input.is(Items.COBBLESTONE) ? Optional.of(new InfernalFurnaceProcess.Smelting(new ItemStack(Items.STONE),.1f)) : Optional.empty();
        }
        @Override public int bellows() { return bellows; }
        @Override public int drainFireVis(int amount) { return 0; }
        @Override public void eject(InfernalFurnaceProcess.Smelting result,ItemStack input) { output.add(result.output().copy()); }
        @Override public void destroyedInput() { destroyed++; }
        @Override public void smelted() {}
        @Override public void changed() {}
    }
}
