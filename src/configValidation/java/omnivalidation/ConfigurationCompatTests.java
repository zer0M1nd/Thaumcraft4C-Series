package omnivalidation;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.*;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;
import cy.jdkdigital.productivebees.common.entity.bee.ConfigurableBee;
import cy.jdkdigital.productivebees.common.block.entity.*;
import cy.jdkdigital.productivebees.common.item.*;
import cy.jdkdigital.productivebees.util.BeeHelper;
import slimeknights.tconstruct.library.tools.nbt.*;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.SlotType;
import thaumcraft.api.config.*;
import thaumicomniverse.RevivalFeatures;
import thaumicomniverse.RevivalFeatures.Group;

/** Installed integration entry points; no production assertions are replaced by the fixture. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public class ConfigurationCompatTests {
    static ItemStack item(String id){return new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id))));}
    static ConfigurableBee bee(GameTestHelper h) {
        var entity=ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse("productivebees:configurable_bee")).create(h.getLevel());
        var bee=(ConfigurableBee)entity;bee.setBeeType("thaumicomniverse:thaumium");bee.setDefaultAttributes();return bee;
    }
    static CompoundTag inventory(IItemHandlerModifiable inv){var out=new CompoundTag();for(int i=0;i<inv.getSlots();i++)out.put("slot"+i,inv.getStackInSlot(i).save(new CompoundTag()));return out;}

    @GameTest(template="wand_test_empty",templateNamespace="thaumcraft",timeoutTicks=100,batch="configuration")
    public static void productiveProductionAndSelfBreedingRespectFeature(GameTestHelper h) {
        var previous=Group.PRODUCTIVE_BEES.option().configured();
        try {
            Group.PRODUCTIVE_BEES.option().set(true);var first=bee(h);var second=bee(h);
            h.assertTrue(!BeeHelper.getBeeProduce(h.getLevel(),first,false,1).isEmpty(),"Enabled bee has no produce");
            h.assertTrue(!BeeHelper.getBreedingRecipes(new BeeHelper.IdentifierInventory(first,second),h.getLevel()).isEmpty(),"Enabled self breeding unavailable");
            h.assertTrue(BeeHelper.getBreedingResult(first,second,h.getLevel())!=null,"Enabled self breeding has no offspring");
            Group.PRODUCTIVE_BEES.option().set(false);
            h.assertTrue(BeeHelper.getBeeProduce(h.getLevel(),first,false,1).isEmpty(),"Existing bee still produces");
            h.assertTrue(BeeHelper.getBreedingRecipes(new BeeHelper.IdentifierInventory(first,second),h.getLevel()).isEmpty(),"Machine self-breeding fallback bypass");
            h.assertTrue(BeeHelper.getBreedingResult(first,second,h.getLevel())==null,"Direct self-breeding fallback bypass");
            h.assertTrue(first.getBeeType().equals("thaumicomniverse:thaumium"),"Disabling erased the genome");
            Group.PRODUCTIVE_BEES.option().set(true);
            h.assertTrue(!BeeHelper.getBeeProduce(h.getLevel(),first,false,1).isEmpty(),"Re-enabling did not resume production");
        } finally {Group.PRODUCTIVE_BEES.option().set(previous);}
        h.succeed();
    }

    @GameTest(template="wand_test_empty",templateNamespace="thaumcraft",timeoutTicks=100,batch="configuration")
    public static void productiveIncubatorPausesGeneAndCageWithoutConsumption(GameTestHelper h) {
        var previous=Group.PRODUCTIVE_BEES.option().configured();
        var pos=new BlockPos(1,1,1);h.setBlock(pos,ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("productivebees:incubator")));
        var tile=(IncubatorBlockEntity)h.getBlockEntity(pos);var inv=(IItemHandlerModifiable)tile.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new);
        var energy=tile.getCapability(ForgeCapabilities.ENERGY).orElseThrow(IllegalStateException::new);
        try {
            for(boolean cageCase:new boolean[]{false,true}) {
                Group.PRODUCTIVE_BEES.option().set(true);
                for(int i=0;i<inv.getSlots();i++)inv.setStackInSlot(i,ItemStack.EMPTY);
                if(cageCase){var cage=item("productivebees:bee_cage");BeeCage.captureEntity(bee(h),cage);inv.setStackInSlot(0,cage);var treat=item("productivebees:honey_treat");treat.setCount(64);inv.setStackInSlot(1,treat);}
                else {inv.setStackInSlot(0,new ItemStack(Items.EGG));inv.setStackInSlot(1,HoneyTreat.getTypeStack("thaumicomniverse:thaumium",100));}
                energy.receiveEnergy(10000,false);tile.recipeProgress=0;tile.isRunning=false;
                IncubatorBlockEntity.tick(h.getLevel(),h.absolutePos(pos),tile.getBlockState(),tile);
                h.assertTrue(tile.isRunning&&tile.recipeProgress==1,"Enabled incubator failed to start; cage="+cageCase);
                var before=inventory(inv);int power=energy.getEnergyStored(),progress=tile.recipeProgress;
                Group.PRODUCTIVE_BEES.option().set(false);
                for(int i=0;i<10;i++)IncubatorBlockEntity.tick(h.getLevel(),h.absolutePos(pos),tile.getBlockState(),tile);
                h.assertTrue(!tile.isRunning&&tile.recipeProgress==progress&&power==energy.getEnergyStored()&&before.equals(inventory(inv)),"Disabled in-progress incubation consumed input/power or progressed");
                Group.PRODUCTIVE_BEES.option().set(true);
                IncubatorBlockEntity.tick(h.getLevel(),h.absolutePos(pos),tile.getBlockState(),tile);
                h.assertTrue(tile.isRunning&&tile.recipeProgress==progress+1,"Incubation did not resume");
            }
        } finally {Group.PRODUCTIVE_BEES.option().set(previous);h.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR);}
        h.succeed();
    }

    @GameTest(template="wand_test_empty",templateNamespace="thaumcraft",timeoutTicks=100,batch="configuration")
    public static void productiveBreedingChamberPausesExistingWork(GameTestHelper h) {
        var previous=Group.PRODUCTIVE_BEES.option().configured();var pos=new BlockPos(1,1,1);
        h.setBlock(pos,ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("productivebees:breeding_chamber")));
        var tile=(BreedingChamberBlockEntity)h.getBlockEntity(pos);
        var inv=(IItemHandlerModifiable)tile.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new);
        var energy=tile.getCapability(ForgeCapabilities.ENERGY).orElseThrow(IllegalStateException::new);
        try {
            Group.PRODUCTIVE_BEES.option().set(true);
            inv.setStackInSlot(0,item("productivebees:bee_cage"));
            for(int slot:new int[]{1,2}) {var cage=item("productivebees:bee_cage");BeeCage.captureEntity(bee(h),cage);inv.setStackInSlot(slot,cage);}
            var definition=cy.jdkdigital.productivebees.setup.BeeReloadListener.INSTANCE.getData("thaumicomniverse:thaumium");
            var food=ConfigurableBee.getBreedingIngredientFromString(definition.getString("breedingItem")).getItems();
            h.assertTrue(food.length>0,"Loaded bee breeding ingredient is empty");
            for(int slot:new int[]{3,4}){var stack=food[0].copy();stack.setCount(64);inv.setStackInSlot(slot,stack);}
            energy.receiveEnergy(10000,false);
            for(int i=0;i<22&&!tile.isRunning;i++)BreedingChamberBlockEntity.tick(h.getLevel(),h.absolutePos(pos),tile.getBlockState(),tile);
            h.assertTrue(tile.isRunning&&tile.recipeProgress>0,"Enabled breeding chamber did not start");
            var before=inventory(inv);int power=energy.getEnergyStored(),progress=tile.recipeProgress;
            Group.PRODUCTIVE_BEES.option().set(false);
            for(int i=0;i<10;i++)BreedingChamberBlockEntity.tick(h.getLevel(),h.absolutePos(pos),tile.getBlockState(),tile);
            h.assertTrue(!tile.isRunning&&tile.recipeProgress==progress&&power==energy.getEnergyStored()&&before.equals(inventory(inv)),"Disabled in-progress breeding consumed input/power or progressed");
            Group.PRODUCTIVE_BEES.option().set(true);
            BreedingChamberBlockEntity.tick(h.getLevel(),h.absolutePos(pos),tile.getBlockState(),tile);
            h.assertTrue(tile.isRunning&&tile.recipeProgress==progress+1,"Breeding did not resume");
        } finally {Group.PRODUCTIVE_BEES.option().set(previous);h.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR);}
        h.succeed();
    }

    @GameTest(template="wand_test_empty",templateNamespace="thaumcraft",timeoutTicks=200,batch="configuration")
    public static void everyOmniverseGroupFiltersItsRecipesResearchAndItems(GameTestHelper h) {
        var previous=new LinkedHashMap<RevivalConfig.Option,FeatureSetting>();
        for(var group:Group.values()){previous.put(group.option(),group.option().configured());group.option().set(true);}
        try {
            h.assertTrue(Group.values().length==23,"Inventory of gameplay groups changed");
            var recipes=FeatureRecipes.definitions(h.getLevel().getRecipeManager());int checkedRecipes=0,checkedResearch=0,checkedItems=0;
            for(var group:Group.values()) {
                group.option().set(false);h.assertTrue(!group.enabled(),"Explicit group disable ignored");
                for(var recipe:recipes)if(RevivalFeatures.recipeGroup(recipe.getId())==group) {
                    h.assertTrue(!FeatureRecipes.enabled(recipe.getId()),"Disabled recipe bypass: "+recipe.getId());checkedRecipes++;
                }
                for(var entry:tc4revival.research.ResearchDefinitions.server().catalog().all())if(RevivalFeatures.researchGroup(entry.key())==group) {
                    h.assertTrue(!tc4revival.research.ResearchCatalogs.configuredEnabled(entry),"Disabled research bypass: "+entry.key());checkedResearch++;
                }
                for(var id:ForgeRegistries.ITEMS.getKeys())if(id.getNamespace().equals("thaumicomniverse")&&RevivalFeatures.itemGroup(id.getPath())==group) {
                    h.assertTrue(!RevivalFeatures.itemEnabled(id.getPath()),"Disabled item list bypass: "+id);checkedItems++;
                }
                group.option().set(true);
            }
            h.assertTrue(checkedRecipes>100&&checkedResearch>20&&checkedItems>30,"Integration content missing: "+checkedRecipes+"/"+checkedResearch+"/"+checkedItems);
            Group.ATM_STORAGE.option().set(false);h.assertTrue(!Group.ATM_CERTIFICATE.enabled(),"Certificate bypasses storage parent");
            Group.AVARITIA.option().set(false);h.assertTrue(!Group.AVARITIA_CATALYST.enabled(),"Catalyst bypasses Avaritia parent");
            org.slf4j.LoggerFactory.getLogger("CONFIGURATION_VALIDATION").info("Omniverse configured content: {} recipes, {} research, {} items checked",checkedRecipes,checkedResearch,checkedItems);
        } finally {previous.forEach(RevivalConfig.Option::set);}
        h.succeed();
    }

    @GameTest(template="wand_test_empty",templateNamespace="thaumcraft",timeoutTicks=100,batch="configuration")
    public static void storedTinkersToolSlotsFollowCurrentModeWithoutNbtLoss(GameTestHelper h) {
        var previous=Group.TINKERS.option().configured();
        try {
            Group.TINKERS.option().set(true);
            var item=item("tconstruct:pickaxe").getItem();var material=MaterialVariantId.parse("thaumicomniverse:thaumium");
            var materials=MaterialNBT.builder().add(material).add(material).add(material).build();
            var tool=ToolStack.createTool(item,((IModifiable)item).getToolDefinition(),materials);
            var diamond=new slimeknights.tconstruct.library.modifiers.ModifierId("tconstruct:diamond");
            tool.addModifier(diamond,1);
            int withBonus=tool.getVolatileData().getSlots(SlotType.UPGRADE);
            var held=tool.createStack();var saved=held.save(new CompoundTag());
            var oldStack=ItemStack.of(saved.copy());oldStack.getOrCreateTag().getCompound(ToolStack.TAG_VOLATILE_MOD_DATA).remove("thaumicomniverse:thaumic_slots");
            var legacySaved=oldStack.save(new CompoundTag());
            Group.TINKERS.option().set(false);
            var loaded=ToolStack.from(held);var oldTool=ToolStack.from(oldStack);
            h.assertTrue(loaded.getVolatileData().getSlots(SlotType.UPGRADE)==withBonus-2,"Saved new tool retained bonus slots");
            h.assertTrue(oldTool.getVolatileData().getSlots(SlotType.UPGRADE)==withBonus-2,"Pre-profile tool retained bonus slots");
            h.assertTrue(saved.equals(loaded.createStack().save(new CompoundTag()))&&legacySaved.equals(oldStack.save(new CompoundTag())),"Slot query erased saved materials/modifiers");
            // ItemStack.of invokes Tinkers verifyTag/rebuildStats; derived volatile data legitimately changes.
            // Existing-tool reads above must be byte-for-byte stable; actual reload must retain player-owned data.
            var reloadedStack=ItemStack.of(saved.copy());var reloaded=ToolStack.from(reloadedStack);
            for(String key:new String[]{ToolStack.TAG_MATERIALS,ToolStack.TAG_UPGRADES,ToolStack.TAG_MODIFIERS,ToolStack.TAG_PERSISTENT_MOD_DATA})
                h.assertTrue(Objects.equals(saved.getCompound("tag").get(key),reloadedStack.getOrCreateTag().get(key)),"Reload erased owned tool data: "+key);
            h.assertTrue(reloaded.getUpgrades().getLevel(diamond)==1&&reloaded.getVolatileData().getSlots(SlotType.UPGRADE)==withBonus-2,"Reload lost installed diamond upgrade or retained bonus");
            Group.TINKERS.option().set(true);
            h.assertTrue(loaded.getVolatileData().getSlots(SlotType.UPGRADE)==withBonus&&oldTool.getVolatileData().getSlots(SlotType.UPGRADE)==withBonus&&reloaded.getVolatileData().getSlots(SlotType.UPGRADE)==withBonus,"Re-enable did not restore slots");
        } finally {Group.TINKERS.option().set(previous);}
        h.succeed();
    }
}
