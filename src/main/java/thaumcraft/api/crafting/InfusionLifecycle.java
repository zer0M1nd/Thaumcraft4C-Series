package thaumcraft.api.crafting;

import java.util.*;
import java.util.function.*;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.*;
import thaumcraft.api.util.ExtensionRegistry;
import thaumcraft.api.wands.MagicOperationRules;

/** Output transformations are pure. Commit participants own reversible operations; observers cannot cancel delivery. */
public final class InfusionLifecycle {
    public record Context(InfusionMatrixBlockEntity matrix,ArcanePedestalBlockEntity center,String playerName,
                          ItemStack result,MagicOperationRules.Context activation) {
        public Context {result=result.copy();}
        @Override public ItemStack result(){return result.copy();}
    }
    private record Transform(String version,UnaryOperator<ItemStack> operation) {}
    private static final ExtensionRegistry<Transform> TRANSFORMS=new ExtensionRegistry<>();
    private static final ExtensionRegistry<Function<Context,Optional<InfusionCommit.Operation>>> PARTICIPANTS=new ExtensionRegistry<>();
    private static final ExtensionRegistry<Consumer<Context>> OBSERVERS=new ExtensionRegistry<>();
    private InfusionLifecycle() {}
    public static void registerResultTransform(String id,int priority,String version,UnaryOperator<ItemStack> transform){
        TRANSFORMS.register(id,priority,new Transform(Objects.requireNonNull(version),transform));
    }
    public static String rulesVersion(){return TRANSFORMS.entries().stream().map(e->e.id()+"="+e.value().version()).collect(java.util.stream.Collectors.joining(";"));}
    public static ItemStack transform(ItemStack input){
        var result=input.copy();for(var entry:TRANSFORMS.entries())result=Objects.requireNonNull(entry.value().operation().apply(result.copy())).copy();return result;
    }
    public static void registerCommitParticipant(String id,int priority,Function<Context,Optional<InfusionCommit.Operation>> prepare){PARTICIPANTS.register(id,priority,prepare);}
    public static void observeCompleted(String id,int priority,Consumer<Context> observer){OBSERVERS.register(id,priority,observer);}
    public static boolean commit(Context context){
        var before=context.center().getItem(0).copy();
        var preparations=new ArrayList<Supplier<? extends InfusionCommit.Operation>>();
        for(var entry:PARTICIPANTS.entries())preparations.add(()->entry.value().apply(context).orElseGet(()->new InfusionCommit.Operation(){
            public boolean validate(){return true;}public boolean commit(){return true;}public void abort(){}
        }));
        boolean committed=InfusionCommit.execute(preparations,new InfusionCommit.Operation(){
            private boolean attempted;
            public boolean validate(){return !context.center().isRemoved()&&ItemStack.matches(before,context.center().getItem(0));}
            public boolean commit(){
                if(!validate())return false;attempted=true;context.center().setFromInfusion(context.result());
                return ItemStack.matches(context.result(),context.center().getItem(0));
            }
            public void abort(){if(attempted)context.center().setFromInfusion(before);}
        });
        if(committed)for(var entry:OBSERVERS.entries())try{entry.value().accept(context);}
            catch(RuntimeException failure){com.mojang.logging.LogUtils.getLogger().error("Infusion observer {} failed after commit",entry.id(),failure);}
        return committed;
    }
}
