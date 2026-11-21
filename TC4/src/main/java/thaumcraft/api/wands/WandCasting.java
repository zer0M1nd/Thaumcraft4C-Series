package thaumcraft.api.wands;

import java.util.*;
import java.util.function.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.Aspect;
import tc4revival.content.CastingWandItem;
import tc4revival.content.WandFocusItem;
import thaumcraft.api.util.ExtensionRegistry;

/** Explicit cast scope, focus ownership and independent sustained-use lifecycle. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumcraft")
public final class WandCasting {
    public enum Action { USE, HOLD_TICK, RELEASE, SWING, START_BREAK, REPLAY, PREVIEW, CANCEL }
    public enum CancelReason { REPLACED, STOPPED, DISABLED, DEATH, DISCONNECT, DIMENSION }
    public record Request(Player player,ItemStack wand,InteractionHand hand,Action action) {
        public Request {Objects.requireNonNull(wand);Objects.requireNonNull(hand);Objects.requireNonNull(action);}
    }
    /** A virtual handle must retain its original storage key and reject stale writes. */
    public interface FocusHandle {
        String source();
        boolean virtual();
        ItemStack snapshot();
        boolean valid();
        boolean commit(ItemStack updated);
    }
    public record Resolution(FocusHandle handle,double strength,double effect) {
        public Resolution {
            Objects.requireNonNull(handle);
            if(!Double.isFinite(strength)||strength<1||!Double.isFinite(effect)||effect<0)
                throw new IllegalArgumentException("Invalid cast parameters");
        }
    }
    public record Context(Request request,Resolution resolution,UUID operation) {
        public ItemStack focus(){return resolution.handle().snapshot().copy();}
        public boolean virtual(){return resolution.handle().virtual();}
        public double strength(){return resolution.strength();}
        public double effect(){return resolution.effect();}
    }
    public record Preview(ItemStack focus,Map<Aspect,Integer> costs,Map<Aspect,Float> modifiers) {
        public Preview {focus=focus.copy();costs=Map.copyOf(costs);modifiers=Map.copyOf(modifiers);}
        @Override public ItemStack focus(){return focus.copy();}
    }
    private static final ThreadLocal<Context> ACTIVE=new ThreadLocal<>();
    private static final ExtensionRegistry<Function<Request,Optional<Resolution>>> FOCI=new ExtensionRegistry<>();
    private WandCasting() {}
    public static void registerFocus(String id,int priority,Function<Request,Optional<Resolution>> resolver){FOCI.register(id,priority,resolver);}
    public static Context current(){return ACTIVE.get();}
    public static Context current(ItemStack wand){var value=current();return value!=null&&value.request().wand()==wand?value:null;}
    public static InteractionHand hand(Player player,ItemStack wand){return player!=null&&player.getOffhandItem()==wand?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;}
    public static Resolution resolve(Request request){
        var physical=WandFocusItem.physical(request.wand());
        if(!physical.isEmpty())return new Resolution(physical(request.wand(),physical),1,1);
        if(request.player()!=null&&request.player().isAlive()&&!request.player().isSpectator())
            for(var entry:FOCI.entries()){
                var value=Objects.requireNonNull(entry.value().apply(request));
                if(value.isPresent()&&value.get().handle().valid())return value.get();
            }
        return new Resolution(physical(request.wand(),ItemStack.EMPTY),1,1);
    }
    private static FocusHandle physical(ItemStack wand,ItemStack initial){
        return new FocusHandle(){
            private ItemStack expected=initial.copy();
            public String source(){return "thaumcraft:physical";}
            public boolean virtual(){return false;}
            public ItemStack snapshot(){return expected.copy();}
            public boolean valid(){return ItemStack.matches(WandFocusItem.physical(wand),expected);}
            public boolean commit(ItemStack value){
                if(!valid()||!value.isEmpty()&&!(value.getItem() instanceof WandFocusItem))return false;
                WandFocusItem.attachPhysical(wand,value);expected=value.copy();return true;
            }
        };
    }
    public static Scope open(Request request){return new Scope(new Context(request,resolve(request),UUID.randomUUID()));}
    public static Scope open(Player player,ItemStack wand,Action action){return open(new Request(player,wand,hand(player,wand),action));}
    /** Dispatch an already resolved context, restoring the enclosing operation even on failure. */
    public static <T> T dispatch(Context context,Supplier<T> operation){
        if(!context.resolution().handle().valid())throw new IllegalStateException("Stale focus handle");
        try(var scope=new Scope(context)){return operation.get();}
    }
    public static final class Scope implements AutoCloseable {
        private final Context previous,context;private boolean closed;
        private Scope(Context value){previous=ACTIVE.get();context=value;ACTIVE.set(value);}
        public Context context(){return context;}
        public ItemStack focus(){return context.focus();}
        @Override public void close(){if(closed)return;closed=true;if(previous==null)ACTIVE.remove();else ACTIVE.set(previous);}
    }
    /** Legacy focus methods consult only the exact wand in the explicit current scope. */
    public static Optional<ItemStack> attached(ItemStack wand){var context=current(wand);return context==null?Optional.empty():Optional.of(context.focus());}
    public static boolean commit(ItemStack wand,ItemStack focus){
        var context=current(wand);if(context==null)return false;
        if(context.request().action()==Action.PREVIEW)throw new IllegalStateException("Cannot write a cast preview");
        if(!context.resolution().handle().commit(focus.copy()))throw new IllegalStateException("Focus source changed during cast");
        return true;
    }
    public static Preview preview(Player player,ItemStack stack){
        try(var scope=open(player,stack,Action.PREVIEW)){
            var focus=scope.focus();var costs=focus.getItem() instanceof WandFocusItem item?SpellParameters.costs(stack,item.costs(focus)):Map.<Aspect,Integer>of();
            var modifiers=new LinkedHashMap<Aspect,Float>();
            if(stack.getItem() instanceof CastingWandItem wand)costs.keySet().forEach(aspect->modifiers.put(aspect,wand.consumptionModifier(stack,player,aspect,false)));
            return new Preview(focus,costs,modifiers);
        }
    }
    /** Transient queues retain the actual source stack and owner, but resolve current parameters on replay. */
    public record Deferred(UUID actor,int slot,ItemStack source,String focusSource) {
        public boolean matches(Player player,Context context){
            return player!=null&&actor.equals(player.getUUID())&&player.getInventory().getItem(slot)==source
                &&context.request().wand()==source&&context.resolution().handle().valid()
                &&focusSource.equals(context.resolution().handle().source());
        }
    }
    public static Deferred defer(Player player,ItemStack wand,int slot){
        var context=current(wand);var resolution=context==null?resolve(new Request(player,wand,hand(player,wand),Action.REPLAY)):context.resolution();
        return new Deferred(player.getUUID(),slot,wand,resolution.handle().source());
    }

    public interface UseMode {
        boolean matches(Request request);
        default boolean enabled(Request request){return true;}
        default void hold(Request request,int remaining){}
        void finish(Request request,CancelReason reason);
    }
    private record Session(Request request,UseMode mode) {}
    private static final ExtensionRegistry<UseMode> MODES=new ExtensionRegistry<>();
    private static final Map<Player,Session> SESSIONS=Collections.synchronizedMap(new IdentityHashMap<>());
    public static void registerUseMode(String id,int priority,UseMode mode){MODES.register(id,priority,mode);}
    private static UseMode mode(Request request){return MODES.entries().stream().map(ExtensionRegistry.Entry::value).filter(m->m.matches(request)).findFirst().orElse(null);}
    public static boolean beginUseMode(Player player,ItemStack stack,InteractionHand hand){
        var request=new Request(player,stack,hand,Action.HOLD_TICK);var mode=mode(request);if(mode==null)return false;
        var old=SESSIONS.get(player);if(old!=null&&(old.request().wand()!=stack||old.mode()!=mode))cancel(player,CancelReason.REPLACED);
        SESSIONS.put(player,new Session(request,mode));return true;
    }
    public static boolean holdMode(Player player,ItemStack stack,int remaining){
        var old=SESSIONS.get(player);
        if(old==null||old.request().wand()!=stack){if(!beginUseMode(player,stack,hand(player,stack)))return false;old=SESSIONS.get(player);}
        if(!old.mode().enabled(old.request())||!old.mode().matches(old.request())){cancel(player,CancelReason.DISABLED);player.stopUsingItem();return true;}
        old.mode().hold(old.request(),remaining);return true;
    }
    public static boolean releaseMode(Player player,ItemStack stack){
        var session=SESSIONS.get(player);
        if(session==null||session.request().wand()!=stack){
            var request=new Request(player,stack,hand(player,stack),Action.RELEASE);var mode=mode(request);if(mode==null)return false;
            mode.finish(request,CancelReason.STOPPED);return true;
        }
        SESSIONS.remove(player);session.mode().finish(new Request(player,stack,session.request().hand(),Action.RELEASE),CancelReason.STOPPED);return true;
    }
    public static void cancel(Player player,CancelReason reason){
        var session=SESSIONS.remove(player);if(session!=null)session.mode().finish(new Request(player,session.request().wand(),session.request().hand(),Action.CANCEL),reason);
    }
    private static void endPlayer(Player player,CancelReason reason){
        cancel(player,reason);
        // Also recover saved mode markers before the first hold tick after reconnect/load.
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++){
            var stack=player.getInventory().getItem(slot);if(!(stack.getItem() instanceof CastingWandItem))continue;
            var request=new Request(player,stack,slot==40?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,Action.CANCEL);
            var mode=mode(request);if(mode!=null)mode.finish(request,reason);
        }
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.PlayerTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;var session=SESSIONS.get(event.player);if(session==null)return;
        var player=event.player;var request=session.request();
        if(!player.isAlive())cancel(player,CancelReason.DEATH);
        else if(player.getItemInHand(request.hand())!=request.wand())cancel(player,CancelReason.REPLACED);
        else if(!player.isUsingItem()||player.getUseItem()!=request.wand()||player.getUsedItemHand()!=request.hand())cancel(player,CancelReason.STOPPED);
        else if(!session.mode().enabled(request)||!session.mode().matches(request))cancel(player,CancelReason.DISABLED);
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){endPlayer(event.getEntity(),CancelReason.DISCONNECT);}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void dimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event){endPlayer(event.getEntity(),CancelReason.DIMENSION);}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void death(net.minecraftforge.event.entity.living.LivingDeathEvent event){if(event.getEntity() instanceof Player player)endPlayer(player,CancelReason.DEATH);}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void unload(net.minecraftforge.event.level.LevelEvent.Unload event){
        List<Player> players; synchronized(SESSIONS){players=new ArrayList<>(SESSIONS.keySet());}
        for(var player:players)if(player.level()==event.getLevel())cancel(player,CancelReason.DISCONNECT);
    }
}
