package tc4revival.compat.jei;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import tc4revival.client.*;
import tc4revival.content.*;
import tc4revival.network.*;
import tc4revival.research.BrowserRecipe;

public class JeiResearchLink {
    private static final ResourceLocation BOOK=ResourceLocation.parse("thaumcraft:textures/items/thaumonomicon.png");
    public static ItemStack categoryIcon(String kind) {
        return switch(kind) {
            case "arcane" -> new ItemStack(TCItems.ARCANE_WORKBENCH.get());
            case "crucible" -> new ItemStack(TCArcane.CRUCIBLE.get());
            case "infusion", "infusion_enchantment" -> new ItemStack(TCArcane.INFUSION_MATRIX.get());
            default -> new ItemStack(TCItems.THAUMOMETER.get());
        };
    }
    public static boolean complete(String key) { return key.isEmpty()||ClientKnowledge.snapshot().map(k->k.researchMarkers().contains(key)).orElse(false); }
    public static void draw(GuiGraphics g,BrowserRecipe recipe) {
        if(recipe.research().isEmpty())return;
        var pos=JeiRecipeLayout.book(recipe.kind());
        LegacyGui.blit(g,BOOK,pos.x(),pos.y(),0,0,18,18,18,18);
        if(!complete(recipe.research()))g.renderItem(new ItemStack(Items.BARRIER),pos.x()+1,pos.y()+1);
    }
    public static List<Component> tooltip(BrowserRecipe recipe) {
        var lines=new ArrayList<Component>();
        if(!recipe.research().isEmpty())lines.add(Component.translatable("thaumcraft.jei.research",Component.translatable("tc.research_name."+recipe.research())));
        lines.addAll(details(recipe));
        lines.add(Component.translatable("thaumcraft.jei.open_research"));
        JeiResearch.modNameForResearch(recipe.research()).ifPresent(lines::add);
        return lines;
    }
    public static List<Component> details(BrowserRecipe recipe) {
        var lines=new ArrayList<Component>();
        if(JeiRecipeLayout.infusion(recipe.kind())) {
            lines.add(Component.translatable("thaumcraft.jei.stability",recipe.instability()));
            if(recipe.xp()>0)lines.add(Component.translatable("thaumcraft.jei.experience",recipe.xp()));
            if(recipe.dynamic())lines.add(Component.translatable("thaumcraft.jei.dynamic"));
        }
        if(recipe.kind().equals("infusion_enchantment"))lines.add(Component.translatable("thaumcraft.jei.enchantment_reference"));
        return lines;
    }
    public static boolean open(BrowserRecipe recipe) {
        return open(recipe.research());
    }
    public static boolean open(String research) {
        var mc=Minecraft.getInstance();if(mc.player==null||research.isEmpty())return false;
        for(int i=0;i<mc.player.getInventory().getContainerSize();i++)if(mc.player.getInventory().getItem(i).getItem() instanceof ThaumonomiconItem) {
            TCNetwork.CHANNEL.sendToServer(new BookRecipeRequest(research));return true;
        }
        mc.player.displayClientMessage(Component.translatable("thaumcraft.jei.book_required"),true);return true;
    }
    public static List<ItemStack> enchantmentBooks(BrowserRecipe recipe) {
        if(!recipe.kind().equals("infusion"))return List.of();
        var before=recipe.inputs().isEmpty()||recipe.inputs().get(0).isEmpty()?Map.<Enchantment,Integer>of():EnchantmentHelper.getEnchantments(recipe.inputs().get(0).get(0));
        return EnchantmentHelper.getEnchantments(recipe.output()).entrySet().stream().filter(e->e.getValue()>before.getOrDefault(e.getKey(),0))
                .map(e->EnchantedBookItem.createForEnchantment(new EnchantmentInstance(e.getKey(),e.getValue()))).toList();
    }
}
