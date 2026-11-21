package ttmodern;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class EnchanterServerTests {
    private static final int[][] COLUMNS={{-4,-4},{-4,0},{-4,4},{4,-4},{4,0},{4,4}};
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper test,BlockPos pos){var p=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTEnchanter"));p.moveTo(pos.getCenter());return p;}
    private static EnchanterEntity setup(GameTestHelper test,BlockPos pos){test.getLevel().setBlock(pos,Tinkerer.ENCHANTER.get().defaultBlockState(),3);for(var c:COLUMNS){var base=pos.offset(c[0],0,c[1]);test.getLevel().setBlock(base,TCArcane.OBSIDIAN_TOTEM.get().defaultBlockState(),3);test.getLevel().setBlock(base.above(),TCArcane.OBSIDIAN_TOTEM.get().defaultBlockState(),3);test.getLevel().setBlock(base.above(2),TCArcane.NITOR.get().defaultBlockState(),3);}return (EnchanterEntity)test.getLevel().getBlockEntity(pos);}
    private static void cleanup(GameTestHelper test,BlockPos pos){test.getLevel().removeBlockEntity(pos);test.getLevel().removeBlock(pos,false);for(var c:COLUMNS)for(int y=0;y<3;y++)test.getLevel().removeBlock(pos.offset(c[0],y,c[1]),false);}
    private static ItemStack wand(){return TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true);}
    @GameTest(template="wand_test_empty")
    public static void tinkererEnchanterRequiresLegacyTotemsNotTiles(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(2,2,2));var entity=setup(test,pos);var player=player(test,pos);
        entity.items.setStackInSlot(0,new ItemStack(Items.DIAMOND_PICKAXE));entity.items.setStackInSlot(1,wand());
        try{
            test.assertTrue(entity.pillars().size()==6,"Legacy obsidian totems form six valid pillars");
            for(var c:COLUMNS)for(int y=0;y<2;y++)test.getLevel().setBlock(pos.offset(c[0],y,c[1]),TCEldritch.OBSIDIAN_TILE.get().defaultBlockState(),3);
            test.assertTrue(entity.choose(player,ResourceLocation.parse("minecraft:efficiency"),1),"Select valid enchantment");
            test.assertTrue(entity.pillars().isEmpty()&&!entity.start(player),"Obsidian tiles are not legacy pillar blocks and cannot start work");
            for(var c:COLUMNS)for(int y=0;y<2;y++)test.getLevel().setBlock(pos.offset(c[0],y,c[1]),TCArcane.OBSIDIAN_TOTEM.get().defaultBlockState(),3);
            test.assertTrue(entity.start(player),"Replacing tiles with totems enables enchanting");
            for(int i=0;i<1000&&entity.working();i++)entity.tick();
            test.assertTrue(!entity.working()&&EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY,entity.items.getStackInSlot(0))==1,"Totem structure completes enchanting");
            test.succeed();
        }finally{cleanup(test,pos);}
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererEnchanterTransactionSaveResumeAndPayment(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(2,2,2));var entity=setup(test,pos);var player=player(test,pos);var tool=new ItemStack(Items.DIAMOND_PICKAXE);tool.setHoverName(net.minecraft.network.chat.Component.literal("Preserved"));tool.setDamageValue(7);entity.items.setStackInSlot(0,tool);entity.items.setStackInSlot(1,wand());
        try{
            test.assertTrue(entity.pillars().size()==6&&entity.choose(player,ResourceLocation.parse("minecraft:unbreaking"),1)&&entity.start(player),"Six pillars and valid selection start work");
            var costs=java.util.Map.copyOf(entity.costs());for(int i=0;i<10;i++)entity.tick();int progress=entity.paid().values().stream().mapToInt(Integer::intValue).sum();test.assertTrue(progress==10,"One vis unit per tick");
            var restored=new EnchanterEntity(pos,entity.getBlockState());restored.load(entity.saveWithoutMetadata());test.getLevel().removeBlockEntity(pos);test.getLevel().setBlockEntity(restored);entity=restored;
            test.assertTrue(entity.working()&&entity.paid().values().stream().mapToInt(Integer::intValue).sum()==10,"Working transaction resumes saved payment");
            for(int i=0;i<1000&&entity.working();i++)entity.tick();var result=entity.items.getStackInSlot(0);
            test.assertTrue(!entity.working()&&EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING,result)==1&&result.getDamageValue()==7&&result.getHoverName().getString().equals("Preserved"),"Enchant once without losing damage/name");
            var charged=entity.items.getStackInSlot(1);for(var cost:costs.entrySet())test.assertTrue(charged.getTag().getInt(cost.getKey())==10000-cost.getValue()*100,"Exact saved transaction payment: "+cost.getKey());
            test.assertTrue(!entity.choose(player,ResourceLocation.parse("minecraft:efficiency"),1),"Already enchanted tool must first use spell cloth");test.succeed();
        }finally{cleanup(test,pos);}
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererEnchanterResearchPermissionsPillarsAndMenu(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(2,2,2));var entity=setup(test,pos);var player=player(test,pos);entity.items.setStackInSlot(0,new ItemStack(Items.DIAMOND_LEGGINGS));entity.items.setStackInSlot(1,wand());var id=ResourceLocation.parse(Tinkerer.ID+":ascent_boost");
        try{
            test.assertTrue(!entity.choose(player,id,1),"Unknown research cannot select exclusive enchantment");thaumcraft.api.ThaumcraftApiHelper.completeResearch(player,Tinkerer.ID+":TTENCH_ASCENT_BOOST");
            test.assertTrue(!entity.choose(player,id,5)&&entity.choose(player,id,1),"Rank validated against maximum");
            var menu=new EnchanterMenu(1,player.getInventory(),entity,EnchanterMenu.options(player));test.assertTrue(menu.clickMenuButton(player,0),"Validated menu starts operation");
            test.assertTrue(!menu.getSlot(0).mayPickup(player)&&menu.quickMoveStack(player,0).isEmpty(),"Working tool cannot be withdrawn");entity.tick();test.assertTrue(!entity.paid().isEmpty(),"Payment started");
            test.getLevel().removeBlock(pos.offset(-4,2,-4),false);entity.tick();test.assertTrue(!entity.working()&&entity.paid().isEmpty()&&menu.getSlot(0).mayPickup(player),"Missing pillar cancels and clears partial payment");
            test.assertTrue(!entity.start(player)&&"ttmodern.enchanter.pillars".equals(entity.startFailure()),"Five pillars cannot restart and expose a specific failure reason");player.moveTo(pos.getCenter().add(20,0,0));test.assertTrue(!menu.clickMenuButton(player,1)&&!entity.choose(player,id,2),"Distance checked for menu and selection");
            test.assertTrue(!entity.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).isPresent(),"Legacy machine exposes no hopper automation");
            test.assertTrue(tc4revival.research.ResearchDefinitions.server().pages().require(Tinkerer.ID+":ENCHANTER").size()==4&&tc4revival.research.ResearchDefinitions.server().pages().require(Tinkerer.ID+":SPELL_CLOTH").size()==2,"Complete obtaining research pages");test.succeed();
        }finally{cleanup(test,pos);}
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererSpellClothCraftIsPureAndExhausts(GameTestHelper test){
        var player=player(test,test.absolutePos(new BlockPos(2,2,2)));var menu=new CraftingMenu(1,player.getInventory());var grid=new TransientCraftingContainer(menu,2,2);var cloth=new ItemStack(Tinkerer.SPELL_CLOTH.get());var tool=new ItemStack(Items.DIAMOND_SWORD);tool.enchant(Enchantments.SHARPNESS,3);tool.setDamageValue(12);tool.setHoverName(net.minecraft.network.chat.Component.literal("Named"));grid.setItem(0,cloth);grid.setItem(1,tool);
        var recipe=test.getLevel().getRecipeManager().byKey(ResourceLocation.parse(Tinkerer.ID+":spell_cloth_disenchant")).orElseThrow();var clean=(SpellClothRecipe)recipe;
        test.assertTrue(clean.matches(grid,test.getLevel()),"Two-item disenchantment recipe matches");var output=clean.assemble(grid,test.getLevel().registryAccess());clean.assemble(grid,test.getLevel().registryAccess());
        test.assertTrue(!output.isEnchanted()&&output.getDamageValue()==12&&output.getHoverName().getString().equals("Named")&&tool.isEnchanted()&&cloth.getDamageValue()==0,"Repeated preview neither mutates inputs nor loses unrelated NBT");
        var remaining=clean.getRemainingItems(grid);test.assertTrue(remaining.get(0).getDamageValue()==1&&remaining.get(1).isEmpty(),"Only damaged cloth remains, never a duplicate tool");
        cloth.setDamageValue(34);test.assertTrue(clean.getRemainingItems(grid).get(0).isEmpty(),"Cloth consumed on 35th use");grid.setItem(2,new ItemStack(Items.STICK));test.assertTrue(!clean.matches(grid,test.getLevel()),"Extra ingredient rejected");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererEnchanterCompatibilityAndCompoundCosts(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(2,2,2));var entity=setup(test,pos);var player=player(test,pos);
        try{
            entity.items.setStackInSlot(0,new ItemStack(Items.DIAMOND_SWORD));
            boolean sharpnessChosen=entity.choose(player,ResourceLocation.parse("minecraft:sharpness"),1);
            boolean smiteChosen=entity.choose(player,ResourceLocation.parse("minecraft:smite"),1);
            boolean nativeCompatible=Enchantments.SMITE.isCompatibleWith(Enchantments.SHARPNESS);
            test.assertTrue(sharpnessChosen&&smiteChosen==nativeCompatible,"Selection respects loaded enchantment compatibility: sharpness="+sharpnessChosen+" smite="+smiteChosen+" nativeCompatible="+nativeCompatible+" selected="+entity.selected());
            test.assertTrue(!entity.choose(player,ResourceLocation.parse("minecraft:lure"),1),"Fishing enchantment cannot be selected for a sword");
            entity.items.setStackInSlot(0,new ItemStack(Items.DIAMOND_PICKAXE));
            test.assertTrue(entity.choose(player,ResourceLocation.parse("minecraft:silk_touch"),1),"Pickaxe accepts silk touch");
            test.assertTrue(entity.choose(player,ResourceLocation.parse("minecraft:fortune"),1)==Enchantments.BLOCK_FORTUNE.isCompatibleWith(Enchantments.SILK_TOUCH),"Fortune selection obeys loaded silk-touch conflict");
            entity.items.setStackInSlot(0,new ItemStack(Items.FISHING_ROD));test.assertTrue(entity.selected().isEmpty()&&entity.choose(player,ResourceLocation.parse("minecraft:lure"),1),"Changing tool clears old selections");
            test.assertTrue(!entity.costs().containsKey("bestia")&&entity.costs().get("aqua")>24,"Compound beast cost decomposed into payable primals instead of silently free");test.succeed();
        }finally{cleanup(test,pos);}
    }
}
