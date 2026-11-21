package thaumcraft.api.crafting;

/** Implement on a Block or BlockEntity for state-dependent stabilization; static blocks may use the infusion_stabilizers tag. */
public interface IInfusionStabiliser {
    boolean canStabiliseInfusion(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos);
}
