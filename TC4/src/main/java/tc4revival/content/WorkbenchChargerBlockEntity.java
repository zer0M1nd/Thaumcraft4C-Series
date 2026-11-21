package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.TCAspects;

public  class WorkbenchChargerBlockEntity extends VisRelayBlockEntity implements thaumcraft.api.devices.DeviceExtensions.Host {
    private final thaumcraft.api.devices.DeviceExtensions.Store extensions=new thaumcraft.api.devices.DeviceExtensions.Store(this);
    @Override public thaumcraft.api.devices.DeviceExtensions.Store extensions(){return extensions;}
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> OPEN_CONTAINERS=net.minecraft.tags.BlockTags.create(net.minecraft.resources.ResourceLocation.parse("thaumcraft:open_vis_containers"));
    public static boolean openContainer(net.minecraft.world.level.Level level,BlockPos pos) { return level.hasChunkAt(pos)&&level.getBlockState(pos).is(OPEN_CONTAINERS); }
    private final thaumcraft.api.devices.ChargeBudget<tc4revival.aspect.Aspect> chargeBudget=new thaumcraft.api.devices.ChargeBudget<>(5);
    public int chargeAllowance(tc4revival.aspect.Aspect aspect,int requested){return level==null||level.isClientSide?0:chargeBudget.allowance(level.getGameTime(),aspect,requested);}
    @Override public int consumeVis(tc4revival.aspect.Aspect aspect,int requested){
        return level==null||level.isClientSide||isRemoved()?0:chargeBudget.pay(level.getGameTime(),aspect,requested,amount->super.consumeVis(aspect,amount));
    }
    public boolean extendedSupport() { return level!=null&&openContainer(level,worldPosition.below()); }
    public WorkbenchChargerBlockEntity(BlockPos pos, BlockState state) { super(TCArcane.WORKBENCH_CHARGER_ENTITY.get(), pos, state); }
    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag){super.saveAdditional(tag);extensions.save(tag);}
    @Override public void load(net.minecraft.nbt.CompoundTag tag){super.load(tag);extensions.load(tag);}
    @Override public void invalidateCaps(){super.invalidateCaps();extensions.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();extensions.revive();}
    @Override public void onChunkUnloaded(){extensions.unload();super.onChunkUnloaded();}
    @Override public boolean charger() { return true; }
    @Override public net.minecraft.world.phys.Vec3 beamPoint() { return net.minecraft.world.phys.Vec3.atCenterOf(worldPosition).add(0,.05,0); }
    @Override public void tick() {
        super.tick();
        if (level == null || level.isClientSide || !level.hasChunkAt(worldPosition.below())) return;
        extensions.tick();
        if(thaumcraft.api.devices.VisChargingTargets.process(this,level.getBlockEntity(worldPosition.below())))return;
        if(level.getBlockEntity(worldPosition.below()) instanceof ArcaneWorkbenchBlockEntity table) {
            if(VisCharging.charge(table.getItem(10),this))table.setChanged();
        } else if(extendedSupport()&&level.getBlockEntity(worldPosition.below()) instanceof net.minecraft.world.Container container) {
            for(int slot=0;slot<container.getContainerSize();slot++)if(VisCharging.charge(container.getItem(slot),this))container.setChanged();
        }
    }
}
