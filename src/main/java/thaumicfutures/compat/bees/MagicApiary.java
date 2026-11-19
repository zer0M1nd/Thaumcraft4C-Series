package thaumicfutures.compat.bees;

import forestry.api.apiculture.*;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.genetics.IGenome;
import forestry.api.genetics.IMutation;
import forestry.apiculture.ApiaryBeeListener;
import forestry.apiculture.IApiary;
import forestry.apiculture.inventory.IApiaryInventory;
import forestry.apiculture.inventory.InventoryApiary;
import forestry.apiculture.tiles.TileBeeHousingBase;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkHooks;

/** Forestry's inventory, frame listener and breeding engine with the original magic-apiary modifiers. */
public final class MagicApiary extends TileBeeHousingBase implements IApiary {
    private final InventoryApiary inventory = new InventoryApiary();
    private final IBeeListener listener = new ApiaryBeeListener(this);
    private final long[] expiry = new long[3];
    private BlockPos provider;
    private final IBeeModifier modifier = new IBeeModifier() {
        @Override public float modifyMutationChance(IGenome genome, IGenome mate, IMutation<IBeeSpecies> recipe, float current) {
            return current * (active(0) ? 2 : 1);
        }
        @Override public float modifyAging(IGenome genome, IGenome mate, float current) { return current * (active(1) ? 2 : 1); }
        @Override public float modifyProductionSpeed(IGenome genome, float current) { return current * (active(2) ? 1.8f : .9f); }
        @Override public float modifyGeneticDecay(IGenome genome, float current) { return current * .8f; }
    };
    public MagicApiary(BlockPos pos, BlockState state) {
        super(BeeAuraContent.APIARY_TILE.get(), pos, state, "apiary");
        setInternalInventory(inventory);
    }
    private boolean active(int channel) { return level != null && expiry[channel] > level.getGameTime(); }
    public int boostFlags() { return (active(0) ? 1 : 0) | (active(1) ? 2 : 0) | (active(2) ? 4 : 0); }
    @Override public IBeeHousingInventory getBeeInventory() { return inventory; }
    @Override public IApiaryInventory getApiaryInventory() { return inventory; }
    @Override public Collection<IBeeModifier> getBeeModifiers() {
        var result = new ArrayList<IBeeModifier>();
        result.add(modifier);
        for (var frame : inventory.getFrames()) result.add(frame.getA().getBeeModifier(frame.getB()));
        return result;
    }
    @Override public Iterable<IBeeListener> getBeeListeners() { return List.of(listener); }
    @Override public void serverTick(Level world, BlockPos pos, BlockState state) {
        if (!thaumicfutures.FutureFeatures.Group.FORESTRY_BEES.enabled()) return;
        long now = world.getGameTime();
        if (now % 5 == 0) {
            var source = findProvider(world, pos);
            if (source != null) for (int channel = 0; channel < expiry.length; channel++) {
                if (expiry[channel] <= now && source.takeCharge(channel)) {
                    // Reset on renewal: the legacy timer otherwise consumes another charge every tick after expiry.
                    expiry[channel] = now + BeeAuraContent.DURATIONS[channel];
                    setChanged();
                }
            }
        }
        super.serverTick(world, pos, state);
    }
    private VisAuraProvider findProvider(Level world, BlockPos pos) {
        if (provider != null && world.hasChunkAt(provider) && pos.distSqr(provider) <= 36
                && world.getBlockEntity(provider) instanceof VisAuraProvider source) return source;
        provider = null;
        // Choose the nearest provider in loaded chunks; do not load neighbors for a booster.
        VisAuraProvider nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (int x = (pos.getX() - 6) >> 4; x <= (pos.getX() + 6) >> 4; x++)
            for (int z = (pos.getZ() - 6) >> 4; z <= (pos.getZ() + 6) >> 4; z++) {
                var chunk = world.getChunkSource().getChunkNow(x, z);
                if (chunk == null) continue;
                for (var candidate : chunk.getBlockEntities().values()) if (candidate instanceof VisAuraProvider source) {
                    double squared = pos.distSqr(source.getBlockPos());
                    if (squared <= 36 && squared < distance) { distance = squared; nearest = source; }
                }
            }
        if (nearest != null) provider = nearest.getBlockPos();
        return nearest;
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        long[] saved = tag.getLongArray("OmniverseAuraExpiry");
        for (int i = 0; i < expiry.length; i++) expiry[i] = i < saved.length ? saved[i] : 0;
        provider = null;
    }
    @Override public void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putLongArray("OmniverseAuraExpiry", expiry); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MagicApiaryMenu(id, inventory, this);
    }
    @Override public void openGui(ServerPlayer player, InteractionHand hand, BlockPos pos) {
        NetworkHooks.openScreen(player, this, pos);
    }
}
