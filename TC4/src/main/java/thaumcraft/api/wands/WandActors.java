package thaumcraft.api.wands;

import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Optional wielder and device origin behind a proxy; never grants research or permissions. */
public final class WandActors {
    public record Origin(ResourceLocation kind,GlobalPos device,UUID owner,UUID operation) {
        public Origin {Objects.requireNonNull(kind);}
    }
    public record ActorContext(Entity actor,Optional<Origin> origin) {
        public ActorContext {Objects.requireNonNull(actor);Objects.requireNonNull(origin);}
    }
    private record Binding(WeakReference<Entity> actor,Origin origin) {}
    private static final Map<Entity,Binding> ACTORS=new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private WandActors() {}
    public static void bind(Entity proxy,Entity actor){bind(proxy,new ActorContext(actor,Optional.empty()));}
    public static void bind(Entity proxy,ActorContext context){
        ACTORS.put(Objects.requireNonNull(proxy),new Binding(new WeakReference<>(context.actor()),context.origin().orElse(null)));
    }
    public static void unbind(Entity proxy){ACTORS.remove(proxy);}
    public static ActorContext resolveContext(Entity proxy){
        Objects.requireNonNull(proxy);
        var binding=ACTORS.get(proxy);var actor=binding==null?null:binding.actor().get();
        if(actor!=null&&actor.isAlive()&&actor.level()==proxy.level())return new ActorContext(actor,Optional.ofNullable(binding.origin()));
        if(binding!=null)ACTORS.remove(proxy);
        return new ActorContext(proxy,Optional.empty());
    }
    public static Entity resolve(Entity proxy){return proxy==null?null:resolveContext(proxy).actor();}
    /** Preserve ordinary player targeting; delegated wielders must not target themselves or allies. */
    public static boolean canTarget(Entity proxy,Entity target){
        var actor=resolve(proxy);
        if(actor==proxy)return target!=proxy;
        return target!=actor && !actor.isAlliedTo(target)
                && (!(actor instanceof net.minecraft.world.entity.Mob mob) || !(target instanceof net.minecraft.world.entity.LivingEntity living) || mob.canAttack(living));
    }
    /** Decomposition may process dropped items for players, but only living enemies for delegated combat. */
    public static boolean canDecompose(Entity proxy,Entity target){
        return resolve(proxy)==proxy?canTarget(proxy,target):target instanceof net.minecraft.world.entity.LivingEntity&&canTarget(proxy,target);
    }
}
