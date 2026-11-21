package tc4revival.client;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;
import tc4revival.research.ResearchText;

/** Book prose uses Minecraft's native font metrics, formatting and line splitting. */
public  class ResearchBookText {
    private ResearchBookText() {}
    public static List<FormattedCharSequence> wrap(Font font, String text, int width) {
        if (width < 1) throw new IllegalArgumentException("Book text width must be positive");
        return List.copyOf(font.split(FormattedText.of(text), width));
    }
    public sealed interface Element permits Line, Divider, Illustration {}
    public record Line(int y, FormattedCharSequence text) implements Element {}
    public record Divider(int y) implements Element {}
    public record Illustration(int y, ResearchText.Picture picture) implements Element {}
    public record Layout(int width, int height, List<Element> elements) {
        public Layout { elements = List.copyOf(elements); }
    }

    /** Layout is rebuilt when the language, font resources, page contents or available width changes. */
    public static Layout layout(Font font, String markup, int width) {
        if (width < 1) throw new IllegalArgumentException("Book text width must be positive");
        var elements = new ArrayList<Element>(); var pending = new StringBuilder();
        Style style = Style.EMPTY; int y = 0; boolean afterInsert = false;
        for (var part : ResearchText.parseForDisplay(markup)) {
            if (part instanceof ResearchText.Text text) { pending.append(text.value()); afterInsert = false; }
            else if (part instanceof ResearchText.Break) {
                // The parser surrounds insertions with a boundary break; the block itself supplies that boundary.
                if (afterInsert) afterInsert = false;
                else pending.append('\n');
            } else {
                if (!pending.isEmpty() && pending.charAt(pending.length() - 1) == '\n') pending.setLength(pending.length() - 1);
                String prose = pending.toString();
                y = append(font, prose, style, width, y, elements); style = endStyle(prose, style); pending.setLength(0);
                if (part instanceof ResearchText.Picture picture) {
                    elements.add(new Illustration(y, picture));
                    y += Math.max(1, (int) Math.ceil(picture.height() * picture.scale()));
                } else { elements.add(new Divider(y)); y += font.lineHeight; }
                afterInsert = true;
            }
        }
        y = append(font, pending.toString(), style, width, y, elements);
        return new Layout(width, y, elements);
    }
    private static int append(Font font, String prose, Style style, int width, int y, List<Element> elements) {
        if (prose.isEmpty()) return y;
        for (var line : font.split(FormattedText.of(prose, style), width)) {
            elements.add(new Line(y, line)); y += font.lineHeight;
        }
        return y;
    }
    private static Style endStyle(String text, Style initial) {
        Style[] current = {initial};
        // A sentinel lets Minecraft apply a trailing formatting code even when no visible character follows it.
        StringDecomposer.iterateFormatted(text + '\0', initial, (index, style, codePoint) -> { current[0] = style; return true; });
        return current[0];
    }
    public static List<Layout> paginate(Layout layout,int lineHeight,int firstHeight,int nextHeight) {
        if(lineHeight<1||firstHeight<lineHeight||nextHeight<lineHeight)throw new IllegalArgumentException("Invalid page dimensions");
        var pages=new ArrayList<Layout>();var elements=new ArrayList<Element>();int y=0,limit=firstHeight;
        for(var element:layout.elements()) {
            int h=element instanceof Illustration image?Math.max(1,(int)Math.ceil(image.picture().height()*image.picture().scale())):lineHeight;
            if(y+h>limit&&!elements.isEmpty()) {pages.add(new Layout(layout.width(),y,elements));elements=new ArrayList<>();y=0;limit=nextHeight;}
            if(element instanceof Line line)elements.add(new Line(y,line.text()));
            else if(element instanceof Divider)elements.add(new Divider(y));
            else if(element instanceof Illustration image) {
                var p=image.picture();
                if(h>limit) {p=new ResearchText.Picture(p.texture(),p.u(),p.v(),p.width(),p.height(),p.scale()*limit/h);h=limit;}
                elements.add(new Illustration(y,p));
            }
            y+=h;
        }
        if(!elements.isEmpty()||pages.isEmpty())pages.add(new Layout(layout.width(),y,elements));
        return List.copyOf(pages);
    }
    public static void draw(GuiGraphics graphics, Font font, Layout layout, int x, int y, int color) {
        for (var element : layout.elements()) {
            if (element instanceof Line line) graphics.drawString(font, line.text(), x, y + line.y(), color, false);
            else if (element instanceof Divider divider) LegacyGui.blit(graphics,ResourceLocation.parse("thaumcraft:textures/gui/gui_researchbook.png"),
                    x + layout.width() / 2 - 48, y + divider.y() + 2, 24, 184, 96, 4);
            else if (element instanceof Illustration illustration) {
                var picture = illustration.picture(); graphics.pose().pushPose();
                try {
                    graphics.pose().translate(x + picture.xOffset(layout.width()), y + illustration.y(), 0);
                    graphics.pose().scale(picture.scale(), picture.scale(), 1);
                    LegacyGui.blit(graphics,ResourceLocation.parse(picture.texture()), 0, 0, picture.u(), picture.v(), picture.width(), picture.height());
                } finally { graphics.pose().popPose(); }
            }
        }
    }
}
