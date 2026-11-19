package thaumicomniverse.mixin;

import net.minecraft.core.Direction;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import thaumicomniverse.compat.backpack.EssentiaTankUpgradeItem;

@Mixin(value = BackpackBlockEntity.class, remap = false)
public abstract class BackpackEssentiaMixin implements thaumcraft.api.aspects.IEssentiaTransport,
        tc4revival.content.RevealedAspectContainer, tc4revival.content.EssentiaVessel, tc4revival.content.EssentiaSource {
    @Unique private EssentiaTankUpgradeItem.Wrapper omniverse$tank() {
        return EssentiaTankUpgradeItem.find(((BackpackBlockEntity) (Object) this).getBackpackWrapper());
    }
    @Override public boolean renderExtendedTube() { return true; }
    @Override public boolean connectable(Direction face) { return omniverse$tank() != null; }
    @Override public boolean canInput(Direction face) { return connectable(face); }
    @Override public boolean canOutput(Direction face) { return connectable(face); }
    @Override public Aspect suctionType(Direction face) { return essentiaType(face); }
    @Override public int suction(Direction face) { var tank = omniverse$tank(); return tank != null && tank.amount() < 64 ? 32 : 0; }
    @Override public int minimumSuction() { return 32; }
    @Override public Aspect essentiaType(Direction face) { var tank = omniverse$tank(); return tank == null ? null : tank.aspect(); }
    @Override public int essentiaAmount(Direction face) { var tank = omniverse$tank(); return tank == null ? 0 : tank.amount(); }
    @Override public int takeEssentia(Aspect aspect, int amount, Direction face) {
        var tank = omniverse$tank(); int moved = tank == null ? 0 : tank.extract(aspect, amount, false);
        if (moved > 0) omniverse$changed(); return moved;
    }
    @Override public int addEssentia(Aspect aspect, int amount, Direction face) {
        var tank = omniverse$tank(); int moved = tank == null ? 0 : tank.insert(aspect, amount, false);
        if (moved > 0) omniverse$changed(); return moved;
    }
    @Unique private void omniverse$changed() {
        var owner = (BackpackBlockEntity) (Object) this;
        EssentiaTankUpgradeItem.Wrapper.changed(owner.getLevel(), owner.getBlockPos());
    }
    @Override public AspectAmounts revealedAspects() { var tank = omniverse$tank(); return tank == null ? new AspectAmounts() : tank.contents(); }
    @Override public Aspect aspect() { return essentiaType(Direction.UP); }
    @Override public int amount() { return essentiaAmount(Direction.UP); }
    @Override public int capacity() { return omniverse$tank() == null ? 0 : 64; }
    @Override public boolean accepts(Aspect aspect) { var tank = omniverse$tank(); return tank != null && tank.capacity(aspect) > 0; }
    @Override public boolean acceptsPhials() { return omniverse$tank() != null; }
    @Override public int addToContainer(Aspect aspect, int amount) { return amount - addEssentia(aspect, amount, Direction.UP); }
    @Override public boolean contains(Aspect aspect, int amount) {
        return aspect != null && aspect.equals(aspect()) && amount >= 0 && amount <= amount();
    }
    @Override public boolean takeFromContainer(Aspect aspect, int amount) {
        return contains(aspect, amount) && takeEssentia(aspect, amount, Direction.UP) == amount;
    }
}
