package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.*;
import tc4revival.content.*;

/** Crystal capacity is not a pipe-accessible essentia tank in the original. */
public final class DestabilizedCrystalBlockEntity extends BlockEntity implements RevealedAspectContainer {
    private String aspect="";
    private int amount;
    private boolean draining;
    private long drainUntil;
    public DestabilizedCrystalBlockEntity(BlockPos pos,BlockState state) { super(ConciliumBlocks.CRYSTAL_TILE.get(),pos,state); }
    public String aspectKey() { return aspect; }
    public int amount() { return amount; }
    public boolean draining() { return draining; }
    public void refreshDrainAnimation(){if(level!=null && !level.isClientSide){drainUntil=level.getGameTime()+5;setDraining(true);}}
    public static void tick(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,DestabilizedCrystalBlockEntity crystal){
        if(!level.isClientSide && crystal.draining && level.getGameTime()>crystal.drainUntil)crystal.setDraining(false);
    }
    public void setDraining(boolean value) { if(draining!=value){draining=value;changed();} }
    @Override public AspectAmounts revealedAspects() {
        var result=new AspectAmounts();
        TCAspects.catalog().find(aspect).ifPresent(value->result.add(value,amount));
        return result;
    }
    public boolean insertCrystal(Player player,ItemStack stack) {
        if(level==null || level.isClientSide || !stack.is(TCItems.ESSENCE_CRYSTAL.get()) || !(stack.getItem() instanceof StoredAspectItem stored))return false;
        var contents=stored.storedAspects(stack);
        if(contents.size()!=1)return false;
        var value=contents.aspects().get(0);
        if(contents.amount(value)<=0 || amount==Integer.MAX_VALUE || (amount>0 && !aspect.equals(value.tag())))return false;
        aspect=value.tag();amount++;
        if(!player.getAbilities().instabuild)stack.shrink(1);
        changed();return true;
    }
    public int drain(String expected,int requested) {
        if(level==null || level.isClientSide || requested<=0 || !aspect.equals(expected))return 0;
        int taken=Math.min(amount,requested);
        if(taken>0){amount-=taken;changed();}
        return taken;
    }
    private void changed() {
        setChanged();
        if(level!=null && !level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);tag.putString("aspect",aspect);tag.putInt("capacity",amount);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);aspect=tag.getString("aspect");amount=Math.max(0,tag.getInt("capacity"));draining=level!=null && level.isClientSide && tag.getBoolean("draining");drainUntil=0;
        if(aspect.isEmpty())amount=0;
    }
    @Override public CompoundTag getUpdateTag() { var tag=saveWithoutMetadata();tag.putBoolean("draining",draining);return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection connection,ClientboundBlockEntityDataPacket packet) { if(packet.getTag()!=null)load(packet.getTag()); }
}
