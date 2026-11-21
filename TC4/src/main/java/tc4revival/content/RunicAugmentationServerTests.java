package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tc4revival.aspect.TCAspects;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class RunicAugmentationServerTests {
    private RunicAugmentationServerTests() {}
    private static InfusionRecipe recipe(GameTestHelper test) {
        return (InfusionRecipe)test.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("thaumcraft:infusion/runic_augmentation")).orElseThrow();
    }
    private static List<ItemStack> inputs(int charge) {
        var stacks=new ArrayList<ItemStack>(); stacks.add(new ItemStack(Items.DIAMOND));
        for (int index=0;index<=Math.max(0,charge);index++) stacks.add(new ItemStack(TCItems.material(MaterialType.SALIS_MUNDUS)));
        return stacks;
    }
    @GameTest(template="wand_test_empty")
    public static void dynamicMatchingRequiresResearchAndExactPedestalCount(GameTestHelper test) {
        var recipe=recipe(test); var goggles=new ItemStack(TCItems.GOGGLES.get()); goggles.getOrCreateTag().putByte("RS.HARDEN",(byte)2);
        test.assertTrue(recipe.matches(new InfusionRecipe.Input(goggles,inputs(2),key->true),test.getLevel()),"Charge two needs one diamond and three separate salis pedestals");
        test.assertTrue(!recipe.matches(new InfusionRecipe.Input(goggles,inputs(2),key->false),test.getLevel()),"Missing runic research must reject the dynamic recipe");
        test.assertTrue(!recipe.matches(new InfusionRecipe.Input(goggles,inputs(1),key->true),test.getLevel()),"Insufficient pedestal components must fail");
        test.assertTrue(!recipe.matches(new InfusionRecipe.Input(goggles,inputs(3),key->true),test.getLevel()),"An extra occupied pedestal must also fail");
        var vanilla=new ItemStack(Items.DIAMOND_CHESTPLATE); vanilla.getOrCreateTag().putByte("RS.HARDEN",(byte)2);
        test.assertTrue(!recipe.matches(new InfusionRecipe.Input(vanilla,inputs(2),key->true),test.getLevel()) && RunicGear.charge(vanilla)==0,"NBT alone must not make new vanilla armor runic eligible");
        tc4revival.TC4Revival.LOGGER.info("PASS dynamicMatchingRequiresResearchAndExactPedestalCount");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void costsGrowFromFinalChargeAndOutputCapturesOriginalItem(GameTestHelper test) {
        var robe=new ItemStack(TCItems.ROBE_CHEST.get()); robe.getOrCreateTag().putString("retained","original"); robe.setDamageValue(7);
        for (int charge=0;charge<5;charge++) {
            robe.getOrCreateTag().putByte("RS.HARDEN",(byte)charge); var plan=recipe(test).plan(robe);
            test.assertTrue(plan.aspects().amount(TCAspects.catalog().require("potentia"))==(32<<charge),"Energy cost doubles at each final charge");
            test.assertTrue(plan.aspects().amount(TCAspects.catalog().require("tutamen"))==(16<<charge) && plan.aspects().amount(TCAspects.catalog().require("praecantatio"))==(16<<charge),"Armor and magic each cost half the energy");
            test.assertTrue(plan.instability()==5+charge/2 && !plan.enchanting() && plan.xp()==0 && plan.components().size()==charge+2,"Dynamic instability and component count follow the starting charge");
        }
        var plan=RunicAugmentation.plan(robe); robe.getOrCreateTag().putString("retained","changed later"); robe.setDamageValue(20);
        var output=InfusionOutput.load(plan.output().save()).apply(robe);
        test.assertTrue(output.getTag().getString("retained").equals("original") && output.getDamageValue()==7 && RunicGear.hardening(output)==5,"Runic output preserves the original item snapshot across transaction save/load");
        tc4revival.TC4Revival.LOGGER.info("PASS costsGrowFromFinalChargeAndOutputCapturesOriginalItem");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void legacySignedByteAndHighCostSaturationRemainExplicit(GameTestHelper test) {
        var goggles=new ItemStack(TCItems.GOGGLES.get()); goggles.getOrCreateTag().putByte("RS.HARDEN",(byte)127);
        var plan=RunicAugmentation.plan(goggles);
        test.assertTrue(plan.aspects().amount(TCAspects.catalog().require("potentia"))==Integer.MAX_VALUE,"Large exponential costs saturate as the original double-to-int conversion did");
        var output=plan.output().apply(goggles);
        test.assertTrue(RunicGear.hardening(output)==-128,"The old signed-byte wrap is retained instead of inventing a new upgrade cap");
        var negative=RunicAugmentation.plan(output);
        test.assertTrue(negative.aspects().isEmpty() && negative.components().size()==2,"Negative legacy charge retains its rounded-zero cost and minimum component list");
        tc4revival.TC4Revival.LOGGER.info("PASS legacySignedByteAndHighCostSaturationRemainExplicit");test.succeed();
    }

    @GameTest(template="wand_test_empty")
    public static void loadedRunicRecipeCoversEquipmentAndAllSignedByteLevels(GameTestHelper test) {
        var recipe=recipe(test);var level=test.getLevel();int eligible=0,checked=0,cases=0;
        var oldBaubles=java.util.Set.of("ItemBaubleBlanks","ItemAmuletVis","ItemGirdleHover","ItemRingRunic","ItemAmuletRunic","ItemGirdleRunic");
        var identities=tc4revival.aspect.item.LegacyItemMappings.identity();
        for(var item:net.minecraftforge.registries.ForgeRegistries.ITEMS) {
            var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);if(!id.getNamespace().equals("thaumcraft"))continue;
            var old=identities.recipeKey(id.toString());
            boolean expected=item instanceof net.minecraft.world.item.ArmorItem||old!=null&&oldBaubles.contains(old.item().replace("Thaumcraft:",""));
            var center=new ItemStack(item);center.getOrCreateTag().putByte("RS.HARDEN",(byte)-128);
            boolean actual=recipe.matches(new InfusionRecipe.Input(center,inputs(0),key->true),level);
            test.assertTrue(actual==expected,"Old armor and bauble eligibility, including zero intrinsic charge: "+id);if(expected)eligible++;checked++;
        }
        var gear=new java.util.LinkedHashMap<net.minecraft.world.item.Item,Integer>();gear.put(TCItems.GOGGLES.get(),0);
        int[] intrinsic={1,5,4,4,8,7,10,9};int index=0;
        for(var type:RunicBaubleType.values())gear.put(TCItems.runicBauble(type),intrinsic[index++]);
        for(var entry:gear.entrySet())for(int hardening=-128;hardening<=127;hardening++) {
            var center=new ItemStack(entry.getKey());center.getOrCreateTag().putByte("RS.HARDEN",(byte)hardening);center.getTag().putString("RunicAudit","retained");
            int charge=entry.getValue()+hardening,energy=(int)(32.0*Math.pow(2.0,charge));var components=inputs(charge);
            var input=new InfusionRecipe.Input(center,components,key->key.equals("RUNICAUGMENTATION"));
            test.assertTrue(InfusionRecipe.find(level,input).orElseThrow()==recipe,"Actual registry lookup accepts every old signed-byte level");
            var plan=recipe.plan(center);
            test.assertTrue(plan.instability()==5+charge/2&&plan.components().size()==Math.max(0,charge)+2&&plan.xp()==0&&!plan.enchanting(),"Intrinsic charge contributes to instability and ingredient count");
            test.assertTrue(plan.aspects().size()==(energy>0?3:0)&&plan.aspects().amount(TCAspects.catalog().require("potentia"))==energy&&plan.aspects().amount(TCAspects.catalog().require("tutamen"))==energy/2&&plan.aspects().amount(TCAspects.catalog().require("praecantatio"))==energy/2,"Legacy rounding, exponential cost and integer saturation");
            var expected=center.copy();expected.getTag().putByte("RS.HARDEN",(byte)(hardening+1));
            test.assertTrue(ItemStack.matches(recipe.assemble(input,level.registryAccess()),expected),"Only hardening increments, preserving other tags and signed wrap");
            var process=new InfusionProcess();process.start(plan,center,"audit",0);process.load(process.save());
            test.assertTrue(process.save().getInt("RecipeInstability")==5+charge/2,"Negative old runic recipe instability must survive reload without clamping");
            var persisted=InfusionOutput.load(process.save().getCompound("Output")).apply(center);
            test.assertTrue(ItemStack.matches(persisted,expected),"Ordinary runic snapshot survives transaction serialization unlike enchanting output");cases++;
        }
        tc4revival.TC4Revival.LOGGER.info("PASS loadedRunicRecipeCoversEquipmentAndAllSignedByteLevels: {} registry items, {} eligible, {} level cases",checked,eligible,cases);test.succeed();
    }

    @GameTest(template="wand_test_empty")
    public static void runicAltarPreservesCapturedEquipmentAcrossReload(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new net.minecraft.core.BlockPos(3,8,3));
        for(var at:net.minecraft.core.BlockPos.betweenClosed(pos.offset(-6,-4,-6),pos.offset(6,1,6)))level.setBlock(at,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),18);
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)if(x!=0||z!=0) {
            level.setBlock(pos.offset(x,-4,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),18);
            level.setBlock(pos.offset(x,-3,z),net.minecraft.world.level.block.Blocks.SKELETON_SKULL.defaultBlockState(),18);
        }
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})level.setBlock(pos.offset(x,-2,z),TCArcane.INFUSION_PILLAR.get().defaultBlockState(),18);
        level.setBlock(pos,TCArcane.INFUSION_MATRIX.get().defaultBlockState(),18);level.setBlock(pos.below(2),TCArcane.ARCANE_PEDESTAL.get().defaultBlockState(),18);
        var pedestals=new ArrayList<ArcanePedestalBlockEntity>();var jars=new ArrayList<EssentiaJarBlockEntity>();
        for(int i=0;i<3;i++) {
            var at=pos.offset(3,-2,i-1);level.setBlock(at,TCArcane.ARCANE_PEDESTAL.get().defaultBlockState(),18);pedestals.add((ArcanePedestalBlockEntity)level.getBlockEntity(at));
            at=pos.offset(-3,-2,1-i);level.setBlock(at,TCArcane.ARCANE_PEDESTAL.get().defaultBlockState(),18);
            at=pos.offset(-5,-2,i-1);level.setBlock(at,TCArcane.ESSENTIA_JAR.get().defaultBlockState(),18);jars.add((EssentiaJarBlockEntity)level.getBlockEntity(at));
        }
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"RunicAltarAudit"));player.moveTo(pos.getCenter());
        tc4revival.research.KnowledgeSavedData.get(level.getServer()).update(player.getUUID(),bound->0,k->k.rememberResearch("RUNICAUGMENTATION"));
        for(int charge=0;charge<2;charge++) {
            var center=(ArcanePedestalBlockEntity)level.getBlockEntity(pos.below(2));var equipment=new ItemStack(TCItems.GOGGLES.get());equipment.setDamageValue(7);equipment.getOrCreateTag().putByte("RS.HARDEN",(byte)charge);
            equipment.getTag().putString("RunicAudit","captured");center.setItem(0,equipment);
            var components=inputs(charge);for(int i=0;i<3;i++)pedestals.get(i).setItem(0,i<components.size()?components.get(i):ItemStack.EMPTY);
            var plan=recipe(test).plan(equipment);int i=0;for(var aspect:plan.aspects().aspects())jars.get(i++).addToContainer(aspect,plan.aspects().amount(aspect));
            var matrix=(InfusionMatrixBlockEntity)level.getBlockEntity(pos);matrix.activate();EssentiaSources.refresh(matrix);matrix.start(player);
            test.assertTrue(matrix.crafting()&&matrix.instability()<=0,"Actual registered runic recipe starts without random instability");
            center.getItem(0).setDamageValue(19);boolean reloaded=false;
            for(int tick=0;tick<2500&&matrix.crafting();tick++) {
                matrix.tick();
                if(charge==0&&!reloaded&&tick==20) {
                    var saved=matrix.saveWithoutMetadata();var replacement=new InfusionMatrixBlockEntity(pos,matrix.getBlockState());replacement.load(saved);
                    level.removeBlockEntity(pos);level.setBlockEntity(replacement);matrix=replacement;reloaded=true;
                }
            }
            var output=center.getItem(0);
            test.assertTrue(!matrix.crafting()&&jars.stream().allMatch(jar->jar.amount()==0)&&pedestals.stream().allMatch(ArcanePedestalBlockEntity::isEmpty),"Runic costs and actual components consumed exactly once");
            test.assertTrue(output.getDamageValue()==7&&RunicGear.hardening(output)==charge+1&&output.getTag().getString("RunicAudit").equals("captured")&&(charge!=0||reloaded),"Runic completion restores original captured wear/NBT, including through reload");
        }
        tc4revival.TC4Revival.LOGGER.info("PASS runicAltarPreservesCapturedEquipmentAcrossReload: two real altar upgrades");test.succeed();
    }
}
