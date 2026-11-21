package tc4revival.compat.jei;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.ingredients.*;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import tc4revival.client.*;
import tc4revival.research.*;

/** Research is an ingredient in its own right, never a disguised ItemStack. */
public class JeiResearch implements BookRecipeViewer.Viewer {
    public record Value(String key) {}
    public static final IIngredientType<Value> TYPE = () -> Value.class;
    public static final RecipeType<JeiResearchRecipe> RECIPES = RecipeType.create("thaumcraft", "research", JeiResearchRecipe.class);
    private static IJeiRuntime runtime;
    private final Map<String, JeiResearchRecipe> registered = new LinkedHashMap<>();
    private List<JeiResearchRecipe> visible = List.of();
    private List<Value> ingredients = List.of();
    private ResearchCatalog catalog;
    private int dataVersion = -1;
    private int aspectVersion = -1;
    private boolean active;
    private long knowledgeRevision = -1;
    private String revelation = "", guidancePolicy = "";

    public void start(IJeiRuntime value) { runtime = value; BookRecipeViewer.viewer = this; }
    public void stop() { BookRecipeViewer.viewer = null; runtime = null; registered.clear(); visible = List.of(); ingredients = List.of(); catalog = null; }
    public void clear() {
        if (runtime != null) {
            if (!visible.isEmpty()) runtime.getRecipeManager().hideRecipes(RECIPES, visible);
            if (!ingredients.isEmpty()) runtime.getIngredientManager().removeIngredientsAtRuntime(TYPE, ingredients);
        }
        visible = List.of(); ingredients = List.of(); catalog = null;
    }
    public void refresh(int version) {
        boolean enabled = BookRecipeViewer.enabled();
        var current = ResearchDefinitions.display();
        if (!enabled || current == null) { if (active || catalog != null) clear(); active = false; return; }
        if (!ClientItemAspects.ready()) return;
        boolean changed = false;
        if (knowledgeRevision != ClientKnowledge.revision()) {
            knowledgeRevision = ClientKnowledge.revision();
            var k = ClientKnowledge.snapshot().orElse(null);
            String next = k == null ? "" : k.researchMarkers().toString() + k.guidance() + k.knownAspects().keySet()
                    + k.scannedKeys(PlayerKnowledge.ScanKind.OBJECT) + k.scannedKeys(PlayerKnowledge.ScanKind.ENTITY);
            changed = !next.equals(revelation); revelation = next;
        }
        String policy = Boolean.toString(ResearchGuidance.enabled());
        if (!policy.equals(guidancePolicy)) { changed = true; guidancePolicy = policy; }
        if (active && !changed && catalog == current.catalog() && dataVersion == version && aspectVersion == ClientItemAspects.version()) return;
        active = true; catalog = current.catalog(); dataVersion = version; aspectVersion = ClientItemAspects.version();
        var definitions = catalog.all().stream().filter(ResearchCatalogs::enabled).filter(r -> !r.mirror()).toList();
        // Knowledge changes reveal recipe conditions, not new research ingredient identities.
        // Re-adding the whole catalog makes JEI sort its ingredient overlay twice per scan.
        var desired = definitions.stream().map(r -> new Value(r.key())).toList();
        var previousIngredients = new HashSet<>(ingredients);
        var desiredIngredients = new HashSet<>(desired);
        var removeIngredients = ingredients.stream().filter(v -> !desiredIngredients.contains(v)).toList();
        var addIngredients = desired.stream().filter(v -> !previousIngredients.contains(v)).toList();
        if (!removeIngredients.isEmpty()) runtime.getIngredientManager().removeIngredientsAtRuntime(TYPE, removeIngredients);
        if (!addIngredients.isEmpty()) runtime.getIngredientManager().addIngredientsAtRuntime(TYPE, addIngredients);
        ingredients = desired;
        var previous = Collections.newSetFromMap(new IdentityHashMap<JeiResearchRecipe, Boolean>());
        previous.addAll(visible);
        var add = new ArrayList<JeiResearchRecipe>();
        var restore = new ArrayList<JeiResearchRecipe>();
        var next = new ArrayList<JeiResearchRecipe>();
        for (var definition : definitions) {
            var recipe = JeiResearchRecipe.create(definition);
            var old = registered.putIfAbsent(recipe.signature(), recipe);
            if (old == null) { add.add(recipe); next.add(recipe); }
            else { next.add(old); if (!previous.remove(old)) restore.add(old); }
        }
        if (!previous.isEmpty()) runtime.getRecipeManager().hideRecipes(RECIPES, new ArrayList<>(previous));
        if (!add.isEmpty()) runtime.getRecipeManager().addRecipes(RECIPES, add);
        if (!restore.isEmpty()) runtime.getRecipeManager().unhideRecipes(RECIPES, restore);
        visible = List.copyOf(next);
    }
    public static Value value(String key) {
        var catalog = ResearchCatalogs.bundled();
        return new Value(catalog.find(key).map(r -> catalog.original(key).key()).orElse(key));
    }
    public static boolean openHovered(net.minecraft.client.gui.screens.Screen screen) {
        if(runtime==null||!BookRecipeViewer.enabled())return false;
        Value hovered=runtime.getIngredientListOverlay().getIngredientUnderMouse(TYPE);
        if(hovered==null)hovered=runtime.getBookmarkOverlay().getIngredientUnderMouse(TYPE);
        if(hovered==null && screen==(Object)runtime.getRecipesGui())hovered=runtime.getRecipesGui().getIngredientUnderMouse(TYPE).orElse(null);
        return hovered!=null && JeiResearchLink.open(hovered.key());
    }
    public static Component name(Value value) { return Component.translatable("tc.research_name." + value.key()); }
    public static Optional<RecipeIngredientRole> keyRole(InputConstants.Key key) {
        if (runtime == null || !BookRecipeViewer.enabled()) return Optional.empty();
        if (runtime.getKeyMappings().getShowRecipe().isActiveAndMatches(key)) return Optional.of(RecipeIngredientRole.OUTPUT);
        if (runtime.getKeyMappings().getShowUses().isActiveAndMatches(key)) return Optional.of(RecipeIngredientRole.INPUT);
        return Optional.empty();
    }
    public static <T> boolean show(IIngredientType<T> type, T value, RecipeIngredientRole role) {
        if (runtime == null || !BookRecipeViewer.enabled()) return false;
        var focus = runtime.getJeiHelpers().getFocusFactory().createFocus(role, type, value);
        var before = Minecraft.getInstance().screen;
        runtime.getRecipesGui().show(focus);
        // A missing recipe must not leave a book session marked as transferring.
        return before != Minecraft.getInstance().screen || !(before instanceof ResearchBrowserScreen || before instanceof ResearchReaderScreen);
    }
    @Override public boolean show(String research, ItemStack item, InputConstants.Key key) {
        // JEI's combined mappings also accept its ordinary left/right recipe clicks.
        // Those buttons belong to the book (reading, purchasing and listening).
        if (key.getType()==InputConstants.Type.MOUSE && (key.getValue()==0 || key.getValue()==1)) return false;
        var role = keyRole(key);
        if (role.isEmpty()) return false;
        if (!item.isEmpty()) return show(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, item, role.get());
        return !research.isEmpty() && show(TYPE, value(research), role.get());
    }
    @Override public Component hint() {
        return Component.translatable("thaumcraft.jei.research_keys", runtime.getKeyMappings().getShowRecipe().getTranslatedKeyMessage(),
                runtime.getKeyMappings().getShowUses().getTranslatedKeyMessage()).withStyle(net.minecraft.ChatFormatting.DARK_GRAY);
    }
    @Override public Optional<Component> modName(String modId) {
        if (runtime == null) return Optional.empty();
        var helper = runtime.getJeiHelpers().getModIdHelper();
        return helper.isDisplayingModNameEnabled()
                ? Optional.of(Component.literal(helper.getFormattedModNameForModId(modId))) : Optional.empty();
    }
    public static Optional<Component> modNameForResearch(String key) {
        var catalog = ResearchCatalogs.bundled();
        return catalog.find(key).flatMap(research -> BookRecipeViewer.modName(catalog.original(key).modId()));
    }
    public static class Helper implements IIngredientHelper<Value> {
        public IIngredientType<Value> getIngredientType() { return TYPE; }
        public String getDisplayName(Value value) { return Component.translatable("thaumcraft.jei.research_name", name(value)).getString(); }
        public String getUniqueId(Value value, UidContext context) { return value.key(); }
        @Override public String getDisplayModId(Value value) {
            var catalog = ResearchCatalogs.bundled();
            return catalog.find(value.key()).map(research -> catalog.original(value.key()).modId())
                    .orElseGet(() -> ResearchOrigins.namespace(value.key()));
        }
        public ResourceLocation getResourceLocation(Value value) {
            // Research keys are case sensitive; lowercasing them would alias different addon entries.
            return ResourceLocation.parse("thaumcraft:research/" + HexFormat.of().formatHex(value.key().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }
        public Value copyIngredient(Value value) { return value; }
        public String getErrorInfo(Value value) { return String.valueOf(value); }
        public boolean isValidIngredient(Value value) { return value != null && ResearchCatalogs.bundled().find(value.key()).isPresent(); }
        public ItemStack getCheatItemStack(Value value) { return ItemStack.EMPTY; }
    }
    public static class Renderer implements IIngredientRenderer<Value> {
        public void render(GuiGraphics graphics, Value value) {
            var research = ResearchCatalogs.bundled().find(value.key()).orElse(null);
            if (research == null) return;
            var display = research.display();
            if (display.iconItem().isPresent()) ResearchDisplayItems.bundled().resolve(display.iconItem().get(), System.currentTimeMillis())
                    .ifPresent(stack -> ResearchMirrorItems.draw(graphics, stack, 0, 0));
            else display.iconResource().ifPresent(texture -> LegacyGui.blit(graphics, ResourceLocation.parse(texture), 0, 0, 0, 0, 16, 16, 16, 16));
        }
        public List<Component> getTooltip(Value value, TooltipFlag flag) {
            return elements(value).stream().flatMap(element -> element.left().stream())
                    .flatMap(text -> ResearchNodeTooltip.textLines(text).stream()).toList();
        }
        @Override public void getTooltip(ITooltipBuilder builder, Value value, TooltipFlag flag) {
            elements(value).forEach(element -> element.ifLeft(text -> builder.addAll(ResearchNodeTooltip.textLines(text))).ifRight(builder::add));
        }
        private List<com.mojang.datafixers.util.Either<net.minecraft.network.chat.FormattedText, net.minecraft.world.inventory.tooltip.TooltipComponent>> elements(Value value) {
            var mc = Minecraft.getInstance();
            // JEI/EMI may build search indices on a worker; player knowledge is client-thread-only.
            if (!mc.isSameThread()) return List.of(com.mojang.datafixers.util.Either.left(name(value)));
            var catalog = ResearchCatalogs.bundled();
            var research = catalog.find(value.key()).orElse(null);
            var knowledge = ClientKnowledge.snapshot().orElse(null);
            if (research == null || knowledge == null) return List.of(com.mojang.datafixers.util.Either.left(name(value)));
            var rules = new ResearchRules(catalog, ResearchCatalogs::enabled);
            boolean complete = rules.completed(knowledge, research.key());
            boolean visible = new ResearchBookActions(tc4revival.aspect.TCAspects.catalog(), catalog, ResearchCatalogs::enabled).visible(knowledge, research);
            boolean ready = visible && rules.prerequisites(knowledge, research.key());
            var node = new ResearchBrowser.Node(research, 0, 0, complete ? ResearchBrowser.State.COMPLETE : ready ? ResearchBrowser.State.AVAILABLE : ResearchBrowser.State.LOCKED,
                    ready, ResearchBrowser.Frame.NORMAL, !visible && !complete, research);
            var hint = ResearchBrowserScreen.hintFor(node).orElse(new ResearchBookHint("", false, List.of()));
            var elements=new ArrayList<>(ResearchNodeTooltip.nativeElements(node, hint, mc.getWindow().getGuiScaledWidth(),false));
            elements.add(com.mojang.datafixers.util.Either.left(Component.translatable("thaumcraft.jei.ctrl_research").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)));
            return List.copyOf(elements);
        }
    }
}
