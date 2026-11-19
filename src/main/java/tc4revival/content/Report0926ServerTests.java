package tc4revival.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class Report0926ServerTests {
    @GameTest(template="wand_test_empty")
    public static void arcaneRecipeWinsConflictWithoutVanillaFallbackOrWrongPayment(GameTestHelper t) {
        var level=t.getLevel();var pos=t.absolutePos(new BlockPos(2,2,2));var old=level.getBlockState(pos);
        var manager=level.getRecipeManager();var recipes=new ArrayList<Recipe<?>>(manager.getRecipes());
        var log=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:greatwood_log"));
        var rod=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:wand_rod_greatwood"));
        var conflict=new ShapelessRecipe(ResourceLocation.parse("thaumcraft:report_conflict"),"",CraftingBookCategory.MISC,new ItemStack(Items.PAPER,32),NonNullList.of(Ingredient.EMPTY,Ingredient.of(log),Ingredient.of(log)));
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ArcanePriority"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.moveTo(pos.getCenter());
        thaumcraft.api.ThaumcraftApiHelper.completeResearch(player,"ROD_greatwood");
        try {
            var augmented=new ArrayList<>(recipes);augmented.add(conflict);manager.replaceRecipes(augmented);
            level.setBlock(pos,TCArcane.BLOCK.get().defaultBlockState(),3);
            var table=(ArcaneWorkbenchBlockEntity)level.getBlockEntity(pos);
            table.setItem(1,new ItemStack(log,2));table.setItem(3,new ItemStack(log,2));
            var menu=new ArcaneWorkbenchMenu(1,player.getInventory(),table);player.containerMenu=menu;
            menu.refresh();t.assertTrue(menu.getSlot(0).getItem().isEmpty(),"Matching arcane recipe without vis cannot fall back to free paper");
            var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true);
            table.setItem(10,wand);menu.refresh();int before=wand.getTag().getInt("perditio");
            t.assertTrue(menu.getSlot(0).getItem().is(rod),"Arcane output takes priority over conflicting ordinary recipe");
            menu.clicked(0,0,ClickType.PICKUP,player);
            t.assertTrue(menu.getCarried().is(rod)&&menu.getCarried().getCount()==1,"Take produces exactly the arcane result");
            t.assertTrue(before-wand.getTag().getInt("perditio")==300,"Taken arcane result costs exactly three vis");
            t.assertTrue(table.getItem(1).getCount()==1&&table.getItem(3).getCount()==1,"One of each ingredient consumed");
            table.setItem(1,ItemStack.EMPTY);table.setItem(3,ItemStack.EMPTY);table.setItem(0,new ItemStack(Items.OAK_LOG));menu.refresh();
            t.assertTrue(menu.getSlot(0).getItem().is(Items.OAK_PLANKS),"Unrelated vanilla crafting still works");
            int charge=wand.getTag().getInt("perditio");menu.setCarried(ItemStack.EMPTY);menu.clicked(0,0,ClickType.PICKUP,player);
            t.assertTrue(wand.getTag().getInt("perditio")==charge,"Vanilla output never consumes vis");t.succeed();
        } finally {manager.replaceRecipes(recipes);level.setBlock(pos,old,3);player.containerMenu=player.inventoryMenu;}
    }
}
