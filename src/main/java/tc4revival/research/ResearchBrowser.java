package tc4revival.research;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import tc4revival.research.ResearchDefinition.Flag;

/** Pure display projection. Clicks still require the server's book transaction checks. */
public  class ResearchBrowser {
    public enum State { COMPLETE, AVAILABLE, LOCKED }
    public enum Frame {
        NORMAL(0), ROUND(54), HIDDEN(86), PURCHASE(110), HIDDEN_PURCHASE(230);
        private final int u;
        Frame(int u) { this.u = u; }
        public int u() { return u; }
    }
    public enum LinkKind { PARENT, SIBLING }
    public enum LinkState { COMPLETE, TARGET_COMPLETE, INCOMPLETE }
    public record Node(ResearchDefinition research, int x, int y, State state, boolean prerequisites, Frame frame, boolean forcedHidden,
            ResearchDefinition original) {
        public Node(ResearchDefinition research,int x,int y,State state,boolean prerequisites,Frame frame,boolean forcedHidden) {
            this(research,x,y,state,prerequisites,frame,forcedHidden,research);
        }
        public Node(ResearchDefinition research, int x, int y, State state, boolean prerequisites, Frame frame) {
            this(research, x, y, state, prerequisites, frame, false);
        }
        /** Completion survives newly added or revoked prerequisites; this never grants research. */
        public boolean iconLocked() { return state != State.COMPLETE && !prerequisites; }
    }
    public record Link(String source, String target, int x, int y, int targetX, int targetY,
                       LinkKind kind, LinkState state) {}
    public record Bounds(int minX, int minY, int maxXExclusive, int maxYExclusive) {
        // A category narrower than the viewport remains centered rather than clamping below its minimum.
        public double clampX(double x) { return maxXExclusive<=minX?(minX+maxXExclusive-1)/2.0:Math.min(maxXExclusive-1,Math.max(minX,x)); }
        public double clampY(double y) { return maxYExclusive<=minY?(minY+maxYExclusive-1)/2.0:Math.min(maxYExclusive-1,Math.max(minY,y)); }
    }
    public record View(ResearchCatalog.Category category, Bounds bounds, List<Node> nodes, List<Link> links,int viewportWidth,int viewportHeight) {
        public View { nodes = List.copyOf(nodes); links = List.copyOf(links); }
        public View(ResearchCatalog.Category category,Bounds bounds,List<Node> nodes,List<Link> links){this(category,bounds,nodes,links,224,196);}
        /** Coordinates relative to the 224 by 196 inner viewport; pan uses research-grid pixels. */
        public Optional<Node> hit(double mouseX, double mouseY, double panX, double panY) {
            if (mouseX < 0 || mouseY < 0 || mouseX >= viewportWidth || mouseY >= viewportHeight) return Optional.empty();
            int offsetX = (int) Math.floor(bounds.clampX(panX)), offsetY = (int) Math.floor(bounds.clampY(panY));
            Node found = null;
            for (var node : nodes) {
                int x = node.x() - offsetX, y = node.y() - offsetY;
                if (mouseX >= x && mouseX <= x + 22 && mouseY >= y && mouseY <= y + 22) found = node;
            }
            return Optional.ofNullable(found);
        }
    }
    private String located="";
    private int viewportWidth=224,viewportHeight=196;
    public void viewport(int width,int height){viewportWidth=Math.max(1,width);viewportHeight=Math.max(1,height);}
    public boolean revealLocation(String key) { if(catalog.find(key).filter(enabled).filter(e->enabled.test(catalog.original(key))).isEmpty())return false;located=key;return true; }
    public boolean canLocate(PlayerKnowledge knowledge,String key) { return knowledge!=null && catalog.find(key).filter(enabled)
            .filter(entry->enabled.test(catalog.original(key)))
            .filter(entry->!catalog.original(key).category().equals("ELDRITCH") || complete(knowledge,"ELDRITCHMINOR")).isPresent(); }
    public boolean tutorial(String key) { return catalog.find(key).filter(enabled).map(ResearchDefinition::tutorial).orElse(false); }
    private boolean located(ResearchDefinition entry) { return entry.key().equals(located); }
    private final ResearchCatalog catalog;
    private final Predicate<ResearchDefinition> enabled;
    private final java.util.function.BooleanSupplier creative;
    public ResearchBrowser(ResearchCatalog catalog, Predicate<ResearchDefinition> enabled) {
        this(catalog, enabled, () -> false);
    }
    public ResearchBrowser(ResearchCatalog catalog, Predicate<ResearchDefinition> enabled, java.util.function.BooleanSupplier creative) {
        this.catalog = java.util.Objects.requireNonNull(catalog);
        this.enabled = java.util.Objects.requireNonNull(enabled);
        this.creative = java.util.Objects.requireNonNull(creative);
    }
    public List<ResearchCatalog.Category> categories(PlayerKnowledge knowledge) {
        return catalog.categories().stream().filter(category -> creative.getAsBoolean() || !category.key().equals("ELDRITCH")
                || complete(knowledge, "ELDRITCHMINOR") || category.research().stream().anyMatch(this::located)).toList();
    }
    public Optional<View> view(PlayerKnowledge knowledge, String categoryKey, int difficulty) {
        if (difficulty < -1 || difficulty > 1) throw new IllegalArgumentException("Invalid research difficulty");
        return categories(knowledge).stream().filter(category -> category.key().equals(categoryKey)).findFirst()
                .map(category -> project(knowledge, category, difficulty));
    }
    private View project(PlayerKnowledge knowledge, ResearchCatalog.Category category, int difficulty) {
        var nodes = new ArrayList<Node>(); var links = new ArrayList<Link>();
        int minColumn = 0, maxColumn = 0, minRow = 0, maxRow = 0;
        int spacing=spacing(category.key());
        for (var entry : category.research()) if (enabled.test(entry) && enabled.test(catalog.original(entry.key()))) {
            var original=catalog.original(entry.key());
            int column = entry.display().column(), row = entry.display().row();
            minColumn = Math.min(minColumn, column); maxColumn = Math.max(maxColumn, column);
            minRow = Math.min(minRow, row); maxRow = Math.max(maxRow, row);
            boolean done = complete(knowledge, original.key()), unlocked = prerequisites(knowledge, original);
            boolean visible = original.tutorial() && enabled.test(entry) || (!category.key().equals("ELDRITCH") || complete(knowledge, "ELDRITCHMINOR"))
                    && enabled.test(entry) && !entry.has(Flag.VIRTUAL) && (done || clue(knowledge, entry) || !entry.has(Flag.LOST)
                    && !entry.has(Flag.HIDDEN) && (!entry.has(Flag.CONCEALED) || revealed(knowledge,entry)));
            if (visible || located(entry) || creative.getAsBoolean() && !entry.has(Flag.VIRTUAL)) {
                nodes.add(new Node(entry, column * spacing, row * spacing, done ? State.COMPLETE : !visible ? State.LOCKED : unlocked ? State.AVAILABLE : State.LOCKED,
                        unlocked && visible, frame(original, difficulty), !visible, original));
            }
            for (var parent : entry.parents()) addLink(knowledge, entry, parent, LinkKind.PARENT, links);
            for (var parent : entry.parentsAny()) addLink(knowledge, entry, parent, LinkKind.PARENT, links);
            for (var sibling : entry.siblings()) addLink(knowledge, entry, sibling, LinkKind.SIBLING, links);
        }
        int extraX=(viewportWidth-224)/2,extraY=(viewportHeight-196)/2;
        return new View(category, new Bounds(minColumn * spacing - 85-extraX, minRow * spacing - 112-extraY,
                maxColumn * spacing - 112-extraX, maxRow * spacing - 61-extraY), nodes, links,viewportWidth,viewportHeight);
    }
    private static int spacing(String category) { return category.equals("TUTORIAL") ? 12 : 24; }
    private void addLink(PlayerKnowledge knowledge, ResearchDefinition source, String targetKey, LinkKind kind, List<Link> links) {
        if (source.category().equals("ELDRITCH") && !complete(knowledge, "ELDRITCHMINOR")) return;
        var target = catalog.find(source.reference().parentMirrors().getOrDefault(targetKey,targetKey)).filter(enabled).orElse(null);
        if (target == null || !enabled.test(catalog.original(target.key())) || !target.category().equals(source.category()) || target.has(Flag.VIRTUAL)
                || kind == LinkKind.SIBLING && target.parents().contains(source.key())) return;
        LinkState state;
        if (complete(knowledge, source.key())) state = LinkState.COMPLETE;
        else {
            // Legacy links deliberately differ from node visibility: a lost clue reveals its node, but not its outgoing links.
            if (source.has(Flag.LOST) || source.has(Flag.HIDDEN) && !clue(knowledge, source)
                    || source.has(Flag.CONCEALED) && !revealed(knowledge, source)) return;
            if (complete(knowledge, target.key())) state = LinkState.TARGET_COMPLETE;
            else {
                if (target.has(Flag.HIDDEN) && !clue(knowledge, target)
                        || target.has(Flag.CONCEALED) && !revealed(knowledge, target)) return;
                state = LinkState.INCOMPLETE;
            }
        }
        int spacing=spacing(source.category());
        links.add(new Link(source.key(), target.key(), source.display().column() * spacing + 11, source.display().row() * spacing + 11,
                target.display().column() * spacing + 11, target.display().row() * spacing + 11, kind, state));
    }
    /** The same link rules as the rendered tree, including explicit cross-page mirrors. */
    public List<Link> prerequisiteLinks(PlayerKnowledge knowledge, ResearchDefinition source) {
        var links = new ArrayList<Link>();
        if (!enabled.test(source)) return List.of();
        for (String parent : source.parents()) addLink(knowledge, source, parent, LinkKind.PARENT, links);
        for (String parent : source.parentsAny()) addLink(knowledge, source, parent, LinkKind.PARENT, links);
        return List.copyOf(links);
    }
    private boolean complete(PlayerKnowledge knowledge, String key) {
        return knowledge.researchMarkers().contains(catalog.find(key).map(e->catalog.original(key).key()).orElse(key));
    }
    private boolean clue(PlayerKnowledge knowledge, ResearchDefinition entry) { return complete(knowledge, "@" + entry.key()); }
    private boolean revealed(PlayerKnowledge knowledge,ResearchDefinition entry) {
        return thaumcraft.api.research.ResearchCategories.revealed(entry.key(),key->complete(knowledge,key),prerequisites(knowledge,entry));
    }
    private boolean prerequisites(PlayerKnowledge knowledge, ResearchDefinition entry) {
        if (complete(knowledge, entry.key())) return true;
        if (entry.tutorial()) return new ResearchRules(catalog,enabled).prerequisites(knowledge,entry.key());
        // The old browser ignores absent parent definitions. This is a display rule, not purchase authority.
        return Stream.concat(entry.parents().stream(), entry.hiddenParents().stream())
                .filter(key -> catalog.find(key).filter(enabled).isPresent()).allMatch(key -> complete(knowledge, key))
                && (entry.parentsAny().isEmpty()||entry.parentsAny().stream().anyMatch(key->complete(knowledge,key)));
    }
    private static Frame frame(ResearchDefinition entry, int difficulty) {
        if (entry.has(Flag.ROUND)) return Frame.ROUND;
        // Frame choice depends on difficulty/secondary, even when an entry has no aspect cost.
        boolean purchase = difficulty == -1 || difficulty == 0 && entry.has(Flag.SECONDARY);
        return entry.has(Flag.HIDDEN) ? purchase ? Frame.HIDDEN_PURCHASE : Frame.HIDDEN
                : purchase ? Frame.PURCHASE : Frame.NORMAL;
    }
}
