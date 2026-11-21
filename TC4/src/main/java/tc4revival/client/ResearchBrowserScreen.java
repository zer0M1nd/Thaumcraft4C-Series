package tc4revival.client;

import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tc4revival.config.TCConfig;
import tc4revival.research.*;

/** The real tree screen. Its reader factory must preserve this opening and navigation on page transitions. */
public  class ResearchBrowserScreen extends Screen {
    @FunctionalInterface public interface ReaderFactory { Screen create(ResearchBrowserScreen tree, ResearchBookNavigation navigation); }
    private final UUID opening;
    private final ResearchBookNavigation navigation;
    private final ReaderFactory reader;
    private boolean transferring, dragging;
    private int left, top, categoryPage;
    private ResearchBrowserLayout layout=ResearchBrowserLayout.NORMAL;
    private String tabSelection="";
    private String requested;
    private String suggestedTutorial = "";
    private record SuggestedLink(String key, int x, int y) {
        boolean hit(double mx,double my) { return mx>=x && mx<x+18 && my>=y && my<y+18; }
    }
    private final java.util.List<SuggestedLink> suggestionLinks = new java.util.ArrayList<>();
    private net.minecraft.client.gui.components.EditBox search;
    private java.util.List<ResearchBrowser.Node> candidates=java.util.List.of();
    private int selected;
    private boolean sealedSearch;
    private static final int SEARCH_WIDTH=100, SEARCH_ROW=18;
    private Component popup;
    private long popupUntil;
    public ResearchBrowserScreen(UUID opening, ResearchBookNavigation navigation, ReaderFactory reader) {
        super(Component.translatable("item.thaumcraft.thaumonomicon"));
        this.opening = java.util.Objects.requireNonNull(opening);
        this.navigation = java.util.Objects.requireNonNull(navigation);
        this.reader = java.util.Objects.requireNonNull(reader);
    }
    public void locateRecipe(String key) {
        refresh();
        if(!navigation.focusExternal(key))return;
        readTutorial(key);
        if(navigation.open(key,false)){transferring=true;minecraft.setScreen(reader.create(this,navigation));}
    }
    public UUID opening() { return opening; }
    @Override protected void init() {
        layout=ResearchBrowserLayout.of(width,height,TCConfig.SERVER.revival.bookSize.get() && TCConfig.CLIENT.maximumResearchBrowser.get());
        left=(width-layout.width())/2;top=(height-layout.height())/2;transferring=false;dragging=false;
        navigation.viewport(layout.viewportWidth(),layout.viewportHeight());tabSelection="";refresh();
        if(searchEnabled()) {
            String previous=search==null?"":search.getValue();
            search=new net.minecraft.client.gui.components.EditBox(font,left+20,top+5,SEARCH_WIDTH-8,10,Component.translatable("thaumcraft.research.search"));
            search.setBordered(false);search.setTextColor(0xffffffff);search.setHint(Component.translatable("thaumcraft.research.search"));search.setMaxLength(80);
            search.setResponder(value->{selected=0;refreshSearch();});search.setValue(previous);addRenderableWidget(search);
        }else search=null;
    }
    /** Addons which originally provided book search can retain it independently of the core addition. */
    protected boolean searchEnabled() { return thaumcraft.api.research.ResearchPresentation.searchEnabled(TCConfig.SERVER.revival.bookSearch.get()); }
    private boolean overSizeButton(double x,double y){return TCConfig.SERVER.revival.bookSize.get() && x>=left+layout.width()-66&&x<left+layout.width()-20&&y>=top+2&&y<top+16;}
    private String shortSearchLabel(Component label) {
        String value=label.getString();int available=SEARCH_WIDTH-8;
        return font.width(value)<=available?value:font.plainSubstrByWidth(value,available-font.width("…"))+"…";
    }
    private boolean overSearchResults(double x,double y) {
        return search!=null && search.isFocused() && !search.getValue().isBlank()
                && x>=search.getX()-4 && x<search.getX()-4+SEARCH_WIDTH
                && y>=search.getY()+12 && y<search.getY()+12+Math.max(1,candidates.size())*SEARCH_ROW;
    }
    private void refreshSearch() {
        if(search==null||search.getValue().isBlank()){candidates=java.util.List.of();sealedSearch=false;return;}
        String query=search.getValue().strip().toLowerCase(java.util.Locale.ROOT);
        var seen=new java.util.HashSet<String>();
        candidates=navigation.searchable().stream().filter(node->ClientTextSearch.contains(Component.translatable("tc.research_name."+node.original().key()).getString(),query)||ClientTextSearch.contains(node.original().key(),query))
                .filter(node->seen.add(node.original().key()))
                .sorted(java.util.Comparator.comparing(node->Component.translatable("tc.research_name."+node.original().key()).getString())).limit(8).toList();
        sealedSearch=candidates.isEmpty() && !ClientKnowledge.displaySnapshot().map(k->k.researchMarkers().contains("ELDRITCHMINOR")).orElse(false)
                && ResearchCatalogs.bundled().all().stream().anyMatch(e->e.category().equals("ELDRITCH") &&
                    (ClientTextSearch.contains(e.key(),query)||ClientTextSearch.contains(Component.translatable("tc.research_name."+e.key()).getString(),query)));
        selected=Math.min(selected,Math.max(0,candidates.size()-1));
        if(sealedSearch) ClientResearchFeedback.sealedResearch();
    }
    private void chooseSearch(int index) {
        if(index<0||index>=candidates.size())return;var node=candidates.get(index);var entry=node.original();
        if(entry.category().equals("ELDRITCH")&&!ClientKnowledge.displaySnapshot().map(k->k.researchMarkers().contains("ELDRITCHMINOR")).orElse(false)) {
            ClientResearchFeedback.sealedResearch();return;
        }
        if(!navigation.focusExternal(entry.key()))return;search.setFocused(false);search.setValue("");
        if(node.research().mirror())return;
        readTutorial(entry.key());
        if(navigation.openSearch(entry.key())){transferring=true;minecraft.setScreen(java.util.Objects.requireNonNull(reader.create(this,navigation)));}
        else { popup=Component.translatable("thaumcraft.research.located",Component.translatable("tc.research_name."+entry.key()));popupUntil=System.currentTimeMillis()+2500; }
    }
    private boolean obfuscate(ResearchBrowser.Node node,ResearchBrowser.View view) {
        return switch(TCConfig.CLIENT.obfuscation()) {
            case NEVER -> false;
            case ALWAYS, DEFAULT -> true;
            case DISTANCE -> view.nodes().stream().filter(other->other.state()==ResearchBrowser.State.AVAILABLE)
                    .mapToDouble(other->Math.hypot(node.x()-other.x(),node.y()-other.y())/24d).min().orElse(Double.POSITIVE_INFINITY)>TCConfig.CLIENT.researchObfuscationDistance.get();
        };
    }
    void refresh() {
        ClientKnowledge.displaySnapshot().ifPresent(knowledge -> {
            var discovered = new java.util.LinkedHashMap<String, Integer>();
            knowledge.knownAspects().forEach((aspect, amount) -> discovered.put(aspect.tag(), amount));
            navigation.refresh(knowledge, discovered, TCConfig.SERVER.researchDifficulty.get());
        });
    }
    @Override public void tick() {
        ClientBookSession.takeResult(opening).ifPresent(result -> {
            if (result.status() == tc4revival.network.BookResultPacket.Status.NOTE_CREATED && requested != null) {
                popup = Component.translatable("tc.research.popup", Component.translatable("tc.research_name." + requested));
                popupUntil = System.currentTimeMillis() + 3000;
            }
            requested = null;
        });
        if (!ClientBookSession.active(opening)) onClose(); else {refresh();refreshSearch();}
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        navigation.view().ifPresent(view -> {
            ResearchBrowserArtwork.draw(graphics, view, visibleCategories(), left, top, navigation.panX(), navigation.panY(),
                    (minecraft.player == null ? 0 : minecraft.player.tickCount) + partialTick, System.currentTimeMillis(), java.util.Set.of(),
                    (g, original, x, y, locked) -> ResearchDisplayItems.bundled().resolve(original, System.currentTimeMillis())
                            .ifPresent(stack -> ResearchMirrorItems.draw(g,stack,x,y)),layout);
            drawSuggestions(graphics,view,mouseX,mouseY);
            view.hit(mouseX - left - 16, mouseY - top - 17, navigation.panX(), navigation.panY()).ifPresent(node -> {
                if(!overSearchResults(mouseX,mouseY) && suggestionLinks.stream().noneMatch(link -> link.hit(mouseX,mouseY)))hint(node).ifPresent(value -> ResearchNodeTooltip.draw(graphics, font, node, value,
                        mouseX, mouseY, width, height, System.currentTimeMillis(),obfuscate(node,view)));
            });
        });
        if (popup != null && System.currentTimeMillis() < popupUntil) {
            var lines = font.split(popup, 150); int centerY = top + layout.height()/2+13, half = lines.size() * font.lineHeight / 2;
            graphics.fill(left + layout.width()/2-78, centerY - half - 3, left + layout.width()/2+78, centerY + half + 3, 0xC0000000);
            for (int i = 0; i < lines.size(); i++) graphics.drawString(font, lines.get(i), left + layout.width()/2-75,
                    centerY - half + i * font.lineHeight, -7302913, false);
        }
        if(search!=null)graphics.fill(search.getX()-4,search.getY()-3,search.getX()-4+SEARCH_WIDTH,search.getY()+11,0xC0000000);
        super.render(graphics, mouseX, mouseY, partialTick);
        if(TCConfig.SERVER.revival.bookSize.get()) {
            ResearchWidgets.smallButton(graphics,left+layout.width()-66,top+2,46,14,overSizeButton(mouseX,mouseY));
            graphics.drawCenteredString(font,Component.translatable(TCConfig.CLIENT.maximumResearchBrowser.get()?"thaumcraft.research.size.maximum":"thaumcraft.research.size.normal"),left+layout.width()-43,top+5,0xfff3e4bb);
        }
        if(search!=null&&search.isFocused()&&!search.getValue().isBlank()) {
            graphics.pose().pushPose();graphics.pose().translate(0,0,500);int x=search.getX()-4,y=search.getY()+12;
            if(candidates.isEmpty()){graphics.fill(x,y,x+SEARCH_WIDTH,y+SEARCH_ROW,0xC0000000);graphics.drawString(font,shortSearchLabel(Component.translatable(sealedSearch?"thaumcraft.research.eldritch_sealed":"thaumcraft.research.no_results")),x+4,y+5,0xffaaaaaa,false);}
            for(int i=0;i<candidates.size();i++) {
                boolean hover=mouseX>=x&&mouseX<x+SEARCH_WIDTH&&mouseY>=y+i*SEARCH_ROW&&mouseY<y+(i+1)*SEARCH_ROW;
                graphics.fill(x,y+i*SEARCH_ROW,x+SEARCH_WIDTH,y+(i+1)*SEARCH_ROW,hover||i==selected?0xD0403048:0xC0000000);
                graphics.drawString(font,shortSearchLabel(Component.translatable("tc.research_name."+candidates.get(i).original().key())),x+4,y+i*SEARCH_ROW+5,0xffffffff,false);
            }
            graphics.pose().popPose();
        }
        var categories = visibleCategories();
        if(displayCategories().size()>layout.tabsPerPage())graphics.drawString(font,(categoryPage+1)+" / "+((displayCategories().size()+layout.tabsPerPage()-1)/layout.tabsPerPage()),left+layout.width()/2-16,top+layout.height()-12,0xffeeeeee,false);
        for (int index = 0; index < categories.size(); index++) {
            int tx = left + layout.tabX(index), ty = top + layout.tabY(index);
            if (mouseX >= tx && mouseX < tx + 24 && mouseY >= ty && mouseY < ty + 24) {
                var category = categories.get(index);
                var lines = new java.util.ArrayList<Component>();
                lines.add(Component.translatable("tc.research_category." + category.key())
                    .append(categories.get(index).key().equals("ELDRITCH") && minecraft.player.getAbilities().instabuild
                        && ClientKnowledge.displaySnapshot().map(k -> !k.researchMarkers().contains("ELDRITCHMINOR")).orElse(false)
                        ? Component.translatable("thaumcraft.research.creative_unlock") : Component.empty()));
                BookRecipeViewer.modName(category.modId()).ifPresent(lines::add);
                graphics.renderComponentTooltip(font,lines.stream().flatMap(line->ResearchNodeTooltip.textLines(line).stream()).toList(),mouseX,mouseY);
            }
        }
    }
    private List<ResearchCatalog.Category> displayCategories() {
        var list=new java.util.ArrayList<>(navigation.categories());
        if(minecraft.player!=null && minecraft.player.getAbilities().instabuild && list.stream().noneMatch(c->c.key().equals("ELDRITCH")))
            ResearchCatalogs.bundled().categories().stream().filter(c->c.key().equals("ELDRITCH")).findFirst().ifPresent(list::add);
        return list;
    }
    private void drawSuggestions(GuiGraphics g,ResearchBrowser.View view,int mx,int my) {
        var hovered=view.hit(mx-left-16,my-top-17,navigation.panX(),navigation.panY());
        if(hovered.filter(node -> node.original().tutorial()).isPresent())
            suggestedTutorial=hovered.get().original().key();
        else if(suggestionLinks.stream().noneMatch(link -> link.hit(mx,my))) {
            // Keep the fan open while crossing the gap between its source and an icon.
            var previous=view.nodes().stream().filter(node -> node.research().key().equals(suggestedTutorial)).findFirst().orElse(null);
            double sx=previous==null?Double.NaN:left+16+previous.x()-Math.floor(view.bounds().clampX(navigation.panX()))+11;
            double sy=previous==null?Double.NaN:top+17+previous.y()-Math.floor(view.bounds().clampY(navigation.panY()))+11;
            if(previous==null || Math.hypot(mx-sx,my-sy)>72)suggestedTutorial="";
        }
        suggestionLinks.clear();
        var source=view.nodes().stream().filter(node -> node.research().key().equals(suggestedTutorial)).findFirst().orElse(null);
        if(source==null)return;
        var keys=source.original().suggestedResearch().stream().filter(key -> ResearchCatalogs.bundled().find(key)
                .filter(ResearchCatalogs::enabled).isPresent()).toList();
        int sx=left+16+source.x()-(int)Math.floor(view.bounds().clampX(navigation.panX()))+11;
        int sy=top+17+source.y()-(int)Math.floor(view.bounds().clampY(navigation.panY()))+11;
        int direction=sx+70<left+layout.width()-16?1:-1;
        g.enableScissor(left+16,top+17,left+layout.width()-16,top+layout.height()-17);
        for(int i=0;i<keys.size();i++) {
            double angle=keys.size()==1?0:-1.05+2.1*i/(keys.size()-1);
            int x=sx+direction*(int)Math.round(52*Math.cos(angle))-8;
            int y=sy+(int)Math.round(52*Math.sin(angle))-8;
            y=Math.max(top+19,Math.min(top+layout.height()-35,y));
            ResearchBrowserArtwork.dashed(g,sx,sy,x+8,y+8,0xffc49b48);
            g.fill(x-1,y-1,x+17,y+17,0xc02c231b);
            ResearchLinkIcons.draw(g,keys.get(i),x,y,16);
            suggestionLinks.add(new SuggestedLink(keys.get(i),x-1,y-1));
        }
        g.disableScissor();
        for(var link:suggestionLinks)if(link.hit(mx,my)) {
            var name=BookComponents.hidden(link.key())?Component.literal("???"):Component.translatable("tc.research_name."+link.key());
            g.renderComponentTooltip(font,List.of(name,Component.translatable("thaumcraft.research.mirror_jump")),mx,my);
        }
    }
    private java.util.List<tc4revival.research.ResearchCatalog.Category> visibleCategories() {
        var all=displayCategories();
        if(!navigation.category().equals(tabSelection)) {
            for(int index=0;index<all.size();index++)if(all.get(index).key().equals(navigation.category()))categoryPage=index/layout.tabsPerPage();
            tabSelection=navigation.category();
        }
        categoryPage=Math.max(0,Math.min(categoryPage,Math.max(0,(all.size()-1)/layout.tabsPerPage())));
        return all.subList(categoryPage*layout.tabsPerPage(),Math.min(all.size(),categoryPage*layout.tabsPerPage()+layout.tabsPerPage()));
    }
    @Override public boolean mouseScrolled(double x,double y,double delta) {
        if(displayCategories().size()>layout.tabsPerPage() && y>=top && y<top+layout.rows()*24 && (x>=left-24&&x<left || x>=left+layout.width()&&x<left+layout.width()+24)) {
            categoryPage=Math.floorMod(categoryPage+(delta<0?1:-1),(displayCategories().size()+layout.tabsPerPage()-1)/layout.tabsPerPage());return true;
        }
        return super.mouseScrolled(x,y,delta);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (!ClientBookSession.active(opening)) return false;
        if(button==0)for(var link:suggestionLinks)if(link.hit(x,y)) {
            dragging=false;suggestedTutorial="";navigation.focusExternal(link.key());return true;
        }
        if(queryJei(x,y,com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(button)))return true;
        if(button==0&&overSizeButton(x,y)){TCConfig.CLIENT.maximumResearchBrowser.set(!TCConfig.CLIENT.maximumResearchBrowser.get());TCConfig.CLIENT_SPEC.save();rebuildWidgets();return true;}
        if(search!=null) {
            if(overSearchResults(x,y)) {dragging=false;chooseSearch((int)(y-search.getY()-12)/SEARCH_ROW);return true;}
            if(super.mouseClicked(x,y,button)) {dragging=false;return true;}
            search.setFocused(false);
        }
        dragging = button == 0 && x >= left + 16 && x < left + layout.width()-16 && y >= top + 17 && y < top + layout.height()-17;
        var hit = navigation.view().flatMap(v -> v.hit(x - left - 16, y - top - 17, navigation.panX(), navigation.panY()));
        if (hit.isPresent()) {
            var node = hit.orElseThrow();
            if(node.research().mirror()) {
                if(button==0) {
                    dragging=false;
                    if(navigation.canLocate(node.original().key()))navigation.focusExternal(node.original().key());
                    else ClientResearchFeedback.sealedResearch();
                }
                return true;
            }
            if (button == 0 && guidance(node.original())) return true;
            if(button==2 && node.state()!=ResearchBrowser.State.COMPLETE && ClientBookSession.creative(opening,node.research().key()))return true;
            if(button!=0)return true;
            if (node.original().tutorial()) readTutorial(node.research().key());
            if ((node.original().tutorial() || node.state() == ResearchBrowser.State.COMPLETE) && navigation.open(node.research().key(), false)) {
                var next = java.util.Objects.requireNonNull(reader.create(this, navigation));
                transferring = true; minecraft.setScreen(next);
            } else if (node.state() == ResearchBrowser.State.AVAILABLE && hint(node).map(ResearchBookHint::requestable).orElse(false)
                    && ClientBookSession.request(opening, node.research().key())) {
                requested = node.research().key(); popupUntil = 0;
            }
            return true;
        }
        var categories = visibleCategories();
        for (int index = 0; index < categories.size(); index++) {
            int tx = left + layout.tabX(index), ty = top + layout.tabY(index);
            if (x >= tx && x < tx + 24 && y >= ty && y < ty + 24) {
                var key=categories.get(index).key();
                if(button==2 && key.equals("ELDRITCH") && minecraft.player.getAbilities().instabuild)
                    ClientBookSession.creative(opening,"ELDRITCHMINOR");
                else if(button==0)navigation.selectCategory(key);
                return true;
            }
        }
        dragging = button == 0 && x >= left + 16 && x < left + layout.width()-16 && y >= top + 17 && y < top + layout.height()-17;
        return dragging || super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (dragging && button == 0) { navigation.drag(dx, dy); return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    public boolean requestFromLink(String key) {
        var node=navigation.searchable().stream().filter(n->n.research().key().equals(key)).findFirst();
        if(node.isEmpty()||node.get().state()!=ResearchBrowser.State.AVAILABLE||!hint(node.get()).map(ResearchBookHint::requestable).orElse(false))return false;
        if(!ClientBookSession.request(opening,key))return false;
        requested=key;popupUntil=0;return true;
    }
    /** Opening a locked tutorial is read-only; eligible clicks use the authenticated server intent. */
    public void readTutorial(String key) {
        if (ResearchCatalogs.bundled().find(key).map(ResearchDefinition::tutorial).orElse(false)) requestFromLink(key);
    }
    /** Shared by tree nodes and research links in the reader. */
    boolean guidance(ResearchDefinition research) {
        if (!ClientBookSession.active(opening)) return false;
        var guidance = ClientResearchGuidance.view(research);
        if (guidance.lines().isEmpty()) return false;
        dragging = false;
        if (guidance.current() != null) {
            var condition = guidance.current().requirement();
            if (condition.kind() == ResearchRequirements.Kind.RESEARCH
                    && !navigation.focusExternal(ResearchRequirements.canonical(condition.key())))
                ClientResearchFeedback.sealedResearch();
        } else ClientBookSession.listen(opening, research.key());
        return true;
    }
    java.util.Optional<ResearchBookHint> hint(ResearchBrowser.Node node) {
        return hintFor(node);
    }
    public static java.util.Optional<ResearchBookHint> hintFor(ResearchBrowser.Node node) {
        var minecraft=net.minecraft.client.Minecraft.getInstance();
        if (minecraft.player == null) return java.util.Optional.empty();
        boolean paper = false, ink = false, existing = false;
        var inventory = new java.util.ArrayList<>(minecraft.player.getInventory().items);
        inventory.addAll(minecraft.player.getInventory().offhand);
        for (var stack : inventory) {
            if (stack.isEmpty()) continue;
            paper |= stack.is(net.minecraft.world.item.Items.PAPER);
            ink |= tc4revival.content.ScribingToolsItem.ink(stack) > 0;
            if (stack.getItem() instanceof tc4revival.content.ResearchNotesItem)
                existing |= ResearchNoteNbt.read(stack.getTag(), tc4revival.aspect.TCAspects.catalog()).note()
                        .filter(note -> note.key().equals(node.research().key())).isPresent();
        }
        final boolean hasPaper = paper, hasInk = ink, hasNote = existing;
        return ClientKnowledge.displaySnapshot().map(knowledge -> ResearchBookHint.describe(node, TCConfig.SERVER.researchDifficulty.get(),
                knowledge, tc4revival.aspect.TCAspects.catalog(), hasPaper, hasInk, hasNote));
    }
    @Override public boolean mouseReleased(double x, double y, int button) { dragging = false; return super.mouseReleased(x, y, button); }
    private boolean queryJei(double x,double y,com.mojang.blaze3d.platform.InputConstants.Key key) {
        if(search!=null&&search.isFocused())return false;
        var node=navigation.view().flatMap(v->v.hit(x-left-16,y-top-17,navigation.panX(),navigation.panY()));
        if(node.isEmpty())return false;
        transferring=true;
        if(BookRecipeViewer.show(node.get().original().key(),net.minecraft.world.item.ItemStack.EMPTY,key))return true;
        transferring=false;return false;
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        double x=minecraft.mouseHandler.xpos()*width/minecraft.getWindow().getScreenWidth();
        double y=minecraft.mouseHandler.ypos()*height/minecraft.getWindow().getScreenHeight();
        if(queryJei(x,y,com.mojang.blaze3d.platform.InputConstants.getKey(key,scan)))return true;
        if(search!=null&&search.isFocused()) {
            if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER||key==org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER){chooseSearch(selected);return true;}
            if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN){selected=Math.min(candidates.size()-1,selected+1);return true;}
            if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_UP){selected=Math.max(0,selected-1);return true;}
            if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE){search.setFocused(false);return true;}
            return search.keyPressed(key,scan,modifiers);
        }
        if (minecraft.options.keyInventory.matches(key, scan)) { onClose(); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public void onClose() { ClientBookSession.close(opening); super.onClose(); }
    @Override public void removed() { ClientBookSession.remember(navigation); if (!transferring) ClientBookSession.close(opening); }
}
