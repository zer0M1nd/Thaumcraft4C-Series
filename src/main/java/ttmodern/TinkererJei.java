package ttmodern;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import mezz.jei.api.*;
import mezz.jei.api.ingredients.*;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.*;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;

/** Client-only JEI discovery; no common registration path links this class. */
@JeiPlugin
public final class TinkererJei implements IModPlugin {
    public record MobDisplay(ResourceLocation entity){}
    public static final IIngredientType<MobDisplay> MOBS=()->MobDisplay.class;
    public static final RecipeType<MobEssenceCatalog.Recipe> SUMMONING=RecipeType.create(Tinkerer.ID,"summoning",MobEssenceCatalog.Recipe.class);
    public static final RecipeType<MobEssenceCatalog.Recipe> EXTRACTION=RecipeType.create(Tinkerer.ID,"soul_extraction",MobEssenceCatalog.Recipe.class);
    public record CropPage(CropDropPreview.Recipe recipe,int page){}
    public static final RecipeType<CropPage> CROPS=RecipeType.create(Tinkerer.ID,"crop_drops",CropPage.class);
    private static List<CropPage> cropPages=List.of();
    private static List<CropPage> pages(){return SummoningClient.crops().stream().flatMap(recipe->java.util.stream.IntStream.range(0,Math.max(1,(recipe.drops().size()+23)/24)).mapToObj(page->new CropPage(recipe,page))).toList();}
    public static void refreshCrops(){if(runtime==null)return;if(!cropPages.isEmpty())runtime.getRecipeManager().hideRecipes(CROPS,cropPages);cropPages=pages();if(cropPages.isEmpty())return;runtime.getRecipeManager().addRecipes(CROPS,cropPages);runtime.getRecipeManager().unhideRecipes(CROPS,cropPages);}
    private static IJeiRuntime runtime;
    private static List<MobEssenceCatalog.Recipe> shown=List.of();
    private static List<MobDisplay> ingredients=List.of();
    @Override public ResourceLocation getPluginUid(){return ResourceLocation.fromNamespaceAndPath(Tinkerer.ID,"main");}
    @Override public void registerItemSubtypes(ISubtypeRegistration registration){registration.registerSubtypeInterpreter(Tinkerer.INFUSED_SEEDS.get(),(stack,context)->InfusedSeedItem.aspect(stack));}
    @Override public void registerIngredients(IModIngredientRegistration registration){ingredients=SummoningClient.recipes().stream().map(recipe->new MobDisplay(recipe.entity())).distinct().toList();registration.register(MOBS,ingredients,new MobHelper(),new MobRenderer());}
    @Override public void registerCategories(IRecipeCategoryRegistration registration){registration.addRecipeCategories(new SummoningCategory(registration.getJeiHelpers().getGuiHelper(),false),new SummoningCategory(registration.getJeiHelpers().getGuiHelper(),true),new CropCategory(registration.getJeiHelpers().getGuiHelper()));}
    @Override public void registerRecipes(IRecipeRegistration registration){shown=SummoningClient.recipes();registration.addRecipes(SUMMONING,shown);registration.addRecipes(EXTRACTION,shown);cropPages=pages();registration.addRecipes(CROPS,cropPages);}
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration){registration.addRecipeCatalyst(new ItemStack(Tinkerer.SUMMONING_TABLET.get()),SUMMONING);registration.addRecipeCatalyst(new ItemStack(Tinkerer.BLOOD_SWORD.get()),EXTRACTION);}
    @Override public void onRuntimeAvailable(IJeiRuntime value){runtime=value;refresh();refreshCrops();}
    @Override public void onRuntimeUnavailable(){runtime=null;cropPages=List.of();shown=List.of();ingredients=List.of();MobRenderer.clear();}
    public static void refresh(){if(runtime==null)return;var next=SummoningClient.recipes();if(next.equals(shown))return;if(!shown.isEmpty()){runtime.getRecipeManager().hideRecipes(SUMMONING,shown);runtime.getRecipeManager().hideRecipes(EXTRACTION,shown);}if(!ingredients.isEmpty())runtime.getIngredientManager().removeIngredientsAtRuntime(MOBS,ingredients);shown=next;ingredients=next.stream().map(recipe->new MobDisplay(recipe.entity())).distinct().toList();if(!ingredients.isEmpty())runtime.getIngredientManager().addIngredientsAtRuntime(MOBS,ingredients);if(next.isEmpty())return;runtime.getRecipeManager().addRecipes(SUMMONING,next);runtime.getRecipeManager().unhideRecipes(SUMMONING,next);runtime.getRecipeManager().addRecipes(EXTRACTION,next);runtime.getRecipeManager().unhideRecipes(EXTRACTION,next);}
    public static void clear(){runtime=null;shown=List.of();ingredients=List.of();cropPages=List.of();MobRenderer.clear();}
    private static Component name(MobDisplay mob){var type=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(mob.entity());return type==null?Component.literal(mob.entity().toString()):type.getDescription();}
    public static final class MobHelper implements IIngredientHelper<MobDisplay> {
        public IIngredientType<MobDisplay> getIngredientType(){return MOBS;}
        public String getDisplayName(MobDisplay mob){return name(mob).getString();}
        public String getUniqueId(MobDisplay mob,UidContext context){return mob.entity().toString();}
        public ResourceLocation getResourceLocation(MobDisplay mob){return mob.entity();}
        public MobDisplay copyIngredient(MobDisplay mob){return mob;}
        public String getErrorInfo(MobDisplay mob){return String.valueOf(mob);}
        @Override public boolean isValidIngredient(MobDisplay mob){return mob!=null&&net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.containsKey(mob.entity());}
        @Override public ItemStack getCheatItemStack(MobDisplay mob){return ItemStack.EMPTY;}
    }
    public static final class MobRenderer implements IIngredientRenderer<MobDisplay> {
        private static final Map<ResourceLocation,net.minecraft.world.entity.LivingEntity> models=new HashMap<>();
        private static net.minecraft.world.level.Level world;
        static void clear(){models.clear();world=null;}
        public void render(GuiGraphics graphics,MobDisplay mob){var level=Minecraft.getInstance().level;if(level==null)return;if(world!=level){clear();world=level;}var type=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(mob.entity());if(type==null)return;var entity=models.computeIfAbsent(mob.entity(),id->{var created=type.create(level);return created instanceof net.minecraft.world.entity.LivingEntity living?living:null;});if(entity==null)return;int scale=Math.max(1,(int)Math.min(13/Math.max(.5f,entity.getBbHeight()),13/Math.max(.5f,entity.getBbWidth())));net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,8,15,scale,0,0,entity);}
        public List<Component> getTooltip(MobDisplay mob,TooltipFlag flag){var lines=new ArrayList<Component>();lines.add(name(mob));lines.add(Component.translatable("ttmodern.jei.summoned_entity"));if(flag.isAdvanced())lines.add(Component.literal(mob.entity().toString()));return lines;}
    }
    public static final class CropCategory implements mezz.jei.api.recipe.category.IRecipeCategory<CropPage> {
        private final mezz.jei.api.gui.drawable.IDrawable icon;
        CropCategory(mezz.jei.api.helpers.IGuiHelper gui){icon=gui.createDrawableItemStack(InfusedSeedItem.seed("aer",Map.of()));}
        public RecipeType<CropPage> getRecipeType(){return CROPS;}
        public Component getTitle(){return Component.translatable("ttmodern.jei.crop_drops");}
        public int getWidth(){return 150;}
        public int getHeight(){return 116;}
        public mezz.jei.api.gui.drawable.IDrawable getIcon(){return icon;}
        public void setRecipe(mezz.jei.api.gui.builder.IRecipeLayoutBuilder builder,CropPage page,IFocusGroup focuses){builder.addSlot(RecipeIngredientRole.INPUT,67,1).addItemStack(InfusedSeedItem.seed(page.recipe().aspect(),Map.of()));var drops=page.recipe().drops();for(int i=page.page()*24;i<Math.min(drops.size(),(page.page()+1)*24);i++){int index=i-page.page()*24;var drop=drops.get(i);builder.addSlot(RecipeIngredientRole.OUTPUT,3+(index%8)*18,28+(index/8)*18).addItemStack(drop.stack()).addTooltipCallback((view,tooltip)->tooltip.add(Component.translatable("ttmodern.jei.crop_chance",String.format(java.util.Locale.ROOT,"%.2f",drop.chance()*100))));}}
        public void draw(CropPage page,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY){graphics.drawWordWrap(Minecraft.getInstance().font,Component.translatable(page.recipe().exact()?"ttmodern.jei.crop_rules":"ttmodern.jei.crop_unknown"),2,86,146,0x555555);}
    }
    public static final class SummoningCategory implements mezz.jei.api.recipe.category.IRecipeCategory<MobEssenceCatalog.Recipe> {
        private final mezz.jei.api.gui.drawable.IDrawable background,icon;private final boolean extraction;
        SummoningCategory(mezz.jei.api.helpers.IGuiHelper gui,boolean extraction){this.extraction=extraction;background=gui.createBlankDrawable(160,64);icon=gui.createDrawableItemStack(new ItemStack(extraction?Tinkerer.BLOOD_SWORD.get():Tinkerer.SUMMONING_TABLET.get().asItem()));}
        public RecipeType<MobEssenceCatalog.Recipe> getRecipeType(){return extraction?EXTRACTION:SUMMONING;}
        public Component getTitle(){return Component.translatable(extraction?"ttmodern.jei.extraction":"block.thaumic_tinkerer.summoning_tablet");}
        public mezz.jei.api.gui.drawable.IDrawable getBackground(){return background;}
        public mezz.jei.api.gui.drawable.IDrawable getIcon(){return icon;}
        public void setRecipe(mezz.jei.api.gui.builder.IRecipeLayoutBuilder builder,MobEssenceCatalog.Recipe recipe,IFocusGroup focuses){for(int i=0;i<3;i++){String aspect=recipe.aspects().get(i);builder.addSlot(extraction?RecipeIngredientRole.OUTPUT:RecipeIngredientRole.INPUT,5+i*24,8).addItemStacks(java.util.stream.IntStream.range(0,extraction?1:3).mapToObj(grade->MobEssenceItem.stack(aspect,grade)).toList());}builder.addSlot(extraction?RecipeIngredientRole.INPUT:RecipeIngredientRole.OUTPUT,130,8).addIngredient(MOBS,new MobDisplay(recipe.entity()));}
        public void draw(MobEssenceCatalog.Recipe recipe,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY){var font=Minecraft.getInstance().font;graphics.drawString(font,extraction?"←":"→",97,12,0x555555,false);graphics.drawWordWrap(font,Component.translatable(extraction?"ttmodern.jei.extraction_rules":"ttmodern.jei.summoning_rules"),3,31,154,0x555555);}
    }
}
