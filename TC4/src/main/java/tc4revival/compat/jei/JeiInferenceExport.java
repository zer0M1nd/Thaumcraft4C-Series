package tc4revival.compat.jei;

import com.google.gson.*;
import java.util.*;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import tc4revival.aspect.inference.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Client-only JEI adapter. Writes a local administrator-owned index, never uploads recipes to a server. */
public class JeiInferenceExport {
    private static IJeiRuntime runtime;
    private static boolean pending;
    private static int delay;
    private static Job job;
    private static JsonObject prepared;
    private static int generation;
    public static int generation(){return generation;}
    private static void invalidate(){job=null;prepared=null;generation++;}
    public static boolean ready(){return runtime!=null;}
    public static List<net.minecraft.world.item.ItemStack> items(){
        if(runtime==null)return List.of();
        try{return new ArrayList<>(runtime.getIngredientManager().getAllIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK));}
        catch(RuntimeException|LinkageError failure){tc4revival.TC4Revival.LOGGER.warn("Cannot enumerate JEI inference items",failure);return List.of();}
    }
    /** JEI layouts must run on the client thread. Spread enumeration over frames. */
    public static Optional<JsonObject> prepared(){
        if(runtime==null||!tc4revival.client.ClientItemAspects.inputsReady())return Optional.empty();
        if(prepared!=null)return Optional.of(prepared);
        try{
            if(job==null)job=new Job();job.advance();
            if(job.done){prepared=new JsonObject();prepared.addProperty("format",1);prepared.add("recipes",job.rows);job=null;}
        }catch(RuntimeException|LinkageError failure){
            tc4revival.TC4Revival.LOGGER.warn("Cannot finish JEI recipe projection; unavailable categories will be omitted from local inference",failure);
            prepared=new JsonObject();prepared.addProperty("format",1);prepared.add("recipes",job==null?new JsonArray():job.rows);job=null;
        }
        return Optional.ofNullable(prepared);
    }
    public static void runtime(IJeiRuntime value){runtime=value;pending=value!=null;delay=1;invalidate();}
    @SubscribeEvent public static void recipes(net.minecraftforge.client.event.RecipesUpdatedEvent event){pending=true;delay=1;invalidate();}
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){pending=true;delay=1;invalidate();}
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END||!pending||runtime==null||Minecraft.getInstance().level==null||--delay>0)return;
        if(!tc4revival.config.TCConfig.SERVER.advancedAspectInference.get())return;
        var snapshot=prepared();if(snapshot.isEmpty())return;pending=false;
        save(snapshot.get());
    }
    private static void save(JsonObject root){
        // Rebuilding the integrated server here blocks its tick and broadcasting recipes
        // restarts JEI, which invalidates this export and the client's ongoing prediction.
        // Persist only: the server consumes the index on the next world load or /reload.
        int count=root.getAsJsonArray("recipes").size();
        InferenceFiles.writeAsync(InferenceFiles.directory(),"jei-recipes.json",root).whenComplete((ignored,error)->{
            if(error!=null)tc4revival.TC4Revival.LOGGER.error("JEI inference index save failed",error);
            else tc4revival.TC4Revival.LOGGER.info("Saved {} local JEI inference recipes; applied on next world load or /reload (no live server refresh)",count);
        });
    }
    @SubscribeEvent public static void commands(net.minecraftforge.client.event.RegisterClientCommandsEvent event){
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("tc_inference_export").executes(context->{
            if(runtime==null){context.getSource().sendFailure(Component.literal("JEI is not ready."));return 0;}
            if(!tc4revival.config.TCConfig.SERVER.advancedAspectInference.get()){context.getSource().sendFailure(Component.literal("Enable 进阶要素推断 (advanced_aspect_inference) on the server first."));return 0;}
            pending=true;delay=0;
            context.getSource().sendSuccess(()->Component.literal("JEI export queued; completion is logged. The saved index applies on next world load or /reload. Dedicated servers: copy config/thaumcraft/advanced-inference/jei-recipes.json to the server first."),false);
            return 1;
        }));
    }
    public static int export()throws java.io.IOException{
        if(runtime==null)throw new IllegalStateException("JEI is not ready");
        // Retain the public entry point without an unbounded render-thread loop.
        if(prepared==null){pending=true;delay=0;return 0;}
        save(prepared);return prepared.getAsJsonArray("recipes").size();
    }
    private static class Job {
        final Iterator<? extends IRecipeCategory<?>> categories=runtime.getRecipeManager().createRecipeCategoryLookup().get().toList().iterator();
        final JsonArray rows=new JsonArray();
        IRecipeCategory category;Iterator<?> recipes=Collections.emptyIterator();int index,failures;boolean done;
        void advance(){
            long end=System.nanoTime()+5_000_000;int count=0;
            while(!done&&count++<48&&System.nanoTime()<end){
                if(!recipes.hasNext()){
                    if(!categories.hasNext()){done=true;break;}
                    category=categories.next();index=0;failures=0;
                    String namespace=category.getRecipeType().getUid().getNamespace();
                    if(namespace.equals("thaumcraft")||namespace.equals("minecraft"))continue;
                    try{recipes=runtime.getRecipeManager().createRecipeLookup(category.getRecipeType()).get().toList().iterator();}
                    catch(RuntimeException|LinkageError error){recipes=Collections.emptyIterator();}
                    continue;
                }
                Object recipe=recipes.next();
                try{if(!append(category,recipe,index,rows))failures++;}
                catch(RuntimeException|LinkageError error){failures++;tc4revival.TC4Revival.LOGGER.debug("Skipping JEI inference recipe in {}: {}",category.getRecipeType().getUid(),error.toString());}
                finally{index++;}
                if(failures>=3){tc4revival.TC4Revival.LOGGER.warn("Stopping JEI inference category {} after repeated layout failures",category.getRecipeType().getUid());recipes=Collections.emptyIterator();}
            }
        }
    }
    private static <T> boolean append(IRecipeCategory<T> category,T recipe,int index,JsonArray rows){
        var manager=runtime.getRecipeManager();
                var layout=manager.createRecipeLayoutDrawable(category,recipe,runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup());
                if(layout.isEmpty())return false;
                var inputs=new ArrayList<List<InferenceEngine.Amount>>();var outputs=new ArrayList<InferenceEngine.Amount>();boolean supported=true;
                for(var slot:layout.get().getRecipeSlotsView().getSlotViews()){
                    if(slot.getRole()!=RecipeIngredientRole.INPUT&&slot.getRole()!=RecipeIngredientRole.OUTPUT)continue;
                    if(slot.isEmpty())continue;
                    var alternatives=new ArrayList<InferenceEngine.Amount>();
                    for(var typed:slot.getAllIngredients().toList()){
                        var value=typed.getIngredient();
                        if(value instanceof net.minecraft.world.item.ItemStack stack&&!stack.isEmpty())alternatives.add(new InferenceEngine.Amount(AdvancedAspectInference.key(stack.copy()),stack.getCount()));
                        else if(value instanceof net.minecraftforge.fluids.FluidStack fluid&&!fluid.isEmpty()&&!fluid.hasTag())alternatives.add(new InferenceEngine.Amount("fluid:"+net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(fluid.getFluid()),fluid.getAmount()/1000.0));
                        else supported=false; // Unknown ingredient types cannot be silently omitted.
                    }
                    if(alternatives.isEmpty())supported=false;
                    if(slot.getRole()==RecipeIngredientRole.INPUT)inputs.add(alternatives);else outputs.addAll(alternatives);
                }
                // Multiple coproducts/chance outputs need a mod-specific valuation policy.
                if(!supported||inputs.isEmpty()||outputs.size()!=1)return true;
                var name=category.getRegistryName(recipe);String id=category.getRecipeType().getUid()+"/"+(name==null?index:name);
                rows.add(AdvancedAspectInference.recipeJson(new InferenceEngine.Recipe(id,outputs.get(0),inputs)));
        return true;
    }
    private JeiInferenceExport(){}
}
