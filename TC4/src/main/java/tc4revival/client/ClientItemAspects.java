package tc4revival.client;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.*;
import tc4revival.aspect.item.*;
import tc4revival.aspect.sync.*;
import tc4revival.network.*;
import tc4revival.research.*;

/** One shared, read-only evaluator for JEI and inventory tooltips; no per-item request packets. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public class ClientItemAspects {
    public record Result(String identity,AspectAmounts amounts) {}
    private static Connection connection;
    private static int revision,version,generation;
    private static AspectDataTransfer.Receiver receiver;
    private static Map<String,String> local=Map.of();
    private static AspectCalculationData calculation;
    private static boolean inputsComplete;
    private static InferenceSettings settings;
    private static ItemAspectCache cache;
    private static final AspectPreparationGate preparation=new AspectPreparationGate();
    public static int version(){return version;}
    public static boolean inputsReady(){return preparation.inputsReady();}
    public static AspectCalculationData calculation(){return ready()?calculation:null;}
    public static boolean ready(){var mc=Minecraft.getInstance();return calculation!=null&&mc.getConnection()!=null&&mc.getConnection().getConnection()==connection;}
    public static Optional<Result> resolve(ItemStack stack){
        if(!ready()||stack.isEmpty())return Optional.empty();
        return resolve(stack,key(stack));
    }
    public static CompoundTag key(ItemStack stack){return ItemAspectCache.key(stack,ready()?calculation:null);}
    public static Optional<Result> resolve(ItemStack stack,CompoundTag key){
        if(!ready()||stack.isEmpty()||key.sizeInBytes()>262144)return Optional.empty();
        var result=cache.resolve(stack,key);
        return Optional.of(new Result(result.identity(),result.amounts()));
    }
    public static void discard(CompoundTag key){if(cache!=null)cache.remove(key);}
    private static void invalidateCache(){cache=null;}
    public static void receive(AspectDataFrame frame,Connection source){
        var mc=Minecraft.getInstance();if(mc.getConnection()==null||mc.getConnection().getConnection()!=source)return;
        try{
            if(!frame.manifest())throw new IllegalArgumentException("Unexpected legacy aspect correction");
            if(source==connection&&frame.revision()==revision&&inputsComplete)return;
            if(frame.index()==0){
                connection=source;revision=frame.revision();generation++;version++;calculation=null;invalidateCache();
                inputsComplete=false;settings=null;local=Map.of();preparation.reset();receiver=new AspectDataTransfer.Receiver();
            }
            if(source!=connection||frame.revision()!=revision||receiver==null)return;
            var values=receiver.accept(frame.index(),frame.count(),frame.bytes());if(values.isEmpty())return;
            var delta=values.get();settings=InferenceSettings.decode(delta.get(AspectInputDelta.SETTINGS));inputsComplete=true;
            int token=generation;
            CompletableFuture.supplyAsync(()->AspectInputDelta.apply(BundledAspectData.all(),delta),net.minecraft.Util.backgroundExecutor()).whenComplete((files,error)->mc.execute(()->{
                if(token!=generation||source!=connection)return;
                if(error!=null){fail(error);return;}
                local=files;preparation.resources();
            }));
        }catch(RuntimeException error){fail(error);}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        var mc=Minecraft.getInstance();
        if(event.phase!=TickEvent.Phase.END||mc.player==null||mc.level==null)return;
        if(preparation.ready())prepareLocally();
    }
    private static void prepareLocally(){
        var mc=Minecraft.getInstance();if(mc.getConnection()==null||mc.level==null)return;
        var manager=new net.minecraft.world.item.crafting.RecipeManager();manager.replaceRecipes(mc.getConnection().getRecipeManager().getRecipes());
        var access=mc.level.registryAccess();var tags=new HashMap<String,Set<String>>();
        for(var item:ForgeRegistries.ITEMS.getValues()){
            String id=ForgeRegistries.ITEMS.getKey(item).toString();item.builtInRegistryHolder().tags().forEach(tag->tags.computeIfAbsent(tag.location().toString(),ignored->new HashSet<>()).add(id));
        }
        preparation.start();int token=generation;var source=connection;var bundled=Map.copyOf(local);var options=settings;
        var membership=ItemTagLookup.snapshot(tags);
        CompletableFuture.supplyAsync(()->{
            var crafting=tc4revival.aspect.recipe.RuntimeCraftingAspects.extract(manager,access);
            return LocalAspectPreparation.prepareAuthoritative(bundled,manager,access,crafting,options,membership);
        },net.minecraft.Util.backgroundExecutor()).whenComplete((result,error)->mc.execute(()->{
            if(token!=generation||source!=connection)return;
            if(error!=null){fail(error);return;}
            calculation=result.calculation();cache=result.cache();version++;
            tc4revival.TC4Revival.LOGGER.info("Client aspect calculation ready: {} precomputed recipe outputs; no server result download",result.prewarmed());
        }));
    }
    @SubscribeEvent public static void recipes(net.minecraftforge.client.event.RecipesUpdatedEvent event){preparation.recipes();}
    @SubscribeEvent public static void tags(net.minecraftforge.event.TagsUpdatedEvent event){
        if(event.getUpdateCause()==net.minecraftforge.event.TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED){
            preparation.tags();
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){generation++;version++;connection=null;calculation=null;invalidateCache();local=Map.of();receiver=null;inputsComplete=false;settings=null;preparation.reset();}
    private static void fail(Throwable error){
        tc4revival.TC4Revival.LOGGER.error("Unable to synchronize item aspect calculation data",error);
        calculation=null;invalidateCache();generation++;version++;
        if(connection!=null)connection.disconnect(Component.literal("Unable to synchronize Thaumcraft item aspect data: "+error.getMessage()));
    }
}
