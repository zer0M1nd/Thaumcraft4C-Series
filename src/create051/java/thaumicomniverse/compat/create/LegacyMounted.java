package thaumicomniverse.compat.create;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.BlockPos;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.fluids.capability.IFluidHandler;
import thaumicomniverse.mixin.Create051StorageAccess;
import thaumicomniverse.mixin.Create051ContraptionAccess;

/** Views of the host's own mounted inventories: no second inventory or save format. */
public record LegacyMounted(MountedStorageManager storage) {
    public static LegacyMounted of(Contraption structure) {
        return new LegacyMounted(((Create051ContraptionAccess)structure).omniverse$legacyStorage());
    }
    public static IItemHandlerModifiable at(MovementContext context) { return of(context.contraption).item(context.localPos); }
    public IItemHandlerModifiable item(BlockPos pos) {
        var mounted=((Create051StorageAccess)storage).omniverse$items().get(pos);
        return mounted==null||!mounted.isValid()?null:mounted.getItemHandler();
    }
    public IFluidHandler fluid(BlockPos pos) {
        var mounted=((Create051StorageAccess)storage).omniverse$fluids().get(pos);
        return mounted==null||!mounted.isValid()?null:mounted.getFluidHandler();
    }
    public IItemHandlerModifiable getAllItems(){return storage.getItems();}
    public IFluidHandler getFluids(){return storage.getFluids();}
}
