package tc4revival.compat.jei;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import mezz.jei.api.*;
import mezz.jei.api.ingredients.*;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.*;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import tc4revival.aspect.*;
import tc4revival.client.*;
import tc4revival.content.*;
import tc4revival.network.*;
import tc4revival.research.*;

/** JEI discovers this class only when installed; dedicated/common startup never links it. */
@JeiPlugin
public class ThaumcraftJei implements IModPlugin {
    public record AspectValue(String tag,int amount) {}
    public static final IIngredientType<AspectValue> ASPECTS=()->AspectValue.class;
    public static final Map<String,RecipeType<BrowserRecipe>> TYPES=new LinkedHashMap<>();
    static {for(var kind:List.of("arcane","crucible","infusion","infusion_enchantment","aspect_combine","item_aspects"))TYPES.put(kind,RecipeType.create("thaumcraft",kind,BrowserRecipe.class));}
    private IJeiRuntime runtime;
    private final JeiResearch researchQueries=new JeiResearch();
    private boolean researchEnabled;
    private boolean researchBookClick;
    private int ticks,catalogRequest,dataVersion,appliedDataVersion=-1,appliedDefinitionsVersion=-1,appliedAspectVersion=-1;
    private boolean pendingCatalog;
    private int emiRefreshAt=-1;
    private boolean emiRefreshFailed;
    private BrowserRecipes.Catalog localCatalog;
    private int sentAt;
    private final ClientQueryCache<net.minecraft.nbt.CompoundTag,ItemStack,BrowserRecipe> queries=new ClientQueryCache<>();
    private final ClientQueryCache<net.minecraft.nbt.CompoundTag,ItemStack,BrowserRecipe> dynamicQueries=new ClientQueryCache<>();
    private List<InfusionRecipe> dynamicDefinitions=List.of();
    private ItemStack dynamicStack=ItemStack.EMPTY;
    private int dynamicAt;
    private final Map<String,BrowserRecipe> registered=new LinkedHashMap<>();
    private final Set<BrowserRecipe> visible=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Deque<BrowserRecipe> incoming=new ArrayDeque<>();
    private final Set<String> visibleAspects=new HashSet<>();
    private String projectionRevision="";
    private List<ItemStack> projectedHiddenItems=List.of();
    private void restoreProjectedItems(){
        if(runtime!=null&&!projectedHiddenItems.isEmpty())runtime.getIngredientManager().addIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,projectedHiddenItems);
        projectedHiddenItems=List.of();
    }
    private void refreshProjection(){
        String revision=thaumcraft.api.client.RecipeViewerProjection.revision();
        if(revision.equals(projectionRevision))return;projectionRevision=revision;dataVersion++;
        if(!visibleAspects.isEmpty())runtime.getIngredientManager().removeIngredientsAtRuntime(ASPECTS,visibleAspects.stream().map(a->new AspectValue(a,1)).toList());visibleAspects.clear();
        restoreProjectedItems();
        projectedHiddenItems=runtime.getIngredientManager().getAllIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK).stream()
            .filter(stack->!thaumcraft.api.client.RecipeViewerProjection.item(stack)).map(ItemStack::copy).toList();
        if(!projectedHiddenItems.isEmpty())runtime.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,projectedHiddenItems);
    }
    private Object connectionIdentity;
    private final QueryFailureIsolation<net.minecraft.nbt.CompoundTag> failures=new QueryFailureIsolation<>();
    public ResourceLocation getPluginUid(){return ResourceLocation.parse("thaumcraft:jei");}
    @Override public void registerItemSubtypes(ISubtypeRegistration r){
        // Wand parts, focus upgrades and runic/enchantment outputs carry recipe identity in NBT.
        net.minecraftforge.registries.ForgeRegistries.ITEMS.getEntries().stream()
                .filter(e->e.getKey().location().getNamespace().equals("thaumcraft")
                        || e.getValue() instanceof tc4revival.content.CastingWandItem)
                .forEach(e->r.registerSubtypeInterpreter(e.getValue(),(stack,context)->BrowserQueryStacks.recipeKey(stack).toString()));
    }
    @Override public void registerIngredients(IModIngredientRegistration r){
        r.register(ASPECTS,List.of(),new AspectHelper(),new AspectRenderer());
        r.register(JeiResearch.TYPE,List.of(),new JeiResearch.Helper(),new JeiResearch.Renderer());
    }
    @Override public void registerAdvanced(IAdvancedRegistration r){
        r.addTypedRecipeManagerPlugin(TYPES.get("infusion_enchantment"),new EnchantmentRecipeLookup(()->List.copyOf(visible)));
    }
    @Override public void registerCategories(IRecipeCategoryRegistration r){
        for(var kind:TYPES.keySet())r.addRecipeCategories(new Category(kind,r.getJeiHelpers().getGuiHelper()));
        r.addRecipeCategories(new JeiResearchCategory(r.getJeiHelpers().getGuiHelper()));
        r.addRecipeCategories(new JeiBookCreation(r.getJeiHelpers().getGuiHelper()));
    }
    @Override public void registerRecipes(IRecipeRegistration r) { r.addRecipes(JeiBookCreation.TYPE, List.of(new JeiBookCreation.Page())); }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r){
        r.addRecipeCatalyst(new ItemStack(TCItems.THAUMONOMICON.get()),JeiResearch.RECIPES);
        r.addRecipeCatalyst(new ItemStack(TCArcane.BLOCK.get()),TYPES.get("arcane"));
        r.addRecipeCatalyst(new ItemStack(TCArcane.CRUCIBLE.get()),TYPES.get("crucible"));
        r.addRecipeCatalyst(new ItemStack(TCItems.THAUMOMETER.get()),TYPES.get("item_aspects"));
        r.addRecipeCatalyst(new ItemStack(TCArcane.INFUSION_MATRIX.get()),TYPES.get("infusion"),TYPES.get("infusion_enchantment"));
    }
    @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration r){
        r.addRecipeTransferHandler(new InfusionInventoryTransfer(),TYPES.get("infusion"));
        var info=new mezz.jei.api.recipe.transfer.IRecipeTransferInfo<ArcaneWorkbenchMenu,BrowserRecipe>(){
            public Class<ArcaneWorkbenchMenu> getContainerClass(){return ArcaneWorkbenchMenu.class;}
            public Optional<net.minecraft.world.inventory.MenuType<ArcaneWorkbenchMenu>> getMenuType(){return Optional.of(TCArcane.MENU.get());}
            public RecipeType<BrowserRecipe> getRecipeType(){return TYPES.get("arcane");}
            public boolean canHandle(ArcaneWorkbenchMenu menu,BrowserRecipe recipe){return recipe.inputs().size()==9;}
            public List<net.minecraft.world.inventory.Slot> getRecipeSlots(ArcaneWorkbenchMenu menu,BrowserRecipe recipe){return List.copyOf(menu.slots.subList(2,11));}
            public List<net.minecraft.world.inventory.Slot> getInventorySlots(ArcaneWorkbenchMenu menu,BrowserRecipe recipe){return List.copyOf(menu.slots.subList(11,47));}
        };
        var delegate=r.getTransferHelper().createUnregisteredRecipeTransferHandler(info);
        r.addRecipeTransferHandler(new mezz.jei.api.recipe.transfer.IRecipeTransferHandler<ArcaneWorkbenchMenu,BrowserRecipe>(){
            public Class<ArcaneWorkbenchMenu> getContainerClass(){return ArcaneWorkbenchMenu.class;}
            public Optional<net.minecraft.world.inventory.MenuType<ArcaneWorkbenchMenu>> getMenuType(){return Optional.of(TCArcane.MENU.get());}
            public RecipeType<BrowserRecipe> getRecipeType(){return TYPES.get("arcane");}
            public mezz.jei.api.recipe.transfer.IRecipeTransferError transferRecipe(ArcaneWorkbenchMenu menu,BrowserRecipe recipe,
                    mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.world.entity.player.Player player,boolean max,boolean transfer){
                var items=r.getTransferHelper().createRecipeSlotsView(ArcaneTransferSlots.items(slots,recipe.inputs()));
                return delegate.transferRecipe(menu,recipe,items,player,max,transfer);
            }
        },TYPES.get("arcane"));
    }
    @Override public void onRuntimeAvailable(IJeiRuntime r){runtime=r;JeiInferenceExport.runtime(r);ClientBrowserRecipes.receiver=this::receive;MinecraftForge.EVENT_BUS.register(this);
        researchQueries.start(r);
        tc4revival.client.EditorSearch.search=query->{
            var filter=r.getIngredientFilter();String previous=filter.getFilterText();
            try{filter.setFilterText(query);var result=new java.util.HashSet<net.minecraft.world.item.Item>();filter.getFilteredIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK).forEach(s->result.add(s.getItem()));return result;}
            finally{filter.setFilterText(previous);}
        };
    }
    @Override public void onRuntimeUnavailable(){restoreProjectedItems();projectionRevision="";researchBookClick=false;researchQueries.stop();JeiInferenceExport.runtime(null);tc4revival.client.EditorSearch.search=null;MinecraftForge.EVENT_BUS.unregister(this);ClientBrowserRecipes.receiver=p->{};runtime=null;queries.clear();failures.clear();dynamicQueries.clear();dynamicDefinitions=List.of();dynamicStack=ItemStack.EMPTY;registered.clear();visible.clear();incoming.clear();visibleAspects.clear();connectionIdentity=null;appliedDataVersion=-1;}
    @SubscribeEvent public void reload(net.minecraftforge.client.event.RecipesUpdatedEvent e){dataVersion++;}
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public void researchClick(net.minecraftforge.client.event.ScreenEvent.MouseButtonPressed.Pre event) {
        if(event.getButton()!=0)return;
        researchBookClick=false;
        if(net.minecraft.client.gui.screens.Screen.hasControlDown()&&JeiResearch.openHovered(event.getScreen())) {
            researchBookClick=true;event.setCanceled(true);
        }
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public void researchRelease(net.minecraftforge.client.event.ScreenEvent.MouseButtonReleased.Pre event) {
        if(event.getButton()==0&&researchBookClick){researchBookClick=false;event.setCanceled(true);}
    }
    @SubscribeEvent public void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){
        researchBookClick=false;restoreProjectedItems();projectionRevision="";
        researchQueries.clear();
        if(runtime!=null)hideAll();
        queries.clear();failures.clear();incoming.clear();pendingCatalog=false;connectionIdentity=null;
        dynamicQueries.clear();dynamicStack=ItemStack.EMPTY;dynamicDefinitions=List.of();
    }
    private void hideAll(){
        for(var type:TYPES.values())runtime.getRecipeManager().hideRecipes(type,visible.stream().filter(v->TYPES.get(v.kind())==type).toList());
        visible.clear();
        scheduleEmiRefresh();
    }
    private boolean isolated(ItemStack stack,Runnable action){
        // Do not invoke addon serialization/name/tooltip callbacks just to identify an error.
        var identity=new net.minecraft.nbt.CompoundTag();
        identity.putString("id",String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem())));
        if(stack.hasTag())identity.put("tag",stack.getTag().copy());
        return failures.run(identity,action,(key,error)->tc4revival.TC4Revival.LOGGER.warn(
                "Skipping failed JEI item query for {} until data reload/reconnect",key.getString("id"),error));
    }
    private void offer(ItemStack stack,boolean priority){
        if(stack.isEmpty())return;
        isolated(stack,()->offerChecked(stack,priority));
    }
    private void offerChecked(ItemStack stack,boolean priority){
        var key=ClientItemAspects.key(stack);
        if(key.sizeInBytes()>262144)return;
        queries.offerLazy(presentationKey(stack,key),()->stack.copyWithCount(1),priority);
        // Only equipment can have the remaining dynamic infusion upgrades.
        // Keep this independent of aspect keys: runic state affects recipes.
        if(!dynamicDefinitions.isEmpty()&&(stack.isDamageableItem()||stack.getItem() instanceof RunicGear)
                &&stack.hasTag()&&BrowserQueryStacks.eligible(stack))
            dynamicQueries.offerLazy(BrowserQueryStacks.key(stack),()->stack.copyWithCount(1),priority);
    }
    private net.minecraft.nbt.CompoundTag presentationKey(ItemStack stack,net.minecraft.nbt.CompoundTag aspects){
        // JEI may distinguish variants with equal aspects (e.g. a wand's attached focus).
        // Such variants need a display row, but share the same calculation table entry.
        var key=new net.minecraft.nbt.CompoundTag();key.put("aspects",aspects);
        key.putString("subtype",runtime.getIngredientManager().getIngredientHelper(mezz.jei.api.constants.VanillaTypes.ITEM_STACK).getUniqueId(stack,UidContext.Recipe));
        return key;
    }
    private void refresh(Minecraft mc){
        researchEnabled=BookRecipeViewer.enabled();
        appliedDataVersion=dataVersion;appliedDefinitionsVersion=ClientBrowserRecipes.dataVersion;appliedAspectVersion=ClientItemAspects.version();
        connectionIdentity=mc.getConnection();catalogRequest++;sentAt=ticks;
        hideAll();incoming.clear();queries.clear();failures.clear();pendingCatalog=true;
        dynamicQueries.clear();dynamicStack=ItemStack.EMPTY;
        dynamicDefinitions=BrowserRecipes.dynamicDefinitions(mc.level);
        updateAspects();
        runtime.getIngredientManager().getAllIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK).forEach(s->offer(s,false));
        localCatalog=BrowserRecipes.catalog(mc.level,ClientItemAspects.calculation().identity());
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||runtime==null)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getConnection()==null)return;ticks++;
        if(ticks%2!=0)return;
        refreshProjection();
        researchQueries.refresh(dataVersion);
        if(!ClientItemAspects.ready()){if(!visible.isEmpty())hideAll();return;}
        // Inventory changes never invalidate a catalog or the completed/queued stack cache.
        // A new connection and server data synchronization are the only full invalidations.
        if(connectionIdentity!=mc.getConnection()||appliedDataVersion!=dataVersion||appliedDefinitionsVersion!=ClientBrowserRecipes.dataVersion||appliedAspectVersion!=ClientItemAspects.version()||researchEnabled!=BookRecipeViewer.enabled())refresh(mc);
        if(ticks%10==0){
            var seen=Collections.newSetFromMap(new IdentityHashMap<ItemStack,Boolean>());
            var inventory=mc.player.getInventory();
            for(int slot=0;slot<inventory.getContainerSize();slot++){var stack=inventory.getItem(slot);if(seen.add(stack))offer(stack,true);}
            if(seen.add(mc.player.containerMenu.getCarried()))offer(mc.player.containerMenu.getCarried(),true);
            for(var slot:mc.player.containerMenu.slots)if(seen.add(slot.getItem()))offer(slot.getItem(),true);
        }
        // JEI runtime registration itself rebuilds indices: coalesce rows instead of doing it 10x/s.
        if(ticks%10==0)flushIncoming();
        refreshEmiWhenReady();
        if(pendingCatalog){
            if(incoming.size()<512)incoming.addAll(localCatalog.drain(32,2_000_000L));
            if(localCatalog.done()){pendingCatalog=false;localCatalog=null;}
            return;
        }
        long started=System.nanoTime();
        for(int i=0;i<32&&incoming.size()<512;i++){
            var batch=queries.begin(1);if(batch.values().isEmpty())break;
            var stack=batch.values().get(0);
            var aspectKey=new net.minecraft.nbt.CompoundTag[1];
            if(!isolated(stack,()->{
                var key=ClientItemAspects.key(stack);aspectKey[0]=key;
                // Validate third-party subtype callbacks before committing an aspect result.
                var displayKey=presentationKey(stack,key);
                var value=ClientItemAspects.resolve(stack,key).orElseThrow();
                var row=BrowserRecipes.item(stack,value.identity(),value.amounts());
                queries.complete(batch.id(),List.of(row),r->displayKey);
                incoming.add(row);
            })){
                queries.discard(batch.id());
                if(aspectKey[0]!=null)ClientItemAspects.discard(aspectKey[0]);
            }
            if(System.nanoTime()-started>=2_000_000L)break;
        }
        // Do not run the entire dynamic recipe set as one unbudgeted stack operation.
        for(int i=0;i<32&&incoming.size()<512&&System.nanoTime()-started<2_000_000L;i++){
            if(dynamicStack.isEmpty()){
                var batch=dynamicQueries.begin(1);if(batch.values().isEmpty())break;
                dynamicStack=batch.values().get(0);dynamicAt=0;
            }
            if(!isolated(dynamicStack,()->incoming.addAll(BrowserRecipes.dynamicItem(mc.level,dynamicStack,dynamicDefinitions.get(dynamicAt++))))){
                dynamicQueries.discard(dynamicQueries.current().id());dynamicStack=ItemStack.EMPTY;
                continue;
            }
            if(dynamicAt==dynamicDefinitions.size()){
                dynamicQueries.complete(dynamicQueries.current().id(),List.of(),r->null);dynamicStack=ItemStack.EMPTY;
            }
        }
    }
    private void updateAspects(){
        var desired=new HashSet<String>();for(var a:TCAspects.catalog().all())if(thaumcraft.api.client.RecipeViewerProjection.aspect(a.tag()))desired.add(a.tag());
        var remove=visibleAspects.stream().filter(a->!desired.contains(a)).map(a->new AspectValue(a,1)).toList();
        var add=desired.stream().filter(a->!visibleAspects.contains(a)).map(a->new AspectValue(a,1)).toList();
        if(!remove.isEmpty())runtime.getIngredientManager().removeIngredientsAtRuntime(ASPECTS,remove);
        if(!add.isEmpty())runtime.getIngredientManager().addIngredientsAtRuntime(ASPECTS,add);
        visibleAspects.clear();visibleAspects.addAll(desired);
    }
    private static String signature(BrowserRecipe r){var s=new StringBuilder(r.kind()).append(r.id()).append(r.research()).append(r.aspectInputs()).append(r.aspectOutputs()).append(r.instability()).append('/').append(r.xp()).append(r.dynamic()).append(BookRecipeViewer.enabled());
        for(var g:r.inputs()){s.append('|');for(var i:g)s.append(i.save(new net.minecraft.nbt.CompoundTag()));}return s.append(r.output().save(new net.minecraft.nbt.CompoundTag())).toString();}
    private void receive(BrowserRecipesPacket p){
        if(runtime==null||!p.catalog()||!pendingCatalog||p.request()!=catalogRequest)return;
        incoming.addAll(p.recipes());sentAt=ticks;
        if(p.last())pendingCatalog=false;
    }
    private void flushIncoming(){
        var additions=new HashMap<String,List<BrowserRecipe>>();var restored=new HashMap<String,List<BrowserRecipe>>();
        for(int i=0;i<128&&!incoming.isEmpty();i++){
            var projected=thaumcraft.api.client.RecipeViewerProjection.recipe(incoming.removeFirst());if(projected.isEmpty())continue;var row=projected.get();
            var key=signature(row);var old=registered.get(key);var type=TYPES.get(row.kind());
            if(old==null){registered.put(key,row);additions.computeIfAbsent(row.kind(),k->new ArrayList<>()).add(row);visible.add(row);}
            else if(!visible.contains(old)){restored.computeIfAbsent(row.kind(),k->new ArrayList<>()).add(old);visible.add(old);}
        }
        additions.forEach((kind,rows)->runtime.getRecipeManager().addRecipes(TYPES.get(kind),rows));
        restored.forEach((kind,rows)->runtime.getRecipeManager().unhideRecipes(TYPES.get(kind),rows));
        if(additions.keySet().stream().anyMatch(kind->!kind.equals("item_aspects"))
                ||restored.keySet().stream().anyMatch(kind->!kind.equals("item_aspects")))scheduleEmiRefresh();
    }
    private void scheduleEmiRefresh(){
        if(!emiRefreshFailed&&net.minecraftforge.fml.ModList.get().isLoaded("emi"))emiRefreshAt=ticks+20;
    }
    private void refreshEmiWhenReady(){
        if(pendingCatalog||emiRefreshAt<0||ticks<emiRefreshAt)return;
        try{
            // EMI's JEI bridge takes a snapshot before our login-time catalog is ready.
            // Coalesce recipe batches; per-stack aspect lookups must not reload the index.
            var manager=Class.forName("dev.emi.emi.runtime.EmiReloadManager");
            if(!Boolean.TRUE.equals(manager.getMethod("isLoaded").invoke(null)))return;
            emiRefreshAt=-1;
            manager.getMethod("reload").invoke(null);
        }catch(ReflectiveOperationException|LinkageError error){
            emiRefreshAt=-1;emiRefreshFailed=true;
            tc4revival.TC4Revival.LOGGER.warn("Unable to refresh EMI after JEI recipe catalog synchronization",error);
        }
    }
    public static class AspectHelper implements IIngredientHelper<AspectValue>{
        public IIngredientType<AspectValue> getIngredientType(){return ASPECTS;}
        public String getDisplayName(AspectValue a){return Component.translatable("thaumcraft.jei.aspect_name",thaumcraft.api.client.ClientAspectViews.view(TCAspects.catalog().require(a.tag())).displayName()).getString()+" "+Component.translatable(thaumcraft.api.client.ClientAspectViews.view(TCAspects.catalog().require(a.tag())).descriptionKey()).getString();}
        public String getUniqueId(AspectValue a,UidContext context){return a.tag();}
        public ResourceLocation getResourceLocation(AspectValue a){return thaumcraft.api.client.ClientAspectViews.view(TCAspects.catalog().require(a.tag())).identity();}
        public AspectValue copyIngredient(AspectValue a){return a;}
        public long getAmount(AspectValue a){return thaumcraft.api.client.ClientAspectViews.visible(a.tag())?a.amount():0;}
        public AspectValue normalizeIngredient(AspectValue a){return new AspectValue(a.tag(),1);}
        public String getErrorInfo(AspectValue a){return String.valueOf(a);}
        public boolean isValidIngredient(AspectValue a){return a!=null&&a.amount()>0&&TCAspects.catalog().find(a.tag()).isPresent();}
        public ItemStack getCheatItemStack(AspectValue a){return ItemStack.EMPTY;}
    }
    public static class AspectRenderer implements IIngredientRenderer<AspectValue>{
        private boolean known(AspectValue a){if(!thaumcraft.api.client.ClientAspectViews.visible(a.tag()))return false;var mc=Minecraft.getInstance();return ClientKnowledge.knownAspectTags().contains(a.tag())||(mc.isSameThread()&&mc.player!=null&&mc.player.isCreative());}
        public void render(GuiGraphics g,AspectValue a){
            if(!thaumcraft.api.client.ClientAspectViews.visible(a.tag())){AspectTooltips.draw(g,Minecraft.getInstance().font,new AspectTooltips.Cell("",""),0,0);return;}
            boolean known=known(a);AspectTooltips.draw(g,Minecraft.getInstance().font,new AspectTooltips.Cell(known?a.tag():"",""),0,0); var font=Minecraft.getInstance().font; String count=Integer.toString(a.amount()); g.pose().pushPose(); g.pose().translate(0,0,200); g.drawString(font,count,17-font.width(count),9,0xFFFFFF,true); g.pose().popPose();}
        public List<Component> getTooltip(AspectValue a,TooltipFlag flag){if(!known(a))return List.of(Component.literal("?"));var aspect=TCAspects.catalog().require(a.tag());return List.of(Component.translatable("thaumcraft.jei.aspect_name",thaumcraft.api.client.ClientAspectViews.view(aspect).displayName()),Component.translatable(thaumcraft.api.client.ClientAspectViews.view(aspect).descriptionKey()),Component.translatable("thaumcraft.jei.amount",a.amount()));}
    }
    public static class Category implements mezz.jei.api.recipe.category.IRecipeCategory<BrowserRecipe>{
        private final String kind;private final mezz.jei.api.gui.drawable.IDrawable background,icon;
        public Category(String kind,mezz.jei.api.helpers.IGuiHelper gui){this.kind=kind;background=gui.createBlankDrawable(JeiRecipeLayout.width(kind),JeiRecipeLayout.height(kind));icon=gui.createDrawableItemStack(JeiResearchLink.categoryIcon(kind));}
        public RecipeType<BrowserRecipe> getRecipeType(){return TYPES.get(kind);}
        public Component getTitle(){return Component.translatable("thaumcraft.jei."+kind);}
        public mezz.jei.api.gui.drawable.IDrawable getBackground(){return background;}
        public mezz.jei.api.gui.drawable.IDrawable getIcon(){return icon;}
        public void setRecipe(mezz.jei.api.gui.builder.IRecipeLayoutBuilder b,BrowserRecipe r,IFocusGroup focus){
            if(BookRecipeViewer.enabled()&&!r.research().isEmpty()&&ResearchCatalogs.bundled().find(r.research()).isPresent()) {
                var pos=JeiRecipeLayout.book(kind);
                b.addSlot(RecipeIngredientRole.INPUT,pos.x(),pos.y()).setSlotName("research")
                        .addIngredient(JeiResearch.TYPE,JeiResearch.value(r.research()))
                        .addRichTooltipCallback((slot,tooltip)->{
                            tooltip.addAll(JeiResearchLink.details(r));
                        });
            }
            for(int i=0;i<r.inputs().size();i++){
                var pos=JeiRecipeLayout.item(kind,i,r.inputs().size());
                b.addSlot(RecipeIngredientRole.INPUT,pos.x(),pos.y()).setSlotName(
                    JeiRecipeLayout.infusion(kind)&&i>0?"components/"+i:"items/"+i).addItemStacks(r.inputs().get(i));
            }
            if(!r.output().isEmpty()) {
                var output=JeiRecipeLayout.output(kind);
                b.addSlot(RecipeIngredientRole.OUTPUT,output.x(),output.y()).addItemStack(r.output());
                var books=JeiResearchLink.enchantmentBooks(r);
                if(!books.isEmpty())b.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(books);
            }
            int i=0;for(var a:new TreeMap<>(r.aspectInputs()).entrySet()){
                var pos=JeiRecipeLayout.cost(kind,i++,r.aspectInputs().size());
                b.addSlot(RecipeIngredientRole.INPUT,pos.x(),pos.y()).setSlotName("cost/"+(i-1)).addIngredient(ASPECTS,new AspectValue(a.getKey(),a.getValue()));
            }
            i=0;for(var a:new TreeMap<>(r.aspectOutputs()).entrySet()){
                b.addSlot(RecipeIngredientRole.OUTPUT,kind.equals("aspect_combine")?132:54,24).setSlotName("aspects/"+i++).addIngredient(ASPECTS,new AspectValue(a.getKey(),a.getValue()));
            }
        }
        @Override public void createRecipeExtras(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder b,BrowserRecipe r,IFocusGroup focus){
            if(BookRecipeViewer.enabled()&&!r.research().isEmpty()) {
                var pos=JeiRecipeLayout.book(kind);
                b.addInputHandler(new mezz.jei.api.gui.inputs.IJeiInputHandler() {
                    public net.minecraft.client.gui.navigation.ScreenRectangle getArea() {
                        return new net.minecraft.client.gui.navigation.ScreenRectangle(pos.x(),pos.y(),18,18);
                    }
                    public boolean handleInput(double x,double y,mezz.jei.api.gui.inputs.IJeiUserInput input) {
                        var key=input.getKey();
                        if(key.getType()!=com.mojang.blaze3d.platform.InputConstants.Type.MOUSE||key.getValue()!=0
                                ||((input.getModifiers()&org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL)==0
                                &&!net.minecraft.client.gui.screens.Screen.hasControlDown()))return false;
                        // Claim both halves of this click before JEI's normal ingredient lookup.
                        return input.isSimulate()||JeiResearchLink.open(r);
                    }
                });
            }
            if(JeiRecipeLayout.infusion(kind)&&JeiRecipeLayout.scrollComponents(r.inputs().size()))grid(b,"components/",6,2,8,66);
            if(r.aspectInputs().size()>JeiRecipeLayout.costCapacity(kind))
                grid(b,"cost/",kind.equals("crucible")?3:kind.equals("aspect_combine")?4:8,kind.equals("aspect_combine")?1:2,kind.equals("crucible")?28:8,JeiRecipeLayout.costY(kind));
            if(kind.equals("item_aspects"))grid(b,"aspects/",6,5,54,24);
            else if(r.aspectOutputs().size()>1)grid(b,"aspects/",1,2,132,24);
        }
        private static void grid(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder b,String prefix,int columns,int rows,int x,int y){
            var slots=b.getRecipeSlots().getSlots().stream().filter(s->s.getSlotName().orElse("").startsWith(prefix)).toList();
            if(!slots.isEmpty())b.addScrollGridWidget(slots,columns,rows).setPosition(x,y);
        }
        public void draw(BrowserRecipe r,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,GuiGraphics g,double mouseX,double mouseY){
            var font=Minecraft.getInstance().font;
            JeiRecipeArtwork.draw(g,kind);
            if(!BookRecipeViewer.enabled())JeiResearchLink.draw(g,r);
            if(kind.equals("infusion_enchantment")){
                var levels=r.id().substring(r.id().lastIndexOf("/levels/")+8).split("/");
                g.drawString(font,Component.translatable("thaumcraft.jei.enchantment_levels",levels[0],levels[1]),8,0,0x555555,false);
            }
            if(kind.equals("item_aspects")&&r.aspectOutputs().isEmpty())g.drawWordWrap(font,Component.translatable(r.dynamic()?"thaumcraft.jei.unknown":"thaumcraft.jei.empty"),52,24,115,0x555555);
        }
        @Override public List<Component> getTooltipStrings(BrowserRecipe r,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,double x,double y) {
            if(!BookRecipeViewer.enabled()&&JeiRecipeLayout.overBook(kind,x,y))return JeiResearchLink.tooltip(r);
            if(kind.equals("infusion_enchantment")&&y<12)return JeiResearchLink.details(r);
            return List.of();
        }
        @Override public boolean handleInput(BrowserRecipe r,double x,double y,com.mojang.blaze3d.platform.InputConstants.Key key) {
            if(key.getType()!=com.mojang.blaze3d.platform.InputConstants.Type.MOUSE||key.getValue()!=0||!JeiRecipeLayout.overBook(kind,x,y))return false;
            if(BookRecipeViewer.enabled())return false;
            return JeiResearchLink.open(r);
        }
    }
}
