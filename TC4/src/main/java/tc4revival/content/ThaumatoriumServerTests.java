package tc4revival.content;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class ThaumatoriumServerTests {
    private ThaumatoriumServerTests() {}
    @GameTest(template="wand_test_empty")
    public static void selectionBackpressureAspectOrderAndCompletion(GameTestHelper test) {
        var host=new Host(); var process=new ThaumatoriumProcess();
        var recipe=recipe("controller_test",java.util.Map.of("ignis",1,"aer",2)); host.recipes.put(recipe.getId(),recipe);
        host.input=new ItemStack(Items.IRON_INGOT,2);
        test.assertTrue(process.add(TCAspects.catalog().require("aer"),1,host)==1,"Unsolicited essentia before a recipe must be rejected safely");
        test.assertTrue(process.toggle(recipe,"first-player",host),"The first recipe must fit an unupgraded controller");
        host.outputFits=false;
        for (int i=0;i<5;i++) process.tick(host);
        test.assertTrue(host.drawn.isEmpty(),"A full output inventory must prevent essentia acquisition");
        host.outputFits=true;
        for (int i=0;i<15;i++) process.tick(host);
        test.assertTrue(host.drawn.equals(java.util.List.of("aer","aer","ignis")) && host.completed==0,
                "Five-tick acquisition must follow sorted aspect names, taking only one unit per pass");
        for (int i=0;i<5;i++) process.tick(host);
        test.assertTrue(host.completed==1 && host.input.getCount()==1 && process.stored().isEmpty() && host.author.equals("first-player"),
                "The following pass consumes one catalyst, clears stored essentia and credits the selecting player");
        host.power=true;
        for (int i=0;i<10;i++) process.tick(host);
        test.assertTrue(host.drawn.size()==3,"Redstone must pause automatic recipe processing"); test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void removedUpgradeRetainsLegacyCacheUntilReload(GameTestHelper test) {
        var host=new Host(); host.limit=3;
        var a=recipe("cache_a",java.util.Map.of("aer",1)); var b=recipe("cache_b",java.util.Map.of("ignis",1)); var c=recipe("cache_c",java.util.Map.of("terra",1));
        for (var recipe:java.util.List.of(a,b,c)) host.recipes.put(recipe.getId(),recipe);
        var process=new ThaumatoriumProcess(); process.refreshLimit(host);
        process.toggle(a,"A",host); process.toggle(b,"B",host);
        host.limit=1; process.refreshLimit(host);
        test.assertTrue(process.selected().equals(java.util.List.of(a.getId())),"Removing upgrades truncates recipe selections from the tail");
        test.assertTrue(!process.toggle(c,"C",host),"A forged over-capacity menu action must be rejected");
        host.limit=3; process.refreshLimit(host); process.toggle(c,"C",host); process.toggle(a,"A",host);
        host.input=new ItemStack(Items.IRON_INGOT);
        for (int i=0;i<5;i++) process.tick(host);
        test.assertTrue(process.suction().tag().equals("terra") && process.stored().amount(TCAspects.catalog().require("terra"))==1,
                "Removing an upgrade keeps recipe, cost and author aligned immediately");
        var saved=new CompoundTag(); process.save(saved); var restored=new ThaumatoriumProcess(); restored.load(saved); host.drawn.clear();
        for (int i=0;i<5;i++) restored.tick(host);
        test.assertTrue(restored.stored().isEmpty() && host.drawn.isEmpty(),
                "Reload uses already paid essentia without drawing twice");
        test.assertTrue(host.author.equals("C"),"Surviving recipe retains its actual author");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void recipeReloadCannotSpendObsoleteCosts(GameTestHelper test) {
        var host=new Host(); var process=new ThaumatoriumProcess();
        var before=recipe("reload_cost",java.util.Map.of("aer",1)); host.recipes.put(before.getId(),before);
        host.input=new ItemStack(Items.IRON_INGOT,2); process.toggle(before,"author",host);
        for (int i=0;i<5;i++) process.tick(host);
        test.assertTrue(process.stored().amount(TCAspects.catalog().require("aer"))==1,"The first recipe has acquired its complete old cost");
        var replacement=recipe("reload_cost",java.util.Map.of("ignis",2)); host.recipes.put(replacement.getId(),replacement);
        for (int i=0;i<5;i++) process.tick(host);
        test.assertTrue(host.completed==0 && host.input.getCount()==2
                        && process.stored().amount(TCAspects.catalog().require("ignis"))==1,
                "Replacing a recipe must invalidate old costs before the next processing pass");
        for (int i=0;i<10;i++) process.tick(host);
        test.assertTrue(host.completed==1 && host.input.getCount()==1 && process.stored().isEmpty(),
                "Only the full replacement cost allows completion; old surplus follows the legacy clear rule");
        host.recipes.clear(); process.tick(host);
        test.assertTrue(process.selected().isEmpty() && process.suction()==null,"Removed datapack recipes must clear selection and suction");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void wandBuildSharedInventoryAndSingleDropOnDismantle(GameTestHelper test) {
        var level=test.getLevel(); var lower=test.absolutePos(new BlockPos(3,2,3)); var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"FixturePlayer"));
        level.setBlock(lower.below(),TCArcane.CRUCIBLE.get().defaultBlockState(),18);
        level.setBlock(lower,TCArcane.ALCHEMICAL_CONSTRUCT.get().defaultBlockState(),18);
        level.setBlock(lower.above(),TCArcane.ALCHEMICAL_CONSTRUCT.get().defaultBlockState(),18);
        tc4revival.research.KnowledgeSavedData.get(level.getServer()).update(player.getUUID(),player.getRandom()::nextInt,k->k.rememberResearch("THAUMATORIUM"));
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wand);
        var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(lower.above()),Direction.EAST,lower.above(),false));
        test.assertTrue(TCItems.CASTING_WAND.get().onItemUseFirst(wand,context).consumesAction(),"Clicking the upper construct must form the machine");
        var core=(ThaumatoriumBlockEntity)level.getBlockEntity(lower); var top=(ThaumatoriumBlockEntity)level.getBlockEntity(lower.above());
        test.assertTrue(top.controller()==core && core.facing()==Direction.EAST,"Both halves must resolve the same oriented controller");
        test.assertTrue(TCItems.CASTING_WAND.get().vis(wand,TCAspects.catalog().require("ignis"))==8500
                && TCItems.CASTING_WAND.get().vis(wand,TCAspects.catalog().require("aqua"))==7000,"Formation charges the original fire and water costs");
        var upperInventory=top.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,Direction.UP).orElseThrow(IllegalStateException::new);
        test.assertTrue(upperInventory.insertItem(0,new ItemStack(Items.IRON_INGOT,7),false).isEmpty() && core.getItem(0).getCount()==7,
                "Automation through the upper half must fill the lower shared slot");
        level.setBlock(lower.west(),TCArcane.MNEMONIC_MATRIX.get().defaultBlockState().setValue(MnemonicMatrixBlock.FACING,Direction.EAST),3);
        test.assertTrue(core.process().limit()==3,"A rear mnemonic matrix adds two immediately available selections");
        level.destroyBlock(lower.above(),false);
        test.assertTrue(level.getBlockState(lower).is(TCArcane.ALCHEMICAL_CONSTRUCT.get()),"Missing upper half restores the lower construct");
        int dropped=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(lower).inflate(2)).stream()
                .map(net.minecraft.world.entity.item.ItemEntity::getItem).filter(stack->stack.is(Items.IRON_INGOT)).mapToInt(ItemStack::getCount).sum();
        test.assertTrue(dropped==7,"Dismantling both linked halves must drop the seven catalysts exactly once"); test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void upperInletWaitsForChestSpaceThenProducesThaumium(GameTestHelper test) {
        var level=test.getLevel(); var lower=test.absolutePos(new BlockPos(3,2,3));
        level.setBlock(lower.below(2),TCArcane.NITOR.get().defaultBlockState(),18);
        level.setBlock(lower.below(),TCArcane.CRUCIBLE.get().defaultBlockState(),18);
        var state=TCArcane.THAUMATORIUM.get().defaultBlockState().setValue(ThaumatoriumBlock.FACING,Direction.EAST);
        level.setBlock(lower,state,18); level.setBlock(lower.above(),state.setValue(ThaumatoriumBlock.UPPER,true),18);
        var core=(ThaumatoriumBlockEntity)level.getBlockEntity(lower);
        level.setBlock(lower.east(),Blocks.CHEST.defaultBlockState(),18);
        var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(lower.east());
        for (int slot=0;slot<chest.getContainerSize();slot++) chest.setItem(slot,new ItemStack(Items.STONE,64));
        var source=lower.above().north(); level.setBlock(source,TCArcane.ALEMBIC.get().defaultBlockState(),18);
        var alembic=(AlembicBlockEntity)level.getBlockEntity(source); alembic.addToContainer(TCAspects.catalog().require("praecantatio"),4);
        core.setItem(0,new ItemStack(Items.IRON_INGOT,2));
        var recipe=core.recipe(new ResourceLocation("thaumcraft","crucible/thaumium")).orElseThrow();
        test.assertTrue(core.process().toggle(recipe,"",core),"The real datapack recipe must be selectable");
        for (int i=0;i<5;i++) core.tick();
        test.assertTrue(alembic.amount()==4 && core.getItem(0).getCount()==2,"Full output storage pauses both source drain and catalyst use");
        chest.setItem(0,ItemStack.EMPTY);
        for (int i=0;i<25;i++) core.tick();
        test.assertTrue(alembic.amount()==0 && core.getItem(0).getCount()==1 && chest.getItem(0).is(TCItems.material(MaterialType.THAUMIUM))
                && chest.getItem(0).getCount()==1,"Upper-half source intake must produce one real thaumium ingot in the forward chest");
        test.succeed();
    }
    private static CrucibleRecipe recipe(String id,java.util.Map<String,Integer> costs) {
        var json=new com.google.gson.JsonObject(); json.addProperty("research","THAUMATORIUM");
        var input=new com.google.gson.JsonObject(); input.addProperty("item","minecraft:iron_ingot"); json.add("catalyst",input);
        var output=new com.google.gson.JsonObject(); output.addProperty("item","minecraft:gold_ingot"); json.add("result",output);
        var aspects=new com.google.gson.JsonObject(); costs.forEach(aspects::addProperty); json.add("aspects",aspects);
        return new CrucibleRecipe.Serializer().fromJson(new ResourceLocation("thaumcraft",id),json);
    }
    private static  class Host implements ThaumatoriumProcess.Host {
        final LinkedHashMap<ResourceLocation,CrucibleRecipe> recipes=new LinkedHashMap<>(); final ArrayList<String> drawn=new ArrayList<>();
        ItemStack input=ItemStack.EMPTY; int limit=1,completed; boolean power,outputFits=true; String author="";
        @Override public Optional<CrucibleRecipe> recipe(ResourceLocation id) { return Optional.ofNullable(recipes.get(id)); }
        @Override public ItemStack input() { return input; }
        @Override public void consumeInput() { input.shrink(1); }
        @Override public boolean heated() { return true; }
        @Override public boolean powered() { return power; }
        @Override public int recipeLimit() { return limit; }
        @Override public boolean outputFits(CrucibleRecipe recipe) { return outputFits; }
        @Override public int draw(Aspect aspect) { drawn.add(aspect.tag()); return 1; }
        @Override public void complete(CrucibleRecipe recipe,String author) { completed++; this.author=author; }
        @Override public void changed() {}
    }
}
