package technomancy.client;

import java.util.*;
import mezz.jei.api.*;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import technomancy.*;

/** Every reachable pass history is shown; JEI distinguishes the metal and all three counters. */
@JeiPlugin
public class TechnomancyJei implements IModPlugin {
    public record Processing(List<ItemStack> inputs,ItemStack output,int cost){}
    private static final List<MachineKind> KINDS=List.of(MachineKind.IGNIS_INCINERATOR,MachineKind.BOTANICAL_PURIFIER,MachineKind.SANGUINE_INFUSER);
    private static final Map<MachineKind,RecipeType<Processing>> TYPES=new EnumMap<>(MachineKind.class);
    private static mezz.jei.api.runtime.IJeiRuntime runtime;
    private static boolean listening;
    private static int refreshDelay=-1;
    private static Map<MachineKind,List<Processing>> registered=new EnumMap<>(MachineKind.class);
    private static List<ItemStack> extra=List.of();
    static {for(var k:KINDS)TYPES.put(k,RecipeType.create(Technomancy.ID,k.id(),Processing.class));}
    public TechnomancyJei(){
        if(!listening){
            listening=true;
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(TechnomancyJei::tick);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.client.event.RecipesUpdatedEvent e)->refreshDelay=10);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TagsUpdatedEvent e)->{if(e.getUpdateCause()==net.minecraftforge.event.TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED)refreshDelay=10;});
        }
    }
    @Override public ResourceLocation getPluginUid(){return Technomancy.id("processing");}
    @Override public void registerItemSubtypes(ISubtypeRegistration r){for(int i=1;i<=6;i++)r.registerSubtypeInterpreter(TechRegistry.item("purified_ore_"+i),(stack,context)->signature(stack));}
    private static String signature(ItemStack stack){return stack.hasTag()?stack.getTag().getString("ingot")+Arrays.toString(stack.getTag().getIntArray("passes")):"unconfigured";}
    @Override public void registerCategories(IRecipeCategoryRegistration r){for(var k:KINDS)if(k.enabled())r.addRecipeCategories(new Category(k,r.getJeiHelpers().getGuiHelper()));}
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r){for(var k:KINDS)if(k.enabled())r.addRecipeCatalyst(new ItemStack(TechRegistry.item(k.id())),TYPES.get(k));}
    @Override public void registerExtraIngredients(IExtraIngredientRegistration r){
        extra=extras(discover());r.addExtraIngredients(VanillaTypes.ITEM_STACK,extra);
    }
    @Override public void registerRecipes(IRecipeRegistration r){registered=discover();registered.forEach((k,v)->r.addRecipes(TYPES.get(k),v));}
    @Override public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime value){runtime=value;refreshDelay=10;}
    @Override public void onRuntimeUnavailable(){runtime=null;registered=new EnumMap<>(MachineKind.class);extra=List.of();refreshDelay=-1;}
    private static List<ItemStack> extras(Map<MachineKind,List<Processing>> recipes){
        var seen=new LinkedHashMap<String,ItemStack>();recipes.values().forEach(list->list.forEach(p->seen.put(signature(p.output()),p.output())));return new ArrayList<>(seen.values());
    }
    private static String fingerprint(Map<MachineKind,List<Processing>> recipes){
        return recipes.entrySet().stream().map(e->e.getKey()+":"+e.getValue().stream().map(p->p.inputs().stream().map(s->s.save(new net.minecraft.nbt.CompoundTag()).toString()).toList()+"->"+signature(p.output())+":"+p.cost()).toList()).toList().toString();
    }
    private static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END||runtime==null||refreshDelay<0||Minecraft.getInstance().level==null)return;
        if(refreshDelay-->0)return;
        var next=discover();
        if(fingerprint(next).equals(fingerprint(registered)))return;
        registered.forEach((k,v)->runtime.getRecipeManager().hideRecipes(TYPES.get(k),v));
        if(!extra.isEmpty())runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK,extra);
        extra=extras(next);if(!extra.isEmpty())runtime.getIngredientManager().addIngredientsAtRuntime(VanillaTypes.ITEM_STACK,extra);
        next.forEach((k,v)->runtime.getRecipeManager().addRecipes(TYPES.get(k),v));registered=next;
        org.slf4j.LoggerFactory.getLogger("TechnomancyJEI").info("Refreshed processing recipes after world/tag synchronization: {}",next.entrySet().stream().map(e->e.getKey()+"="+e.getValue().size()).toList());
    }
    private static Map<MachineKind,List<Processing>> discover(){
        var results=new EnumMap<MachineKind,List<Processing>>(MachineKind.class);for(var k:KINDS)if(k.enabled())results.put(k,new ArrayList<>());
        var level=Minecraft.getInstance().level;if(level==null)return results;
        var ores=new TreeMap<String,List<ItemStack>>();
        for(var item:ForgeRegistries.ITEMS){var in=new ItemStack(item);var output=OreProcessing.output(in,MachineKind.IGNIS_INCINERATOR,level);if(!output.isEmpty())ores.computeIfAbsent(output.getTag().getString("ingot"),key->new ArrayList<>()).add(in);}
        for(var base:ores.values()){
            var pending=new ArrayDeque<List<ItemStack>>();pending.add(base);var seen=new HashSet<String>();
            while(!pending.isEmpty()){
                var inputs=pending.removeFirst();for(var k:KINDS)if(k.enabled()){
                    var out=OreProcessing.output(inputs.get(0),k,level);if(out.isEmpty())continue;
                    int stage=((PurifiedOreItem)out.getItem()).stage,passes=out.getTag().getIntArray("passes")[KINDS.indexOf(k)];
                    int cost=(k==MachineKind.IGNIS_INCINERATOR?Math.max(1,stage+2*passes):k==MachineKind.BOTANICAL_PURIFIER?stage*150+1500*passes:stage*100+1000*passes)*60;
                    results.get(k).add(new Processing(inputs,out,cost));if(seen.add(signature(out)))pending.add(List.of(out));
                }
            }
        }return results;
    }
    private static class Category implements IRecipeCategory<Processing>{
        private final MachineKind kind;private final IDrawable background,icon;
        Category(MachineKind kind,IGuiHelper gui){this.kind=kind;background=gui.createBlankDrawable(154,64);icon=gui.createDrawableItemStack(new ItemStack(TechRegistry.item(kind.id())));}
        public RecipeType<Processing> getRecipeType(){return TYPES.get(kind);}
        public Component getTitle(){return Component.translatable("block.technomancy."+kind.id());}
        public IDrawable getBackground(){return background;}
        public IDrawable getIcon(){return icon;}
        public void setRecipe(IRecipeLayoutBuilder b,Processing r,IFocusGroup f){b.addSlot(RecipeIngredientRole.INPUT,16,6).addItemStacks(r.inputs());b.addSlot(RecipeIngredientRole.OUTPUT,120,6).addItemStack(r.output());}
        @Override public void draw(Processing recipe,IRecipeSlotsView slots,GuiGraphics g,double x,double y){
            var font=Minecraft.getInstance().font;g.drawString(font,"→",73,10,0xff353535,false);
            g.drawString(font,Component.translatable("jei.technomancy.duration"),8,32,0xff353535,false);
            String key=kind==MachineKind.IGNIS_INCINERATOR?"ignis":kind==MachineKind.BOTANICAL_PURIFIER?"mana":"lp";
            g.drawString(font,Component.translatable("jei.technomancy.cost."+key,recipe.cost()),8,47,0xff353535,false);
        }
    }
}
