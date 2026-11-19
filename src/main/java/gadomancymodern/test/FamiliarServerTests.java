package gadomancymodern.test;

import gadomancymodern.familiar.*;
import gadomancymodern.item.EtherealFamiliarItem;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.TCAspects;
import tc4revival.content.*;

@GameTestHolder("gadomancy") @PrefixGameTestTemplate(false)
public class FamiliarServerTests {
    @GameTest(templateNamespace="gadomancy",template="test_empty",timeoutTicks=400)
    public static void familiarBookPreviewsUseValidAspectAndPreviousAugments(GameTestHelper test){
        int checked=0;
        for(var raw:test.getLevel().getRecipeManager().getRecipes()){
            if(!(raw instanceof FamiliarInfusion recipe)||recipe.template()||recipe.augment.isEmpty())continue;
            var json=recipe.displayDefinition();
            var centers=new InfusionIngredient(json.getAsJsonObject("center")).displayStacks();
            test.assertTrue(!centers.isEmpty(),"Familiar preview has center "+recipe.getId());
            for(var center:centers){
                var before=center.copy();
                test.assertTrue(recipe.acceptsCenter(center),"Preview respects aspect and previous augment: "+recipe.getId());
                var plan=recipe.plan(center);var ingredients=plan.components().stream().map(i->i.displayStacks().get(0)).toList();
                test.assertTrue(recipe.matches(new InfusionRecipe.Input(center,ingredients,key->true),test.getLevel()),"Displayed recipe is craftable "+recipe.getId());
                var page=new tc4revival.research.ResearchPage(tc4revival.research.ResearchPage.Type.INFUSION_CRAFTING,Optional.empty(),Optional.empty(),Optional.empty(),Map.of(),Optional.empty(),new tc4revival.research.ResearchPage.Reference(List.of(recipe.getId().toString()),"modern"));
                var rows=tc4revival.research.LiveBookRecipes.resolve(test.getLevel(),page);
                test.assertTrue(rows.size()==1&&ItemStack.isSameItemSameTags(rows.get(0).output(),plan.output().apply(center)),"Live book shows real resulting augment "+recipe.getId());
                test.assertTrue(ItemStack.matches(before,center),"Preview does not consume or mutate center");checked++;
            }
        }
        test.assertTrue(checked>=19,"All augment stages and expanded aspects covered: "+checked);test.succeed();
    }
    public static net.minecraftforge.common.util.FakePlayer player(GameTestHelper test){var p=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"GadFamiliar"));var pos=test.absolutePos(new BlockPos(3,100,3));p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);p.getAbilities().instabuild=false;return p;}
    public static ItemStack familiar(String aspect){var stack=new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("gadomancy","ethereal_familiar")));EtherealFamiliarItem.aspect(stack,aspect);return stack;}
    public static net.minecraft.world.entity.monster.Creeper enemy(GameTestHelper test,net.minecraft.world.entity.player.Player player){var mob=EntityType.CREEPER.create(test.getLevel());mob.setPos(player.position().add(2,0,0));mob.setNoAi(true);test.getLevel().addFreshEntity(mob);return mob;}
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void researchGateAndRealCuriosSlotControlFamiliar(GameTestHelper test){
        var p=player(test);var stack=familiar("praecantatio");var item=(EtherealFamiliarItem)stack.getItem();var context=new top.theillusivec4.curios.api.SlotContext("necklace",p,0,false,true);
        test.assertTrue(!item.canEquip(context,stack),"Unknown familiar research blocks equipping");thaumcraft.api.ThaumcraftApiHelper.completeResearch(p,"gadomancy:ethereal_familiar");test.assertTrue(item.canEquip(context,stack),"Completed actual research allows equipping");
        var inventory=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).resolve().orElseThrow();inventory.reset();var slots=inventory.getStacksHandler("necklace").orElseThrow().getStacks();slots.setStackInSlot(0,stack);
        test.assertTrue(WandInventory.wornBaubles(p).stream().anyMatch(s->s.getItem() instanceof EtherealFamiliarItem),"Real Curios amulet inventory contains familiar");var target=enemy(test,p);item.curioTick(context,slots.getStackInSlot(0));test.assertTrue(target.getHealth()==19,"Worn Curios tick runs real server attack");item.onUnequip(context,ItemStack.EMPTY,stack);slots.setStackInSlot(0,ItemStack.EMPTY);target.discard();test.succeed();
    }
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void secondaryResearchRequiresAnyOneElementNotAllFour(GameTestHelper test){
        for(String element:List.of("fire","shock","poison","weakness")){
            var p=player(test);thaumcraft.api.ThaumcraftApiHelper.completeResearch(p,"gadomancy:ethereal_familiar");thaumcraft.api.ThaumcraftApiHelper.completeResearch(p,"VOIDMETAL");var data=tc4revival.research.KnowledgeSavedData.get(p.server);var rules=tc4revival.research.ResearchCatalogs.rules();
            test.assertTrue(!rules.prerequisites(data.snapshot(p.getUUID()).orElseThrow(),"gadomancy:familiar_damage")&&!rules.prerequisites(data.snapshot(p.getUUID()).orElseThrow(),"gadomancy:familiar_range"),"Base familiar and void metal alone are insufficient");
            thaumcraft.api.ThaumcraftApiHelper.completeResearch(p,"gadomancy:familiar_"+element);test.assertTrue(rules.prerequisites(data.snapshot(p.getUUID()).orElseThrow(),"gadomancy:familiar_damage")&&rules.prerequisites(data.snapshot(p.getUUID()).orElseThrow(),"gadomancy:familiar_range"),"Any one elemental research unlocks the secondary branch: "+element);
        }test.succeed();
    }
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void allTwentyRecipesBindActualAspectAndPreserveAugmentNbt(GameTestHelper test){
        var level=test.getLevel();var recipes=level.getRecipeManager().getRecipes().stream().filter(r->r instanceof FamiliarInfusion f&&!f.template()).map(r->(FamiliarInfusion)r).toList();test.assertTrue(recipes.size()==20*TCAspects.catalog().all().size(),"Creation plus all 19 upgrade tiers exist for every live aspect");
        for(var recipe:recipes){var definition=recipe.templateDefinition();String aspect=definition.get("familiar_aspect").getAsString();String augment=definition.has("augment")?definition.get("augment").getAsString():"";int prior=definition.has("previous_level")?definition.get("previous_level").getAsInt():0;
            var center=augment.isEmpty()?new ItemStack(TCItems.runicBauble(RunicBaubleType.AMULET)):familiar(aspect);if(!augment.isEmpty()){if(!FamiliarAugment.REGISTRY.get(augment).element)EtherealFamiliarItem.augment(center,"fire",1);if(prior>0)EtherealFamiliarItem.augment(center,augment,prior);center.getOrCreateTag().putString("OtherModData","retained");}
            var plan=recipe.plan(center);var parts=plan.components().stream().map(p->p.displayStacks().get(0)).toList();test.assertTrue(recipe.matches(new InfusionRecipe.Input(center,parts,key->true),level),"Actual ingredients match "+recipe.getId());var output=recipe.assemble(new InfusionRecipe.Input(center,parts,key->true),level.registryAccess());test.assertTrue(EtherealFamiliarItem.aspect(output).equals(aspect),"Craft output keeps matching aspect");if(!augment.isEmpty())test.assertTrue(EtherealFamiliarItem.augments(output).get(augment)==prior+1&&output.getTag().getString("OtherModData").equals("retained"),"Upgrade increments actual state and preserves unrelated NBT");
            var wrong=new ArrayList<>(parts);for(int i=0;i<wrong.size();i++)if(wrong.get(i).is(TCItems.WISP_ESSENCE.get())){wrong.set(i,TCItems.WISP_ESSENCE.get().of(TCAspects.catalog().require(aspect.equals("aer")?"ignis":"aer")));break;}test.assertTrue(!recipe.matches(new InfusionRecipe.Input(center,wrong,key->true),level),"Mixed-color essence never matches "+recipe.getId());test.assertTrue(!recipe.matches(new InfusionRecipe.Input(center,parts,key->false),level),"Actual research requirement gates infusion");
        }test.succeed();
    }
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void visIsSimulatedThenSpentInHundredthsAndDefaultAttackNeedsNoWand(GameTestHelper test){
        var p=player(test);var enemy=enemy(test,p);var item=familiar("praecantatio");var controller=new FamiliarController();controller.update(p,item);test.assertTrue(enemy.getHealth()==19&&controller.cooldown==32,"Unaugmented familiar attacks for one magic damage without vis");enemy.discard();
        var augmented=familiar("tempestas");EtherealFamiliarItem.augment(augmented,"shock",1);var next=enemy(test,p);new FamiliarController().update(p,augmented);test.assertTrue(next.getHealth()==20,"Augmented familiar waits without an affordable wand");
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true);p.getInventory().items.set(35,wand);var air=TCAspects.catalog().require("aer");int before=TCItems.CASTING_WAND.get().vis(wand,air);var costs=FamiliarAugment.costs(Map.of("shock",1));test.assertTrue(FamiliarController.consumeVis(p,costs,false)&&TCItems.CASTING_WAND.get().vis(wand,air)==before,"Simulation never spends EU/vis");test.assertTrue(FamiliarController.consumeVis(p,costs,true)&&before-TCItems.CASTING_WAND.get().vis(wand,air)==3,"Original three hundredths of Aer, not three whole vis");new FamiliarController().update(p,augmented);test.assertTrue(next.getHealth()<20,"Real charged inventory enables enhanced attack");next.discard();test.succeed();
    }
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void fourElementsApplyLegacyStatusesAndWeaknessSelectsThree(GameTestHelper test){
        var p=player(test);for(String element:List.of("shock","fire","poison","weakness")){var enemies=new ArrayList<net.minecraft.world.entity.monster.Creeper>();for(int i=0;i<(element.equals("weakness")?3:1);i++){var enemy=enemy(test,p);enemy.setPos(p.position().add(2,0,i));enemies.add(enemy);}
            test.assertTrue(FamiliarController.attack(p,Map.of(element,3)),"Hostile target found for "+element);for(var target:enemies){test.assertTrue(target.getHealth()<20,"Actual magic damage applied");if(element.equals("fire"))test.assertTrue(target.isOnFire()&&target.hasEffect(TCEffects.SUN_SCORNED.get())&&target.getEffect(TCEffects.SUN_SCORNED.get()).getAmplifier()==2,"Fire III ignites and inflicts Sun Scorned III");if(element.equals("poison"))test.assertTrue(target.hasEffect(MobEffects.POISON)&&target.getEffect(MobEffects.POISON).getAmplifier()==1&&target.getEffect(MobEffects.BLINDNESS).getDuration()==2400,"Poison III supplies Poison II and two-minute Blindness IV");if(element.equals("weakness"))test.assertTrue(target.getEffect(MobEffects.WEAKNESS).getAmplifier()==1&&target.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()==1,"Weakness III affects all three selected enemies");target.discard();}
        }var cow=EntityType.COW.create(test.getLevel());cow.setPos(p.position().add(1,0,0));test.getLevel().addFreshEntity(cow);test.assertTrue(!FamiliarController.attack(p,Map.of()),"Passive creatures are excluded");cow.discard();test.succeed();
    }
}
