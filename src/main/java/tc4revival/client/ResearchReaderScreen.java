package tc4revival.client;



import java.util.HashMap;

import java.util.Map;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.gui.screens.Screen;

import net.minecraft.network.chat.Component;

import net.minecraft.resources.ResourceLocation;

import tc4revival.aspect.TCAspects;

import tc4revival.research.ResearchBookNavigation;

import tc4revival.research.ResearchPage;



/** Two-page reader using the original book atlas and Minecraft's current font implementation. */

public  class ResearchReaderScreen extends Screen {

    @FunctionalInterface public interface RecipePainter {

        void draw(ResearchReaderScreen reader, GuiGraphics graphics, ResearchPage page, int side, int x, int y, int mouseX, int mouseY);

    }

    private static final ResourceLocation BOOK = ResourceLocation.parse("thaumcraft:textures/gui/gui_researchbook.png");

    private final ResearchBrowserScreen tree;

    private final ResearchBookNavigation navigation;

    private final RecipePainter recipes;

    private BookFlow.Result flow;
    private final BookAmendments.View amendments = new BookAmendments.View();

    private final Map<String,BookImage> animations=new HashMap<>();

    private final java.util.Set<String> failedImages=new java.util.HashSet<>();

    private record AnimationButton(int x,int y,int width,BookImage image,boolean replay) {}

    private final java.util.List<AnimationButton> animationButtons=new java.util.ArrayList<>();

    private net.minecraft.network.chat.Style hoveredStyle;

    private java.util.List<ResearchPage> sourcePages=java.util.List.of(), formattedPages=java.util.List.of();

    private net.minecraft.locale.Language layoutLanguage;

    private long layoutKnowledge=-1;
    private long layoutResources=-1;
    private tc4revival.config.TCConfig.LegacyTextMode layoutTextMode;

    private int left, top;
    // TC4 draws instability at recipe origin (top - 8) + 194, independent of the title.
    int recipeFooterY() { return top + 186; }

    private boolean returning;

    private record CustomView(ResearchPage page, thaumcraft.api.client.ResearchPageRenderers.Renderer renderer,

            thaumcraft.api.client.ResearchPageRenderers.Context context) {}

    private final Map<Integer, CustomView> customViews = new HashMap<>();

    private long customGeneration = -1;

    private CustomView captured;

    private void closeCustomPages() {

        captured = null;

        customViews.values().forEach(view -> view.renderer().close()); customViews.clear();

        customGeneration = thaumcraft.api.client.ResearchPageRenderers.generation();

    }

    private void customPage(GuiGraphics graphics, ResearchPage page, int side, int x, int y, int mx, int my, float partial) {

        var bounds = new thaumcraft.api.client.ResearchPageRenderers.Bounds(x, y, 139, top + 181 - y);

        var view = customViews.get(side);

        if (view == null || view.page() != page || !view.context().bounds().equals(bounds)) {

            if (view != null) view.renderer().close();

            view = new CustomView(page, thaumcraft.api.client.ResearchPageRenderers.create((ResearchPage.Custom)page.recipe()),

                    new thaumcraft.api.client.ResearchPageRenderers.Context(this, bounds));

            customViews.put(side, view);

        }

        graphics.enableScissor(x, y, x + bounds.width(), y + bounds.height()); graphics.pose().pushPose();

        try { view.renderer().render(view.context(), graphics, mx, my, partial); }

        finally { graphics.pose().popPose(); graphics.disableScissor(); graphics.setColor(1, 1, 1, 1); }

    }

    private Runnable tooltip;
    private Component longTooltip;
    private int tooltipOffset, tooltipMaxOffset;
    private boolean scrollableTooltip;

    private net.minecraft.world.item.ItemStack hoveredItem=net.minecraft.world.item.ItemStack.EMPTY;

    private String hoveredResearch="";

    private record TutorialButton(int x,int y,ResourceLocation target) {}

    private final java.util.List<TutorialButton> tutorials=new java.util.ArrayList<>();

    private final ResearchAspectSources aspectSources = new ResearchAspectSources();

    private record RecipeLink(int x, int y, int size, tc4revival.research.ResearchRecipeLinks.Target target) {}

    private final java.util.List<RecipeLink> links = new java.util.ArrayList<>();

    private tc4revival.research.ResearchRecipeLinks recipeLinks;

    private long cycleStart;

