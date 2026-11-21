package thaumcraft.api.nodes;

import net.minecraft.resources.ResourceLocation;
import tc4revival.content.*;
import thaumcraft.api.aspects.*;

/** Permanent data is separate from expiring device influences. All mutations are server-thread owned. */
public  class NodeApi {
    public record Influence(AuraNodeType type,boolean overrideModifier,AuraNodeModifier modifier,double capacityMultiplier,double regenerationMultiplier,int priority){
        public Influence {if(!Double.isFinite(capacityMultiplier)||capacityMultiplier<.0625||capacityMultiplier>16||!Double.isFinite(regenerationMultiplier)||regenerationMultiplier<.01||regenerationMultiplier>100)throw new IllegalArgumentException("Invalid node multiplier");}
    }
    public record Snapshot(String identity,AspectList capacity,AspectList charge,AuraNodeType type,AuraNodeModifier modifier){}
    public static void check(AuraNodeBlockEntity node){if(node.getLevel()==null||node.getLevel().isClientSide||!node.getLevel().getServer().isSameThread()||node.isRemoved()||node.getLevel().getBlockEntity(node.getBlockPos())!=node)throw new IllegalStateException("Live server node required on server thread");node.expireInfluences();}
    public static AspectList getBase(AuraNodeBlockEntity node){return AspectList.from(node.permanentBase());}
    public static Snapshot snapshot(AuraNodeBlockEntity node){return new Snapshot(node.nodeId(),AspectList.from(node.base()),AspectList.from(node.aspects()),node.nodeType(),node.nodeModifier());}
    /** Changes permanent capacities/type/quality, preserving charge up to the new effective capacity. Never fills the node. */
    public static void edit(AuraNodeBlockEntity node,AspectList capacity,AuraNodeType type,AuraNodeModifier modifier){check(node);node.editPermanent(capacity.unwrap(),java.util.Objects.requireNonNull(type),modifier);}
    /** Compatibility initializer; unlike edit, this deliberately resets charge. */
    public static void configure(AuraNodeBlockEntity node,AspectList base,AuraNodeType type,AuraNodeModifier modifier,boolean empty){check(node);node.configure(base.unwrap(),type,modifier,node.nodeId(),empty);node.sync();}
    /** Device-specific stable ID; renew on tick. Highest priority wins, equal priorities use lexical ID order. */
    public static void influence(AuraNodeBlockEntity node,ResourceLocation device,Influence value,int lifetimeTicks){check(node);node.putInfluence(device,value,lifetimeTicks);}
    public static void removeInfluence(AuraNodeBlockEntity node,ResourceLocation device){check(node);node.removeInfluence(device);}
    public static int insert(AuraNodeBlockEntity node,Aspect aspect,int amount,boolean simulate){check(node);if(amount<0)throw new IllegalArgumentException("Negative charge");int n=Math.min(amount,Math.max(0,node.base().amount(aspect.unwrap())-node.aspects().amount(aspect.unwrap())));if(n>0&&!simulate)node.addCharge(aspect.unwrap(),n);return n;}
    public static int extract(AuraNodeBlockEntity node,Aspect aspect,int amount,boolean simulate){check(node);if(amount<0)throw new IllegalArgumentException("Negative charge");int n=Math.min(amount,node.aspects().amount(aspect.unwrap()));if(n>0&&!simulate){node.take(aspect.unwrap(),n);node.sync();}return n;}
    public static boolean take(AuraNodeBlockEntity node,Aspect aspect,int amount){check(node);boolean result=node.take(aspect.unwrap(),amount);if(result)node.sync();return result;}
    public static NodeObservation.State stateSnapshot(AuraNodeBlockEntity node){return NodeObservation.snapshot(node);}
    private NodeApi(){}
}
