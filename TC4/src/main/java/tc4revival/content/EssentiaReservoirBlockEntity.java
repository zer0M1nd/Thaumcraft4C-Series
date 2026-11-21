package tc4revival.content;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectNbt;
import tc4revival.aspect.TCAspects;

public  class EssentiaReservoirBlockEntity extends BlockEntity implements EssentiaSource,EssentiaTransport,RevealedAspectContainer,WandInteractable {
    public static final int CAPACITY=256;
    private AspectAmounts contents=new AspectAmounts();
    private Direction facing=Direction.DOWN;
    private int ticks;
    private Aspect displayAspect;
    private float red=1,green=1,blue=1,redStep,greenStep,blueStep;
    public Aspect displayAspect() { return displayAspect; }
    public float red() { return red; }
    public float green() { return green; }
    public float blue() { return blue; }
    public EssentiaReservoirBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.ESSENTIA_RESERVOIR_ENTITY.get(),pos,state); }
    public int amount() { return (int)Math.min(Integer.MAX_VALUE,contents.total()); }
    public Direction facing() { return facing; }
    public void face(Direction face) { facing=face; changed(); }
    @Override public AspectAmounts revealedAspects() { return contents.copy(); }
    public int addToContainer(Aspect aspect,int quantity) {
        if (aspect==null || quantity<=0) return quantity;
        int inserted=Math.min(quantity,Math.max(0,CAPACITY-amount()));
        if (inserted>0) { contents.add(aspect,inserted); changed(); }
        return quantity-inserted;
    }
    @Override public boolean contains(Aspect aspect,int quantity) { return aspect!=null && quantity>=0 && contents.amount(aspect)>=quantity; }
    @Override public boolean takeFromContainer(Aspect aspect,int quantity) {
        if (!contains(aspect,quantity)) return false;
        contents.remove(aspect,quantity); changed(); return true;
    }
    @Override public boolean connectable(Direction side) { return side==facing; }
    @Override public boolean canInput(Direction side) { return connectable(side); }
    @Override public boolean canOutput(Direction side) { return connectable(side); }
    @Override public Aspect suctionType(Direction side) { return null; }
    @Override public int suction(Direction side) { return amount()<CAPACITY ? 24 : 0; }
    @Override public int minimumSuction() { return 24; }
    // A real pipe face never advertises a type. Null represents legacy UNKNOWN inspection.
    @Override public Aspect essentiaType(Direction side) { return side==null && amount()>0 ? contents.aspects().get(0) : null; }
    @Override public int essentiaAmount(Direction side) { return amount(); }
    @Override public int takeEssentia(Aspect aspect,int quantity,Direction side) { return canOutput(side) && takeFromContainer(aspect,quantity) ? quantity : 0; }
    @Override public int addEssentia(Aspect aspect,int quantity,Direction side) { return canInput(side) ? quantity-addToContainer(aspect,quantity) : 0; }
    public void tick() {
        if (level==null) return;
        ticks++;
        if (level.isClientSide) {
            if (amount()<=0) return;
            if (level.random.nextInt(Math.max(1,500-amount()))==0) level.playLocalSound(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,
                    TCSounds.CREAK.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1.4f+level.random.nextFloat()*.2f,false);
            if (ticks%20==0 && !contents.isEmpty()) {
                displayAspect=contents.aspects().get(Math.floorMod(ticks/20,contents.size())); int color=displayAspect.color();
                redStep=(red-(color>>16&255)/255f)/20f; greenStep=(green-(color>>8&255)/255f)/20f; blueStep=(blue-(color&255)/255f)/20f;
            }
            if (displayAspect==null) { redStep=0; greenStep=0; blueStep=0; }
            else { red-=redStep; green-=greenStep; blue-=blueStep; }
            return;
        }
        if (ticks%5!=0 || amount()>=CAPACITY) return;
        var adjacent=worldPosition.relative(facing); var side=facing.getOpposite();
        if (!level.hasChunkAt(adjacent) || !(level.getBlockEntity(adjacent) instanceof EssentiaTransport source)
                || !source.connectable(side) || !source.canOutput(side)) return;
        if (source.essentiaAmount(side)<=0 || source.suction(side)>=suction(facing) || suction(facing)<source.minimumSuction()) return;
        var aspect=source.essentiaType(side);
        if (aspect!=null) addToContainer(aspect,source.takeEssentia(aspect,1,side));
    }
    private void changed() {
        setChanged();
        if (level!=null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
            level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());
        }
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); AspectNbt.write(tag,contents); tag.putByte("face",(byte)facing.get3DDataValue()); }
    @Override public void load(CompoundTag tag) {
        super.load(tag); contents=AspectNbt.read(tag,TCAspects.catalog()).amounts();
        for(var aspect:contents.aspects())if(contents.amount(aspect)<0)contents.remove(aspect);
        int face=tag.getByte("face"); facing=face>=0 && face<6 ? Direction.from3DDataValue(face) : Direction.DOWN;
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public InteractionResult onWandUseFirst(ItemStack wand,UseOnContext context) {
        var player=context.getPlayer(); if (player==null) return InteractionResult.PASS;
        if (!context.getLevel().mayInteract(player,worldPosition) || !player.mayUseItemAt(worldPosition,context.getClickedFace(),wand)) return InteractionResult.FAIL;
        if (!context.getLevel().isClientSide) face(player.isShiftKeyDown() ? context.getClickedFace() : context.getClickedFace().getOpposite());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public Optional<ItemStack> onWandUse(Level level,ItemStack wand,Player player) { return Optional.empty(); }
    @Override public void onWandUseTick(ItemStack wand,Player player,int remainingTicks) {}
    @Override public void onWandRelease(ItemStack wand,Level level,Player player,int remainingTicks) {}
}
