package tc4revival.content;

import java.util.List;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.config.TCConfig;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class RefactorServerTests {
    @GameTest(template="wand_test_empty")
    public static void modernLogsAndShearedMagicalLeaves(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new net.minecraft.core.BlockPos(2,2,2));
        for(var block:List.of(TCItems.SILVERWOOD_LEAVES.get(),TCItems.GREATWOOD_LEAVES.get())) {
            var state=((BlockItem)block).getBlock().defaultBlockState();
            var drops=net.minecraft.world.level.block.Block.getDrops(state,level,pos,null,null,new ItemStack(Items.SHEARS));
            test.assertTrue(drops.size()==1&&drops.get(0).is(block)&&drops.get(0).getCount()==1,"Shears yield one magical leaf block");
        }
        int tested=0;
        for(var block:ForgeRegistries.BLOCKS) {
            var state=block.defaultBlockState();if(!state.is(net.minecraft.tags.BlockTags.LOGS))continue;
            if(state.getDestroySpeed(level,pos)<0)continue;
            level.setBlockAndUpdate(pos,state);level.setBlockAndUpdate(pos.above(),state);level.setBlockAndUpdate(pos.above(2),state);
            test.assertTrue(ElementalTreeSearch.furthest(level,pos).equals(pos.above(2)),"Tree search accepts registered log "+ForgeRegistries.BLOCKS.getKey(block));
            level.removeBlock(pos,false);level.removeBlock(pos.above(),false);level.removeBlock(pos.above(2),false);tested++;
        }
        test.assertTrue(tested>=20,"All vanilla and loaded mod log tags exercised");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void registeredAddonSwordsHaveModernDefaults(GameTestHelper test) {
        var namespaces=java.util.Set.of("thaumcraft","automagy","emt","forbidden_magic","gadomancy",
                "nodalmechanics","taintedmagic","thaumicbases","thaumicenergistics","thaumichorizons",
                "thaumicinfusion","thaumicomniverse","thaumic_tinkerer","warptheory","witchinggadgets");
        int checked=0;
        for(var item:ForgeRegistries.ITEMS) {
            var id=ForgeRegistries.ITEMS.getKey(item);
            if(id==null||!namespaces.contains(id.getNamespace())||!(item instanceof SwordItem))continue;
            var stack=new ItemStack(item);checked++;
            var modifiers=stack.getAttributeModifiers(EquipmentSlot.MAINHAND)
                    .get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
            test.assertTrue(modifiers.stream().anyMatch(m->Math.abs(m.getAmount()+2.4)<.00001),
                    "Registered sword has vanilla attack speed: "+id);
            // Blood Sword pays health to defend: an explicit special ability, not inherited sword blocking.
            if(!item.getClass().getName().equals("ttmodern.BloodSwordItem"))
                test.assertTrue(stack.getUseAnimation()!=UseAnim.BLOCK,"No generic sword blocking: "+id);
        }
        test.assertTrue(checked>=2,"Base sword registry included");
        tc4revival.TC4Revival.LOGGER.info("Refactor modern sword registry checked {} swords",checked);
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void enhancedAltarPaysFullCostsAcrossReload(GameTestHelper test) {
        boolean old=TCConfig.SERVER.enhancedInfusionEnchanting.get();
        try {
            TCConfig.SERVER.enhancedInfusionEnchanting.set(true);
            var level=test.getLevel();var pos=test.absolutePos(new net.minecraft.core.BlockPos(3,6,3));
            for(var at:net.minecraft.core.BlockPos.betweenClosed(pos.offset(-5,-3,-5),pos.offset(5,1,5)))level.setBlock(at,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),18);
            for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})level.setBlock(pos.offset(x,-2,z),TCArcane.INFUSION_PILLAR.get().defaultBlockState(),18);
            level.setBlock(pos.below(2),TCArcane.ARCANE_PEDESTAL.get().defaultBlockState(),18);
            level.setBlock(pos,TCArcane.INFUSION_MATRIX.get().defaultBlockState(),18);
            var center=(ArcanePedestalBlockEntity)level.getBlockEntity(pos.below(2));center.setItem(0,new ItemStack(Items.DIAMOND_SWORD));
            var positions=List.of(pos.offset(3,-2,0),pos.offset(0,-2,3),pos.offset(-3,-2,0));
            var stacks=List.of(new ItemStack(Items.DIAMOND),new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:salis_mundus")),64),new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:salis_mundus"))));
            for(int i=0;i<positions.size();i++) {level.setBlock(positions.get(i),TCArcane.ARCANE_PEDESTAL.get().defaultBlockState(),18);((ArcanePedestalBlockEntity)level.getBlockEntity(positions.get(i))).setItem(0,stacks.get(i));}
            level.setBlock(pos.offset(4,-2,0),TCArcane.ESSENTIA_JAR.get().defaultBlockState(),18);
            var jar=(EssentiaJarBlockEntity)level.getBlockEntity(pos.offset(4,-2,0));var magic=tc4revival.aspect.TCAspects.catalog().require("praecantatio");jar.addToContainer(magic,8);
            var recipe=new InfusionRecipe(ResourceLocation.parse("thaumcraft:refactor_payment"),com.google.gson.JsonParser.parseString("""
                {"instability":0,"enchantment":"minecraft:sharpness","components":[{"ingredient":{"item":"minecraft:diamond"}}],"aspects":{"praecantatio":4}}
                """).getAsJsonObject());
            var input=new InfusionRecipe.Input(center.getItem(0),stacks,k->true);var plan=recipe.planForCraft(input,level);
            var process=new InfusionProcess();process.start(plan,center.getItem(0),"refactor",-1000);
            var saved=new net.minecraft.nbt.CompoundTag();saved.putBoolean("Active",true);saved.put("Process",process.save());
            var matrix=(InfusionMatrixBlockEntity)level.getBlockEntity(pos);matrix.load(saved);EssentiaSources.refresh(matrix);
            for(int tick=0;tick<20;tick++)matrix.tick();
            test.assertTrue(jar.amount()==6&&matrix.crafting(),"Actual altar has paid two units before reload");
            saved=matrix.saveWithoutMetadata();var restored=new InfusionMatrixBlockEntity(pos,matrix.getBlockState());restored.load(saved);level.removeBlockEntity(pos);level.setBlockEntity(restored);matrix=restored;EssentiaSources.refresh(matrix);
            for(int tick=0;tick<500&&matrix.crafting();tick++)matrix.tick();
            test.assertTrue(!matrix.crafting()&&jar.amount()==0,"Reload finishes and pays all eight essentia without experience source");
            test.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,center.getItem(0))==2,"Two levels survive real matrix save/load");
            test.assertTrue(((ArcanePedestalBlockEntity)level.getBlockEntity(positions.get(1))).isEmpty(),"Salt pedestal follows existing altar stack consumption, grants only one level");
            test.assertTrue(((ArcanePedestalBlockEntity)level.getBlockEntity(positions.get(0))).isEmpty()&&((ArcanePedestalBlockEntity)level.getBlockEntity(positions.get(2))).isEmpty(),"Other required ingredients consumed exactly once");
        }finally{TCConfig.SERVER.enhancedInfusionEnchanting.set(old);}
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void modernMaterialTagsAndWeaponDefaults(GameTestHelper test) {
        test.assertTrue(LegacyCraftingCategories.matches(new ItemStack(Items.CHERRY_PLANKS),new ItemStack(Items.OAK_PLANKS)),"Modern wood variants share vanilla planks");
        test.assertTrue(LegacyCraftingCategories.matches(new ItemStack(Items.DEEPSLATE_IRON_ORE),new ItemStack(Items.IRON_ORE)),"Ore variants share specific Forge material tag");
        test.assertTrue(!LegacyCraftingCategories.matches(new ItemStack(Items.COPPER_ORE),new ItemStack(Items.IRON_ORE)),"Different ores are not interchangeable");
        test.assertTrue(!LegacyCraftingCategories.matches(new ItemStack(Items.LAPIS_LAZULI),new ItemStack(Items.BLUE_DYE)),"Lapis no longer belongs to modern dyes");
        int swords=0;
        for(var item:ForgeRegistries.ITEMS)if(item instanceof ThaumiumSwordItem) {
            var stack=new ItemStack(item);swords++;
            var speed=stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
            test.assertTrue(speed.stream().anyMatch(m->Math.abs(m.getAmount()+2.4)<.00001),"Modern sword attack speed: "+ForgeRegistries.ITEMS.getKey(item));
            test.assertTrue(stack.getUseAnimation()!=UseAnim.BLOCK,"Ordinary sword cannot block");
        }
        test.assertTrue(swords>=2,"Registered swords checked");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void orderedInfusionAndSingleLevelEnchantingBoundaries(GameTestHelper test) {
        var config=TCConfig.SERVER;boolean old=config.enhancedInfusionEnchanting.get();
        try {
            config.enhancedInfusionEnchanting.set(true);
            for(String name:List.of("mending","silk_touch")) {
                var recipe=new InfusionRecipe(ResourceLocation.parse("thaumcraft:refactor_"+name),com.google.gson.JsonParser.parseString("""
                    {"instability":1,"enchantment":"minecraft:%s","components":[{"ingredient":{"item":"minecraft:diamond"}}],"aspects":{"praecantatio":4}}
                    """.formatted(name)).getAsJsonObject());
                var tool=new ItemStack(Items.DIAMOND_PICKAXE);
                var salt=new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:salis_mundus")),64);
                var seed=new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:void_seed")));
                var single=new InfusionRecipe.Input(tool,List.of(new ItemStack(Items.DIAMOND),salt),k->true);
                test.assertTrue(recipe.matches(single,test.getLevel())&&recipe.plan(single).xp()==0,"One salt pedestal means one level regardless of stack count");
                var twice=new InfusionRecipe.Input(tool,List.of(new ItemStack(Items.DIAMOND),salt,salt.copy(),seed),k->true);
                test.assertTrue(recipe.matches(twice,test.getLevel())==name.equals("mending"),"Only Mending may exceed a vanilla level-one cap");
            }
            var recipe=new InfusionRecipe(ResourceLocation.parse("thaumcraft:refactor_order"),com.google.gson.JsonParser.parseString("""
                {"instability":1,"center":{"ingredient":{"item":"minecraft:stone"}},"components":[{"ingredient":{"item":"minecraft:diamond"}},{"ingredient":{"item":"minecraft:iron_ingot"}},{"ingredient":{"item":"minecraft:gold_ingot"}}],"aspects":{},"result":{"item":"minecraft:emerald"}}
                """).getAsJsonObject());
            var stone=new ItemStack(Items.STONE);
            var cyclic=List.of(new ItemStack(Items.GOLD_INGOT),new ItemStack(Items.DIAMOND),new ItemStack(Items.IRON_INGOT));
            var reversed=List.of(new ItemStack(Items.GOLD_INGOT),new ItemStack(Items.IRON_INGOT),new ItemStack(Items.DIAMOND));
            test.assertTrue(recipe.matches(new InfusionRecipe.Input(stone,cyclic,k->true,true),test.getLevel()),"Clockwise cyclic offset accepted");
            test.assertTrue(!recipe.matches(new InfusionRecipe.Input(stone,reversed,k->true,true),test.getLevel()),"Reflected order rejected");
            test.assertTrue(recipe.matches(new InfusionRecipe.Input(stone,reversed,k->true,false),test.getLevel()),"Default unordered mode preserved");
        }finally{config.enhancedInfusionEnchanting.set(old);}
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void enhancedEnchantingConsumesEssentiaAndSaltWithoutExperience(GameTestHelper test) {
        var config=TCConfig.SERVER;boolean old=config.enhancedInfusionEnchanting.get();
        try {
            config.enhancedInfusionEnchanting.set(true);
            var json=com.google.gson.JsonParser.parseString("""
                {"instability":1,"enchantment":"minecraft:sharpness","components":[{"ingredient":{"item":"minecraft:diamond"}}],"aspects":{"praecantatio":4}}
                """).getAsJsonObject();
            var recipe=new InfusionRecipe(ResourceLocation.parse("thaumcraft:refactor_test"),json);
            var sword=new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
            var salt=new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:salis_mundus")));
            var seed=new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:void_seed")));
            var ingredients=new java.util.ArrayList<ItemStack>();ingredients.add(new ItemStack(net.minecraft.world.item.Items.DIAMOND));
            for(int i=0;i<5;i++)ingredients.add(salt.copy());
            var input=new InfusionRecipe.Input(sword,ingredients,key->true);
            test.assertTrue(recipe.matches(input,test.getLevel()),"Addon recipe without salt accepts five inserted salts");
            var plan=recipe.plan(input);
            test.assertTrue(plan.xp()==0 && plan.aspects().amount(tc4revival.aspect.TCAspects.catalog().require("praecantatio"))==20,"Five times essentia and no XP");
            var upgraded=InfusionOutput.load(plan.output().save()).apply(sword);
            test.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,upgraded)==5,"Saved output retains five upgrades");
            ingredients.add(salt.copy());
            test.assertTrue(!recipe.matches(new InfusionRecipe.Input(sword,ingredients,key->true),test.getLevel()),"Overlevel requires seed");
            ingredients.add(seed);
            var over=new InfusionRecipe.Input(sword,ingredients,key->true);
            test.assertTrue(recipe.matches(over,test.getLevel()),"Vanilla max plus one with seed");
            test.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,recipe.assemble(over,test.getLevel().registryAccess()))==6,"Sharpness VI output");
            ingredients.add(salt.copy());
            test.assertTrue(!recipe.matches(new InfusionRecipe.Input(sword,ingredients,key->true),test.getLevel()),"Cannot exceed max plus one");
            config.enhancedInfusionEnchanting.set(false);
            test.assertTrue(recipe.plan(sword).xp()>0 && recipe.plan(sword).components().size()==1,"Disabled mode retains old XP and ingredients");
        }finally{config.enhancedInfusionEnchanting.set(old);}
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void warpingWeaponsOnlyContributeFromEffectiveHand(GameTestHelper test) {
        int tools=0;
        for(var item:ForgeRegistries.ITEMS)if(item instanceof WarpingGear gear) {
            var stack=new ItemStack(item);
            if(gear.warp(stack,null)<=0)continue;
            if(item instanceof TieredItem||item instanceof ProjectileWeaponItem||item instanceof TridentItem||item instanceof ShearsItem) {
                tools++;
                test.assertTrue(!stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(TCAttributes.WARP.get()).isEmpty(),"Active hand contributes warp: "+ForgeRegistries.ITEMS.getKey(item));
                test.assertTrue(stack.getAttributeModifiers(EquipmentSlot.OFFHAND).get(TCAttributes.WARP.get()).isEmpty(),"Offhand does not contribute weapon/tool warp: "+ForgeRegistries.ITEMS.getKey(item));
            }
        }
        test.assertTrue(tools>=3,"Actual registered void/crimson tools were checked");
        var stone=new ItemStack(TCItems.SINISTER_STONE.get());
        test.assertTrue(!stone.getAttributeModifiers(EquipmentSlot.OFFHAND).get(TCAttributes.WARP.get()).isEmpty(),"Non-tool held sinister stone keeps its old behavior");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void dimensionRatesReloadAndZeroSkipsGeneration(GameTestHelper test) {
        var config=TCConfig.SERVER;var previous=config.nodeDimensionMultipliers.get();
        try {
            config.nodeDimensionMultipliers.set(List.of("twilightforest:twilight_forest=2","minecraft:overworld=0"));
            test.assertTrue(config.nodeMultiplier(ResourceLocation.parse("twilightforest:twilight_forest"))==2,"Default Twilight rate");
            test.assertTrue(config.nodeMultiplier(ResourceLocation.parse("custom:world"))==1,"Unknown dimension keeps default generation");
            var chunk=new ChunkPos(test.absolutePos(net.minecraft.core.BlockPos.ZERO));
            test.assertTrue(!NaturalAuraStructures.wildNode(test.getLevel(),chunk,net.minecraft.util.RandomSource.create(1),false),"Disabled dimension does not place wild nodes");
            config.nodeDimensionMultipliers.set(List.of("minecraft:overworld=3"));
            test.assertTrue(config.nodeMultiplier(Level.OVERWORLD.location())==3&&NaturalAuraStructures.allowsWildNodes(Level.OVERWORLD,chunk),"Config reload invalidates parsed rules immediately");
        }finally{config.nodeDimensionMultipliers.set(previous);}
        test.succeed();
    }
}
