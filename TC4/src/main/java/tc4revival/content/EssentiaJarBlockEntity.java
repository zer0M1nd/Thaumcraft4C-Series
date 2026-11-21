package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectNbt;
import tc4revival.aspect.TCAspects;

public  class EssentiaJarBlockEntity extends BlockEntity implements EssentiaVessel,EssentiaSource,EssentiaTransport,RevealedAspectContainer {
    @Override public boolean renderExtendedTube() { return true; }
    private Aspect aspect,filter;
    private int amount,ticks;
    private Direction facing=Direction.NORTH;
    public EssentiaJarBlockEntity(BlockPos pos,BlockState state) { this(TCArcane.ESSENTIA_JAR_ENTITY.get(),pos,state); }
    protected EssentiaJarBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state) { super(type,pos,state); }
    public boolean voidJar() { return getBlockState().is(TCArcane.VOID_JAR.get()); }
    @Override public Aspect aspect() { return aspect; }
    public Aspect filter() { return filter; }
    public Direction facing() { return facing; }
    public void face(Direction direction) { if (direction.getAxis().isHorizontal()) { facing=direction; changed(); } }
    @Override public int amount() { return amount; }
    @Override public int capacity() { return 64; }
    @Override public boolean accepts(Aspect offered) { return offered!=null && (filter==null || filter.equals(offered)); }
    @Override public boolean acceptsPhials() { return true; }
    @Override public AspectAmounts revealedAspects() { return aspect!=null && amount>0 ? new AspectAmounts().add(aspect,amount) : new AspectAmounts(); }
    public void label(Aspect selected,Direction side) {
        if (filter!=null || selected==null) return;
        if (amount==0) aspect=selected;
        filter=aspect; if (side.getAxis().isHorizontal()) facing=side; changed();
    }
    public void removeLabel() { filter=null; changed(); }
    public void empty() { amount=0; if (filter==null) aspect=null; changed(); }
    @Override public int addToContainer(Aspect offered,int quantity) {
        if (offered==null || quantity<=0) return quantity;
        if (!accepts(offered)) return quantity;
        if (amount!=0 && !offered.equals(aspect)) return quantity;
        int added=Math.min(quantity,capacity()-amount); boolean dirty=amount<capacity();
        if (added>0 || voidJar()) aspect=offered;
        amount+=added; if (dirty || !voidJar()) changed();
        return voidJar() ? 0 : quantity-added;
    }
    @Override public boolean contains(Aspect requested,int quantity) { return requested!=null && quantity>=0 && requested.equals(aspect) && amount>=quantity; }
    @Override public boolean takeFromContainer(Aspect requested,int quantity) {
        if (!contains(requested,quantity)) return false;
        amount-=quantity; if (amount==0) aspect=null; changed(); return true;
    }
    @Override public boolean connectable(Direction side) { return side==Direction.UP; }
    @Override public boolean canInput(Direction side) { return connectable(side); }
    @Override public boolean canOutput(Direction side) { return connectable(side); }
    @Override public Aspect suctionType(Direction side) { return filter!=null ? filter : aspect; }
    @Override public int minimumSuction() { return filter==null ? 32 : voidJar() ? 48 : 64; }
    @Override public int suction(Direction side) {
        if (voidJar()) return filter!=null && amount<capacity() ? 48 : 32;
        return amount==capacity() ? 0 : minimumSuction();
    }
    @Override public Aspect essentiaType(Direction side) { return aspect; }
    @Override public int essentiaAmount(Direction side) { return amount; }
    @Override public int takeEssentia(Aspect selected,int quantity,Direction side) { return canOutput(side) && takeFromContainer(selected,quantity) ? quantity : 0; }
    @Override public int addEssentia(Aspect selected,int quantity,Direction side) { return canInput(side) ? quantity-addToContainer(selected,quantity) : 0; }
    public void tick() {
        if (level==null || level.isClientSide || ++ticks%5!=0 || (!voidJar() && amount==capacity())) return;
        var above=worldPosition.above(); if (!level.hasChunkAt(above)) return;
        if (!(level.getBlockEntity(above) instanceof EssentiaTransport source) || !source.connectable(Direction.DOWN) || !source.canOutput(Direction.DOWN)) return;
        Aspect wanted=filter!=null ? filter : amount>0 ? aspect : null;
        if (wanted==null && source.essentiaAmount(Direction.DOWN)>0 && source.suction(Direction.DOWN)<suction(Direction.UP) && suction(Direction.UP)>=source.minimumSuction()) wanted=source.essentiaType(Direction.DOWN);
        if (wanted!=null && source.suction(Direction.DOWN)<suction(Direction.UP)) addToContainer(wanted,source.takeEssentia(wanted,1,Direction.DOWN));
    }
    public ItemStack asItem() {
        boolean filled=amount>0 || filter!=null;
        var stack=new ItemStack(voidJar() ? filled ? TCItems.FILLED_VOID_JAR.get() : TCItems.VOID_JAR.get() : filled ? TCItems.FILLED_ESSENTIA_JAR.get() : TCItems.ESSENTIA_JAR.get());
        if (amount>0) AspectNbt.write(stack.getOrCreateTag(),revealedAspects());
        if (filter!=null) stack.getOrCreateTag().putString("AspectFilter",filter.tag());
        return stack;
    }
    public void readItem(ItemStack stack) {
        aspect=null; filter=null; amount=0;
        if (stack.hasTag()) {
            var stored=AspectNbt.read(stack.getTag(),TCAspects.catalog()).amounts();
            if (stored.size()==1) { aspect=stored.aspects().get(0); amount=Math.max(0,Math.min(capacity(),stored.amount(aspect))); }
            filter=TCAspects.catalog().find(stack.getTag().getString("AspectFilter")).orElse(null);
        }
        changed();
    }
    public void changed() {
        thaumcraft.api.aspects.EssentiaStorageApi.changed(this);
        setChanged(); if (level!=null && !level.isClientSide) {
            updateLight();
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
            level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());
        }
    }
    private void updateLight() {
        var state=getBlockState();
        if(level!=null && !level.isClientSide && state.hasProperty(EssentiaJarBlock.LIT)
                && state.getValue(EssentiaJarBlock.LIT)!=(amount>0))
            level.setBlock(worldPosition,state.setValue(EssentiaJarBlock.LIT,amount>0),3);
    }
    @Override public void onLoad() { super.onLoad(); updateLight(); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (aspect!=null) tag.putString("Aspect",aspect.tag()); if (filter!=null) tag.putString("AspectFilter",filter.tag());
        tag.putShort("Amount",(short)amount); tag.putByte("facing",(byte)facing.get3DDataValue());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); aspect=TCAspects.catalog().find(tag.getString("Aspect")).orElse(null); filter=TCAspects.catalog().find(tag.getString("AspectFilter")).orElse(null);
        amount=aspect==null ? 0 : Math.max(0,Math.min(capacity(),tag.getShort("Amount")));
        facing=Direction.from3DDataValue(tag.getByte("facing")); if (!facing.getAxis().isHorizontal()) facing=Direction.NORTH;
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
