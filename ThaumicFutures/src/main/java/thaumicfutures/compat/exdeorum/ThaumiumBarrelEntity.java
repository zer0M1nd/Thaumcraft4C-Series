package thaumicfutures.compat.exdeorum;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;
import thedarkcolour.exdeorum.blockentity.BarrelBlockEntity;

/** Extends native rain, compost, mixing and fluid capability semantics; special inputs are atomic. */
public class ThaumiumBarrelEntity extends BarrelBlockEntity {
    private static final Ticker NATIVE_TICKER = new Ticker();
    private static final ResourceLocation WITCH_WATER = ResourceLocation.parse("exdeorum:witch_water");
    private int summonTicks;
    private String summon = "";
    private LazyOptional<IItemHandler> items = LazyOptional.of(Inputs::new);
    private LazyOptional<IFluidHandler> fluids = LazyOptional.of(FluidsGuard::new);
    public ThaumiumBarrelEntity(BlockPos pos, BlockState state) { super(pos, state); }
    @Override public BlockEntityType<?> getType() { return ExDeorumCompat.BARREL_ENTITY.get(); }
    public boolean isSummoning() { return !summon.isEmpty(); }
    private IItemHandler nativeItems() { return super.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElseThrow(IllegalStateException::new); }
    private IFluidHandler nativeFluids() { return super.getCapability(ForgeCapabilities.FLUID_HANDLER, null).orElseThrow(IllegalStateException::new); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return items.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluids.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); items.invalidate(); fluids.invalidate(); }
    @Override public void reviveCaps() {
        super.reviveCaps(); items = LazyOptional.of(Inputs::new); fluids = LazyOptional.of(FluidsGuard::new);
    }
    @Override public boolean hasNoSolids() { return !isSummoning() && super.hasNoSolids(); }
    @Override public void tryInWorldFluidMixing() { if (!isSummoning()) super.tryInWorldFluidMixing(); }
    @Override public void updateFluidTransform() { if (!isSummoning()) super.updateFluidTransform(); }
    private String conversion(ItemStack stack) {
        if (level == null || !ExDeorumCompat.enabled() || isSummoning() || !super.hasNoSolids()
                || getTank().getFluidAmount() != 1000) return "";
        var fluid = getTank().getFluid().getFluid();
        if (fluid == Fluids.WATER && ExDeorumCompat.FREEZING_DOLL != null && stack.is(ExDeorumCompat.FREEZING_DOLL.get()))
            return "thermal:blizz";
        if (!WITCH_WATER.equals(ForgeRegistries.FLUIDS.getKey(fluid))) return "";
        if (stack.is(ExDeorumCompat.PECH_DOLL.get())) return "thaumcraft:pech";
        if (stack.is(Items.OBSIDIAN)) return "item:thaumcraft:obsidian_totem";
        if (ResourceLocation.parse("thaumicfutures:bee_trap").equals(ForgeRegistries.ITEMS.getKey(stack.getItem())))
            return "item:thaumicfutures:infused_bee_trap";
        return "";
    }
    private void convert(String result) {
        nativeFluids().drain(1000, IFluidHandler.FluidAction.EXECUTE);
        progress = 0; currentTransformRecipe = null;
        if (result.startsWith("item:")) {
            var output = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(result.substring(5)));
            ((IItemHandlerModifiable) nativeItems()).setStackInSlot(0, new ItemStack(output));
        } else { summon = result; summonTicks = 1000; }
        markUpdated();
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (isSummoning()) return InteractionResult.sidedSuccess(level.isClientSide);
        var held = player.getItemInHand(hand);
        var result = conversion(held);
        if (!result.isEmpty()) {
            if (!level.isClientSide) { convert(result); if (!player.getAbilities().instabuild) held.shrink(1); }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.use(level, player, hand);
    }
    public void tick() {
        if (!isSummoning()) { NATIVE_TICKER.tick(level, worldPosition, getBlockState(), this); return; }
        if (!ExDeorumCompat.enabled()) return;
        if (level.isClientSide) {
            if (level.random.nextInt(summonTicks > 0 ? 20 : 5) == 0)
                level.addParticle(summon.equals("thermal:blizz") ? ParticleTypes.SNOWFLAKE : ParticleTypes.WITCH,
                        worldPosition.getX()+.2+level.random.nextDouble()*.6, worldPosition.getY()+1,
                        worldPosition.getZ()+.2+level.random.nextDouble()*.6, 0, .02, 0);
            return;
        }
        if (summonTicks > 0) { summonTicks--; setChanged(); if (summonTicks == 0) markUpdated(); return; }
        if (level.getDifficulty() == Difficulty.PEACEFUL) return;
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(summon));
        if (type == null) return; // Keep paid work if a companion mod is temporarily absent.
        var entity = type.create(level);
        if (entity == null) return;
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-1,-1,-1), worldPosition.offset(1,1,1))) {
            if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos) || level.random.nextInt(10) != 0) continue;
            entity.moveTo(pos.getX()+.5, pos.getY(), pos.getZ()+.5, level.random.nextFloat()*360, 0);
            if (!level.noCollision(entity) || level.containsAnyLiquid(entity.getBoundingBox())) continue;
            if (entity instanceof Mob mob) mob.finalizeSpawn((ServerLevel)level, level.getCurrentDifficultyAt(pos), MobSpawnType.TRIGGERED, null, null);
            if (level.addFreshEntity(entity)) { summon=""; summonTicks=0; markUpdated(); }
            return;
        }
    }
    @Override public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putString("thaumicSummon", summon); tag.putInt("thaumicSummonTicks", summonTicks);
    }
    @Override public void load(CompoundTag tag) {
        summon = tag.getString("thaumicSummon"); summonTicks = Math.max(0, Math.min(1000, tag.getInt("thaumicSummonTicks"))); super.load(tag);
    }
    @Override public void writeVisualData(FriendlyByteBuf buffer) { super.writeVisualData(buffer); buffer.writeUtf(summon); buffer.writeVarInt(summonTicks); }
    @Override public void readVisualData(FriendlyByteBuf buffer) { super.readVisualData(buffer); summon = buffer.readUtf(); summonTicks = buffer.readVarInt(); }
    private final class Inputs implements IItemHandler {
        public int getSlots() { return 1; }
        public ItemStack getStackInSlot(int slot) { return nativeItems().getStackInSlot(slot); }
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || isSummoning() || stack.isEmpty()) return stack;
            String result = conversion(stack);
            if (result.isEmpty()) return nativeItems().insertItem(slot, stack, simulate);
            if (!simulate) convert(result);
            return stack.copyWithCount(stack.getCount()-1);
        }
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return isSummoning() ? ItemStack.EMPTY : nativeItems().extractItem(slot,amount,simulate);
        }
        public int getSlotLimit(int slot) { return nativeItems().getSlotLimit(slot); }
        public boolean isItemValid(int slot, ItemStack stack) {
            return !isSummoning() && (!conversion(stack).isEmpty() || nativeItems().isItemValid(slot,stack));
        }
    }
    private final class FluidsGuard implements IFluidHandler {
        public int getTanks() { return 1; }
        public FluidStack getFluidInTank(int tank) { return nativeFluids().getFluidInTank(tank); }
        public int getTankCapacity(int tank) { return 1000; }
        public boolean isFluidValid(int tank, FluidStack stack) { return !isSummoning() && nativeFluids().isFluidValid(tank,stack); }
        public int fill(FluidStack stack, FluidAction action) { return isSummoning() ? 0 : nativeFluids().fill(stack,action); }
        public FluidStack drain(FluidStack stack, FluidAction action) { return isSummoning() ? FluidStack.EMPTY : nativeFluids().drain(stack,action); }
        public FluidStack drain(int amount, FluidAction action) { return isSummoning() ? FluidStack.EMPTY : nativeFluids().drain(amount,action); }
    }
}
