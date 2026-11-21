package thaumcraft.api.wands;

import java.util.*;
import java.util.function.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import tc4revival.aspect.Aspect;
import tc4revival.content.CastingWandItem;
import thaumcraft.api.util.ExtensionRegistry;

/** Pure conjunctive authorization; no allow decision can override a veto. Hosts own payment and mutation. */
public final class MagicOperationRules {
    public enum Kind { USE, STRUCTURE, ARCANE, INFUSION }
    public record Context(Kind kind,String id,Player player,Level level,BlockPos position,InteractionHand hand,
                          ItemStack wand,ItemStack output,String research,Map<Aspect,Integer> costs,boolean simulate) {
        public Context {wand=wand.copy();output=output.copy();costs=Map.copyOf(costs);if(position!=null)position=position.immutable();}
        @Override public ItemStack wand(){return wand.copy();}
        @Override public ItemStack output(){return output.copy();}
    }
    public record Verdict(boolean allowed,Component reason,Map<String,Integer> requirements) {
        public Verdict {requirements=Map.copyOf(requirements);reason=reason==null?null:reason.copy();}
        @Override public Component reason(){return reason==null?null:reason.copy();}
        public static Verdict pass(){return new Verdict(true,null,Map.of());}
        public static Verdict deny(Component reason){return new Verdict(false,reason,Map.of());}
    }
    private record Trigger(String id,UseOnContext context) {}
    private static final ThreadLocal<Trigger> TRIGGER=new ThreadLocal<>();
    private static final ExtensionRegistry<Function<Context,Verdict>> RULES=new ExtensionRegistry<>();
    private MagicOperationRules() {}
    public static void register(String id,int priority,Function<Context,Verdict> rule){RULES.register(id,priority,rule);}
    public static Verdict evaluate(Context context){
        boolean allowed=true;Component reason=null;var requirements=new LinkedHashMap<String,Integer>();
        for(var entry:RULES.entries()){
            var value=Objects.requireNonNull(entry.value().apply(context));
            value.requirements().forEach((key,n)->requirements.merge(key,n,Math::max));
            if(!value.allowed()){allowed=false;if(reason==null)reason=value.reason();}
        }
        return new Verdict(allowed,reason,requirements);
    }
    public static boolean allows(Context context,boolean notify){
        var decision=evaluate(context);
        if(!decision.allowed()&&notify&&context.player()!=null&&!context.level().isClientSide&&decision.reason()!=null)
            context.player().displayClientMessage(decision.reason(),true);
        return decision.allowed();
    }
    public static <T> T withTrigger(String id,UseOnContext context,Supplier<T> action){
        var previous=TRIGGER.get();TRIGGER.set(new Trigger(id,context));
        try{return action.get();}finally{if(previous==null)TRIGGER.remove();else TRIGGER.set(previous);}
    }
    public static boolean triggerAllows(ItemStack wand,Player player,Map<Aspect,Integer> costs,boolean commit){
        var trigger=TRIGGER.get();return trigger==null||allows(new Context(Kind.STRUCTURE,trigger.id(),player,player.level(),trigger.context().getClickedPos(),trigger.context().getHand(),wand,ItemStack.EMPTY,"",costs,!commit),true);
    }
    public static boolean payStructure(String id,CastingWandItem casting,ItemStack wand,Player player,Map<Aspect,Integer> costs,boolean whole,boolean crafting,boolean commit){
        return allows(new Context(Kind.STRUCTURE,id,player,player.level(),player.blockPosition(),player.getMainHandItem()==wand?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND,wand,ItemStack.EMPTY,"",costs,!commit),true)
            &&casting.consumeForPlayer(wand,player,costs,whole,crafting,commit);
    }
}
