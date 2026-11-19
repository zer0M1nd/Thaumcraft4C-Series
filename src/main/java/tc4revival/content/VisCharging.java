package tc4revival.content;

import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;

/** Shared centivis charging contract for wands (including addon subclasses) and vis amulets. */
public final class VisCharging {
    public static boolean accepts(ItemStack stack) { return stack.getItem() instanceof CastingWandItem || stack.getItem() instanceof VisAmuletItem; }
    private static int receive(ItemStack stack,Aspect aspect,int amount,boolean simulate) {
        if(stack.getItem() instanceof CastingWandItem wand)return wand.receiveVis(stack,aspect,amount,simulate);
        if(stack.getItem() instanceof VisAmuletItem amulet)return amount-amulet.addVis(stack,aspect,amount,false,!simulate);
        return 0;
    }
    public static boolean needsCharge(ItemStack stack) {
        return accepts(stack)&&TCAspects.catalog().primals().stream().anyMatch(a->receive(stack,a,1,true)>0);
    }
    public static boolean charge(ItemStack stack,VisRelayBlockEntity source) {
        return charge(stack,source::consumeVis);
    }
    public static boolean charge(ItemStack stack,java.util.function.ToIntBiFunction<Aspect,Integer> source) {
        boolean changed=false;
        for(var aspect:TCAspects.catalog().primals()) {
            int request=receive(stack,aspect,5,true);if(request<=0)continue;
            int amount=source.applyAsInt(aspect,request);
            if(amount>0){receive(stack,aspect,amount,false);changed=true;}
        }
        return changed;
    }
    private VisCharging() {}
}
