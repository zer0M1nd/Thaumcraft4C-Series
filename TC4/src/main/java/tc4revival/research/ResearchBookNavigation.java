package tc4revival.research;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Navigation shared by the tree and reader, without owning or changing player knowledge. */
public  class ResearchBookNavigation {
    private record Visit(String research, int spread) {}
    private final ResearchBrowser browser;
    private final ResearchPages pages;
    private final ArrayDeque<Visit> history = new ArrayDeque<>();
    private PlayerKnowledge knowledge;
    private Map<String, Integer> discovered = Map.of();
    private int difficulty, spread;
    private String category = "TUTORIAL", reading;
    private boolean searchPreview;
    private java.util.function.Function<List<ResearchPage>,List<ResearchPage>> pageLayout=java.util.function.Function.identity();
    /** Client font layout may repeat a source page for overflow; source objects keep link identity. */
    public void pageLayout(java.util.function.Function<List<ResearchPage>,List<ResearchPage>> layout) {
        pageLayout=layout==null?java.util.function.Function.identity():layout;
    }
    private double panX = -85, panY = -112;
    private int viewportWidth=224,viewportHeight=196;
    public void viewport(int width,int height){
        panX-=(width-viewportWidth)/2d;panY-=(height-viewportHeight)/2d;
        viewportWidth=width;viewportHeight=height;browser.viewport(width,height);clampPan();
    }
    public ResearchBookNavigation(ResearchBrowser browser, ResearchPages pages) {
        this.browser = java.util.Objects.requireNonNull(browser); this.pages = java.util.Objects.requireNonNull(pages);
    }
    public void refresh(PlayerKnowledge knowledge, Map<String, Integer> discovered, int difficulty) {
        if (difficulty < -1 || difficulty > 1) throw new IllegalArgumentException("Research difficulty");
        this.knowledge = knowledge.copy(); this.discovered = Map.copyOf(discovered); this.difficulty = difficulty;
        var categories = categories();
        if (categories.stream().noneMatch(c -> c.key().equals(category))) category = categories.isEmpty() ? "" : categories.get(0).key();
        if (reading != null && !canRead(reading) && (!searchPreview || searchable().stream().noneMatch(n -> n.research().key().equals(reading)))) showBrowser();
        clampPan(); clampSpread();
    }
    public List<ResearchCatalog.Category> categories() { return knowledge == null ? List.of() : browser.categories(knowledge); }
    public Optional<ResearchBrowser.View> view() { return knowledge == null ? Optional.empty() : browser.view(knowledge, category, difficulty); }
    public String category() { return category; }
    public double panX() { return panX; }
    public double panY() { return panY; }
    public record Location(String category,double x,double y) {}
    public Location location(){return new Location(category,panX,panY);}
    public boolean restoreLocation(Location location){
        if(location==null||!Double.isFinite(location.x())||!Double.isFinite(location.y())||!selectCategory(location.category()))return false;
        panX=location.x();panY=location.y();showBrowser();clampPan();return true;
    }
    public boolean selectCategory(String key) {
        if (categories().stream().noneMatch(c -> c.key().equals(key))) return false;
        category = key; showBrowser(); clampPan(); return true;
    }
    public List<ResearchBrowser.Node> searchable() {
        if(knowledge==null)return List.of();
        return thaumcraft.api.research.ResearchPresentation.search(category(),categories().stream().flatMap(c->browser.view(knowledge,c.key(),difficulty).stream()).flatMap(v->v.nodes().stream()).toList());
    }
    public boolean focusExternal(String key) { return browser.canLocate(knowledge,key) && browser.revealLocation(key) && focus(key); }
    public boolean focus(String key) {
        if(!browser.canLocate(knowledge,key))return false;
        var selected=searchable().stream().filter(node->node.research().key().equals(key)).findFirst();if(selected.isEmpty())return false;
        var node=selected.get();if(!selectCategory(node.research().category()))return false;
        panX=node.x()+11-viewportWidth/2d;panY=node.y()+11-viewportHeight/2d;clampPan();return true;
    }
    public void drag(double dx, double dy) {
        if (!Double.isFinite(dx) || !Double.isFinite(dy)) return;
        panX -= dx; panY -= dy; clampPan();
    }
    private void clampPan() { view().ifPresent(v -> { panX = v.bounds().clampX(panX); panY = v.bounds().clampY(panY); }); }
    public boolean canLocate(String key) { return browser.canLocate(knowledge,key); }
    public boolean open(String key, boolean link) {
        if (!canLocate(key)) return false;
        if (!canRead(key) || !pages.entries().containsKey(key)) return false;
        searchPreview = false;
        if (link && reading != null) history.push(new Visit(reading, spread)); else history.clear();
        reading = key; spread = 0; return true;
    }
    public Optional<String> reading() { return Optional.ofNullable(reading); }
    public boolean openSearch(String key) {
        if(!browser.canLocate(knowledge,key))return false;
        if (open(key, false)) return true;
        if (searchable().stream().noneMatch(n -> n.research().key().equals(key))) return false;
        reading = key; spread = 0; history.clear(); searchPreview = true; return true;
    }
    public boolean canRead(String key) { return complete(key) || knowledge != null && browser.tutorial(key); }
    public boolean follow(ResearchRecipeLinks.Target target) {
        if (!canRead(target.research()) || !pages.entries().containsKey(target.research())) return false;
        var source = pages.require(target.research());
        if (target.sourcePage() < 0 || target.sourcePage() >= source.size()) return false;
        if (!open(target.research(), true)) return false;
        int displayed=visiblePages().indexOf(source.get(target.sourcePage()));
        spread = Math.max(0,displayed) / 2 * 2;
        clampSpread();
        return true;
    }
    public List<ResearchPage> visiblePages() {
        if (reading != null && searchPreview && !canRead(reading)) return pageLayout.apply(List.of(new ResearchPage(
                ResearchPage.Type.TEXT, Optional.of("thaumcraft.research.locked_preview"), Optional.empty(), Optional.empty(),
                Map.of(), Optional.empty(), new ResearchPage.Empty()).withId("thaumcraft:locked_preview")));
        return reading == null ? List.of() : pageLayout.apply(pages.visible(reading, this::complete, discovered));
    }
    public int spread() { return spread; }
    public void goPage(int page) { spread=Math.max(0,page)/2*2;clampSpread(); }
    public boolean hasHistory() { return !history.isEmpty(); }
    public boolean turn(int direction) {
        if (reading == null || direction == 0) return false;
        int before = spread;
        spread = (int) Math.max(0L, Math.min(lastSpread(), (long) spread + Integer.signum(direction) * 2L));
        return spread != before;
    }
    public boolean backLink() {
        while (!history.isEmpty()) {
            var previous = history.pop();
            if (!canRead(previous.research())) continue;
            reading = previous.research(); spread = previous.spread(); clampSpread(); return true;
        }
        return false;
    }
    public void showBrowser() { reading = null; spread = 0; history.clear(); searchPreview = false; }
    private boolean complete(String key) { return knowledge != null && knowledge.researchMarkers().contains(key); }
    private int lastSpread() { return Math.max(0, visiblePages().size() - 1) / 2 * 2; }
    private void clampSpread() { spread = Math.min(spread, lastSpread()); }
}
