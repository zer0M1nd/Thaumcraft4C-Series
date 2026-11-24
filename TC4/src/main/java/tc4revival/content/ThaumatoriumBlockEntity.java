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
    @Override public boolean soulHeat() {
        var pos=worldPosition.below(2);
        if (!level.hasChunkAt(pos)) return false;
        var heat=level.getBlockState(pos);
        return heat.is(Blocks.SOUL_FIRE) || heat.is(Blocks.SOUL_CAMPFIRE)
                && heat.getValue(net.minecraft.world.level.block.CampfireBlock.LIT);
    }
    @Override public int recipeLimit() { return 1+2*matrixCount(TCArcane.MNEMONIC_MATRIX.get()); }
    @Override public int chaosMatrices() { return matrixCount(TCArcane.CHAOS_MATRIX.get()); }
    private int matrixCount(MnemonicMatrixBlock block) {
        int count=0;
        for (int y=0;y<=1;y++) for (var side:Direction.values()) if (side!=Direction.DOWN && side!=facing()) {
            var pos=worldPosition.above(y).relative(side);
            if (level.hasChunkAt(pos) && level.getBlockState(pos).is(block)
                    && level.getBlockState(pos).getValue(MnemonicMatrixBlock.FACING)==side.getOpposite()) count++;
        }
        return count;
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
        return outputFits(recipe.getResultItem(level.registryAccess()));
    }
    private boolean outputFits(ItemStack result) {
        if (!level.hasChunkAt(worldPosition.offset(facing().getStepX(),0,facing().getStepZ()))) return false;
        return outputInventory().map(target->ItemHandlerHelper.insertItemStacked(target,result,true).isEmpty()).orElse(true);
    }
    @Override public int draw(Aspect aspect) { return drawFromPipe(aspect)==null ? 0 : 1; }
    @Override public Aspect drawAny() { return drawFromPipe(null); }
    private Aspect drawFromPipe(Aspect requested) {
        for (int y=0;y<=1;y++) for (var side:Direction.values()) {
            if (side==facing() || side==Direction.DOWN || y==0 && side==Direction.UP) continue;
            var pos=worldPosition.above(y).relative(side);
            if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof EssentiaTransport source)
                    || !source.connectable(side.getOpposite()) || source.essentiaAmount(side.getOpposite())<=0
                    || source.suction(side.getOpposite())>=process.suctionAmount() || source.minimumSuction()>process.suctionAmount()) continue;
            var offered=source.essentiaType(side.getOpposite());
            var aspect=requested!=null ? requested : offered;
            if (aspect!=null && source.takeEssentia(aspect,1,side.getOpposite())>0) return aspect;
        }
        return null;
    }
    private ItemStack candidateInput=ItemStack.EMPTY;
    private java.util.List<CrucibleRecipe> candidates=java.util.List.of();
    private long candidatesAt=Long.MIN_VALUE;
    private java.util.List<CrucibleRecipe> recipesFor(CrucibleRecipe.Input input,boolean readyOnly) {
        var catalyst=input.getItem(0);
        long now=level.getGameTime();
        if (!ItemStack.isSameItemSameTags(catalyst,candidateInput) || now-candidatesAt>=40 || now<candidatesAt) {
            candidateInput=catalyst.copy();
            candidatesAt=now;
            candidates=level.getRecipeManager().getAllRecipesFor(TCArcane.CRUCIBLE_RECIPE_TYPE.get()).stream()
                    .filter(r->r.catalystMatches(catalyst))
                    .sorted(java.util.Comparator.comparingInt(CrucibleRecipe::legacyOrder).thenComparing(CrucibleRecipe::getId)).toList();
        }
        var available=thaumcraft.api.crafting.MagicRecipeProviders.crucibleCandidates(level,input,readyOnly);
        for (var recipe:candidates)
            if (level.getRecipeManager().byKey(recipe.getId()).orElse(null)==recipe
                    && thaumcraft.api.research.ResearchCategories.enabled(recipe.research())
                    && !recipe.getResultItem(level.registryAccess()).isEmpty()
                    && (!readyOnly || recipe.matches(input,level))) available.add(recipe);
        return available;
    }
    @Override public Optional<ThaumatoriumProcess.Craft> planChaos(AspectAmounts stock) {
        var input=new CrucibleRecipe.Input(input().copy(),stock,key->true);
        for (var recipe:recipesFor(input,true)) {
            var result=recipe.assemble(input,level.registryAccess());
            if (outputFits(result)) return Optional.of(new ThaumatoriumProcess.Craft(recipe,result));
        }
        return Optional.empty();
    }
    public Optional<CrucibleRecipe> closest(ItemStack catalyst,AspectAmounts stock,boolean requireOutput) {
        if (level==null || catalyst.isEmpty()) return Optional.empty();
        var input=new CrucibleRecipe.Input(catalyst,stock,key->true);
        CrucibleRecipe best=null;
        long shortest=Long.MAX_VALUE;
        for (var recipe:recipesFor(input,requireOutput)) {
            var cost=recipe.costs();
            long missing=0;
            for (var entry:cost.view().entrySet()) missing+=Math.max(0,entry.getValue()-stock.amount(entry.getKey()));
            if (requireOutput && (missing!=0 || !outputFits(recipe))) continue;
            if (missing<shortest) {
                best=recipe; shortest=missing;
            }
        }
        return Optional.ofNullable(best);
    }
    @Override public void finishChaos(ThaumatoriumProcess.Craft craft) { deliver(craft.result(),""); }
    @Override public void complete(CrucibleRecipe recipe,String author) {
        deliver(recipe.getResultItem(level.registryAccess()),author);
    }
    private void deliver(ItemStack output,String author) {
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
