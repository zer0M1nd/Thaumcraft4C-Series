package thmodern.node;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.*;
import tc4revival.content.*;

/** Per-world-tick budgets avoid extra output when consumers tick before the source. */
public abstract class DynamoEntity extends VisNetworkNodeBlockEntity implements RevealedAspectContainer {
    protected AspectAmounts output = new AspectAmounts();
    private AspectAmounts remaining = new AspectAmounts();
    private long budgetTick = Long.MIN_VALUE;
    protected long expires;
    public float rise,previousRise,rotation,previousRotation,tilt,previousTilt;
    protected DynamoEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type,pos,state); }
    // Modern endpoint validation requires a positive range. Relays still use their own
    // connection range; idle dynamos must not become one-block relay consumers.
    @Override public int range() { return 1; }
    @Override public void tickNetwork() {
        if(level==null||level.isClientSide)return;
        if(active())super.tickNetwork();else VisNetwork.unregister(this);
    }
    @Override public boolean source() { return !output.isEmpty(); }
    public boolean active() { return level != null && level.getGameTime() < expires && source(); }
    public int visualColor(){
        long r=0,g=0,b=0,n=0;
        for(var aspect:output.aspects()){int amount=output.amount(aspect);r+=(long)(aspect.color()>>16&255)*amount;g+=(long)(aspect.color()>>8&255)*amount;b+=(long)(aspect.color()&255)*amount;n+=amount;}
        return n==0?0xffffff:(int)(r/n)<<16|(int)(g/n)<<8|(int)(b/n);
    }
    @Override public AspectAmounts revealedAspects() { return active() ? output.copy() : new AspectAmounts(); }
    protected void supply(AspectAmounts amounts, int duration) {
        boolean active = source();
        // Disconnect while source() still reports its previous state.
        if (active) refreshNetwork();
        output = amounts.copy(); expires = level.getGameTime()+duration;
        // Do not replenish an already-used budget in this same tick.
        if (budgetTick == level.getGameTime()) for (var aspect:remaining.aspects())
            if (remaining.amount(aspect)>output.amount(aspect)) remaining.remove(aspect,remaining.amount(aspect)-output.amount(aspect));
        refreshNetwork(); tickNetwork(); sync();
    }
    @Override protected int drainSource(Aspect aspect,int requested) {
        if (!active()) return 0;
        if (budgetTick != level.getGameTime()) { remaining=output.copy(); budgetTick=level.getGameTime(); }
        int amount=Math.min(requested,remaining.amount(aspect));
        if (amount>0) remaining.reduce(aspect,amount);
        return amount;
    }
    public void tick() {
        if (level == null) return;
        if(level.isClientSide){
            previousRise=rise;previousRotation=rotation;previousTilt=tilt;
            if(active()) {if(rise<.3f)rise=Math.min(.3f,rise+.02f);else tilt+=2;rotation+=2;}
            else if(tilt%360>0)tilt-=Math.min(8,tilt%360);else rise=Math.max(0,rise-.02f);
            return;
        }
        if (!active() && !output.isEmpty()) { refreshNetwork(); output=new AspectAmounts(); sync(); }
        tickNetwork();
    }
    protected void sync() { setChanged(); if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putLong("Expires",expires); AspectNbt.write(tag,"Output",output);
        // Preserve spent budget across unload/reload within one game tick.
        tag.putLong("BudgetTick",budgetTick); AspectNbt.write(tag,"Remaining",remaining);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); expires=tag.getLong("Expires"); output=AspectNbt.read(tag,"Output",TCAspects.catalog()).amounts();
        for(var aspect:output.aspects()) if(!aspect.isPrimal()||output.amount(aspect)<0||output.amount(aspect)>12)output.remove(aspect);
        budgetTick=tag.contains("BudgetTick")?tag.getLong("BudgetTick"):Long.MIN_VALUE;
        remaining=AspectNbt.read(tag,"Remaining",TCAspects.catalog()).amounts();
        for(var aspect:remaining.aspects()) if(remaining.amount(aspect)<0||remaining.amount(aspect)>output.amount(aspect))remaining.remove(aspect);
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
