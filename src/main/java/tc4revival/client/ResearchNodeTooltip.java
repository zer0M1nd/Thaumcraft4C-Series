package tc4revival.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import tc4revival.aspect.TCAspects;
import tc4revival.research.ResearchBookHint;
import tc4revival.research.ResearchBrowser;
import tc4revival.research.ResearchDefinition;
import tc4revival.resources.ResearchSubtitleLocalization;

/** Legacy tooltip hierarchy with modern font layout and the original aspect glyphs. */
public  class ResearchNodeTooltip {
    private record Text(List<FormattedCharSequence> lines, int color) {}
    private ResearchNodeTooltip() {}
    private static AspectTooltips.Cell cell(ResearchBookHint.Cost cost) {
        return new AspectTooltips.Cell(cost.known()?cost.aspect():"",cost.owned()+"/"+cost.amount(),
                cost.enough()?0xff55ff55:0xffff5555,!cost.enough());
    }
    /** Native tooltip providers receive one component for every explicit line. */
    public static List<Component> textLines(FormattedText text) {
        var lines = text.getString().contains("\n") ? net.minecraft.client.Minecraft.getInstance().font.getSplitter()
                .splitLines(text, 1_000_000, Style.EMPTY) : List.of(text);
        return lines.stream().map(line -> {
            if (line instanceof Component component) return component;
            var component = Component.empty();
            line.visit((style, part) -> {
                component.append(Component.literal(part).setStyle(style));
                return java.util.Optional.empty();
            }, Style.EMPTY);
            return (Component) component;
        }).toList();
    }
    /** Book links use vanilla's tooltip container while keeping the research tree's text colours. */
    public static void drawNative(GuiGraphics g,Font font,ResearchBrowser.Node node,ResearchBookHint hint,
            int mouseX,int mouseY,int screenWidth) {
        g.renderComponentTooltipFromElements(font,new ArrayList<>(nativeElements(node,hint,screenWidth)),mouseX,mouseY,net.minecraft.world.item.ItemStack.EMPTY);
    }
    public static List<com.mojang.datafixers.util.Either<net.minecraft.network.chat.FormattedText,net.minecraft.world.inventory.tooltip.TooltipComponent>> nativeElements(
            ResearchBrowser.Node node,ResearchBookHint hint,int screenWidth) {
        return nativeElements(node,hint,screenWidth,true);
    }
    public static List<com.mojang.datafixers.util.Either<net.minecraft.network.chat.FormattedText,net.minecraft.world.inventory.tooltip.TooltipComponent>> nativeElements(
            ResearchBrowser.Node node,ResearchBookHint hint,int screenWidth,boolean bookControls) {
        return nativeElements(node,hint,screenWidth,bookControls,false);
    }
    private static List<com.mojang.datafixers.util.Either<net.minecraft.network.chat.FormattedText,net.minecraft.world.inventory.tooltip.TooltipComponent>> nativeElements(
            ResearchBrowser.Node node,ResearchBookHint hint,int screenWidth,boolean bookControls,boolean obfuscate) {
        boolean locked=node.state()==ResearchBrowser.State.LOCKED;
        var title=Component.translatable("tc.research_name."+node.original().key());
        if(locked&&obfuscate&&!node.research().mirror())title.withStyle(net.minecraft.ChatFormatting.OBFUSCATED);
        if(node.forcedHidden())title.append(Component.translatable("thaumcraft.research.hidden_suffix").withStyle(style->style.withObfuscated(false)));
        var description=Component.translatable(locked&&!node.research().mirror()?"tc.researchmissing":"tc.research_text."+node.original().key());
        var event=new thaumcraft.api.research.ResearchTooltipEvent(node.original().key(),locked,description);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        description=ResearchSubtitleLocalization.normalize(net.minecraft.client.Minecraft.getInstance().options.languageCode,event.description());
        var jeiHint=bookControls?BookRecipeViewer.hint():java.util.Optional.<Component>empty();
        if(locked&&jeiHint.isPresent())description=Component.empty().append(jeiHint.get()).append("\n").append(description);
        var guidance = ClientResearchGuidance.view(node.original(),bookControls);
        var costs = guidance.costs().isEmpty() ? hint.costs() : guidance.costs();
        for (var line : guidance.lines()) description.append("\n").append(line);
        var player=net.minecraft.client.Minecraft.getInstance().player;
        if(bookControls&&player!=null&&player.getAbilities().instabuild&&node.state()!=ResearchBrowser.State.COMPLETE&&!node.research().mirror())
            description.append("\n").append(Component.translatable("thaumcraft.research.creative_complete"));
        int titleColor=node.research().has(ResearchDefinition.Flag.SPECIAL)?(locked?-8355776:-128):(locked?-8355712:-1);
        var elements=new ArrayList<com.mojang.datafixers.util.Either<net.minecraft.network.chat.FormattedText,net.minecraft.world.inventory.tooltip.TooltipComponent>>();
        elements.add(com.mojang.datafixers.util.Either.left(title.withStyle(s->s.applyTo(net.minecraft.network.chat.Style.EMPTY.withColor(titleColor&0xffffff)))));
        elements.add(com.mojang.datafixers.util.Either.left(description.withStyle(s->s.applyTo(net.minecraft.network.chat.Style.EMPTY.withColor((locked?-9416624:-7302913)&0xffffff)))));
        if(!locked&&node.research().warp()>0)elements.add(com.mojang.datafixers.util.Either.left(forbidden(node.research().warp())));
        if(!costs.isEmpty())elements.add(com.mojang.datafixers.util.Either.right(new AspectTooltips.Data(List.of(
                new AspectTooltips.Row(Component.empty(),costs.stream().map(ResearchNodeTooltip::cell).toList())),Math.max(24,Math.min(240,screenWidth-24)))));
        if(bookControls&&(!locked||node.research().mirror()||node.original().tutorial())&&!hint.message().isEmpty()) {
            int color=hint.message().equals("tc.research.hasnote")?16753920:hint.requestable()?8900331:14423100;
            elements.add(com.mojang.datafixers.util.Either.left(Component.translatable(hint.message()).withStyle(s->s.withColor(color))));
        }
        if(!locked)jeiHint.ifPresent(line -> elements.add(com.mojang.datafixers.util.Either.left(line)));
        var lines=new ArrayList<com.mojang.datafixers.util.Either<FormattedText,net.minecraft.world.inventory.tooltip.TooltipComponent>>();
        for(var element:elements) {
            element.ifLeft(text -> textLines(text).forEach(line -> lines.add(com.mojang.datafixers.util.Either.left(line))));
            element.ifRight(image -> lines.add(com.mojang.datafixers.util.Either.right(image)));
        }
        return List.copyOf(lines);
    }
    public static void draw(GuiGraphics g, Font font, ResearchBrowser.Node node, ResearchBookHint hint,
            int mouseX, int mouseY, int screenWidth, int screenHeight, long milliseconds, boolean obfuscate) {
        if(tc4revival.config.TCConfig.CLIENT.nativeResearchTooltips.get()) {
            var elements = new ArrayList<>(nativeElements(node,hint,screenWidth,true,obfuscate));
            BookRecipeViewer.researchModName(node.original(),node.research().category())
                    .ifPresent(line -> elements.add(com.mojang.datafixers.util.Either.left(line)));
            g.renderComponentTooltipFromElements(font,elements,mouseX,mouseY,net.minecraft.world.item.ItemStack.EMPTY);
            return;
        }
        boolean locked = node.state() == ResearchBrowser.State.LOCKED;
        var title = Component.translatable("tc.research_name." + node.original().key());
        if (locked && obfuscate && !node.research().mirror()) title.withStyle(net.minecraft.ChatFormatting.OBFUSCATED);
        if (node.forcedHidden()) title.append(Component.translatable("thaumcraft.research.hidden_suffix").withStyle(style -> style.withObfuscated(false)));
        var description = Component.translatable(locked && !node.research().mirror() ? "tc.researchmissing" : "tc.research_text." + node.original().key());
        var event = new thaumcraft.api.research.ResearchTooltipEvent(node.original().key(), locked, description);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        description = ResearchSubtitleLocalization.normalize(net.minecraft.client.Minecraft.getInstance().options.languageCode,event.description());
        var jeiHint=BookRecipeViewer.hint();
        if(locked&&jeiHint.isPresent())description=Component.empty().append(jeiHint.get()).append("\n").append(description);
        var guidance = ClientResearchGuidance.view(node.original());
        var costs = guidance.costs().isEmpty() ? hint.costs() : guidance.costs();
        for (var line : guidance.lines()) description.append("\n").append(line);
        var player=net.minecraft.client.Minecraft.getInstance().player;
        if(player!=null && player.getAbilities().instabuild && node.state()!=ResearchBrowser.State.COMPLETE && !node.research().mirror())
            description.append("\n").append(Component.translatable("thaumcraft.research.creative_complete"));
        if(!locked&&jeiHint.isPresent())description.append("\n").append(jeiHint.get());
        var modName = BookRecipeViewer.researchModName(node.original(), node.research().category());
        float scale=tc4revival.config.TCConfig.CLIENT.smallBookFont.get()?.5f:1f;
        int descriptionWidth=description.getString().lines().mapToInt(font::width).max().orElse(0);
        int width = Math.max(font.width(title), (int)Math.ceil(descriptionWidth*scale));
        if((!locked || node.research().mirror() || node.original().tutorial()) && !hint.message().isEmpty())width=Math.max(width,(int)Math.ceil(font.width(Component.translatable(hint.message()))*scale));
        if(!locked && node.research().warp()>0)width=Math.max(width,(int)Math.ceil(font.width(forbidden(node.research().warp()))*scale));
        if(modName.isPresent())width=Math.max(width,(int)Math.ceil(font.width(modName.get())*scale));
        width = Math.max(width, costs.stream().map(ResearchNodeTooltip::cell).mapToInt(c -> AspectTooltips.cellWidth(font,c)).sum());
        width = Math.max(32, Math.min(width, Math.max(32, screenWidth - 12)));
        var titleLines = font.split(title, width);
        int textWidth = (int)(width/scale);
        var texts = new ArrayList<Text>();
        texts.add(new Text(font.split(description, textWidth), locked ? -9416624 : -7302913));
        if (!locked && node.research().warp() > 0) texts.add(new Text(font.split(forbidden(node.research().warp()), textWidth), 0xFFFFFF));
        boolean hasHint = (!locked || node.research().mirror() || node.original().tutorial()) && !hint.message().isEmpty();
        var hintLines = hasHint ? font.split(Component.translatable(hint.message()), textWidth) : List.<FormattedCharSequence>of();
        int headerHeight = titleLines.size() * font.lineHeight + 5;
        int textHeight = texts.stream().mapToInt(text -> (int)Math.ceil(text.lines().size() * font.lineHeight * scale) + 4).sum();
        int costHeight = costs.isEmpty() ? 0 : 23;
        int hintHeight=(int)Math.ceil(hintLines.size()*font.lineHeight*scale);
        var modLines = modName.map(line -> font.split(line, textWidth)).orElse(List.of());
        int modHeight = modLines.isEmpty() ? 0 : 4 + (int)Math.ceil(modLines.size()*font.lineHeight*scale);
        // Paragraph spacing belongs between sections, not after the final visible row.
        int tail=hasHint?costHeight+hintHeight:costHeight>0?16:-4;
        int height = headerHeight + textHeight + tail + modHeight + 3;
        int x = Math.max(4, Math.min(mouseX + 6, screenWidth - width - 4));
        int y = Math.max(4, Math.min(mouseY - 4, screenHeight - height - 4));
        g.pose().pushPose();
        try {
            g.pose().translate(0, 0, 400);
            g.fill(x - 3, y - 3, x + width + 3, y + height, 0xC0000000);
            int titleColor = node.research().has(ResearchDefinition.Flag.SPECIAL) ? (locked ? -8355776 : -128) : (locked ? -8355712 : -1);
            for (int i = 0; i < titleLines.size(); i++) g.drawString(font, titleLines.get(i), x, y + i * font.lineHeight, titleColor);
            int cursor = y + headerHeight;
            for (var text : texts) {
                small(g, font, text.lines(), x, cursor, text.color());
                cursor += (int)Math.ceil(text.lines().size() * font.lineHeight * scale) + 4;
            }
            int costX=x;
            for (var cost : costs) {
                var cell=cell(cost);
                AspectTooltips.draw(g,font,cell,costX,cursor);
                costX+=AspectTooltips.cellWidth(font,cell);
            }
            int hintColor = hint.message().equals("tc.research.hasnote") ? 16753920 : hint.requestable() ? 8900331 : 14423100;
            small(g, font, hintLines, x, cursor + costHeight, hintColor);
            small(g, font, modLines, x, cursor + tail + 4, 0xFFFFFF);
        } finally { g.setColor(1, 1, 1, 1); g.pose().popPose(); }
    }
    private static Component forbidden(int warp) {
        // Give the inserted severity its own colour; legacy formatting in the
        // outer translation does not carry across the component boundary.
        String level=Component.translatable("tc.forbidden.level."+Math.min(5,warp)).getString().strip();
        return Component.translatable("tc.forbidden",Component.literal(net.minecraft.ChatFormatting.stripFormatting(level)).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE)).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE);
    }
    private static void small(GuiGraphics g, Font font, List<FormattedCharSequence> lines, int x, int y, int color) {
        g.pose().pushPose();
        try {
            g.pose().translate(x, y, 0); float scale=tc4revival.config.TCConfig.CLIENT.smallBookFont.get()?.5f:1f; g.pose().scale(scale, scale, 1);
            for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), 0, i * font.lineHeight, color);
        } finally { g.pose().popPose(); }
    }
}
