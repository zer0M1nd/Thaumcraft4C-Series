package tc4revival.content;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;

/** Both halves expose the same catalyst slot and essentia controller. */
public  class ThaumatoriumBlockEntity extends BlockEntity implements WorldlyContainer,MenuProvider,EssentiaTransport,RevealedAspectContainer,ThaumatoriumProcess.Host {
    private final ThaumatoriumProcess process=new ThaumatoriumProcess();
    private ItemStack input=ItemStack.EMPTY;
    private int venting;
    private LazyOptional<IItemHandler> automation=LazyOptional.of(()->new InvWrapper(this));
    public ThaumatoriumBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.THAUMATORIUM_ENTITY.get(),pos,state); }
    public boolean upper() { return getBlockState().getValue(ThaumatoriumBlock.UPPER); }
    public ThaumatoriumBlockEntity controller() {
        if (!upper()) return this;
        return level!=null && level.hasChunkAt(worldPosition.below()) && level.getBlockEntity(worldPosition.below()) instanceof ThaumatoriumBlockEntity core && !core.upper() ? core : null;
    }
    public ThaumatoriumProcess process() { var core=controller(); return core==null ? process : core.process; }
    public Direction facing() { var core=controller(); return (core==null ? getBlockState() : core.getBlockState()).getValue(ThaumatoriumBlock.FACING); }
    public void tick() {
        if (level==null) return;
        if (level.isClientSide) {
            if (!upper() && venting>0) {
                venting--; float x=.1f-level.random.nextFloat()*.2f,z=.1f-level.random.nextFloat()*.2f,y=.1f-level.random.nextFloat()*.2f;
                float dx=.1f-level.random.nextFloat()*.2f,dz=.1f-level.random.nextFloat()*.2f,dy=.1f-level.random.nextFloat()*.2f;
                tc4revival.client.EssentiaVentParticles.emit(level,worldPosition.getX()+.5+x+facing().getStepX()/2f,worldPosition.getY()+.5+y,
                        worldPosition.getZ()+.5+z+facing().getStepZ()/2f,facing().getStepX()/4f+dx,dy,facing().getStepZ()/4f+dz,0xFFFFFF);
            }
            return;
        }
        if (upper()) {
            if (controller()==null) level.setBlock(worldPosition,TCArcane.ALCHEMICAL_CONSTRUCT.get().defaultBlockState(),3);
        } else process.tick(this);
    }
    @Override public Optional<CrucibleRecipe> recipe(net.minecraft.resources.ResourceLocation id) {
        if (level==null) return Optional.empty();
        return level.getRecipeManager().byKey(id).filter(CrucibleRecipe.class::isInstance).map(CrucibleRecipe.class::cast);
    }
    @Override public ItemStack input() { return input; }
    @Override public void consumeInput() { removeItem(0,1); }
    @Override public boolean heated() {
        var pos=worldPosition.below(2); if (!level.hasChunkAt(pos)) return false;
        var state=level.getBlockState(pos);
        return CrucibleHeat.heated(state);
    }
    @Override public boolean powered() {
        return level.hasNeighborSignal(worldPosition) || level.hasNeighborSignal(worldPosition.below()) || level.hasNeighborSignal(worldPosition.above());
    }
    @Override public int recipeLimit() {
        int capacity=1;
        for (int y=0;y<=1;y++) for (var side:Direction.values()) if (side!=Direction.DOWN && side!=facing()) {
            var pos=worldPosition.above(y).relative(side);
            if (level.hasChunkAt(pos) && level.getBlockState(pos).is(TCArcane.MNEMONIC_MATRIX.get())
                    && level.getBlockState(pos).getValue(MnemonicMatrixBlock.FACING)==side.getOpposite()) capacity+=2;
        }
        return capacity;
    }
    private Optional<IItemHandler> outputInventory() {
        var pos=worldPosition.offset(facing().getStepX(),0,facing().getStepZ()); if (!level.hasChunkAt(pos)) return Optional.empty();
        var target=level.getBlockEntity(pos); if (target==null) return Optional.empty();
        var capability=target.getCapability(ForgeCapabilities.ITEM_HANDLER,facing().getOpposite()).resolve();
        if (capability.isPresent()) return capability;
        if (target instanceof WorldlyContainer sided) return Optional.of(new SidedInvWrapper(sided,facing().getOpposite()));
        if (target instanceof net.minecraft.world.Container container) return Optional.of(new InvWrapper(container));
        return Optional.empty();
    }
    @Override public boolean outputFits(CrucibleRecipe recipe) {
        if (!level.hasChunkAt(worldPosition.offset(facing().getStepX(),0,facing().getStepZ()))) return false;
        return outputInventory().map(target->ItemHandlerHelper.insertItemStacked(target,recipe.getResultItem(level.registryAccess()),true).isEmpty()).orElse(true);
    }
    @Override public int draw(Aspect aspect) {
        for (int y=0;y<=1;y++) for (var side:Direction.values()) {
            if (side==facing() || side==Direction.DOWN || y==0 && side==Direction.UP) continue;
            var pos=worldPosition.above(y).relative(side);
            if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof EssentiaTransport source)
                    || !source.connectable(side.getOpposite()) || source.essentiaAmount(side.getOpposite())<=0
                    || source.suction(side.getOpposite())>=process.suctionAmount() || source.minimumSuction()>process.suctionAmount()) continue;
            int drawn=source.takeEssentia(aspect,1,side.getOpposite()); if (drawn>0) return drawn;
        }
        return 0;
    }
    @Override public void complete(CrucibleRecipe recipe,String author) {
        var output=recipe.getResultItem(level.registryAccess());
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            var player=server.getServer().getPlayerList().getPlayerByName(author);
            if (player!=null && player.level()==level) net.minecraftforge.event.ForgeEventFactory.firePlayerCraftingEvent(player,output,new net.minecraft.world.SimpleContainer(input.copy()));
        }
        var remainder=output;
        // A handler may accept less than its earlier preflight promised; eject the remainder.
        var inventory=outputInventory();
        if (inventory.isPresent()) remainder=ItemHandlerHelper.insertItemStacked(inventory.get(),output,false);
        if (!remainder.isEmpty()) {
            var entity=new net.minecraft.world.entity.item.ItemEntity(level,worldPosition.getX()+.5+facing().getStepX()*.66,
                    worldPosition.getY()+.33+facing().getOpposite().getStepY(),worldPosition.getZ()+.5+facing().getStepZ()*.66,remainder.copy());
            entity.setDeltaMovement(.075f*facing().getStepX(),.025f,.075f*facing().getStepZ()); level.addFreshEntity(entity);
            level.blockEvent(worldPosition,getBlockState().getBlock(),0,0);
        }
        level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.25f,
                2.6f+(level.random.nextFloat()-level.random.nextFloat())*.8f);
    }
    @Override public void changed() {
        setChanged(); if (level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    @Override public AspectAmounts revealedAspects() { return process().stored(); }
    @Override public boolean connectable(Direction face) { return controller()!=null && face!=facing(); }
    @Override public boolean canInput(Direction face) { return connectable(face); }
    @Override public boolean canOutput(Direction face) { return false; }
    @Override public Aspect suctionType(Direction face) { return process().suction(); }
    @Override public int suction(Direction face) { return process().suctionAmount(); }
    @Override public int minimumSuction() { return 0; }
    @Override public Aspect essentiaType(Direction face) { return null; }
    @Override public int essentiaAmount(Direction face) { return 0; }
    @Override public int takeEssentia(Aspect aspect,int amount,Direction face) { return 0; }
    @Override public int addEssentia(Aspect aspect,int amount,Direction face) {
        var core=controller(); return core==null || !canInput(face) ? 0 : amount-core.process.add(aspect,amount,core);
    }
    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return getItem(0).isEmpty(); }
    @Override public ItemStack getItem(int slot) { var core=controller(); return slot!=0 || core==null ? ItemStack.EMPTY : core.input; }
    @Override public ItemStack removeItem(int slot,int amount) {
        var core=controller(); if (slot!=0 || core==null || amount<=0) return ItemStack.EMPTY;
        var taken=core.input.split(amount); if (core.input.isEmpty()) core.input=ItemStack.EMPTY; core.changed(); return taken;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { var core=controller(); if (slot!=0 || core==null) return ItemStack.EMPTY; var old=core.input; core.input=ItemStack.EMPTY; return old; }
    @Override public void setItem(int slot,ItemStack stack) {
        var core=controller(); if (slot!=0 || core==null) return; core.input=stack;
        if (stack.getCount()>64) stack.setCount(64); core.changed();
    }
    @Override public void clearContent() { setItem(0,ItemStack.EMPTY); }
    @Override public boolean stillValid(Player player) {
        var core=controller(); return core!=null && level!=null && level.getBlockEntity(core.worldPosition)==core
                && player.distanceToSqr(core.worldPosition.getX()+.5,core.worldPosition.getY()+.5,core.worldPosition.getZ()+.5)<=64;
    }
    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side) { return slot==0; }
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side) { return slot==0; }
    @Override public Component getDisplayName() { return Component.translatable("container.alchemyfurnace"); }
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player) { return new ThaumatoriumMenu(id,inventory,controller()); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); if (!upper()) { process.save(tag); tag.put("Input",input.save(new CompoundTag())); }
    }
    @Override public void load(CompoundTag tag) { super.load(tag); process.load(tag); input=ItemStack.of(tag.getCompound("Input")); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public boolean triggerEvent(int event,int parameter) { if (event>=0) { if (level!=null && level.isClientSide) venting=7; return true; } return super.triggerEvent(event,parameter); }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox() { return new net.minecraft.world.phys.AABB(worldPosition.getX()-1,worldPosition.getY(),worldPosition.getZ()-1,worldPosition.getX()+2,worldPosition.getY()+2,worldPosition.getZ()+2); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction side) {
        return capability==ForgeCapabilities.ITEM_HANDLER && !isRemoved() ? automation.cast() : super.getCapability(capability,side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); automation.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); automation=LazyOptional.of(()->new InvWrapper(this)); }
}
