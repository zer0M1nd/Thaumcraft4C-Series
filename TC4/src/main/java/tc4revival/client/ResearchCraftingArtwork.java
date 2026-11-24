package tc4revival.client;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import tc4revival.research.*;

/** Crafting, smelting, crucible and infusion artwork. Other page types require their own renderer. */
public  class ResearchCraftingArtwork {
    private static final ResourceLocation OVERLAY = ResourceLocation.parse("thaumcraft:textures/gui/gui_researchbook_overlay.png");
    private ResearchCraftingArtwork() {}
    public static boolean draw(ResearchReaderScreen reader, GuiGraphics g, ResearchPage page, int side, int x, int y, int mx, int my) {
        int start = x + side * 152; long time = reader.cycleMillis();
        if (LiveBookRecipes.supported(page.type())) return live(reader,g,page,start,y,mx,my,time);
        if (page.type() == ResearchPage.Type.COMPOUND_CRAFTING) {
            var chosen = select(page.recipe(), time);
            if (!(chosen instanceof ResearchPage.Reference reference)
                    || !(ResearchPageRecipes.bundled().require(reference.displayKey(time)).definition()
                    instanceof ResearchPageRecipes.Compound structure))
                throw new IllegalArgumentException("Missing compound page definition");
            label(g, "recipe.type.construct", start + 56, y);
            var costs = structure.aspects().entrySet().stream()
                    .sorted(java.util.Map.Entry.<String, Integer>comparingByValue().reversed()).toList();
            if (!costs.isEmpty()) {
                g.setColor(1, 1, 1, .4f);
                overlay(g, start, y + 174, 0, 0, 68, 76, 12, 12);
                g.setColor(1, 1, 1, 1);
                for (int i = 0; i < costs.size(); i++) aspect(reader, g, costs.get(i).getKey(), costs.get(i).getValue(),
                        start + 14 + 18 * i + (5 - costs.size()) * 8, y + 182, mx, my);
            }
            var layout = ResearchCompoundLayout.of(structure);
            g.pose().pushPose();
            try {
                g.pose().translate(start + layout.x(), y + layout.y(), 0);
                g.pose().scale(layout.scale(), layout.scale(), layout.scale());
                g.setColor(1, 1, 1, .5f);
                overlay(g, layout.baseX(), layout.baseY(), 0, 0, 0, 72, 64, 44);
                g.setColor(1, 1, 1, 1);
                for (var cell : layout.cells()) {
                    var selected = select(structure.blocks().elements().get(cell.index()), time);
                    var special = selected instanceof ResearchPage.Item ingredient ? ResearchStructureDisplay.of(ingredient.stack()) : Optional.<ResearchStructureDisplay>empty();
                    if (special.isPresent()) {
                        g.pose().pushPose();
                        try {
                            g.pose().translate(0, 0, cell.depth());
                            ResearchStructureArtwork.draw(g, special.get(), cell.x(), cell.y());
                        } finally { g.pose().popPose(); }
                        int hitX=(int)(start+layout.x()+cell.x()*layout.scale());
                        int hitY=(int)(y+layout.y()+cell.y()*layout.scale());
                        int size=(int)Math.ceil(16*layout.scale());
                        if (mx>=hitX && mx<hitX+size && my>=hitY && my<hitY+size)
                            reader.structureTooltip(g,Component.translatable(special.get().translation()),mx,my);
                        continue;
                    }
                    var resolved = selected instanceof ResearchPage.Item ingredient ? ResearchDisplayItems.bundled().resolve(ingredient.stack(), time)
                            : ResearchStructureDisplay.literal(selected, time);
                    if (resolved.isEmpty()) continue;
                    var stack = resolved.get(); stack.setCount(1);
                    g.pose().pushPose();
                    try {
                        g.pose().translate(0, 0, cell.depth());
                        g.renderItem(stack, cell.x(), cell.y());
                        g.renderItemDecorations(Minecraft.getInstance().font, stack, cell.x(), cell.y());
                    } finally { g.pose().popPose(); }
                    int hitX = (int) (start + layout.x() + cell.x() * layout.scale());
                    int hitY = (int) (y + layout.y() + cell.y() * layout.scale());
                    int size = (int) Math.ceil(16 * layout.scale());
                    if (mx >= hitX && mx < hitX + size && my >= hitY && my < hitY + size)
                        reader.ingredientTooltip(g, stack, hitX, hitY, size, mx, my);
                }
            } finally { g.pose().popPose(); g.setColor(1, 1, 1, 1); }
            return true;
        }
        return false;
    }
    private static boolean live(ResearchReaderScreen reader,GuiGraphics g,ResearchPage page,int start,int y,int mx,int my,long time) {
        var rows=ClientLiveBookRecipes.get(page);
        if(rows.isEmpty()) { centered(g,Component.translatable("thaumcraft.research.recipe_unavailable"),start+56,y+60,0x505050);return true; }
        // Cycle recipes slower than ingredient variants, so every small tag is visible in a branch.
        var row=rows.get((int)Math.floorMod(time/5000,rows.size()));
        var type=row.type();var inputs=row.inputs();
        boolean arcane=type==ResearchPage.Type.ARCANE_CRAFTING;
        if(arcane||type==ResearchPage.Type.NORMAL_CRAFTING) {
            overlay(g,start,y,2,arcane?27:32,arcane?112:60,15,52,52);
            overlay(g,start,y,20,arcane?7:12,20,3,16,16);
            label(g,arcane?"recipe.type.arcane":row.width()==0?"recipe.type.workbenchshapeless":"recipe.type.workbench",start+56,y);
            liveItem(reader,g,List.of(row.output()),start+48,y+(arcane?22:32),mx,my,time,false);
            int width=row.width()==0?3:row.width();
            for(int i=0;i<Math.min(9,inputs.size());i++)liveItem(reader,g,inputs.get(i),start+16+i%width*32,y+(arcane?66:76)+i/width*32,mx,my,time,true);
            if(arcane)for(var glyph:ResearchAspectLayout.grid(row.aspects(),6,start+4,y+172))aspect(reader,g,glyph.aspect(),tc4revival.content.ArcaneRecipe.formatHundredths(glyph.amount()),glyph.x(),glyph.y(),mx,my);
        } else if(type==ResearchPage.Type.CRUCIBLE_CRAFTING) {
            label(g,"recipe.type.crucible",start+56,y);overlay(g,start,y+28,0,0,0,3,56,17);overlay(g,start,y+92,0,0,0,20,56,48);overlay(g,start+42,y+76,0,0,100,84,11,13);
            liveItem(reader,g,List.of(row.output()),start+48,y+36,mx,my,time,false);
            liveItem(reader,g,inputs.get(0),start+26,y+72,mx,my,time,true);
            for(var glyph:ResearchAspectLayout.grid(row.aspects(),3,start+28,y+128))aspect(reader,g,glyph.aspect(),glyph.amount(),glyph.x(),glyph.y(),mx,my);
        } else if(type==ResearchPage.Type.SMELTING) {
            label(g,"recipe.type.smelting",start+56,y);overlay(g,start,y+28,0,0,0,192,56,64);
            liveItem(reader,g,inputs.get(0),start+48,y+64,mx,my,time,true);liveItem(reader,g,List.of(row.output()),start+48,y+144,mx,my,time,false);
        } else {
            boolean enchant=type==ResearchPage.Type.INFUSION_ENCHANTMENT;
            label(g,enchant?"recipe.type.infusionenchantment":"recipe.type.infusion",start+56,y);
            instability(g,row.instability(),start+56,reader.recipeFooterY());overlay(g,start,y+58,0,0,200,77,60,44);
            if(enchant) {
                var enchantments=net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(row.output());
                var name=enchantments.isEmpty()?row.output().getHoverName():Component.translatable(enchantments.keySet().iterator().next().getDescriptionId());
                centered(g,name,start+56,y+20,7360656);centered(g,Component.literal(row.caption()),start+56,y+34,7360656);
                if(row.xp()>0)centered(g,Component.literal(row.xp()+" levels"),start+56,y+46,0x508850);
            } else {
                overlay(g,start,y+20,0,0,0,3,56,17);liveItem(reader,g,List.of(row.output()),start+48,y+28,mx,my,time,false);
                liveItem(reader,g,inputs.get(0),start+48,y+94,mx,my,time,true);
            }
            var ring=enchant?inputs:inputs.subList(1,inputs.size());int step=ring.isEmpty()?0:360/ring.size();
            for(int i=0;i<ring.size();i++) {
                float angle=(-90f+step*i)*net.minecraft.util.Mth.DEG_TO_RAD;
                liveItem(reader,g,ring.get(i),start+48+(int)(net.minecraft.util.Mth.cos(angle)*40),y+94+(int)(net.minecraft.util.Mth.sin(angle)*40),mx,my,time,true);
            }
            for(var glyph:ResearchAspectLayout.grid(row.aspects(),5,start+8,y+164))aspect(reader,g,glyph.aspect(),glyph.amount(),glyph.x(),glyph.y(),mx,my);
        }
        return true;
    }
    private static void liveItem(ResearchReaderScreen reader,GuiGraphics g,List<ItemStack> choices,int x,int y,int mx,int my,long time,boolean input) {
        if(choices.isEmpty())return;
        var stack=choices.get((int)Math.floorMod(time/1000,choices.size())).copy();
        g.renderItem(stack,x,y);g.renderItemDecorations(Minecraft.getInstance().font,stack,x,y);
        if(mx>=x&&mx<x+16&&my>=y&&my<y+16) {
            if(input)reader.ingredientTooltip(g,stack,x,y,mx,my);else reader.itemTooltip(g,stack,mx,my);
        }
    }
    private static void centered(GuiGraphics g, Component text, int center, int y, int color) {
        var font = Minecraft.getInstance().font;
        g.drawString(font, text, center - font.width(text) / 2, y, color, false);
    }
    private static void instability(GuiGraphics g, int value, int center, int y) {
        centered(g, Component.translatable("tc.inst").append(" ").append(
                Component.translatable("tc.inst." + Math.max(0, Math.min(5, value / 2)))), center, y, 0x505050);
    }
    private static void aspect(ResearchReaderScreen reader, GuiGraphics g, String tag, int amount, int x, int y, int mx, int my) {
        aspect(reader,g,tag,amount>0?Integer.toString(amount):"",x,y,mx,my);
    }
    private static void aspect(ResearchReaderScreen reader, GuiGraphics g, String tag, String text, int x, int y, int mx, int my) {
        var aspect = tc4revival.aspect.TCAspects.catalog().require(tag);
        int color = thaumcraft.api.client.ClientAspectViews.view(aspect).color();
        g.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 1);
        LegacyGui.blit(g,thaumcraft.api.client.ClientAspectViews.view(aspect).texture(), x, y, 16, 16, 0, 0, 16, 16, 16, 16);
        g.setColor(1, 1, 1, 1);
        if (!text.isEmpty()) {
            var font = Minecraft.getInstance().font;
            g.drawString(font, text, x + 17 - font.width(text), y + 10, 0xFFFFFF);
        }
        if (mx >= x && mx < x + 16 && my >= y && my < y + 16) reader.aspectTooltip(g, aspect, mx, my);
    }
    private static ResearchPage.Recipe select(ResearchPage.Recipe recipe, long time) {
        List<ResearchPage.Recipe> choices;
        if (recipe instanceof ResearchPage.Sequence sequence) choices = sequence.elements();
        else if (recipe instanceof ResearchPage.Ore ore) choices = ore.elements();
        else return recipe;
        return choices.isEmpty() ? new ResearchPage.Empty() : select(choices.get((int) Math.floorMod(time / 1000, choices.size())), time);
    }
    private static void overlay(GuiGraphics g, int x, int y, int dx, int dy, int u, int v, int width, int height) {
        g.pose().pushPose();
        try { g.pose().translate(x, y, 0); g.pose().scale(2, 2, 1); LegacyGui.blit(g,OVERLAY, dx, dy, u, v, width, height); }
        finally { g.pose().popPose(); }
    }
    private static void label(GuiGraphics g, String key, int center, int y) {
        var text = Component.translatable(key); var font = Minecraft.getInstance().font;
        g.drawString(font, text, center - font.width(text) / 2, y, 0x505050, false);
    }
}
