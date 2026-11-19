package tc4revival.compat.jei;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import tc4revival.client.ClientKnowledge;
import tc4revival.client.GuiStatusIcon;
import tc4revival.research.ResearchRequirements;

public class JeiResearchCategory implements IRecipeCategory<JeiResearchRecipe> {
    public static final int WIDTH = 166, HEIGHT = 158, ROW_Y = 39, ROW_HEIGHT = 11, CAPACITY = 10;
    private final IDrawable background, icon;
    public JeiResearchCategory(IGuiHelper gui) {
        background = gui.createBlankDrawable(WIDTH, HEIGHT);
        icon = gui.createDrawableItemStack(new ItemStack(tc4revival.content.TCItems.THAUMONOMICON.get()));
    }
    public RecipeType<JeiResearchRecipe> getRecipeType() { return JeiResearch.RECIPES; }
    public Component getTitle() { return Component.translatable("thaumcraft.jei.research_category"); }
    public IDrawable getBackground() { return background; }
    public IDrawable getIcon() { return icon; }
    public void setRecipe(IRecipeLayoutBuilder builder, JeiResearchRecipe recipe, IFocusGroup focus) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, WIDTH / 2 - 8, 3).addIngredient(JeiResearch.TYPE, new JeiResearch.Value(recipe.research().key()));
        var inputs = builder.addInvisibleIngredients(RecipeIngredientRole.INPUT);
        for (var row : recipe.requirements()) {
            if (!recipe.revealed(row)) continue;
            switch (row.requirement().kind()) {
                case RESEARCH -> inputs.addIngredient(JeiResearch.TYPE, JeiResearch.value(row.requirement().key()));
                case ITEM -> inputs.addItemStacks(row.items());
                case ASPECT -> inputs.addIngredient(ThaumcraftJei.ASPECTS, new ThaumcraftJei.AspectValue(row.requirement().key(), 1));
                case ENTITY, WARP, SPECIAL -> { }
            }
        }
    }
    public ResearchRequirements.Page<tc4revival.research.ResearchRequirementData.Entry> page(JeiResearchRecipe recipe) {
        var knowledge = ClientKnowledge.snapshot().orElse(null);
        return ResearchRequirements.page(recipe.requirements(), entry -> entry.complete(knowledge), CAPACITY);
    }
    private String fit(String text, int width) {
        var font = Minecraft.getInstance().font;
        return font.width(text) <= width ? text : font.plainSubstrByWidth(text, width - font.width("…")) + "…";
    }
    public void draw(JeiResearchRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        var title = fit(JeiResearch.name(new JeiResearch.Value(recipe.research().key())).getString(), WIDTH - 8);
        graphics.drawString(font, title, (WIDTH - font.width(title)) / 2, 23, 0x333333, false);
        graphics.fill(4, 34, WIDTH - 4, 35, 0xffb7b7b7);
        var page = page(recipe);
        for (int i = 0; i < page.rows().size(); i++) {
            var row = page.rows().get(i);
            String text = fit((recipe.revealed(row.value()) ? recipe.label(row.value()).getString() : "？？？"), WIDTH - 8 - 12);
            int y = ROW_Y + i * ROW_HEIGHT;
            if (mouseX >= 4 && mouseX < WIDTH - 4 && mouseY >= y && mouseY < y + ROW_HEIGHT)
                graphics.fill(3, y - 1, WIDTH - 3, y + ROW_HEIGHT - 1, 0x20808080);
            graphics.drawString(font, text, 4, y, row.complete() ? 0x168126 : 0x333333, false);
            int statusX = 4 + font.width(text) + 3;
            var status = row.complete() ? GuiStatusIcon.CHECK : GuiStatusIcon.CROSS;
            status.draw(graphics, statusX, y + (8 - status.height()));
        }
        if (page.truncated()) {
            var summary = Component.translatable("thaumcraft.jei.requirements_omitted", page.omittedComplete(), page.omittedIncomplete());
            // Keep both counters readable even in languages with a longer translation.
            float scale = Math.min(1, (WIDTH - 8f) / Math.max(1, font.width(summary)));
            graphics.pose().pushPose();
            graphics.pose().translate(4, ROW_Y + page.rows().size() * ROW_HEIGHT, 0);
            graphics.pose().scale(scale, scale, 1);
            graphics.drawString(font, summary, 0, 0, 0x666666, false);
            graphics.pose().popPose();
        } else if (page.rows().isEmpty()) graphics.drawString(font, Component.translatable("thaumcraft.jei.no_requirements"), 4, ROW_Y, 0x666666, false);
    }
    private ResearchRequirements.Row<tc4revival.research.ResearchRequirementData.Entry> hit(JeiResearchRecipe recipe, double x, double y) {
        if (x < 4 || x >= WIDTH - 4 || y < ROW_Y) return null;
        var rows = page(recipe).rows();
        int index = (int)((y - ROW_Y) / ROW_HEIGHT);
        return index >= 0 && index < rows.size() ? rows.get(index) : null;
    }
    @Override public List<Component> getTooltipStrings(JeiResearchRecipe recipe, IRecipeSlotsView slots, double x, double y) {
        var row = hit(recipe, x, y);
        if (row == null || !recipe.revealed(row.value())) return List.of();
        var lines = new java.util.ArrayList<Component>();
        lines.add(recipe.label(row.value()));
        if (row.value().requirement().kind() == ResearchRequirements.Kind.RESEARCH)
            JeiResearch.modNameForResearch(row.value().requirement().key()).ifPresent(lines::add);
        return lines;
    }
    @Override public void getTooltip(ITooltipBuilder tooltip, JeiResearchRecipe recipe, IRecipeSlotsView slots, double x, double y) {
        var row = hit(recipe, x, y);
        if (row == null || !recipe.revealed(row.value())) return;
        if (row.value().requirement().kind() == ResearchRequirements.Kind.RESEARCH) {
            if (recipe.depth(row.value()) > 1) tooltip.add(recipe.label(row.value()));
            new JeiResearch.Renderer().getTooltip(tooltip, JeiResearch.value(row.value().requirement().key()),
                    net.minecraft.world.item.TooltipFlag.NORMAL);
            JeiResearch.modNameForResearch(row.value().requirement().key()).ifPresent(tooltip::add);
        } else tooltip.addAll(getTooltipStrings(recipe, slots, x, y));
    }
    @Override public boolean handleInput(JeiResearchRecipe recipe, double x, double y, InputConstants.Key key) {
        var row = hit(recipe, x, y);
        if (row == null || !recipe.revealed(row.value())) return false;
        if(key.getType()==InputConstants.Type.MOUSE&&key.getValue()==0&&net.minecraft.client.gui.screens.Screen.hasControlDown()
                &&row.value().requirement().kind()==ResearchRequirements.Kind.RESEARCH)
            return JeiResearchLink.open(row.value().requirement().key());
        var role = JeiResearch.keyRole(key);
        if (key.getType() == InputConstants.Type.MOUSE && key.getValue() == 0) role = java.util.Optional.of(RecipeIngredientRole.OUTPUT);
        if (role.isEmpty()) return false;
        var entry = row.value();
        return switch (entry.requirement().kind()) {
            case RESEARCH -> JeiResearch.show(JeiResearch.TYPE, JeiResearch.value(entry.requirement().key()), role.get());
            case ITEM -> !entry.items().isEmpty() && JeiResearch.show(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, entry.items().get(0), role.get());
            case ASPECT -> JeiResearch.show(ThaumcraftJei.ASPECTS, new ThaumcraftJei.AspectValue(entry.requirement().key(), 1), role.get());
            case ENTITY, WARP, SPECIAL -> false;
        };
    }
}
