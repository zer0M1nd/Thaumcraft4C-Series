package tc4revival.content;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.PlayerKnowledge;

public  class CrucibleBlockEntity extends BlockEntity implements RevealedAspectContainer,WandInteractable,thaumcraft.api.devices.DeviceExtensions.Host {
    private final thaumcraft.api.devices.DeviceExtensions.Store extensions=new thaumcraft.api.devices.DeviceExtensions.Store(this,"AutomationItems","Aspects");
    @Override public thaumcraft.api.devices.DeviceExtensions.Store extensions(){return extensions;}
    private final CrucibleContents contents=new CrucibleContents(TCAspects.catalog());
    private final CrucibleFluidHandler fluids=new CrucibleFluidHandler(contents,this::synchronize);
    private LazyOptional<IFluidHandler> capability=LazyOptional.of(()->fluids);
    private final CrucibleAutomation automation = new CrucibleAutomation(this);
    private LazyOptional<net.minecraftforge.items.IItemHandler> items = LazyOptional.of(() -> automation);
    public CrucibleAutomation automation() { return automation; }
    private boolean processing;
    /** Retained for binary compatibility. Container automation no longer has a player identity. */
    @Deprecated public UUID automationOwner() { return null; }
    @Deprecated public void claimAutomation(Player player) {}
    public CrucibleBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.CRUCIBLE_ENTITY.get(),pos,state); }
    public CrucibleContents contents() { return contents; }
    public IFluidHandler fluids() { return fluids; }
    @Override public AspectAmounts revealedAspects() { return contents.aspects(); }
    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        var below=level.getBlockState(worldPosition.below());
        boolean soulHeat=CrucibleHeat.soul(below);
        boolean heated=CrucibleHeat.heated(below);
        int before=contents.heat();
        // Bellows are discovered by type once that device is present, independent of facing as in TC4.
        int bellows=0;
        for (var side : Direction.Plane.HORIZONTAL) {
            var neighbor=worldPosition.relative(side);
            if (level.hasChunkAt(neighbor) && level.getBlockState(neighbor).is(CrucibleBlock.BELLOWS)) bellows++;
        }
        var change=contents.tick(heated,soulHeat,bellows,level.random,()->FluxSpills.spill(server,worldPosition));
        if (change.synchronize()) synchronize(); else if (before!=contents.heat()) setChanged();
        automation.tick();extensions.tick();
    }
    /** Empty Optional means not hot/wet yet; an empty stack means the ingredient was dissolved or destroyed. */
    public Optional<ItemStack> processAutomation(ItemStack item) {
        if (!(level instanceof ServerLevel server) || processing || item.isEmpty() || !contents.boiling() || contents.water() == 0) return Optional.empty();
        processing = true;
        try {
            var input = new CrucibleRecipe.Input(item.copyWithCount(1), contents.aspects(), key -> true);
            var recipe = CrucibleRecipe.find(level, input);
            if (recipe.isPresent()) {
                var result = recipe.get().assemble(input, level.registryAccess());
                if (!contents.pay(recipe.get())) return Optional.empty();
                level.blockEvent(worldPosition, getBlockState().getBlock(), 2, 5);
                synchronize(); return Optional.of(result);
            }
            var aspects = ItemStackAspects.resolve(item.copyWithCount(1)).orElseGet(AspectAmounts::new);
            // Automation has no item entity to bounce back: aspectless inputs are consumed as requested.
            if (aspects.total() > 0) contents.dissolve(aspects);
            level.blockEvent(worldPosition, getBlockState().getBlock(), 2, 1);
            synchronize(); return Optional.of(ItemStack.EMPTY);
        } finally { processing = false; }
    }
    public void synchronize() {
        setChanged();
        if (level!=null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
            level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());
        }
    }
    public void empty() {
        if (!(level instanceof ServerLevel server) || contents.water()==0 && contents.aspects().isEmpty()) return;
        FluxSpills.empty(server,worldPosition,contents.empty()); synchronize();
        level.blockEvent(worldPosition,getBlockState().getBlock(),2,5);
    }
    public void dissolve(ItemEntity dropped) {
        if (!(level instanceof ServerLevel server) || processing || dropped instanceof SpecialItemEntity || !dropped.isAlive()
                || !contents.boiling() || contents.water()==0) return;
        processing=true; boolean dissolved=false,crafted=false;
        try {
            var saved=new CompoundTag(); dropped.addAdditionalSaveData(saved);
            UUID thrower=saved.hasUUID("Thrower") ? saved.getUUID("Thrower") : null;
            Set<String> known=thrower==null ? Set.of() : KnowledgeSavedData.get(server.getServer()).snapshot(thrower).map(PlayerKnowledge::researchMarkers).orElse(Set.of());
            ServerPlayer player=thrower==null ? null : server.getServer().getPlayerList().getPlayer(thrower);
            // Bound the work by the original stack size; debit each item only once.
            int remaining=dropped.getItem().getCount();
            for (int index=0;index<remaining;index++) {
                var item=dropped.getItem(); if (item.isEmpty() || isRemoved()) break;
                var input=new CrucibleRecipe.Input(item,contents.aspects(),research->research.isEmpty() || known.contains(research));
                var recipe=CrucibleRecipe.find(level,input);
                if (recipe.isPresent() && contents.water()>0) {
                    var output=recipe.get().assemble(input,level.registryAccess());
                    if (player!=null) net.minecraftforge.event.ForgeEventFactory.firePlayerCraftingEvent(player,output,new net.minecraft.world.SimpleContainer(item.copy()));
                    if (isRemoved() || !contents.pay(recipe.get())) break;
                    debit(dropped); crafted=true;
                    eject(output); // Catalyst is already debited before any later unsupported item can return early.
                } else {
                    var aspects=ItemStackAspects.resolve(item).orElseGet(AspectAmounts::new);
                    if (!contents.dissolve(aspects)) {
                        dropped.setDeltaMovement((level.random.nextFloat()-level.random.nextFloat())*.2,.35,(level.random.nextFloat()-level.random.nextFloat())*.2);
                        level.playSound(null,dropped.getX(),dropped.getY(),dropped.getZ(),SoundEvents.ITEM_PICKUP,SoundSource.BLOCKS,.2f,1+(level.random.nextFloat()-level.random.nextFloat())*.7f);
                        break;
                    }
                    debit(dropped); dissolved=true;
                }
            }
            if (dissolved) {
                level.playSound(null,worldPosition,TCSounds.BUBBLE.get(),SoundSource.BLOCKS,.2f,1+level.random.nextFloat()*.4f);
                level.blockEvent(worldPosition,getBlockState().getBlock(),2,1);
            }
            if (crafted) level.blockEvent(worldPosition,getBlockState().getBlock(),2,5);
        } finally {
            processing=false;
            if (dissolved || crafted) synchronize();
        }
    }
    private static void debit(ItemEntity dropped) {
        var remaining=dropped.getItem().copy(); remaining.shrink(1);
        dropped.setItem(remaining); if (remaining.isEmpty()) dropped.discard();
    }
    private void eject(ItemStack output) {
        boolean first=true;
        while (!output.isEmpty()) {
            var entity=TCEntities.SPECIAL_ITEM.get().create(level); if (entity==null) return;
            entity.setItem(output.split(output.getMaxStackSize())); entity.setPos(worldPosition.getX()+.5,worldPosition.getY()+.71,worldPosition.getZ()+.5);
            entity.setDefaultPickUpDelay();
            entity.setDeltaMovement(first ? 0 : (level.random.nextFloat()-level.random.nextFloat())*.01,.1,
                    first ? 0 : (level.random.nextFloat()-level.random.nextFloat())*.01);
            level.addFreshEntity(entity); first=false;
        }
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); extensions.save(tag); contents.save(tag); tag.put("AutomationItems", automation.serializeNBT()); }
    @Override public void load(CompoundTag tag) { super.load(tag); contents.load(tag); if (tag.contains("AutomationItems")) automation.deserializeNBT(tag.getCompound("AutomationItems"));  extensions.load(tag); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side) { var extra=extensions.capability(cap,side);if(extra.isPresent())return extra; if (!isRemoved() && cap == ForgeCapabilities.ITEM_HANDLER) return items.cast(); return cap==ForgeCapabilities.FLUID_HANDLER && !isRemoved() ? capability.cast() : super.getCapability(cap,side); }
    @Override public void onChunkUnloaded(){extensions.unload();super.onChunkUnloaded();}
    @Override public void invalidateCaps() { super.invalidateCaps(); extensions.invalidate(); capability.invalidate(); items.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); extensions.revive(); capability=LazyOptional.of(()->fluids); items=LazyOptional.of(()->automation); }
    @Override public InteractionResult onWandUseFirst(ItemStack wand,UseOnContext context) {
        claimAutomation(context.getPlayer());
        if (context.getPlayer()!=null && context.getPlayer().isShiftKeyDown()) empty();
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public Optional<ItemStack> onWandUse(Level level,ItemStack wand,Player player) { if (player.isShiftKeyDown()) empty(); return Optional.of(wand); }
    @Override public void onWandUseTick(ItemStack wand,Player player,int remainingTicks) {}
    @Override public void onWandRelease(ItemStack wand,Level level,Player player,int remainingTicks) {}
}
