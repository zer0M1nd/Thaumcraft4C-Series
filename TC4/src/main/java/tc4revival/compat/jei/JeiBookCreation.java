package tc4revival.compat.jei;

import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.*;

/** Item slots keep JEI's normal click and key navigation on the introductory information page. */
public class JeiBookCreation implements IRecipeCategory<JeiBookCreation.Page> {
    public record Page() {}
    public static final RecipeType<Page> TYPE = RecipeType.create("thaumcraft", "book_creation", Page.class);
    private final IDrawable background, icon;
    public JeiBookCreation(IGuiHelper gui) {
        background = gui.createBlankDrawable(174, 98);
        icon = gui.createDrawableItemStack(new ItemStack(TCItems.THAUMONOMICON.get()));
    }
    public RecipeType<Page> getRecipeType() { return TYPE; }
    public Component getTitle() { return Component.translatable("thaumcraft.jei.book_creation_title"); }
    public IDrawable getBackground() { return background; }
    public IDrawable getIcon() { return icon; }
    public void setRecipe(IRecipeLayoutBuilder builder, Page page, IFocusGroup focus) {
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(new ItemStack(TCItems.THAUMONOMICON.get()));
        var cap = new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:wand_cap_iron")));
        var wand = TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD, WandParts.Cap.IRON, false, false);
        var items = List.of(cap, wand, new ItemStack(Items.BOOKSHELF));
        for (int i = 0; i < items.size(); i++) builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 31 + 48 * i, 66).addItemStack(items.get(i));
    }
    public void draw(Page page, IRecipeSlotsView slots, GuiGraphics graphics, double mx, double my) {
        var font = Minecraft.getInstance().font;
        var lines = font.split(Component.translatable("thaumcraft.jei.book_creation"), 162);
        for (int i = 0; i < lines.size(); i++) graphics.drawString(font, lines.get(i), 6, 8 + i * 11, 0x333333, false);
    }
}