    public ResearchReaderScreen(ResearchBrowserScreen tree, ResearchBookNavigation navigation, RecipePainter recipes) {

        super(Component.literal("Thaumonomicon"));

        this.tree = java.util.Objects.requireNonNull(tree); this.navigation = java.util.Objects.requireNonNull(navigation);

        this.recipes = java.util.Objects.requireNonNull(recipes);

    }

    @Override protected void init() { left = (width - 256) / 2; top = (height - 181) / 2; returning = false; resetText(); navigation.pageLayout(this::formatPages); }

    private java.util.List<ResearchPage> formatPages(java.util.List<ResearchPage> source) {

        source=amendments.visible(source);
        var language=net.minecraft.locale.Language.getInstance();

        var textMode=tc4revival.config.TCConfig.CLIENT.legacyTextMode.get();
        if(layoutTextMode==textMode && source.equals(sourcePages)&&layoutLanguage==language&&layoutKnowledge==ClientKnowledge.revision()
                &&layoutResources==BookTextResources.generation())return formattedPages;
        layoutTextMode=textMode;
        layoutResources=BookTextResources.generation();

        layoutKnowledge=ClientKnowledge.revision();

        if(layoutLanguage!=language){animations.values().forEach(BookImage::close);animations.clear();failedImages.clear();}

        sourcePages=java.util.List.copyOf(source);layoutLanguage=language;

        flow=BookFlow.layout(font,source,navigation.reading().orElse(""));formattedPages=flow.pages();return formattedPages;

    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void tick() {

        if (!ClientBookSession.active(tree.opening())) { minecraft.setScreen(null); return; }

        tree.tick();

        if (navigation.visiblePages().stream().anyMatch(page -> page.type() == ResearchPage.Type.ASPECTS)) aspectSources.tick();

        if (navigation.reading().isEmpty()) onClose();

    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {

        if (customGeneration != thaumcraft.api.client.ResearchPageRenderers.generation()) resetText();

        tooltip = null;
        scrollableTooltip = false;

        hoveredStyle=null;animationButtons.clear();

        hoveredItem=net.minecraft.world.item.ItemStack.EMPTY;hoveredResearch="";tutorials.clear();

        links.clear();

        renderBackground(g);

        g.pose().pushPose();

        try {

            g.pose().translate((width - 256 * 1.3f) / 2, (height - 181 * 1.3f) / 2, 0);

            g.pose().scale(1.3f, 1.3f, 1); LegacyGui.blit(g,BOOK, 0, 0, 0, 0, 256, 181);

        } finally { g.pose().popPose(); }

        var pages = navigation.visiblePages();

        // Conditional pages can disappear when player knowledge changes without a page-turn event.

        customViews.entrySet().removeIf(entry -> {

            int index = navigation.spread() + entry.getKey();

            boolean stale = index >= pages.size() || pages.get(index) != entry.getValue().page();

            if (stale) { if (captured == entry.getValue()) captured = null; entry.getValue().renderer().close(); }

            return stale;

        });

        BookTypography.inBook(() -> {

        for (int side = 0; side < 2; side++) {

            int index = navigation.spread() + side;

            if (index >= pages.size()) continue;

            int y = top;

            if (index == 0) {

                LegacyGui.blit(g,BOOK, left + 4, top - 13, 24, 184, 96, 4);

                LegacyGui.blit(g,BOOK, left + 4, top + 4, 24, 184, 96, 4);

                var name = Component.translatable("tc.research_name." + navigation.reading().orElseThrow());

                float scale = Math.min(1, 130f / Math.max(1, font.width(name)));

                g.pose().pushPose();

                try {

                    g.pose().translate(left + 52, top - 6, 0); g.pose().scale(scale, scale, 1);

                    g.drawString(font, name, -font.width(name) / 2, 0, 0x303030, false);

                } finally { g.pose().popPose(); }

                if(inside(mouseX,mouseY,left-15,top-13,139,20))hoveredResearch=navigation.reading().orElse("");

                y += 25;

            }

            var page = pages.get(index);

            switch (page.type()) {

                case CUSTOM -> customPage(g, page, side, left - 15 + side * 152, y - 10, mouseX, mouseY, partialTick);

                case TEXT, TEXT_CONCEALED, DIVIDER, ILLUSTRATION, ITEM -> prose(g, page, side, left - 15 + side * 152, y - 10, top + 181 - (y - 10),mouseX,mouseY);

                case IMAGE -> {int h=Math.min(160,top+181-y-(BookFlow.gif(page)?12:0));image(g,page,left-8+side*152,y,Math.round(128*h/160f),h);}

                case ASPECTS -> aspects(g, page, left - 8 + side * 152, y - 8, mouseX, mouseY);

                default -> recipes.draw(this, g, page, side, left - 4, y - 8, mouseX, mouseY);

            }

            if(page.type()==ResearchPage.Type.COMPOUND_CRAFTING) {

                var tutorial=thaumcraft.api.client.ResearchBookTutorials.structureId(navigation.reading().orElse(""),page,cycleMillis());

                if(tutorial.isPresent()) {

                    // Compact tab in the upper right margin of its own page.

                    int bx=left+91+side*152,by=top-23;

                    tutorials.add(new TutorialButton(bx,by,tutorial.get()));

                    boolean hover=inside(mouseX,mouseY,bx,by,32,16);

                    g.fill(bx,by,bx+32,by+16,hover?0xFF40372D:0xFF241E19);

                    g.drawCenteredString(font,Component.translatable("thaumcraft.research.ponder"),bx+16,by+4,0xFFF3C8);

                }

            }

        }

        });

        if (navigation.spread() > 0) LegacyGui.blit(g,BOOK, left - 16, top + 190, 0, 184, 12, 8);

        if (navigation.spread() < pages.size() - 2) LegacyGui.blit(g,BOOK, left + 262, top + 190, 12, 184, 12, 8);

        if (navigation.hasHistory()) LegacyGui.blit(g,BOOK, left + 118, top + 189, 38, 202, 20, 12);

        if (tooltip != null) tooltip.run();

        super.render(g, mouseX, mouseY, partialTick);

    }

    private void prose(GuiGraphics g, ResearchPage page, int side, int x, int y, int height,int mx,int my) {

        var layout=flow==null?null:flow.sheets().get(navigation.spread()+side);

        if(layout==null)return;

        g.enableScissor(x, y, x + 139, y + height);

        try {

            for(var element:layout.elements()) {

                int ey=y+element.y();

                if(element.line()!=null) {

                    g.drawString(font,element.line(),x,ey,0,false);

                    float[] cursor={x};

                    element.line().accept((index,style,code)->{

                        String insertion=style.getInsertion();

                        if(insertion!=null&&insertion.startsWith(BookComponents.ICON)) {

                            var aspect=TCAspects.catalog().require(insertion.substring(BookComponents.ICON.length()));

                            icon(g,aspect,(int)cursor[0],ey,BookComponents.ICON_WIDTH);

                        }

                        cursor[0]+=font.getSplitter().stringWidth(net.minecraft.network.chat.FormattedText.of(new String(Character.toChars(code)),style));

                        return true;

                    });

                    if(inside(mx,my,x,ey,139,font.lineHeight)) {

                        hoveredStyle=font.getSplitter().componentStyleAtWidth(element.line(),mx-x);

                        componentTooltip(g,hoveredStyle,mx,my);

                    }

                }else {

                    var widget=element.widget();

                    switch(widget.type()) {

                        case DIVIDER -> LegacyGui.blit(g,BOOK,x+139/2-48,ey+2,24,184,96,4);

                        case ITEM -> {
                            if (widget.properties().has("research_links")) {
                                var keys = widget.properties().getAsJsonArray("research_links").asList().stream()
                                        .map(com.google.gson.JsonElement::getAsString)
                                        .filter(key -> tc4revival.research.ResearchCatalogs.bundled().find(key)
                                                .filter(tc4revival.research.ResearchCatalogs::enabled).isPresent()).toList();
                                int size = Math.min(16, element.height()), ix = x + (139 - size * keys.size()) / 2;
                                for (String key : keys) {
                                    ResearchLinkIcons.draw(g, key, ix, ey, size);
                                    if (inside(mx,my,ix,ey,size,size)) {
                                        var target = BookComponents.LINK + "research/" + BookComponents.encode(key) + "#";
                                        var style = net.minecraft.network.chat.Style.EMPTY.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                                net.minecraft.network.chat.ClickEvent.Action.CHANGE_PAGE, target));
                                        componentTooltip(g,style,mx,my);
                                        hoveredStyle = style.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                                net.minecraft.network.chat.ClickEvent.Action.CHANGE_PAGE,
                                                BookComponents.LINK + "locate/" + BookComponents.encode(key)));
                                    }
                                    ix += size;
                                }
                                break;
                            }

                            var values=new java.util.ArrayList<com.google.gson.JsonElement>();var properties=widget.properties();

                            if(properties.has("items"))properties.getAsJsonArray("items").forEach(values::add);else values.add(properties.get("item"));

                            int size=element.height(),ix=x+(139-size*values.size())/2;

                            for(var value:values) {

                                var stack=BookComponents.item(value);

                                g.pose().pushPose();try{g.pose().translate(ix,ey,0);g.pose().scale(size/16f,size/16f,1);g.renderItem(stack,0,0);}

                                finally{g.pose().popPose();}

                                if(inside(mx,my,ix,ey,size,size))itemTooltip(g,stack,mx,my);

                                ix+=size;

                            }

                        }

                        case ILLUSTRATION -> {

                            int sourceHeight=Math.max(1,BookFlow.number(widget,"height",64));

                            int imageHeight=element.height()-(BookFlow.gif(widget)?12:0);

                            int imageWidth=Math.min(139,Math.round(BookFlow.number(widget,"width",64)*(float)imageHeight/sourceHeight));

                            image(g,widget,x+(139-imageWidth)/2,ey,imageWidth,imageHeight);

                        }

                        default -> {}

                    }

                }

            }

        }

        finally { g.disableScissor(); }

    }

    private void image(GuiGraphics g,ResearchPage page,int x,int y,int w,int h) {

        if(page.image().isEmpty())return;

        var resource=ResourceLocation.parse(page.image().get());

        if(!BookFlow.gif(page)) {

            if(page.type()==ResearchPage.Type.IMAGE)g.blit(resource,x,y,w,h,0,0,128,160,128,160);

            else LegacyGui.blit(g,resource,x,y,w,h,BookFlow.number(page,"u",0),BookFlow.number(page,"v",0),

                    BookFlow.number(page,"width",64),BookFlow.number(page,"height",64),BookFlow.number(page,"texture_width",256),BookFlow.number(page,"texture_height",256));

            return;

        }

        if(failedImages.contains(page.id())){g.drawString(font,"[GIF unavailable]",x,y,0x993333,false);return;}

        var animation=animations.get(page.id());

        if(animation==null)try{animation=new BookImage(resource);animations.put(page.id(),animation);}

        catch(Exception failure){failedImages.add(page.id());tc4revival.TC4Revival.LOGGER.warn("Cannot load book GIF {}",resource,failure);return;}

        float fit=Math.min(w/(float)animation.width(),h/(float)animation.height());

        int fittedWidth=Math.max(1,Math.round(animation.width()*fit));h=Math.max(1,Math.round(animation.height()*fit));

        x+=(w-fittedWidth)/2;w=fittedWidth;

        animation.draw(g,x,y,w,h);

        int by=y+h+1;

        g.fill(x,by,x+30,by+10,0xff40372d);g.fill(x+32,by,x+62,by+10,0xff40372d);

        g.drawCenteredString(font,Component.translatable(animation.playing()?"thaumcraft.book.pause":"thaumcraft.book.play"),x+15,by+1,0xfff3c8);

        g.drawCenteredString(font,Component.translatable("thaumcraft.book.replay"),x+47,by+1,0xfff3c8);

        animationButtons.add(new AnimationButton(x,by,30,animation,false));animationButtons.add(new AnimationButton(x+32,by,30,animation,true));

    }

    private void componentTooltip(GuiGraphics g,net.minecraft.network.chat.Style style,int mx,int my) {

        if(style==null)return;

        if(style.getHoverEvent()!=null){

            var item=style.getHoverEvent().getValue(net.minecraft.network.chat.HoverEvent.Action.SHOW_ITEM);

            if(item!=null)hoveredItem=item.getItemStack().copy();

            var text=style.getHoverEvent().getValue(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT);
            if(text!=null) {
                var lines=font.split(text,Math.min(360,Math.max(40,width-24)));
                // Native tooltip rows advance by ten pixels, including blank lines.
                // Reserve its border, footer and screen margins before slicing.
                int visible=Math.max(1,(height-32)/10-1);
                if(lines.size()>visible) {
                    if(!text.equals(longTooltip)){longTooltip=text.copy();tooltipOffset=0;}
                    tooltipMaxOffset=lines.size()-visible;tooltipOffset=Math.min(tooltipOffset,tooltipMaxOffset);scrollableTooltip=true;
                    var shown=new java.util.ArrayList<net.minecraft.util.FormattedCharSequence>(lines.subList(tooltipOffset,tooltipOffset+visible));
                    shown.add(Component.literal(BookTypography.locale().startsWith("zh")?"滚轮滚动":"Scroll to read")
                            .append(" ("+(tooltipOffset+1)+"–"+(tooltipOffset+visible)+"/"+lines.size()+")")
                            .withStyle(net.minecraft.ChatFormatting.GRAY).getVisualOrderText());
                    tooltip=()->g.renderTooltip(font,shown,(sw,sh,px,py,tw,th)->new org.joml.Vector2i(
                            net.minecraft.util.Mth.clamp(px+12,8,Math.max(8,sw-tw-8)),
                            net.minecraft.util.Mth.clamp(py-12,8,Math.max(8,sh-th-12))),mx,my);return;
                }
            }
            tooltip=()->g.renderComponentHoverEffect(font,style,mx,my);return;

        }

        var click=style.getClickEvent();if(click==null||!click.getValue().startsWith(BookComponents.LINK))return;

        String target=click.getValue().substring(BookComponents.LINK.length());

        if(target.startsWith("aspect/")) {

            var aspect=TCAspects.catalog().require(target.substring(7));

            tooltip=()->{

                var elements=new java.util.ArrayList<com.mojang.datafixers.util.Either<net.minecraft.network.chat.FormattedText,net.minecraft.world.inventory.tooltip.TooltipComponent>>();

                elements.add(com.mojang.datafixers.util.Either.left(Component.literal(thaumcraft.api.client.ClientAspectViews.view(aspect).displayName()).withStyle(net.minecraft.ChatFormatting.AQUA)));

                elements.add(com.mojang.datafixers.util.Either.left(Component.translatable(thaumcraft.api.client.ClientAspectViews.view(aspect).descriptionKey()).withStyle(net.minecraft.ChatFormatting.GRAY)));

                if(navigation.canRead("RESEARCHER1")&&!aspect.isPrimal())elements.add(com.mojang.datafixers.util.Either.right(new AspectTooltips.Data(java.util.List.of(

                        new AspectTooltips.Row(Component.empty(),aspect.components().stream().map(tag->new AspectTooltips.Cell(tag,"")).toList())),80)));

                g.renderComponentTooltipFromElements(font,elements,mx,my-8,net.minecraft.world.item.ItemStack.EMPTY);

            };

        }else if(target.startsWith("research/")) {

            String key=BookComponents.uncode(target.substring(9).split("#",-1)[0]);

            if(BookComponents.hidden(key)){tooltip=()->g.renderTooltip(font,Component.literal("???"),mx,my);return;}

            hoveredResearch=key;

            navigation.searchable().stream().filter(node->node.research().key().equals(key)).findFirst().ifPresent(node->

                    tree.hint(node).ifPresent(hint->tooltip=()->ResearchNodeTooltip.drawNative(g,font,node,hint,mx,my,width)));

        }

    }

    private void aspects(GuiGraphics g, ResearchPage page, int x, int y, int mouseX, int mouseY) {

        int row = 0;

        for (var entry : page.aspects().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {

            var aspect = TCAspects.catalog().require(entry.getKey()); int ay = y + row++ * 50;

            icon(g, aspect, x, ay, 32);

            if (entry.getValue() > 0) g.drawString(font, Integer.toString(entry.getValue()), x + 24, ay + 24, 0xFFFFFF);

            aspectLabel(g, thaumcraft.api.client.ClientAspectViews.view(aspect).displayName(), x + 16, ay + 33, 40);

            if (aspect.isPrimal()) g.drawString(font, Component.translatable("tc.aspect.primal"), x + 48, ay + 12, 0x444444, false);

            else {

                for (int i = 0; i < 2; i++) {

                    var component = TCAspects.catalog().require(aspect.components().get(i)); int cx = x + 54 + i * 42;

                    icon(g, component, cx, ay + 4, 24);

                    aspectLabel(g, thaumcraft.api.client.ClientAspectViews.view(component).displayName(), cx + 12, ay + 30, 40);

                }

                g.drawString(font, "=", x + 39, ay + 12, 0x999999, false);

                g.drawString(font, "+", x + 83, ay + 12, 0x999999, false);

            }

            if (mouseX >= x && mouseX < x + 40 && mouseY >= ay && mouseY < ay + 40) {

                // Legacy hover enlarges the selected aspect and displays its scanned item sources.

                icon(g, aspect, x - 4, ay - 4, 40);

                tooltip = () -> drawAspectSources(g, aspect, mouseX, mouseY);

            }

        }

    }

    private void aspectLabel(GuiGraphics g, String label, int centerX, int y, int maxWidth) {
        int textWidth = font.width(label);
        float scale = Math.min(1f, maxWidth / (float)Math.max(1, textWidth));
        g.pose().pushPose();
        try {
            g.pose().translate(centerX, y, 0);
            g.pose().scale(scale, scale, 1);
            g.drawString(font, label, -textWidth / 2, 0, 0x505050, false);
        } finally {
            g.pose().popPose();
        }
    }

    private void drawAspectSources(GuiGraphics g, tc4revival.aspect.Aspect aspect, int mouseX, int mouseY) {

        var items = aspectSources.get(aspect.tag());

        if (items.isEmpty()) {

            g.renderTooltip(font,java.util.List.of(Component.literal(thaumcraft.api.client.ClientAspectViews.view(aspect).displayName()).getVisualOrderText(),

                    Component.translatable(thaumcraft.api.client.ClientAspectViews.view(aspect).descriptionKey()).getVisualOrderText()),mouseX,mouseY); return;

        }

        int columns = Math.min(8, items.size()), rows = Math.max(1,(height - 24) / 17);

        int capacity = columns * rows, pages = (items.size() + capacity - 1) / capacity;

        int page = (int)(System.currentTimeMillis() / 3000 % pages);

        int from = page * capacity, count = Math.min(capacity, items.size() - from);

        int x = Math.max(2, Math.min(width - columns * 17 - 2, mouseX + 8));

        int y = Math.max(12, Math.min(height - ((count + columns - 1) / columns) * 17 - 2, mouseY - (4 + count / 8 * 8)));

        g.pose().pushPose();

        try {

            g.pose().translate(0,0,400);

            for (int i = 0; i < count; i++) {

                var stack = items.get(from + i); int sx = x + i % columns * 17, sy = y + i / columns * 17;

                g.renderItem(stack,sx,sy); g.renderItemDecorations(font,stack,sx,sy);

            }

            if (pages > 1) g.drawString(font,(page + 1) + "/" + pages,x,y - 10,0xffffff);

        } finally { g.pose().popPose(); }

    }

    private static void icon(GuiGraphics g, tc4revival.aspect.Aspect aspect, int x, int y, int size) {

        int color = thaumcraft.api.client.ClientAspectViews.view(aspect).color(); g.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 1);

        LegacyGui.blit(g,thaumcraft.api.client.ClientAspectViews.view(aspect).texture(), x, y, size, size, 0, 0, 16, 16, 16, 16);

        g.setColor(1, 1, 1, 1);

    }

    @Override public boolean mouseClicked(double x, double y, int button) {

        if(queryJei(com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(button)))return true;

        if(button==0) {

            if (!hoveredResearch.isEmpty() && guidance(hoveredResearch)) return true;

            for(var control:animationButtons)if(inside(x,y,control.x(),control.y(),control.width(),10)) {

                if(control.replay())control.image().replay();else control.image().toggle();return true;

            }

            if(hoveredStyle!=null&&handleComponentClicked(hoveredStyle))return true;

        }

        if(button==0)for(var tutorial:tutorials)if(inside(x,y,tutorial.x(),tutorial.y(),32,16)&&openTutorial(tutorial.target()))return true;

        if (button==0&&(inside(x, y, left - 17, top + 189, 14, 10) && navigation.turn(-1)

                || inside(x, y, left + 261, top + 189, 14, 10) && navigation.turn(1)

                || inside(x, y, left + 118, top + 189, 20, 12) && navigation.backLink())) { pageSound(); resetText(); return true; }

        for (var link : links) if (button==0&&inside(x, y, link.x(), link.y(), link.size(), link.size())) {

            if(!carriesBook())return true;

            if (sealedResearch(link.target().research())) return true;

            var source=tc4revival.research.ResearchPages.bundled().require(link.target().research());

            if(link.target().sourcePage()>=source.size()||!follow(link.target().research(),source.get(link.target().sourcePage()).id()))continue;

            pageSound(); return true;

        }

        for (var view : customViews.values()) if (view.context().bounds().contains(x, y)

                && view.renderer().mouseClicked(view.context(), x, y, button)) { captured = view; return true; }

        if(button==1&&hoveredItem.isEmpty()&&tooltip==null&&inside(x,y,left-40,top-28,336,236)) {

            if(navigation.backLink()){pageSound();resetText();}else onClose();

            return true;

        }

        return super.mouseClicked(x, y, button);

    }

    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {

        if (captured != null && captured.renderer().mouseDragged(captured.context(), x, y, button, dx, dy)) return true;

        return super.mouseDragged(x, y, button, dx, dy);

    }

    @Override public boolean mouseReleased(double x, double y, int button) {

        var view = captured; captured = null;

        if (view != null && view.renderer().mouseReleased(view.context(), x, y, button)) return true;

        return super.mouseReleased(x, y, button);

    }

    @Override public boolean mouseScrolled(double x, double y, double delta) {
        if(scrollableTooltip && delta!=0) {
            tooltipOffset=net.minecraft.util.Mth.clamp(tooltipOffset+(delta>0?-3:3),0,tooltipMaxOffset);return true;
        }

        for (var view : customViews.values()) if (view.context().bounds().contains(x, y)

                && view.renderer().mouseScrolled(view.context(), x, y, delta)) return true;

        if(delta!=0&&navigation.turn(delta>0?-1:1)){pageSound();resetText();return true;}

        return super.mouseScrolled(x, y, delta);

    }

    private boolean sealedResearch(String research) {

        if (navigation.canLocate(research) || !tc4revival.research.ResearchCatalogs.bundled().find(research).map(e->e.category().equals("ELDRITCH")).orElse(false)) return false;

        ClientResearchFeedback.sealedResearch();

        return true;

    }

    private boolean carriesBook(){return minecraft.player!=null&&java.util.stream.Stream.concat(minecraft.player.getInventory().items.stream(),

            minecraft.player.getInventory().offhand.stream()).anyMatch(s->s.getItem() instanceof tc4revival.content.ThaumonomiconItem);}

    public boolean follow(String research) { if (!carriesBook()||sealedResearch(research) || !navigation.open(research, true)) return false; tree.readTutorial(research); resetText(); return true; }

    public boolean follow(String research,String target) {

        if(!carriesBook())return false;

        if(guidance(research))return true;

        if(!navigation.canRead(research))return tree.requestFromLink(research);

        if(!follow(research))return false;

        var pages=navigation.visiblePages();

        if(target.startsWith("aspect/")) {

            String tag=target.substring(7);for(int i=0;i<pages.size();i++)if(pages.get(i).aspects().containsKey(tag)){navigation.goPage(i);return true;}

        }else if(!target.isEmpty()&&flow!=null) {

            var index=flow.anchors().get(target);if(index!=null){navigation.goPage(index);return true;}

        }

        if(!target.isEmpty()){navigation.backLink();resetText();return false;}

        return true;

    }

    private boolean guidance(String key) {

        if (!carriesBook()) return false;

        var research = tc4revival.research.ResearchCatalogs.bundled().find(key);

        if (research.isEmpty() || !tree.guidance(research.get())) return false;

        // A research clue focuses the tree. Keep the authenticated book session alive.

        if (navigation.reading().isEmpty()) {

            returning = true;

            minecraft.setScreen(tree);

        }

        return true;

    }

    @Override public boolean handleComponentClicked(net.minecraft.network.chat.Style style) {

        if(style==null)return false;var click=style.getClickEvent();

        if(hasShiftDown()&&style.getInsertion()!=null&&!style.getInsertion().startsWith("thaumcraft:"))return super.handleComponentClicked(style);

        if(click!=null&&click.getAction()==net.minecraft.network.chat.ClickEvent.Action.CHANGE_PAGE) {

            String value=click.getValue();

            if(value.startsWith(BookComponents.LINK+"aspect/"))return follow("ASPECTS","aspect/"+value.substring((BookComponents.LINK+"aspect/").length()));
            if(value.startsWith(BookComponents.LINK+"locate/")) {
                String key=BookComponents.uncode(value.substring((BookComponents.LINK+"locate/").length()));
                if(!carriesBook() || !navigation.focusExternal(key))return false;
                returning=true;minecraft.setScreen(tree);return true;
            }

            if(value.startsWith(BookComponents.LINK+"research/")) {

                String[] target=value.substring((BookComponents.LINK+"research/").length()).split("#",-1);

                return follow(BookComponents.uncode(target[0]),target.length>1?BookComponents.uncode(target[1]):"");

            }

            if(!carriesBook())return false;

            try{navigation.goPage(Integer.parseInt(value)-1);return true;}catch(NumberFormatException ignored){return false;}

        }

        return super.handleComponentClicked(style);

    }

    @Override public void insertText(String text,boolean overwrite){minecraft.setScreen(new net.minecraft.client.gui.screens.ChatScreen(text));}

    public long cycleMillis() { return Math.max(0, System.currentTimeMillis() - cycleStart); }

    private void pageSound() { if (minecraft.player != null) minecraft.player.playSound(tc4revival.content.TCSounds.PAGE.get(), .66f, 1f); }

    public void structureTooltip(GuiGraphics graphics, Component name, int mouseX, int mouseY) {

        tooltip = () -> graphics.renderTooltip(font, name, mouseX, mouseY);

    }

    public void itemTooltip(GuiGraphics graphics, net.minecraft.world.item.ItemStack stack, int mouseX, int mouseY) {

        hoveredItem=stack.copy();

        tooltip = () -> graphics.renderTooltip(font, stack, mouseX, mouseY);

    }

    public void ingredientTooltip(GuiGraphics graphics, net.minecraft.world.item.ItemStack stack,

            int x, int y, int mouseX, int mouseY) {

        ingredientTooltip(graphics, stack, x, y, 16, mouseX, mouseY);

    }

    public void ingredientTooltip(GuiGraphics graphics, net.minecraft.world.item.ItemStack stack,

            int x, int y, int size, int mouseX, int mouseY) {

        hoveredItem=stack.copy();

        if (recipeLinks == null) recipeLinks = new tc4revival.research.ResearchRecipeLinks(

                tc4revival.research.ResearchPages.bundled(), tc4revival.research.ResearchPageRecipes.bundled());

        var target = recipeLinks.find(output -> tc4revival.research.ResearchDisplayItems.bundled().resolve(output, cycleMillis())

                .map(candidate -> candidate.getItem() == stack.getItem() && candidate.getDamageValue() == stack.getDamageValue())

                .orElse(false), navigation::canRead).filter(link -> !navigation.reading().orElse("").equals(link.research()));

        if (target.isEmpty()) { itemTooltip(graphics, stack, mouseX, mouseY); return; }

        links.add(new RecipeLink(x, y, size, target.get()));

        var lines = new java.util.ArrayList<>(stack.getTooltipLines(minecraft.player,

                minecraft.options.advancedItemTooltips ? net.minecraft.world.item.TooltipFlag.Default.ADVANCED

                        : net.minecraft.world.item.TooltipFlag.Default.NORMAL));

        lines.add(Component.translatable("recipe.clickthrough").withStyle(net.minecraft.ChatFormatting.DARK_GRAY,

                net.minecraft.ChatFormatting.ITALIC));

        tooltip = () -> graphics.renderTooltip(font, lines.stream().map(Component::getVisualOrderText).toList(), mouseX, mouseY);

    }

    public void aspectTooltip(GuiGraphics graphics, tc4revival.aspect.Aspect aspect, int mouseX, int mouseY) {

        tooltip = () -> graphics.renderTooltip(font, java.util.List.of(

                Component.literal(thaumcraft.api.client.ClientAspectViews.view(aspect).displayName()).getVisualOrderText(),

                Component.translatable(thaumcraft.api.client.ClientAspectViews.view(aspect).descriptionKey()).getVisualOrderText()), mouseX, mouseY);

    }

    private void resetText() { closeCustomPages(); flow=null; sourcePages=java.util.List.of(); formattedPages=java.util.List.of(); layoutKnowledge=-1; links.clear(); cycleStart = System.currentTimeMillis(); }

    private static boolean inside(double x, double y, int left, int top, int width, int height) { return x >= left && x < left + width && y >= top && y < top + height; }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {

        if(queryJei(com.mojang.blaze3d.platform.InputConstants.getKey(key,scan)))return true;

        if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_W&&openTutorial(hoveredItem))return true;

        if (minecraft.options.keyInventory.matches(key, scan)) { onClose(); return true; }

        return super.keyPressed(key, scan, modifiers);

    }

    private boolean queryJei(com.mojang.blaze3d.platform.InputConstants.Key key) {

        returning=true;

        if(BookRecipeViewer.show(hoveredResearch,hoveredItem,key))return true;

        returning=false;return false;

    }

    private boolean openTutorial(net.minecraft.world.item.ItemStack stack) {

        if(!thaumcraft.api.client.ResearchBookTutorials.has(stack))return false;

        returning=true;

        boolean opened=thaumcraft.api.client.ResearchBookTutorials.open(stack,this);

        if(!opened)returning=false;

        return opened;

    }

    private boolean openTutorial(ResourceLocation target) {

        returning=true;

        boolean opened=thaumcraft.api.client.ResearchBookTutorials.open(target,this);

        if(!opened)returning=false;

        return opened;

    }

    @Override public void onClose() { returning = true; navigation.showBrowser(); minecraft.setScreen(tree); }

    @Override public void removed() { closeCustomPages();animations.values().forEach(BookImage::close);animations.clear(); ClientBookSession.remember(navigation); if (!returning) ClientBookSession.close(tree.opening()); }

}

