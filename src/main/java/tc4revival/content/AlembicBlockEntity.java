package tc4revival.content;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;

/** An alembic exports through pipes and phials; unlike a jar it is not a remote aspect source. */
public  class AlembicBlockEntity extends BlockEntity implements EssentiaVessel,EssentiaTransport,RevealedAspectContainer,WandInteractable {
    private Aspect aspect,filter;
    private int amount;
    private Direction facing=Direction.NORTH;
    public AlembicBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.ALEMBIC_ENTITY.get(),pos,state); }
    @Override public Aspect aspect() { return aspect; }
    public Aspect filter() { return filter; }
    public Direction facing() { return facing; }
    public void face(Direction side) { if (side.getAxis().isHorizontal()) { facing=side; changed(); } }
    @Override public int amount() { return amount; }
    @Override public int capacity() { return 32; }
    @Override public boolean accepts(Aspect selected) { return selected!=null; }
    @Override public boolean acceptsPhials() { return false; }
    @Override public AspectAmounts revealedAspects() { return aspect==null ? new AspectAmounts() : new AspectAmounts().add(aspect,amount); }
    public void label(Aspect selected) { if (filter==null && selected!=null) { if (amount==0) aspect=selected; filter=aspect; changed(); } }
    public void removeLabel() { filter=null; changed(); }
    public void empty() { amount=0; aspect=null; changed(); }
    @Override public int addToContainer(Aspect selected,int quantity) {
        if (selected==null || quantity<0) return quantity;
        if (amount>0 && !selected.equals(aspect)) return quantity;
        int inserted=Math.min(quantity,capacity()-amount); aspect=selected; amount+=inserted; changed(); return quantity-inserted;
    }
    @Override public boolean contains(Aspect selected,int quantity) { return selected!=null && selected.equals(aspect) && quantity>=0 && amount>=quantity; }
    @Override public boolean takeFromContainer(Aspect selected,int quantity) {
        if (!contains(selected,quantity)) return false;
        amount-=quantity; if (amount==0) aspect=null; changed(); return true;
    }
    @Override public boolean connectable(Direction side) { return side!=facing && side!=Direction.DOWN; }
    @Override public boolean canInput(Direction side) { return false; }
    @Override public boolean canOutput(Direction side) { return connectable(side); }
    @Override public Aspect suctionType(Direction side) { return null; }
    @Override public int suction(Direction side) { return 0; }
    @Override public int minimumSuction() { return 0; }
    @Override public Aspect essentiaType(Direction side) { return aspect; }
    @Override public int essentiaAmount(Direction side) { return amount; }
    @Override public int takeEssentia(Aspect selected,int quantity,Direction side) { return canOutput(side) && takeFromContainer(selected,quantity) ? quantity : 0; }
    @Override public int addEssentia(Aspect selected,int quantity,Direction side) { return 0; }
    public void changed() { setChanged(); if (level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); if (aspect!=null) tag.putString("aspect",aspect.tag()); if (filter!=null) tag.putString("AspectFilter",filter.tag());
        tag.putShort("amount",(short)amount); tag.putByte("facing",(byte)facing.get3DDataValue());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); aspect=TCAspects.catalog().find(tag.getString("aspect")).orElse(null); filter=TCAspects.catalog().find(tag.getString("AspectFilter")).orElse(null);
        amount=aspect==null ? 0 : Math.max(0,Math.min(capacity(),tag.getShort("amount"))); facing=Direction.from3DDataValue(tag.getByte("facing"));
        if (!facing.getAxis().isHorizontal()) facing=Direction.NORTH;
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox() { return new net.minecraft.world.phys.AABB(worldPosition).inflate(1,0,1); }
    @Override public InteractionResult onWandUseFirst(ItemStack stack,UseOnContext context) {
        var player=context.getPlayer(); if (player==null) return InteractionResult.PASS;
        if (!level.mayInteract(player,worldPosition) || !player.mayUseItemAt(worldPosition,context.getClickedFace(),stack)) return InteractionResult.FAIL;
        if (!level.isClientSide) face(context.getClickedFace()); return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public Optional<ItemStack> onWandUse(Level level,ItemStack wand,Player player) { return Optional.empty(); }
    @Override public void onWandUseTick(ItemStack wand,Player player,int remaining) {}
    @Override public void onWandRelease(ItemStack wand,Level level,Player player,int remaining) {}
}
