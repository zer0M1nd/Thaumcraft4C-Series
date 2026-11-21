package tc4revival.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class GroupFeedbackServerTests {
    @GameTest(template="wand_test_empty")
    public static void earlierPearlScanSurvivesMissingParentsAndOldKnowledgeMigration(GameTestHelper test) {
        var catalog=ResearchCatalogs.catalog();var entry=catalog.require("PRIMPEARL");var aspects=tc4revival.aspect.TCAspects.catalog();
        var id=UUID.randomUUID();var data=new KnowledgeSavedData(aspects);
        data.update(id,n->0,k->{aspects.all().forEach(k::discover);return null;});
        var stack=ResearchDisplayItems.bundled().resolve(entry.triggers().items().orElseThrow().get(0),0).orElseThrow();
        var target=ScanTargetResolver.inventory(stack).orElseThrow();var rules=ResearchCatalogs.rules();
        test.assertTrue(!rules.prerequisites(data.snapshot(id).orElseThrow(),entry.key()),"Pearl is scanned before its parents");
        var scan=data.update(id,n->0,k->target.completeWithClue(k,aspects,PlayerKnowledge.ScanMode.THAUMOMETER,1000,rules,n->0));
        test.assertTrue(scan.scan().succeeded(),"The real item observation is accepted");
        var observed=data.snapshot(id).orElseThrow();var pools=Map.copyOf(observed.knownAspects());
        test.assertTrue(observed.researchMarkers().contains("@PRIMPEARL")&&!observed.researchMarkers().contains("PRIMPEARL"),"Scan retains a clue without completing the research");
        // Simulate an older save which remembers the scan but lost the clue.
        observed.forgetResearch("@PRIMPEARL");
        var saved=data.save(new net.minecraft.nbt.CompoundTag());
        saved.getList("players",net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0).put("knowledge",KnowledgeNbt.write(observed));
        var restored=KnowledgeSavedData.load(saved,aspects);
        restored.update(id,n->0,k->{
            entry.parents().forEach(k::rememberResearch);entry.hiddenParents().forEach(k::rememberResearch);
            entry.parentsAny().stream().findFirst().ifPresent(k::rememberResearch);k.rememberResearch("ELDRITCHMINOR");return null;
        });
        var ready=restored.snapshot(id).orElseThrow();
        var book=new ResearchBookActions(aspects,catalog,ResearchCatalogs::enabled);
        test.assertTrue(rules.prerequisites(ready,entry.key())&&book.visible(ready,entry)&&ready.researchMarkers().contains("@PRIMPEARL"),"Completing parents restores the visible pearl clue without another scan");
        test.assertTrue(!rules.completed(ready,entry.key())&&ready.knownAspects().equals(pools),"Recovery neither completes the research nor repeats rewards");
        test.assertTrue(target.completeWithClue(ready,aspects,PlayerKnowledge.ScanMode.THAUMOMETER,1000,rules,n->0).scan().status()==ScanRules.Status.ALREADY_SCANNED,"Saved scan still prevents duplicate rewards");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void disguiseExamplesUseTheirCorrelatedInputsAndLiveRecipe(GameTestHelper test) {
        var level=test.getLevel();var manager=level.getRecipeManager();var original=List.copyOf(manager.getRecipes());
        var id=net.minecraft.resources.ResourceLocation.parse("gadomancy:armor_disguise");
        test.assertTrue(manager.byKey(id).isPresent(),"Disguise recipe is installed for this addon regression");
        ResearchPage valid=null;
        for(String disguise:List.of("minecraft:leather_chestplate","minecraft:potion","minecraft:leather_boots")) {
            var definition=com.google.gson.JsonParser.parseString("""
                {"type":"INFUSION_CRAFTING","recipe":{"kind":"runic_augmentation_example",
                "research":"gadomancy:armor_disguise","input":{"item":"minecraft:diamond_chestplate"},
                "output":{"item":"minecraft:dirt"},"aspects":{"aer":999},"instability":99,
                "ingredients":{"kind":"sequence","elements":[
                {"kind":"stack","stack":{"item":"minecraft:slime_ball"}},
                {"kind":"stack","stack":{"item":"minecraft:slime_ball"}},
                {"kind":"stack","stack":{"item":"thaumcraft:quicksilver"}},
                {"kind":"stack","stack":{"item":"thaumcraft:quicksilver"}},
                {"kind":"stack","stack":{"item":"minecraft:leather_chestplate"}}]}}}
                """).getAsJsonObject();
            var sample=definition.getAsJsonObject("recipe").getAsJsonObject("ingredients").getAsJsonArray("elements").get(4).getAsJsonObject().getAsJsonObject("stack");
            sample.addProperty("item",disguise);
            if(disguise.equals("minecraft:potion"))sample.addProperty("nbt","{Potion:\"minecraft:invisibility\"}");
            var page=ResearchPage.decode(definition);var rows=LiveBookRecipes.resolve(level,page);
            if(disguise.equals("minecraft:leather_boots")){test.assertTrue(rows.isEmpty(),"Wrong armor slot cannot produce a preview");continue;}
            test.assertTrue(rows.size()==1,"Chosen same-slot armor and invisibility potion each resolve");var row=rows.get(0);
            test.assertTrue(row.output().is(Items.DIAMOND_CHESTPLATE)&&row.output().getOrCreateTag().contains("gadomancy"),"Output is the real disguised armor, not the stale page output");
            test.assertTrue(row.instability()==-20&&row.aspects().equals(Map.of("limus",12,"tutamen",10,"praecantatio",8)),"Costs and stability come from the current recipe");
            test.assertTrue(row.inputs().size()==6&&row.inputs().get(1).get(0).is(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse(disguise))),"Preview retains the selected disguise");
            valid=page;
        }
        try {
            manager.replaceRecipes(original.stream().filter(r->!r.getId().equals(id)).toList());
            test.assertTrue(LiveBookRecipes.resolve(level,valid).isEmpty(),"Removed recipes never return as stale inline previews");
        } finally {manager.replaceRecipes(original);}
        tc4revival.TC4Revival.LOGGER.info("GROUP_FEEDBACK_PASS disguiseExamplesUseTheirCorrelatedInputsAndLiveRecipe");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void configuredStoragePagesFollowServerRecipes(GameTestHelper test) {
        var pages=ResearchPages.bundled().require("thaumicenergistics:STORAGE");
        for(String tier:List.of("1024k","4096k","16384k")) {
            String item="thaumicenergistics:storage_component_"+tier;
            boolean enabled=test.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse(item)).isPresent();
            long visible=pages.stream().filter(p->p.recipe() instanceof ResearchPage.Arcane a&&a.output().key().item().equals(item)).count();
            test.assertTrue(visible==(enabled?1:0),"Server book and configured recipe agree for "+tier+": "+visible+" / "+enabled);
        }
        test.assertTrue(pages.stream().filter(p->p.recipe() instanceof ResearchPage.Arcane).count()>=5,"Ordinary storage pages remain");
        test.assertTrue(ResearchDefinitions.server().catalog().require("thaumicenergistics:STORAGE").display().pageCount()==pages.size(),"Synchronized catalog count matches filtered pages");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void shareBookPreviewKeepsItsShapedPattern(GameTestHelper test) {
        var rows=LiveBookRecipes.resolve(test.getLevel(),page(ResearchPage.Type.NORMAL_CRAFTING,"thaumic_tinkerer:share_book"));
        test.assertTrue(rows.size()==1,"Share book has a live recipe");var row=rows.get(0);
        test.assertTrue(row.width()==3&&row.inputs().size()==9,"Share book retains a 3 by 3 shaped preview");
        test.assertTrue(row.inputs().get(0).isEmpty()&&row.inputs().get(2).isEmpty()&&row.inputs().get(6).isEmpty()&&row.inputs().get(8).isEmpty(),"Corner cells remain empty");
        test.assertTrue(row.inputs().get(1).get(0).is(TCItems.SCRIBING_TOOLS.get())&&row.inputs().get(4).get(0).is(TCItems.THAUMONOMICON.get()),"Tools and book use actual slots");
        for(int slot:new int[]{3,5,7})test.assertTrue(row.inputs().get(slot).get(0).is(Items.PAPER),"Paper occupies actual slot "+slot);
        test.assertTrue(!row.output().isEmpty(),"Share book output exists");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void arcaneDisplaySamplesRespectActualCustomRules(GameTestHelper test) {
        for(var key:List.of("thaumicinfusion:essentia_block","thaumicinfusion:essentia_bricks","thaumicinfusion:chiseled_essentia","witchinggadgets:photo_developing")) {
            var rows=LiveBookRecipes.resolve(test.getLevel(),page(ResearchPage.Type.ARCANE_CRAFTING,key));
            test.assertTrue(rows.size()==1,"Custom recipe has one live preview: "+key);
            var row=rows.get(0);var recipe=(ArcaneRecipe)test.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse(row.id())).orElseThrow();
            var grid=new net.minecraft.world.inventory.TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null,-1){
                public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}
                public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}
            },3,3);
            int width=row.width()==0?3:row.width();
            for(int i=0;i<row.inputs().size();i++)if(!row.inputs().get(i).isEmpty())grid.setItem(i%width+3*(i/width),row.inputs().get(i).get(0).copy());
            test.assertTrue(recipe.matches(grid,test.getLevel()),"Sample matches actual custom rule: "+key);
            test.assertTrue(ItemStack.isSameItemSameTags(row.output(),recipe.assemble(grid,test.getLevel().registryAccess())),"Live output preserves sample data: "+key);
            grid.getItem(0).setTag(null);
            test.assertTrue(!recipe.matches(grid,test.getLevel()),"Missing required aspect/photo NBT stays rejected: "+key);
        }
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void propertyOutputAliasesUseActualInfusionRecipes(GameTestHelper test) {
        var rows=LiveBookRecipes.resolve(test.getLevel(),page(ResearchPage.Type.INFUSION_CRAFTING,"thaumichorizons:advanced_golem"));
        test.assertTrue(rows.size()==1,"Property alias resolves one recipe");
        var row=rows.get(0);
        test.assertTrue(row.id().equals("thaumichorizons:infusion/advanced_golem"),"Use the loaded recipe, not a display fallback");
        var recipe=(InfusionRecipe)test.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse(row.id())).orElseThrow();
        var center=row.inputs().get(0).get(0);
        test.assertTrue(!center.hasTag()||!center.getTag().getBoolean("advanced"),"Preview leaves input unchanged");
        test.assertTrue(row.output().hasTag()&&row.output().getTag().getBoolean("advanced"),"Output contains the actual upgrade");
        test.assertTrue(ItemStack.isSameItemSameTags(row.output(),recipe.plan(center).output().apply(center)),"Output equals actual plan");
        test.succeed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=400)
    public static void explicitInfusionDisplaysDoNotBypassCustomIngredients(GameTestHelper test) {
        int count=0;
        for(var recipe:test.getLevel().getRecipeManager().getAllRecipesFor(TCArcane.INFUSION_RECIPE_TYPE.get())) {
            if(!recipe.getId().getNamespace().equals("thaumicbases")||!recipe.getId().getPath().startsWith("revolver/"))continue;
            var definition=recipe.displayDefinition().getAsJsonObject("center");
            var custom=net.minecraft.world.item.crafting.Ingredient.fromJson(definition.get("ingredient"));
            test.assertTrue(custom.getItems().length==0,"Preview must not enter category-equivalence candidates");
            var samples=new InfusionIngredient(definition).displayStacks();
            test.assertTrue(samples.size()==1,"Every revolver level has one display sample: "+recipe.getId());
            var center=samples.get(0);test.assertTrue(recipe.acceptsCenter(center),"Sample respects custom ingredient");
            var spec=definition.getAsJsonObject("ingredient");var wrong=center.copy();
            wrong.getOrCreateTag().putInt("tbUpgrade:"+spec.get("upgrade").getAsString(),spec.get("level").getAsInt()+1);
            test.assertTrue(!recipe.acceptsCenter(wrong),"Wrong upgrade level stays rejected");
            var rows=LiveBookRecipes.resolve(test.getLevel(),page(ResearchPage.Type.INFUSION_CRAFTING,recipe.getId().toString()));
            test.assertTrue(rows.size()==1&&ItemStack.isSameItemSameTags(rows.get(0).output(),recipe.plan(center).output().apply(center)),"Live book shows actual next level: "+recipe.getId());
            count++;
        }
        test.assertTrue(count==58,"All 58 shipped revolver level recipes: "+count);test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void bookAddonAliasesPreserveLegacyCase(GameTestHelper test) {
        int checked=0;
        for(var key:List.of("thaumichorizons:transductionAmplifier","thaumichorizons:crystalWand","thaumicomniverse:THAUMATURGE_CERTIFICATE")) {
            if(!net.minecraftforge.fml.ModList.get().isLoaded(key.substring(0,key.indexOf(':'))))continue;
            var definition=ResearchPageRecipes.bundled().require(key).definition();
            var type=definition instanceof ResearchPageRecipes.Infusion?ResearchPage.Type.INFUSION_CRAFTING:ResearchPage.Type.ARCANE_CRAFTING;
            var rows=LiveBookRecipes.resolve(test.getLevel(),page(type,key));
            test.assertTrue(!rows.isEmpty()&&rows.stream().allMatch(row->!row.output().isEmpty()),"Legacy-case addon alias has a usable book recipe: "+key);
            checked++;
        }
        test.assertTrue(checked>0,"Run with a legacy-case addon installed");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void migratedBrassBookUsesLiveRecipe(GameTestHelper test) {
        test.assertTrue(net.minecraftforge.fml.ModList.get().isLoaded("thaumicomniverse"),"Run with migrated alchemy owner installed");
        var rows=LiveBookRecipes.resolve(test.getLevel(),page(ResearchPage.Type.CRUCIBLE_CRAFTING,"AlchemicalBrass"));
        test.assertTrue(rows.size()==1,"Unqualified migrated alias resolves exactly one recipe");
        var row=rows.get(0);
        var actual=test.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse(row.id())).orElseThrow();
        test.assertTrue(actual instanceof CrucibleRecipe&&ItemStack.isSameItemSameTags(row.output(),actual.getResultItem(test.getLevel().registryAccess())),"Book uses the loaded modern-owner recipe, not stale display data");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void bookUsesActualRecipes(GameTestHelper test) {
        var level=test.getLevel();var missing=new ArrayList<String>();
        LiveBookRecipes.bindings().forEach((key,ids)->ids.forEach(id->{if(level.getRecipeManager().byKey(id).isEmpty())missing.add(key+" -> "+id);}));
        test.assertTrue(missing.isEmpty(),"Book binding IDs exist: "+missing);
        int count=0;var empty=new ArrayList<String>();
        for(var entry:ResearchPages.bundled().entries().entrySet())for(var page:entry.getValue()) {
            if(!tc4revival.research.ResearchCatalogs.enabled(tc4revival.research.ResearchCatalogs.bundled().require(entry.getKey())))continue;
            if(!LiveBookRecipes.supported(page.type()))continue;
            var rows=LiveBookRecipes.resolve(level,page);count++;
            if(rows.isEmpty())empty.add(entry.getKey()+":"+page.recipe());
        }
        tc4revival.TC4Revival.LOGGER.info("BOOK_LIVE_AUDIT {} recipe pages, unresolved {}",count,empty);
        test.assertTrue(empty.isEmpty(),"Every shipped book recipe resolves to a real recipe: "+empty);
        for(String key:List.of("PureIron","PureGold","PureCopper")) {
            var page=page(ResearchPage.Type.CRUCIBLE_CRAFTING,key);var rows=LiveBookRecipes.resolve(level,page);
            test.assertTrue(rows.size()==2,"Ore and raw branches both appear: "+key);
            test.assertTrue(rows.stream().anyMatch(v->v.output().getCount()==2&&v.aspects().get("metallum")==2),"Ore cost and output doubled");
            test.assertTrue(rows.stream().anyMatch(v->v.output().getCount()==1&&v.aspects().get("metallum")==1),"Raw cost and output stay single");
        }
        var iron=LiveBookRecipes.resolve(level,page(ResearchPage.Type.CRUCIBLE_CRAFTING,"PureIron"));
        test.assertTrue(iron.stream().flatMap(v->v.inputs().get(0).stream()).anyMatch(s->s.is(Items.DEEPSLATE_IRON_ORE)),"Actual tag includes deepslate ore");
        tc4revival.TC4Revival.LOGGER.info("GROUP_FEEDBACK_PASS {}",Thread.currentThread().getStackTrace()[1].getMethodName());test.succeed();
    }
    private static ResearchPage page(ResearchPage.Type type,String key) {
        return new ResearchPage(type,Optional.empty(),Optional.empty(),Optional.empty(),Map.of(),Optional.empty(),new ResearchPage.Reference(List.of(key),""));
    }
    @GameTest(template="wand_test_empty")
    public static void swingCannotDiscardTradeSpread(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(1,4,2));
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TradeSpread"));
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.setPos(pos.getX(),pos.getY()+2,pos.getZ());
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true);
        WandFocusItem.attach(wand,new ItemStack(TCItems.TRADE_FOCUS.get()));player.getInventory().setItem(0,wand);
        try {
            for(boolean swingFirst:List.of(true,false)) {
                for(int i=0;i<6;i++)level.setBlockAndUpdate(pos.east(i),Blocks.STONE.defaultBlockState());
                BlockSwaps.enqueue(player,net.minecraft.world.InteractionHand.MAIN_HAND,pos,new ItemStack(Items.DIRT),swingFirst?0:4);
                BlockSwaps.enqueue(player,net.minecraft.world.InteractionHand.MAIN_HAND,pos,new ItemStack(Items.DIRT),swingFirst?4:0);
                for(int i=0;i<6;i++)BlockSwaps.process(level,id->player);
                for(int i=0;i<=4;i++)test.assertTrue(level.getBlockState(pos.east(i)).is(Blocks.DIRT),"Life-four cast reaches block "+i+"; swingFirst="+swingFirst);
                test.assertTrue(level.getBlockState(pos.east(5)).is(Blocks.STONE),"Spread respects its life limit");
            }
        } finally { BlockSwaps.process(level,id->null); }
        tc4revival.TC4Revival.LOGGER.info("GROUP_FEEDBACK_PASS {}",Thread.currentThread().getStackTrace()[1].getMethodName());test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void hungryNodeConsumesItemAspects(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        boolean enabled=tc4revival.config.TCConfig.SERVER.hardNodes.get();tc4revival.config.TCConfig.SERVER.hardNodes.set(true);
        try {
            level.setBlockAndUpdate(pos,TCArcane.AURA_NODE.get().defaultBlockState());
            var node=(AuraNodeBlockEntity)level.getBlockEntity(pos);
            for(int count:List.of(1,64)) {
                node.configure(new tc4revival.aspect.AspectAmounts(),AuraNodeType.HUNGRY,null,"food",false);
                var item=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,new ItemStack(Items.COBBLESTONE,count));
                item.setNoGravity(true);level.addFreshEntity(item);
                for(int i=0;i<5;i++)node.tick();
                test.assertTrue(!item.isAlive(),"Hungry node actually destroys the dropped stack");
                test.assertTrue(node.permanentBase().total()==1,"One swallowed stack offers one item, not its count");
            }
        } finally {tc4revival.config.TCConfig.SERVER.hardNodes.set(enabled);level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());}
        tc4revival.TC4Revival.LOGGER.info("GROUP_FEEDBACK_PASS {}",Thread.currentThread().getStackTrace()[1].getMethodName());test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void manaPodsSeeNewCanopiesAndHighTerrain(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2)).atY(160);var registry=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        var proto=new net.minecraft.world.level.chunk.ProtoChunk(new net.minecraft.world.level.ChunkPos(pos),net.minecraft.world.level.chunk.UpgradeData.EMPTY,level,registry,null);
        proto.setStatus(net.minecraft.world.level.chunk.ChunkStatus.CARVERS);
        net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(proto,Set.of(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG));
        proto.setBlockState(pos.above(),Blocks.OAK_LOG.defaultBlockState(),false);
        test.assertTrue(LegacySurfaceHeight.mapFor(proto)==net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,"Decoration reads the updated map, not the stale WG canopy");
        test.assertTrue(proto.getHeight(LegacySurfaceHeight.mapFor(proto),pos.getX()&15,pos.getZ()&15)==161,"Live map includes the new branch");
        var forest=registry.getHolderOrThrow(TCBiomes.MAGICAL_FOREST);var cp=new net.minecraft.world.level.ChunkPos(pos);
        for(int cx=cp.x-1;cx<=cp.x+1;cx++)for(int cz=cp.z-1;cz<=cp.z+1;cz++)level.getChunk(cx,cz).fillBiomesFromNoise((x,y,z,s)->forest,level.getChunkSource().randomState().sampler());
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos.below(),Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos.above(),Blocks.OAK_LOG.defaultBlockState());
        boolean placed=TCWorldFeatures.MANA_POD.get().place(new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(Optional.empty(),level,level.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(42),pos,net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE));
        test.assertTrue(placed&&level.getBlockState(pos).is(TCArcane.MANA_POD.get()),"Natural feature hangs a pod above old Y=128 ceiling");tc4revival.TC4Revival.LOGGER.info("GROUP_FEEDBACK_PASS manaPodsSeeNewCanopiesAndHighTerrain");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void addonBookAliasesUseActualRecipes(GameTestHelper test) {
        var level=test.getLevel();var manager=level.getRecipeManager();var original=List.copyOf(manager.getRecipes());
        var id=net.minecraft.resources.ResourceLocation.parse("book_test:current_recipe");
        var json=com.google.gson.JsonParser.parseString("{\"ingredients\":[{\"item\":\"minecraft:diamond\"}],\"result\":{\"item\":\"minecraft:paper\",\"count\":7}}").getAsJsonObject();
        var recipe=net.minecraft.world.item.crafting.RecipeSerializer.SHAPELESS_RECIPE.fromJson(id,json);
        var definitions=com.google.gson.JsonParser.parseString("[{\"key\":\"book_test:old_alias\",\"class\":\"modern\",\"data\":{\"kind\":\"crafting_shapeless\",\"output\":{\"kind\":\"stack\",\"stack\":{\"item\":\"minecraft:paper\",\"count\":1}},\"ingredients\":{\"kind\":\"sequence\",\"elements\":[{\"kind\":\"stack\",\"stack\":{\"item\":\"minecraft:dirt\"}}]}}}]").getAsJsonArray();
        var definition=ResearchPageRecipes.decode(definitions).require("book_test:old_alias").definition();
        try {
            var next=new ArrayList<net.minecraft.world.item.crafting.Recipe<?>>(original);next.add(recipe);manager.replaceRecipes(next);
            var views=LiveBookRecipes.resolveAddonAlias(level,"book_test:old_alias",ResearchPage.Type.NORMAL_CRAFTING,definition);
            test.assertTrue(views.size()==1&&views.get(0).output().getCount()==7,"Addon alias reads actual output count");
            test.assertTrue(views.get(0).inputs().get(0).get(0).is(Items.DIAMOND),"Addon alias reads actual ingredients, not stale dirt display");
        } finally { manager.replaceRecipes(original); }
        tc4revival.TC4Revival.LOGGER.info("GROUP_FEEDBACK_PASS addonBookAliasesUseActualRecipes");test.succeed();
    }

}
