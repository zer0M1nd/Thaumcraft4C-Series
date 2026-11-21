package fmmodern;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.ResourceLocation;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.aspect.entity.EntityAspects;
import tc4revival.content.WandParts;
import thaumcraft.api.wands.WandAttributes;

@GameTestHolder("forbidden_magic") @PrefixGameTestTemplate(false)
public final class AddonApiServerTests {
    @GameTest(template="test_empty")
    public static void fmRecipeDerivedAspectsAndReload(GameTestHelper test){
        var cage=new ItemStack(Content.CAGE.get());
        var first=ItemStackAspects.resolve(cage).orElseThrow();
        test.assertTrue(first.amount(TCAspects.catalog().require("forbidden_magic:ira"))>0,"Cage inherits sins from its recipe, without a fixed aspect row");
        test.assertTrue(first.amount(TCAspects.catalog().require("metallum"))>0,"Cage inherits thaumium material aspects");
        test.assertTrue(ItemStackAspects.scanHash(cage).isPresent(),"Derived addon item has a stable scan identity");
        var old=first.view();
        AspectInputPack.worldReady(new net.minecraftforge.event.server.ServerAboutToStartEvent(test.getLevel().getServer()));
        test.assertTrue(old.equals(ItemStackAspects.resolve(cage).orElseThrow().view()),"Reload replaces the input snapshot without accumulating values");
        var data=tc4revival.aspect.recipe.RecipeAspects.data();
        var wells=data.recipes().stream().filter(r->r.output().item().equals("forbidden_magic:crystalwell")).toList();
        test.assertTrue(wells.size()==1&&wells.get(0).ingredients().size()==(RecipeConfigCondition.value("easy_research")?4:6),"Only the world-appropriate crystalwell input recipe is selected");
        var cakes=data.recipes().stream().filter(r->r.output().item().equals("forbidden_magic:arcane_cake")).toList();
        int food=ForbiddenConfig.value(ForbiddenConfig.GLUTTONY);
        test.assertTrue(cakes.size()==(food==1?0:1),"Disabled food recipes cannot contribute calculation inputs");
        if(food!=1){
            long stars=cakes.get(0).ingredients().stream().filter(i->i.alternatives().stream().anyMatch(k->k.item().equals("forbidden_magic:nether_star_block"))).count();
            test.assertTrue(stars==(food==2?2:0),"Hardcore calculation uses actual nether-star-block ingredients");
        }
        if(IntegrationContent.botanyEnabled()){
            var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("forbidden_magic:wand_rod_livingwood"));
            test.assertTrue(!ItemStackAspects.resolve(new ItemStack(item)).orElseThrow().isEmpty(),"Mana-activated rod retains the crafting-derived aspects");
        }
        test.succeed();
    }
    @GameTest(template="test_empty")
    public static void fmEntityContributionsPreserveIdentityAndBase(GameTestHelper test){
        var normal=EntityType.CREEPER.create(test.getLevel());var powered=EntityType.CREEPER.create(test.getLevel());
        var tag=new net.minecraft.nbt.CompoundTag();powered.saveWithoutId(tag);tag.putBoolean("powered",true);powered.load(tag);
        var a=EntityAspects.resolve(normal).orElseThrow();var b=EntityAspects.resolve(powered).orElseThrow();
        var wrath=TCAspects.catalog().require("forbidden_magic:ira");
        test.assertTrue(a.aspects().amount(wrath)==2&&b.aspects().amount(wrath)==4,"Charged creeper is four wrath, not six");
        test.assertTrue(a.aspects().amount(TCAspects.catalog().require("ignis"))==2&&a.aspects().amount(TCAspects.catalog().require("herba"))==2&&b.aspects().amount(TCAspects.catalog().require("potentia"))==3,"Original normal and powered creeper aspects survive addition");
        test.assertTrue(!a.legacyIdentity().equals(b.legacyIdentity()),"Normal and charged scan identities remain distinct");
        var pig=EntityAspects.resolve(EntityType.PIG.create(test.getLevel())).orElseThrow();
        test.assertTrue(pig.aspects().amount(TCAspects.catalog().require("forbidden_magic:gula"))==3,"Pig gains original gluttony");
        var tentacle=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse("thaumcraft:taintacle")).create(test.getLevel());
        test.assertTrue(EntityAspects.resolve(tentacle).orElseThrow().aspects().amount(TCAspects.catalog().require("forbidden_magic:luxuria"))==(ForbiddenConfig.value(ForbiddenConfig.NO_LUST)?0:3),"Entity lust obeys noLust");
        test.succeed();
    }
    @GameTest(template="test_empty")
    public static void fmWandConfigChangesRealCapacityAndCosts(GameTestHelper test){
        var capacity=WandConfiguration.CAPACITY.get("profane");int previous=capacity.get();
        var cost=WandConfiguration.COST.get("alchemical");double previousCost=cost.get();
        try{
            capacity.set(173);cost.set(.67);var snapshot=WandAttributes.refreshServer();
            var stack=thaumcraft.api.ThaumcraftApi.createWand("forbidden_magic:profane","forbidden_magic:alchemical",false,true);
            var wand=(tc4revival.content.CastingWandItem)stack.getItem();
            test.assertTrue(wand.capacity(stack)==17300,"Actual wand capacity uses configured vis, converted to centivis");
            test.assertTrue(WandParts.Rod.require("forbidden_magic:profaned").capacity()==173,"Expired contract retains configured capacity");
            test.assertTrue(Math.abs(WandParts.Cap.require("forbidden_magic:alchemical").cost("aqua")-.57f)<.00001,"Alchemical aqua discount remains ten percentage points");
            test.assertTrue(Math.abs(WandParts.Cap.require("forbidden_magic:alchemical").cost("ignis")-.67f)<.00001,"Other aspects use the configured cost");
            test.assertTrue(snapshot.getAsJsonObject("rods").get("forbidden_magic:profane").getAsInt()==173,"Configured capacity is included in base synchronization snapshot");
        }finally{capacity.set(previous);cost.set(previousCost);WandAttributes.refreshServer();}
        test.succeed();
    }
    @GameTest(template="test_empty")
    public static void fmResearchPrefixTogglePreservesDescription(GameTestHelper test){
        boolean previous=ForbiddenConfig.TAG_RESEARCH.get();
        try{
            ForbiddenConfig.TAG_RESEARCH.set(true);
            var event=new thaumcraft.api.research.ResearchTooltipEvent("forbidden_magic:BOTANY",false,net.minecraft.network.chat.Component.literal("description"));
            ResearchSourceLabels.decorate(event);test.assertTrue(event.description().getString().equals("[FM] [B] description"),"Botania source prefix is applied before layout");
            ForbiddenConfig.TAG_RESEARCH.set(false);
            var off=new thaumcraft.api.research.ResearchTooltipEvent("forbidden_magic:BOTANY",false,net.minecraft.network.chat.Component.literal("description"));
            ResearchSourceLabels.decorate(off);test.assertTrue(off.description().getString().equals("description"),"Disabled prefix leaves original text untouched");
            ForbiddenConfig.TAG_RESEARCH.set(true);
            var base=new thaumcraft.api.research.ResearchTooltipEvent("BASICARTIFICE",false,net.minecraft.network.chat.Component.literal("base"));
            ResearchSourceLabels.decorate(base);test.assertTrue(base.description().getString().equals("base"),"Base research remains unchanged");
        }finally{ForbiddenConfig.TAG_RESEARCH.set(previous);}
        test.succeed();
    }
}
