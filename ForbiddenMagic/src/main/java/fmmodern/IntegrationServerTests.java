package fmmodern;

import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import com.google.gson.JsonParser;
import tc4revival.content.ArcaneRecipe;

/** Final server batch exercises loaded data, not just generator output. */
@GameTestHolder("forbidden_magic") @PrefixGameTestTemplate(false)
public final class IntegrationServerTests {
    @GameTest(template="test_empty")
    public static void botaniaWandInfusionsKeepTheirDistinctCenterLogs(GameTestHelper test) {
        var level=test.getLevel();
        for(String wood:java.util.List.of("livingwood","dreamwood")) {
            var id=ResourceLocation.parse("forbidden_magic:botania/"+wood+"_rod");
            var recipe=(tc4revival.content.InfusionRecipe)level.getRecipeManager().byKey(id).orElseThrow();
            var center=new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("botania:"+wood+"_log")));
            var components=new java.util.ArrayList<ItemStack>();
            for(var entry:recipe.displayDefinition().getAsJsonArray("components"))components.add(new tc4revival.content.InfusionIngredient(entry.getAsJsonObject()).displayStacks().get(0));
            for(boolean ordered:java.util.List.of(false,true)) {
                var input=new tc4revival.content.InfusionRecipe.Input(center,components,key->true,ordered);
                test.assertTrue(tc4revival.content.InfusionRecipe.find(level,input).orElseThrow().getId().equals(id),"Correct center selects "+wood+" in ordered="+ordered);
                test.assertTrue(!recipe.acceptsCenter(new ItemStack(Items.OAK_LOG)),"Ordinary wood is not a Botania wand center");
            }
        }
        test.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=600)
    public static void fmOrdinaryVillagerAcquiresHereticWorkstation(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new net.minecraft.core.BlockPos(3,2,3));
        for(var at:net.minecraft.core.BlockPos.betweenClosed(pos.offset(-3,-1,-3),pos.offset(3,-1,3)))level.setBlock(at,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        level.setBlock(pos.offset(1,0,0),Content.CAGE.get().defaultBlockState(),3);
        var job=pos.offset(1,0,0);level.setDayTime(6000);
        test.assertTrue(level.getPoiManager().getType(job).map(p->p.value()==Content.HERETIC_POI.get()).orElse(false),"Cage registers a real POI");
        var villager=net.minecraft.world.entity.EntityType.VILLAGER.create(level);villager.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);villager.setAge(0);level.addFreshEntity(villager);
        test.assertTrue(villager.getVillagerData().getProfession()==net.minecraft.world.entity.npc.VillagerProfession.NONE,"Ordinary villager starts unemployed");
        test.succeedWhen(()->{
            test.assertTrue(villager.getVillagerData().getProfession()==Content.HERETIC_PROFESSION.get(),"Vanilla brain acquires nearby Heretic job");
            test.assertTrue(villager.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE).map(p->p.pos().equals(job)).orElse(false),"Vanilla brain retains actual job-site position");
            test.assertTrue(!villager.getOffers().isEmpty(),"Novice trades provided through VillagerTradesEvent");
            villager.discard();
        });
    }
    @GameTest(template="test_empty")
    public static void fmConfigurationSelectsActualData(GameTestHelper test){
        var lust=tc4revival.aspect.TCAspects.catalog().require("forbidden_magic:luxuria");
        var lead=tc4revival.aspect.item.ItemStackAspects.resolve(new ItemStack(Items.LEAD)).orElseThrow();
        test.assertTrue(lead.amount(lust)==(ForbiddenConfig.value(ForbiddenConfig.NO_LUST)?0:2),"noLust controls actual lead aspects, not only the research screen");
        test.assertTrue(IntegrationContent.creativeVisible("lust_shard")!=ForbiddenConfig.value(ForbiddenConfig.NO_LUST),"noLust controls creative acquisition");
        var manager=test.getLevel().getRecipeManager();int food=ForbiddenConfig.value(ForbiddenConfig.GLUTTONY);
        var ring=manager.byKey(ResourceLocation.parse("forbidden_magic:ring_food"));var cake=manager.byKey(ResourceLocation.parse("forbidden_magic:arcane_cake"));
        if(food==1)test.assertTrue(ring.isEmpty()&&cake.isEmpty(),"Disabled food mode removes both recipes");
        else {
            var ringJson=recipeDefinition(ring.orElseThrow());var cakeJson=recipeDefinition(cake.orElseThrow());
            test.assertTrue(ringJson.getAsJsonObject("key").getAsJsonObject("D").get("item").getAsString().equals(food==2?"forbidden_magic:nether_star_block":"minecraft:diamond"),"Ring cost follows food mode");
            long stars=java.util.stream.StreamSupport.stream(cakeJson.getAsJsonArray("components").spliterator(),false).filter(e->e.getAsJsonObject().getAsJsonObject("ingredient").has("item")&&e.getAsJsonObject().getAsJsonObject("ingredient").get("item").getAsString().equals("forbidden_magic:nether_star_block")).count();
            test.assertTrue(stars==(food==2?2:0),"Hardcore cake consumes two star blocks");
        }
        test.succeed();
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static com.google.gson.JsonObject recipeDefinition(net.minecraft.world.item.crafting.Recipe<?> recipe){
        var buffer=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{((net.minecraft.world.item.crafting.RecipeSerializer)recipe.getSerializer()).toNetwork(buffer,recipe);return JsonParser.parseString(buffer.readUtf(1048576)).getAsJsonObject();}finally{buffer.release();}
    }
    @GameTest(template="test_empty")
    public static void fmCrystalwellUsesWorldResearchMode(GameTestHelper test){
        var recipe=test.getLevel().getRecipeManager().byKey(ResourceLocation.parse("forbidden_magic:crystalwell")).orElseThrow();
        test.assertTrue(recipe instanceof ArcaneRecipe,"Conditional wrapper must expose a real arcane recipe under the stable ID");
        var buffer=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{
            var arcane=(ArcaneRecipe)recipe;
            @SuppressWarnings("unchecked") var serializer=(net.minecraft.world.item.crafting.RecipeSerializer<ArcaneRecipe>)arcane.getSerializer();
            serializer.toNetwork(buffer,arcane);
            var definition=JsonParser.parseString(buffer.readUtf(1048576)).getAsJsonObject();
            boolean easy=tc4revival.config.TCConfig.SERVER.researchDifficulty.get()==-1;
            test.assertTrue(definition.getAsJsonArray("ingredients").size()==(easy?4:6),"Recipe must match the current server research difficulty");
            test.assertTrue(arcane.getResultItem(test.getLevel().registryAccess()).is(Content.CRYSTALWELL.get()),"Recipe output remains crystalwell");
        }finally{buffer.release();}
        test.succeed();
    }
    @GameTest(template="test_empty")
    public static void fmOptionalItemsAreHiddenWithoutTheirDependency(GameTestHelper test){
        test.assertTrue(IntegrationContent.creativeVisible("bloodwell")==IntegrationContent.bloodEnabled(),"Inactive bloodwell is hidden");
        test.assertTrue(IntegrationContent.creativeVisible("whisperweed")==IntegrationContent.botanyEnabled(),"Inactive Botania flower is hidden");
        test.assertTrue(IntegrationContent.creativeVisible("bloodthorn")== (IntegrationContent.bloodEnabled()&&IntegrationContent.botanyEnabled()),"Bloodthorn requires both integrations");
        test.assertTrue(IntegrationContent.creativeVisible("taint_pickaxe"),"Core tools remain visible");test.succeed();
    }
    @GameTest(template="test_empty")
    public static void fmLegacyVanillaAspectsPreserveBaseValues(GameTestHelper test){
        var catalog=tc4revival.aspect.TCAspects.catalog();
        var netherrack=tc4revival.aspect.item.ItemStackAspects.resolve(new ItemStack(Items.NETHERRACK)).orElseThrow();
        test.assertTrue(netherrack.amount(catalog.require("forbidden_magic:infernus"))==1,"Nether aspect is added to netherrack");
        var gold=tc4revival.aspect.item.ItemStackAspects.resolve(new ItemStack(Items.GOLDEN_SWORD)).orElseThrow();
        test.assertTrue(gold.amount(catalog.require("forbidden_magic:superbia"))==2,"Gold sword receives pride");
        test.assertTrue(gold.amount(catalog.require("telum"))>0,"Existing weapon aspects are retained");
        var bed=tc4revival.aspect.item.ItemStackAspects.resolve(new ItemStack(Items.BLUE_BED)).orElseThrow();
        test.assertTrue(bed.amount(catalog.require("forbidden_magic:desidia"))==4,"Colored beds retain the legacy sloth definition");test.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=100)
    public static void fmHereticUsesModernProfessionAndRestockingAfterReload(GameTestHelper test){
        var villager=Content.HERETIC.get().create(test.getLevel());
        test.assertTrue(villager.getVillagerData().getProfession()==net.minecraft.world.entity.npc.VillagerProfession.NONE,"Birth does not force a profession");
        villager.setVillagerData(villager.getVillagerData().setProfession(Content.HERETIC_PROFESSION.get()));villager.setVillagerXp(1);
        var offers=villager.getOffers();int count=offers.size();var offer=offers.get(0);
        while(!offer.isOutOfStock())villager.notifyTrade(offer);
        var tag=new net.minecraft.nbt.CompoundTag();villager.saveWithoutId(tag);
        var restored=Content.HERETIC.get().create(test.getLevel());restored.load(tag);restored.setNoAi(true);
        var at=test.absolutePos(new net.minecraft.core.BlockPos(3,2,3));restored.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        test.getLevel().addFreshEntity(restored);
        test.assertTrue(restored.getOffers().get(0).isOutOfStock(),"Out-of-stock state survives save/reload");
        test.runAfterDelay(45,()->{
            test.assertTrue(restored.getOffers().get(0).isOutOfStock(),"No legacy 40-tick free restock without work");
            restored.restock();
            test.assertTrue(!restored.getOffers().get(0).isOutOfStock(),"Vanilla workstation restock resets uses");
            test.assertTrue(restored.getOffers().size()==count,"Refresh and experience changes do not duplicate offers");
            test.assertTrue(restored.getVillagerData().getProfession()==Content.HERETIC_PROFESSION.get(),"Profession is retained");
            restored.discard();test.succeed();
        });
    }

    @GameTest(template="test_empty")
    public static void fmSporkKeepsItemIdentityAndMechanics(GameTestHelper test){
        boolean previous=fmmodern.ForbiddenConfig.value(ForbiddenConfig.SPORK);var stack=new ItemStack(Content.FORK.get());
        try{
            ForbiddenConfig.SPORK.set(false);String ordinary=stack.getDescriptionId();
            ForbiddenConfig.SPORK.set(true);
            test.assertTrue(stack.getItem()==Content.FORK.get()&&stack.getDescriptionId().equals(ordinary),"Spork is the same registered fork, not another item");
            test.assertTrue(stack.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text&&text.getKey().equals("item.forbidden_magic.diabolist_spork"),"Configured display name uses the spork translation");
            test.assertTrue(stack.getItem() instanceof thaumcraft.api.IRepairable,"Repair API stays available");
        }finally{ForbiddenConfig.SPORK.set(previous);}
        test.succeed();
    }

    @GameTest(template="test_empty")
    public static void fmBookDisplayReferencesResolveToLoadedCraftingRecipes(GameTestHelper test){
        var snapshot=tc4revival.research.ResearchDefinitions.server();
        test.assertTrue(snapshot!=null,"Server book snapshot loaded");int count=0;
        for(var entry:snapshot.recipes().entries().values()){
            String key=entry.key();if(!key.startsWith("forbidden_magic:"))continue;count++;
            if(key.endsWith("_easy_display"))continue;
            var id=ResourceLocation.parse(key);var resource=ResourceLocation.fromNamespaceAndPath(id.getNamespace(),"recipes/"+id.getPath()+".json");
            try(var reader=test.getLevel().getServer().getResourceManager().getResourceOrThrow(resource).openAsReader()){
                var json=JsonParser.parseReader(reader).getAsJsonObject();
                boolean enabled=net.minecraftforge.common.crafting.CraftingHelper.processConditions(json,"conditions",net.minecraftforge.common.crafting.conditions.ICondition.IContext.EMPTY);
                if(json.has("recipes"))enabled&=java.util.stream.StreamSupport.stream(json.getAsJsonArray("recipes").spliterator(),false).anyMatch(branch->net.minecraftforge.common.crafting.CraftingHelper.processConditions(branch.getAsJsonObject(),"conditions",net.minecraftforge.common.crafting.conditions.ICondition.IContext.EMPTY));
                test.assertTrue(test.getLevel().getRecipeManager().byKey(id).isPresent()==enabled,"Book recipe availability follows its real conditions: "+key);
            }catch(java.io.IOException exception){throw new IllegalStateException(exception);}
        }
        test.assertTrue(count==51,"All 50 referenced recipes plus the easy research display are present");test.succeed();
    }

    @GameTest(template="test_empty")
    public static void fmBotaniaEssenceProduction(GameTestHelper test){
        if(!IntegrationContent.botanyEnabled()){test.succeed();return;}
        fmmodern.compat.BotanyServerChecks.euclidaisy(test);
    }
    @GameTest(template="test_empty")
    public static void fmBotaniaCosmeticPersistence(GameTestHelper test){
        if(!IntegrationContent.botania()){test.succeed();return;}
        fmmodern.compat.BotanyServerChecks.cosmetics(test);
    }
    @GameTest(template="test_empty")
    public static void fmBloodInkPaymentAndOrbRegistration(GameTestHelper test){
        if(!IntegrationContent.bloodEnabled()){test.succeed();return;}
        fmmodern.compat.BloodServerChecks.inkAndOrb(test);
    }
    @GameTest(template="test_empty")
    public static void fmBloodSealBlocksDirectLpGrowth(GameTestHelper test){
        if(!IntegrationContent.bloodEnabled()){test.succeed();return;}
        fmmodern.compat.BloodServerChecks.seal(test);
    }
    @GameTest(template="test_empty")
    public static void fmBloodthornManaBudget(GameTestHelper test){
        if(!IntegrationContent.bloodEnabled()||!IntegrationContent.botanyEnabled()){test.succeed();return;}
        fmmodern.compat.BotanyServerChecks.bloodthorn(test);
    }
    @GameTest(template="test_empty")
    public static void fmTainthistleFluxConversion(GameTestHelper test){
        if(!IntegrationContent.botanyEnabled()){test.succeed();return;}
        fmmodern.compat.BotanyServerChecks.tainthistle(test);
    }
    @GameTest(template="test_empty",timeoutTicks=650)
    public static void fmSanityRitualPaymentAndWard(GameTestHelper test){
        if(!IntegrationContent.bloodEnabled()){test.succeed();return;}
        fmmodern.compat.BloodServerChecks.ritual(test);
    }
}
