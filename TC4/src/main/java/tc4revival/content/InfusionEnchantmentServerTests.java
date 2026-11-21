package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class InfusionEnchantmentServerTests {
    private record Baseline(ResearchPageRecipes.Enchanting old,LegacyRandomEnchantments.Definition definition) {
        ResourceLocation id() { return ResourceLocation.parse("thaumcraft:infusion/enchantment/"+net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getKey(definition.enchantment()).getPath()); }
        List<ItemStack> components() {
            return old.ingredients().elements().stream().map(value->ResearchDisplayItems.bundled().resolve(((ResearchPage.Item)value).stack(),0).orElseThrow()).toList();
        }
        ItemStack center() {
            int id=old.enchantmentId();
            Item item=id<=4?Items.DIAMOND_BOOTS:id<=6?Items.DIAMOND_HELMET:id==7?Items.DIAMOND_CHESTPLATE:
                    id>=16&&id<=21?Items.DIAMOND_SWORD:id>=32&&id<=35?Items.DIAMOND_PICKAXE:
                    id>=48&&id<=51?Items.BOW:id==150?Items.DIAMOND_BOOTS:TCItems.ELEMENTAL_PICKAXE.get();
            var stack=new ItemStack(item);stack.setDamageValue(7);stack.getOrCreateTag().putString("AuditOwner","retained");return stack;
        }
    }
    private static List<Baseline> baselines() {
        var result=new ArrayList<Baseline>();
        // Addons can contribute enchanting pages with their own IDs. This baseline audits TC4's 24 definitions.
        for(var entry:ResearchPageRecipes.bundled().entries().values())if(entry.definition() instanceof ResearchPageRecipes.Enchanting old && old.research().equals("INFUSIONENCHANTMENT")) {
            var definition=Arrays.stream(LegacyRandomEnchantments.Definition.values()).filter(value->value.legacyId==old.enchantmentId()).findFirst().orElseThrow();
            result.add(new Baseline(old,definition));
        }
        return result;
    }
    private static InfusionRecipe recipe(GameTestHelper test,Baseline baseline) {
        return (InfusionRecipe)test.getLevel().getRecipeManager().byKey(baseline.id()).orElseThrow(()->new AssertionError("Missing loaded recipe: "+baseline.id()));
    }
    private static InfusionRecipe.Input input(ItemStack center,List<ItemStack> components) { return new InfusionRecipe.Input(center,components,key->key.equals("INFUSIONENCHANTMENT")); }
    @GameTest(template="wand_test_empty")
    public static void allLegacyInfusionEnchantmentsMatchCostsLevelsAndConflicts(GameTestHelper test) {
        // This audit deliberately exercises the legacy XP-based enchanting profile.
        var option=tc4revival.config.TCConfig.SERVER.enhancedInfusionEnchanting;boolean previous=option.get();
        try {option.set(false);allLegacyInfusionEnchantmentsMatchCostsLevelsAndConflictsLegacy(test);} finally {option.set(previous);}
    }
    private static void allLegacyInfusionEnchantmentsMatchCostsLevelsAndConflictsLegacy(GameTestHelper test) {
        var level=test.getLevel();var rows=baselines();test.assertTrue(rows.size()==24,"All 24 old runtime definitions are audited");int levels=0,pairs=0;
        for(var row:rows) {
            var recipe=recipe(test,row);var components=row.components();var enchantment=row.definition.enchantment();
            for(int previous=0;previous<=row.old.maxLevel();previous++) {
                var center=row.center();if(previous>0)center.enchant(enchantment,previous);var before=center.copy();
                boolean permitted=previous<row.old.maxLevel();
                test.assertTrue(recipe.matches(input(center,components),level)==permitted,"All levels and maximum bound: "+row.id()+"/"+previous);
                if(permitted) {
                    test.assertTrue(InfusionRecipe.find(level,input(center,components)).orElseThrow()==recipe,"Actual ordered recipe lookup chooses correct enchantment");
                    var plan=recipe.plan(center);
                    test.assertTrue(plan.enchanting()&&plan.xp()==row.old.baseXp()*(previous+1)&&plan.instability()==row.old.instability()+previous/2,"Legacy XP and instability: "+row.id());
                    test.assertTrue(plan.aspects().size()==row.old.aspects().size(),"No missing or invented aspect cost");
                    for(var cost:row.old.aspects().entrySet())test.assertTrue(plan.aspects().amount(TCAspects.catalog().require(cost.getKey()))==cost.getValue()*(previous+1),"Every old essentia cost scales with requested level");
                    var output=recipe.assemble(input(center,components),level.registryAccess());
                    test.assertTrue(output.is(center.getItem())&&output.getCount()==1&&output.getDamageValue()==7&&output.getTag().getString("AuditOwner").equals("retained")&&EnchantmentHelper.getItemEnchantmentLevel(enchantment,output)==previous+1,"Output upgrades one level and preserves central item NBT/wear");
                    test.assertTrue(ItemStack.matches(center,before),"Assembly does not mutate its input");
                }
                levels++;
            }
            for(var other:LegacyRandomEnchantments.Definition.values())if(other!=row.definition) {
                var center=row.center();center.enchant(other.enchantment(),1);
                int a=row.old.enchantmentId(),b=other.legacyId;
                boolean conflict=!enchantment.isCompatibleWith(other.enchantment())||!other.enchantment().isCompatibleWith(enchantment);
                test.assertTrue(recipe.matches(input(center,components),level)==!conflict,"Registered two-way compatibility: "+a+"/"+b);pairs++;
            }
            var extra=row.center();var companion=row.definition==LegacyRandomEnchantments.Definition.UNBREAKING||row.definition==LegacyRandomEnchantments.Definition.REPAIR?Enchantments.BLOCK_EFFICIENCY:Enchantments.UNBREAKING;
            extra.enchant(companion,2);var extraPlan=recipe.plan(extra);
            for(var cost:row.old.aspects().entrySet())test.assertTrue(extraPlan.aspects().amount(TCAspects.catalog().require(cost.getKey()))==cost.getValue()+(int)(cost.getValue()*.2f),"Other levels add ten percent each with old truncation");
            test.assertTrue(extraPlan.xp()==row.old.baseXp()&&extraPlan.instability()==row.old.instability()+1,"Other enchantments affect instability and essentia, not XP");
            test.assertTrue(!recipe.matches(new InfusionRecipe.Input(row.center(),components,key->false),level),"Research required");
            for(var item:List.of(Items.BOOK,Items.DIAMOND))test.assertTrue(recipe.matches(input(new ItemStack(item),components),level)==enchantment.canEnchant(new ItemStack(item)),"Eligibility follows the registered enchantment");
            var bad=new ArrayList<ItemStack>(components);bad.set(bad.size()-1,new ItemStack(Items.DIRT));test.assertTrue(!recipe.matches(input(row.center(),bad),level),"Salis mundus cannot be omitted");
            for(int i=0;i<components.size();i++)if(components.get(i).isDamageableItem()) {
                bad=new ArrayList<>(components);var worn=components.get(i).copy();worn.setDamageValue(1);bad.set(i,worn);
                test.assertTrue(!recipe.matches(input(row.center(),bad),level),"Pedestal gear is metadata zero, unlike the worn central tool");
            }
            var buffer=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {var codec=new InfusionRecipe.Serializer();codec.toNetwork(buffer,recipe);var decoded=codec.fromNetwork(row.id(),buffer);
                test.assertTrue(decoded.matches(input(row.center(),components),level)&&decoded.plan(row.center()).xp()==row.old.baseXp(),"Recipe synchronization preserves matching and legacy XP");
            }finally{buffer.release();}
        }
        for(String id:List.of("efficiency","silk_touch","fortune")) {
            var row=rows.stream().filter(value->value.id().getPath().equals("infusion/enchantment/"+id)).findFirst().orElseThrow();
            test.assertTrue(recipe(test,row).matches(input(new ItemStack(Items.DIAMOND_HOE),row.components()),level),"Modern hoes accept efficiency, silk touch and fortune");
        }
        tc4revival.TC4Revival.LOGGER.info("PASS allLegacyInfusionEnchantmentsMatchCostsLevelsAndConflicts: {} recipes, {} level cases, {} compatibility pairs",rows.size(),levels,pairs);test.succeed();
    }

    @GameTest(template="wand_test_empty")
    public static void enchantmentExperienceWaitsAndChargesOnlyNearbyPlayers(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,4,3));
        level.setBlock(pos,TCArcane.INFUSION_MATRIX.get().defaultBlockState(),18);
        var matrix=(InfusionMatrixBlockEntity)level.getBlockEntity(pos);
        var air=TCAspects.catalog().require("aer");var center=new ItemStack(Items.DIAMOND_PICKAXE);
        var process=new InfusionProcess();
        process.start(new InfusionRecipe.Plan(List.of(),new AspectAmounts().add(air,2),4,2,true,
                InfusionOutput.enchant(ResourceLocation.parse("minecraft:efficiency"))),center,"owner",-1000);
        class Host implements InfusionProcess.Host {
            int drained,finished;ItemStack output=ItemStack.EMPTY;
            public boolean centerMatches(ItemStack expected){return ItemStack.matches(expected,center);}
            public InfusionProcess.Experience drainExperience(){return matrix.drainExperience();}
            public boolean drainEssentia(Aspect aspect){test.assertTrue(aspect.equals(air),"Only requested source drained");drained++;return true;}
            public List<ArcanePedestalBlockEntity> pedestals(){throw new AssertionError("No ingredients in this transaction");}
            public void ingredientStarted(BlockPos position){throw new AssertionError("No ingredient phase");}
            public void instabilityEvent(int event){throw new AssertionError("Stable transaction");}
            public void refreshSurroundings(){throw new AssertionError("Source never runs out");}
            public void failed(){throw new AssertionError("Center is unchanged");}
            public void finish(InfusionOutput result,String player){finished++;output=result.apply(center);}
            public void changed(){}
        }
        var host=new Host();var random=net.minecraft.util.RandomSource.create(371);var expectedRandom=net.minecraft.util.RandomSource.create(371);
        for(int cycle=0;cycle<64;cycle++)process.cycle(random,host);
        test.assertTrue(process.save().getInt("Experience")==2&&process.remaining().amount(air)==2&&process.instability()==-996&&host.drained==0,"No nearby player means no XP, essentia, or penalty");
        var empty=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"InfusionEmptyXP"));
        var donor=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"InfusionDonorXP"));
        empty.moveTo(pos.getCenter());empty.setGameMode(GameType.CREATIVE);level.addNewPlayer(empty);
        donor.moveTo(pos.getCenter().add(12,0,0));donor.setGameMode(GameType.CREATIVE);donor.experienceLevel=2;level.addNewPlayer(donor);
        int expectedCost=2,expectedInstability=-996;
        try {
            for(int cycle=0;cycle<64;cycle++) {
                if(expectedRandom.nextInt(3)==0){expectedCost++;expectedRandom.nextInt(1);if(expectedRandom.nextInt(42)==0)expectedInstability++;}
                process.cycle(random,host);
            }
            test.assertTrue(expectedCost>2&&process.remaining().amount(air)==expectedCost&&process.instability()==expectedInstability,"Nearby zero-level players add old random costs and instability");
            test.assertTrue(donor.experienceLevel==2&&process.save().getInt("Experience")==2&&host.drained==0,"Out-of-range donor cannot pay; no essentia before XP");
            donor.moveTo(pos.getCenter().add(2,0,0));
            process.cycle(random,host);
            test.assertTrue(donor.experienceLevel==1&&empty.experienceLevel==0&&process.delay()==20&&process.save().getInt("Experience")==1,"Skip empty player, debit exactly one level, wait twenty ticks");
            test.assertTrue(matrix.sources().stream().anyMatch(source->source.entityId()==donor.getId()&&!source.essentia()),"XP drain publishes donor entity for effect synchronization");
            process.cycle(random,host);
            test.assertTrue(donor.experienceLevel==0&&host.drained==0&&process.save().getInt("Experience")==0,"Final XP cycle still precedes essentia");
            for(int cycle=0;cycle<expectedCost;cycle++)process.cycle(random,host);
            test.assertTrue(process.delay()==10&&host.drained==expectedCost&&host.finished==0,"All original and penalty essentia paid at ten-tick cadence before output");
            process.cycle(random,host);
            test.assertTrue(!process.crafting()&&host.finished==1&&EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY,host.output)==1,"Waiting craft resumes and completes once");
        }finally{empty.discard();donor.discard();}
        tc4revival.TC4Revival.LOGGER.info("PASS enchantmentExperienceWaitsAndChargesOnlyNearbyPlayers: 128 waiting cycles, {} paid essentia",expectedCost);test.succeed();
    }

    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void actualAltarEnchantmentsPreserveOtherEnchantmentsAndRejectCaps(GameTestHelper test) {
        // This audit deliberately exercises the legacy XP-based enchanting profile.
        var option=tc4revival.config.TCConfig.SERVER.enhancedInfusionEnchanting;boolean previous=option.get();
        var ordered=tc4revival.config.TCConfig.SERVER.orderedInfusion;boolean oldOrder=ordered.get();
        try {option.set(false);ordered.set(false);actualAltarEnchantmentsDrainCostsAndPreserveLegacyReloadLossLegacy(test);} finally {option.set(previous);ordered.set(oldOrder);}
    }
    private static void actualAltarEnchantmentsDrainCostsAndPreserveLegacyReloadLossLegacy(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,7,3));
        for(var target:BlockPos.betweenClosed(pos.offset(-7,-4,-7),pos.offset(7,1,7)))level.setBlock(target,Blocks.AIR.defaultBlockState(),18);
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)if(x!=0||z!=0) {
            level.setBlock(pos.offset(x,-4,z),Blocks.STONE.defaultBlockState(),18);level.setBlock(pos.offset(x,-3,z),Blocks.SKELETON_SKULL.defaultBlockState(),18);
        }
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}) {
            level.setBlock(pos.offset(x,-2,z),TCArcane.INFUSION_PILLAR.get().defaultBlockState(),18);
            level.setBlock(pos.offset(x,-1,z),TCArcane.INFUSION_PILLAR_TOP.get().defaultBlockState(),18);
        }
        level.setBlock(pos.below(2),TCArcane.ARCANE_PEDESTAL.get().defaultBlockState(),18);level.setBlock(pos,TCArcane.INFUSION_MATRIX.get().defaultBlockState(),18);
        var pedestals=new ArrayList<ArcanePedestalBlockEntity>();
        for(var offset:List.of(new BlockPos(-3,-2,0),new BlockPos(3,-2,0),new BlockPos(0,-2,-3),new BlockPos(0,-2,3))) {
            var at=pos.offset(offset);level.setBlock(at,TCArcane.ARCANE_PEDESTAL.get().defaultBlockState(),18);pedestals.add((ArcanePedestalBlockEntity)level.getBlockEntity(at));
        }
        var jars=new ArrayList<EssentiaJarBlockEntity>();
        for(int z=-2;z<=2;z++){var at=pos.offset(6,-2,z);level.setBlock(at,TCArcane.ESSENTIA_JAR.get().defaultBlockState(),18);jars.add((EssentiaJarBlockEntity)level.getBlockEntity(at));}
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"InfusionEnchantAudit"));
        player.moveTo(pos.getCenter().add(2,0,0));player.setGameMode(GameType.CREATIVE);level.addNewPlayer(player);
        KnowledgeSavedData.get(level.getServer()).update(player.getUUID(),bound->0,knowledge->knowledge.rememberResearch("INFUSIONENCHANTMENT"));
        var rows=new ArrayList<>(baselines());rows.add(rows.get(0));int count=0;
        try {
            for(int index=0;index<rows.size();index++) {
                var row=rows.get(index);boolean reload=index==24;
                var matrix=(InfusionMatrixBlockEntity)level.getBlockEntity(pos);matrix.activate();
                var center=(ArcanePedestalBlockEntity)level.getBlockEntity(pos.below(2));center.setItem(0,row.center());
                center.getItem(0).enchant(row.definition==LegacyRandomEnchantments.Definition.UNBREAKING||row.definition==LegacyRandomEnchantments.Definition.REPAIR?Enchantments.BLOCK_EFFICIENCY:Enchantments.UNBREAKING,1);
                var opaque=new net.minecraft.nbt.CompoundTag();opaque.putString("id","absent_mod:preserved");opaque.putShort("lvl",(short)3);opaque.putString("ExtensionData","keep");
                center.getItem(0).getEnchantmentTags().add(opaque);
                var before=center.getItem(0).copy();var plan=recipe(test,row).plan(before);
                for(var pedestal:pedestals)pedestal.setItem(0,ItemStack.EMPTY);
                var components=row.components();for(int i=0;i<components.size();i++)pedestals.get(i).setItem(0,components.get(i).copy());
                for(var jar:jars)if(jar.aspect()!=null)jar.takeFromContainer(jar.aspect(),jar.amount());
                int i=0;for(var cost:plan.aspects().view().entrySet())jars.get(i++).addToContainer(cost.getKey(),cost.getValue());
                EssentiaSources.refresh(matrix);player.experienceLevel=30;matrix.start(player);
                test.assertTrue(matrix.crafting()&&matrix.instability()<=0,"Real altar starts and symmetric stabilizers prevent random failures: "+row.id());
                boolean reloaded=false;int ticks=0;
                while(matrix.crafting()&&ticks++<4000) {
                    matrix.tick();
                    if(reload&&!reloaded&&player.experienceLevel<30) {
                        var saved=matrix.saveWithoutMetadata();var replacement=new InfusionMatrixBlockEntity(pos,matrix.getBlockState());replacement.load(saved);
                        level.removeBlockEntity(pos);level.setBlockEntity(replacement);matrix=replacement;reloaded=true;
                    }
                }
                test.assertTrue(!matrix.crafting()&&player.experienceLevel==30-row.old.baseXp(),"Actual altar completes after exact old XP levels: "+row.id());
                test.assertTrue(jars.stream().allMatch(jar->jar.amount()==0)&&pedestals.stream().allMatch(ArcanePedestalBlockEntity::isEmpty),"All actual source essentia and pedestal components consumed exactly");
                if(reload)test.assertTrue(reloaded&&EnchantmentHelper.getItemEnchantmentLevel(row.definition.enchantment(),center.getItem(0))==1,"Reload completes the original enchantment output");
                else test.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(row.definition.enchantment(),center.getItem(0))==1&&center.getItem(0).getDamageValue()==7&&center.getItem(0).getTag().getString("AuditOwner").equals("retained"),"Actual central output is enchanted with original NBT intact");
                test.assertTrue(InfusionOutput.onlyEnchantmentIncreased(before,center.getItem(0)),"Only the target enchantment changes, including unknown enchantment metadata");
                var capped=row.center();capped.enchant(row.definition.enchantment(),row.old.maxLevel());center.setItem(0,capped);
                for(int slot=0;slot<components.size();slot++)pedestals.get(slot).setItem(0,components.get(slot).copy());
                matrix.start(player);
                test.assertTrue(!matrix.crafting()&&ItemStack.matches(capped,center.getItem(0)),"At maximum level the altar cannot start");
                for(int slot=0;slot<components.size();slot++)test.assertTrue(ItemStack.matches(components.get(slot),pedestals.get(slot).getItem(0)),"Rejected start does not consume or alter pedestal items");
                count++;
            }
        }finally{player.discard();}
        tc4revival.TC4Revival.LOGGER.info("PASS actualAltarEnchantmentsPreserveOtherEnchantmentsAndRejectCaps: {} complete altar transactions and rejected starts",count);test.succeed();
    }

    @GameTest(template="wand_test_empty")
    public static void frozenEnchantmentsPreserveLiveDataAndMigrateOldIntents(GameTestHelper test) {
        var original=new ItemStack(Items.DIAMOND_PICKAXE);original.enchant(Enchantments.UNBREAKING,2);
        original.getOrCreateTag().putString("Owner","before");
        var operation=InfusionOutput.enchant(ResourceLocation.parse("minecraft:efficiency")).freeze("test:efficiency","INFUSIONENCHANTMENT",original);
        test.assertTrue(operation.save().contains("Enchantment")&&!operation.save().contains("Item"),"Persist enchantment intent instead of item replacement");
        var live=original.copy();live.setDamageValue(19);live.enchant(Enchantments.MENDING,1);live.getOrCreateTag().putString("Owner","during");
        var out=InfusionOutput.load(operation.save()).apply(live);
        test.assertTrue(InfusionOutput.onlyEnchantmentIncreased(live,out)&&out.getDamageValue()==19&&out.getTag().getString("Owner").equals("during"),"Reloaded intent preserves changes made while crafting");
        var old=InfusionOutput.item(operation.apply(original)).freeze("test:old","INFUSIONENCHANTMENT",original);
        test.assertTrue(InfusionOutput.onlyEnchantmentIncreased(live,old.asEnchantment(original).apply(live)),"Old valid saved outputs become live operations");
        var lossy=InfusionOutput.item(new ItemStack(Items.DIAMOND_PICKAXE));boolean rejected=false;
        try{lossy.asEnchantment(original);}catch(IllegalArgumentException expected){rejected=true;}
        test.assertTrue(rejected,"Lossy addon output is rejected before it can replace a central item");
        // ItemStack.enchant casts to byte; a literal 256 there would store zero.
        var cap=original.copy();var capped=new net.minecraft.nbt.CompoundTag();
        capped.putString("id","minecraft:efficiency");capped.putShort("lvl",(short)256);cap.getEnchantmentTags().add(capped);
        test.assertTrue(operation.apply(cap).isEmpty(),"No output is produced when the target cannot increase");
        test.succeed();
    }
}
