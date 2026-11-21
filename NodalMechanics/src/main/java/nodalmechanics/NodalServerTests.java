package nodalmechanics;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;
import tc4revival.content.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class NodalServerTests {
    private static TransientCraftingContainer grid() {
        return new TransientCraftingContainer(new AbstractContainerMenu(null, 0) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p, int slot) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player p) { return true; }
        }, 3, 3);
    }
    private static NodeInfusionRecipe node(GameTestHelper t) {
        return (NodeInfusionRecipe)t.getLevel().getRecipeManager().byKey(NodalMechanics.id("node")).orElseThrow();
    }
    private static ItemStack salt() { return new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
            net.minecraft.resources.ResourceLocation.parse("thaumcraft:salis_mundus"))); }
    @GameTest(template="wand_test_empty")
    public static void nodalOriginalMatrixAndAllAttunementSizes(GameTestHelper t) {
        var level=t.getLevel(); var grid=grid(); var air=TCAspects.catalog().require("aer");
        var matrix=(ArcaneRecipe)level.getRecipeManager().byKey(NodalMechanics.id("matrix")).orElseThrow();
        for(int slot=0;slot<9;slot++) grid.setItem(slot,slot==4?new ItemStack(TCItems.ESSENTIA_JAR.get()):slot%2==0?salt():new ItemStack(TCItems.GREATWOOD_LOG.get()));
        t.assertTrue(matrix.matches(grid,level)&&matrix.assemble(grid,level.registryAccess()).is(NodalMechanics.MATRIX.get()),"Original salis/greatwood/jar matrix recipe");
        t.assertTrue(matrix.visCost(grid).get(TCAspects.catalog().require("ordo"))==20&&matrix.visCost(grid).get(TCAspects.catalog().require("perditio"))==20,"Original 20 order + 20 entropy");
        for(int count=1;count<=8;count++) {
            grid.clearContent(); grid.setItem(0,new ItemStack(NodalMechanics.MATRIX.get(),2));
            for(int i=1;i<=count;i++)grid.setItem(i,EssentiaPhialItem.filled(air));
            var recipe=(ArcaneRecipe)level.getRecipeManager().byKey(NodalMechanics.id("attune_"+count)).orElseThrow();
            t.assertTrue(recipe.matches(grid,level),"Shapeless match "+count);
            var result=recipe.assemble(grid,level.registryAccess());
            t.assertTrue(result.getCount()==1&&MatrixItem.counts(result).get("aer")==count,"Counts occupied phials, not stack size or vis amount");
            t.assertTrue(grid.getItem(0).getCount()==2&&!grid.getItem(0).hasTag(),"Preview does not mutate ingredients");
            t.assertTrue(!recipe.unlocked(grid,key->false)&&recipe.unlocked(grid,NodalMechanics.RESEARCH::equals),"Research remains server gated");
            t.assertTrue(recipe.visCost(grid).size()==1&&recipe.visCost(grid).get(TCAspects.catalog().require("ordo"))==10,"Original attunement costs 10 order only");
            grid.setItem(0,result); t.assertTrue(recipe.assemble(grid,level.registryAccess()).isEmpty(),"Cannot retune an attuned matrix");
            grid.setItem(0,new ItemStack(NodalMechanics.MATRIX.get()));grid.setItem(1,new ItemStack(TCItems.ESSENTIA_PHIAL.get()));
            t.assertTrue(recipe.assemble(grid,level.registryAccess()).isEmpty(),"Empty malformed phials fail closed");
        }
        com.mojang.logging.LogUtils.getLogger().info("NODAL_PASS nodaloriginalmatrixandallattunementsizes");t.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void nodalDynamicInfusionAndNetworkRoundTrip(GameTestHelper t) {
        var recipe=node(t); var center=MatrixItem.attuned(List.of("aer","aer","praecantatio"));
        var inputs=new InfusionRecipe.Input(center,List.of(salt(),salt()),key->true);
        t.assertTrue(recipe.matches(inputs,t.getLevel()),"Compound and repeated aspects supported");
        t.assertTrue(!recipe.matches(new InfusionRecipe.Input(center,List.of(salt()),key->true),t.getLevel()),"Two pedestal salis required");
        t.assertTrue(!recipe.matches(new InfusionRecipe.Input(center,List.of(salt(),salt()),key->false),t.getLevel()),"Unknown research cannot start infusion");
        var plan=recipe.plan(center);var jar=NodeJarData.read(plan.output().apply(center).getTag());
        t.assertTrue(plan.aspects().amount(TCAspects.catalog().require("aer"))==300&&plan.aspects().amount(TCAspects.catalog().require("praecantatio"))==150,"Costs are 150 per phial");
        t.assertTrue(jar.aspects().amount(TCAspects.catalog().require("aer"))==30&&jar.aspects().amount(TCAspects.catalog().require("praecantatio"))==15&&plan.instability()==10,"Exact node capacity and instability");
        var buffer=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try { var serializer=NodalMechanics.NODE_RECIPE.get();serializer.toNetwork(buffer,recipe);var copy=serializer.fromNetwork(recipe.getId(),buffer);
            t.assertTrue(copy.matches(inputs,t.getLevel())&&copy.plan(center).aspects().view().equals(plan.aspects().view()),"Recipe packet preserves dynamic behavior");
        } finally { buffer.release(); }
        var invalid=center.copy();invalid.getOrCreateTag().putString("aspects","nonexistent");
        t.assertTrue(!recipe.acceptsCenter(invalid)&&!recipe.acceptsCenter(new ItemStack(NodalMechanics.MATRIX.get())),"Invalid and blank centers rejected");
        com.mojang.logging.LogUtils.getLogger().info("NODAL_PASS nodaldynamicinfusionandnetworkroundtrip");t.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void nodalCraftResumesWithoutRerollOrDuplication(GameTestHelper t) {
        var recipe=node(t);var center=MatrixItem.attuned(List.of("ignis"));
        var first=new BlockPos(2,1,2);var second=new BlockPos(4,1,2);
        t.setBlock(first,TCArcane.ARCANE_PEDESTAL.get());t.setBlock(second,TCArcane.ARCANE_PEDESTAL.get());
        var a=(ArcanePedestalBlockEntity)t.getBlockEntity(first);var b=(ArcanePedestalBlockEntity)t.getBlockEntity(second);
        a.setItem(0,salt());b.setItem(0,salt());
        var plan=recipe.planForCraft(center,t.getLevel());var expected=plan.output().apply(center);
        var process=new InfusionProcess();process.start(plan,center,"nodal-test",-100);
        var random=RandomSource.create(1002);
        class Host implements InfusionProcess.Host {
            int drained,finished;ItemStack output=ItemStack.EMPTY;
            public boolean centerMatches(ItemStack input){return ItemStack.isSameItemSameTags(input,center);}
            public InfusionProcess.Experience drainExperience(){throw new AssertionError("No XP cost");}
            public boolean drainEssentia(Aspect aspect){t.assertTrue(aspect.tag().equals("ignis"),"Correct essence");drained++;return true;}
            public List<ArcanePedestalBlockEntity> pedestals(){return List.of(a,b);}
            public void ingredientStarted(BlockPos pos){}
            public void instabilityEvent(int event){throw new AssertionError("Stable test altar");}
            public void refreshSurroundings(){}
            public void failed(){throw new AssertionError("No center loss");}
            public void finish(InfusionOutput result,String player){finished++;output=result.apply(center);}
            public void changed(){}
        }
        var host=new Host();
        for(int i=0;i<50;i++)process.cycle(random,host);
        process.load(process.save());
        t.assertTrue(process.remaining().total()==100,"Already paid essence survives save");
        for(int i=0;i<200;i++)process.cycle(random,host);
        t.assertTrue(host.finished==1&&host.drained==150&&a.isEmpty()&&b.isEmpty(),"Exactly 150 essence and two salis consumed once");
        t.assertTrue(ItemStack.isSameItemSameTags(host.output,expected),"Node type, modifier and identity survive save without reroll");
        com.mojang.logging.LogUtils.getLogger().info("NODAL_PASS nodalcraftresumeswithoutrerollorduplication");t.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void nodalRealAltarCallsCraftHookAndPersistsRandomNode(GameTestHelper t) {
        var matrixPos=new BlockPos(3,3,3);var centerPos=matrixPos.below(2);
        t.setBlock(matrixPos,TCArcane.INFUSION_MATRIX.get());t.setBlock(centerPos,TCArcane.ARCANE_PEDESTAL.get());
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})t.setBlock(centerPos.offset(x,0,z),TCArcane.INFUSION_PILLAR.get());
        var center=(ArcanePedestalBlockEntity)t.getBlockEntity(centerPos);
        center.setItem(0,MatrixItem.attuned(List.of("aer")));
        for(int x:new int[]{-2,2}){
            var pos=centerPos.offset(x,0,0);t.setBlock(pos,TCArcane.ARCANE_PEDESTAL.get());
            ((ArcanePedestalBlockEntity)t.getBlockEntity(pos)).setItem(0,salt());
        }
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(t.getLevel(),new com.mojang.authlib.GameProfile(
                java.util.UUID.fromString("f12b37c9-e8a1-4b82-b46b-512b56392020"),"NodalTest"));
        var knowledge=tc4revival.research.KnowledgeSavedData.get(t.getLevel().getServer());
        knowledge.update(player.getUUID(),bound->0,k->{k.rememberResearch(NodalMechanics.RESEARCH);return null;});
        var altar=(InfusionMatrixBlockEntity)t.getBlockEntity(matrixPos);altar.activate();altar.start(player);
        t.assertTrue(altar.crafting(),"Actual altar discovers custom serializer recipe");
        var saved=altar.saveWithoutMetadata();var process=saved.getCompound("Process");
        var output=InfusionOutput.load(process.getCompound("Output")).apply(center.getItem(0));
        t.assertTrue(output.is(TCItems.NODE_JAR.get()),"Craft hook stores jar output");
        var node=NodeJarData.read(output.getTag());
        t.assertTrue(!node.id().equals("nodalmechanics-preview")&&!node.id().isEmpty()&&node.aspects().total()==15,"Actual craft realizes random node rather than preview");
        var restored=new InfusionMatrixBlockEntity(t.absolutePos(matrixPos),altar.getBlockState());restored.setLevel(t.getLevel());restored.load(saved);
        t.assertTrue(restored.crafting()&&restored.saveWithoutMetadata().getCompound("Process").getCompound("Output").equals(process.getCompound("Output")),"World block entity load preserves selected node");
        com.mojang.logging.LogUtils.getLogger().info("NODAL_PASS nodalrealaltarcalls crafthook");t.succeed();
    }
}
