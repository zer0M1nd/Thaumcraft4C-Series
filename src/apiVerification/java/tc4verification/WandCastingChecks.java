package tc4verification;
import thaumcraft.api.wands.*;

import java.util.*;

import static tc4verification.Checks.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.LegacyAspect;

public final class WandCastingChecks {
    private static class Handle implements WandCasting.FocusHandle {
        boolean valid=true;int writes;
        public String source(){return "test:virtual";}
        public boolean virtual(){return true;}
        public ItemStack snapshot(){return ItemStack.EMPTY;}
        public boolean valid(){return valid;}
        public boolean commit(ItemStack focus){writes++;return valid;}
    }
    private WandCasting.Context context(double strength,double effect,WandCasting.Action action,Handle handle){
        return new WandCasting.Context(new WandCasting.Request(null,ItemStack.EMPTY,InteractionHand.OFF_HAND,action),new WandCasting.Resolution(handle,strength,effect),UUID.randomUUID());
    }
    public void nestedFailureRestoresOuterContextAndThenClearsIt(){
        var outer=context(3,2,WandCasting.Action.USE,new Handle());var inner=context(8,4,WandCasting.Action.REPLAY,new Handle());
        WandCasting.dispatch(outer,()->{
            assertSame(outer,WandCasting.current());
            assertThrows(IllegalStateException.class,()->WandCasting.dispatch(inner,()->{assertSame(inner,WandCasting.current());throw new IllegalStateException("injected");}));
            assertSame(outer,WandCasting.current());return null;
        });
        assertNull(WandCasting.current());
    }
    public void costsStayLinearIndependentOfEffectsAndDoNotScaleTwice(){
        var aspect=LegacyAspect.AIR.create();var raw=Map.of(aspect,11);
        var scaled=WandCasting.dispatch(context(2.5,80,WandCasting.Action.USE,new Handle()),()->{
            var plan=SpellParameters.costs(ItemStack.EMPTY,raw);assertEquals(28,plan.get(aspect));
            assertSame(plan,SpellParameters.costs(ItemStack.EMPTY,plan));
            assertThrows(UnsupportedOperationException.class,()->plan.put(aspect,1));return plan;
        });
        WandCasting.dispatch(context(4,1,WandCasting.Action.REPLAY,new Handle()),()->{assertEquals(44,SpellParameters.costs(ItemStack.EMPTY,scaled).get(aspect));return null;});
        assertEquals(11,raw.get(aspect));
    }
    public void previewAndStaleHandleNeverWrite(){
        var handle=new Handle();
        WandCasting.dispatch(context(1,1,WandCasting.Action.PREVIEW,handle),()->{assertThrows(IllegalStateException.class,()->WandCasting.commit(ItemStack.EMPTY,ItemStack.EMPTY));return null;});
        assertEquals(0,handle.writes);handle.valid=false;
        assertThrows(IllegalStateException.class,()->WandCasting.dispatch(context(1,1,WandCasting.Action.USE,handle),()->{fail("stale source ran");return null;}));
        assertNull(WandCasting.current());
    }
    public void explicitPrioritySelectsOneEffectAndBoundsProtectTheHost(){
        SpellParameters.register("cast_test:low",0,(channel,base,context)->channel.equals("test.effect")?OptionalDouble.of(base*20):OptionalDouble.empty());
        SpellParameters.register("cast_test:high",100,(channel,base,context)->channel.equals("test.effect")?OptionalDouble.of(base*3):OptionalDouble.empty());
        WandCasting.dispatch(context(9,40,WandCasting.Action.USE,new Handle()),()->{
            assertEquals(6,SpellParameters.scale("test.effect",2));
            assertEquals(12,SpellParameters.bounded("test.radius",3,12));
            assertEquals(1,SpellParameters.duration("test.speed",20));return null;
        });
        assertEquals(2,SpellParameters.scale("test.effect",2));
        assertThrows(IllegalArgumentException.class,()->SpellParameters.cost(Integer.MAX_VALUE,2));
        assertThrows(IllegalArgumentException.class,()->context(Double.NaN,1,WandCasting.Action.USE,new Handle()));
        assertThrows(IllegalArgumentException.class,()->SpellParameters.cost(-1,1));
    }
}
