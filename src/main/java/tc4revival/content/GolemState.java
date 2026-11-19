package tc4revival.content;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;

/** Authoritative golem data: physical cargo is separate from ghost filter samples and transient AI state. */
public  class GolemState {
    private final Runnable changed;
    private boolean loading;
    private GolemConfiguration config=new GolemConfiguration(GolemMaterial.WOOD,false);
    private final GolemFilterInventory filters;
    private final GolemMarkers markers=new GolemMarkers();
    private BlockPos home=BlockPos.ZERO;
    private Direction homeFacing=Direction.DOWN;
    private thaumcraft.api.golems.GolemAnchor homeAnchor;
    private String owner="";
    private ItemStack carried=ItemStack.EMPTY,watched=ItemStack.EMPTY;
    private FluidStack fluid=FluidStack.EMPTY;
    private Aspect essentia;
    private int essentiaAmount;
    private boolean watchingFluid,paused,inactive;
    public GolemState(Runnable changed) { this.changed=Objects.requireNonNull(changed); filters=new GolemFilterInventory(this::changed); }
    public GolemConfiguration configuration() { return config; }
    public GolemFilterInventory filters() { return filters; }
    public GolemMarkers markers() { return markers; }
    public BlockPos home() { return home; }
    public Direction homeFacing() { return homeFacing; }
    public void home(BlockPos pos,Direction facing) { home=pos.immutable(); homeFacing=Objects.requireNonNull(facing); homeAnchor=null; changed(); }
    public thaumcraft.api.golems.GolemAnchor homeAnchor(){return homeAnchor;}
    public void homeAnchor(thaumcraft.api.golems.GolemAnchor anchor){homeAnchor=anchor;changed();}
    public void projectHome(BlockPos pos,Direction face){home=pos.immutable();homeFacing=face;}
    public String owner() { return owner; }
    public void owner(String name) { owner=Objects.requireNonNull(name); changed(); }
    public ItemStack carried() { return carried.copy(); }
    public void carried(ItemStack stack) { carried=stack.copy(); changed(); }
    public FluidStack fluid() { return fluid.copy(); }
    public void fluid(FluidStack stack) { fluid=stack.copy(); changed(); }
    public Aspect essentia() { return essentia; }
    public int essentiaAmount() { return essentiaAmount; }
    public void essentia(Aspect aspect,int amount) { essentia=aspect; essentiaAmount=Math.max(0,amount); changed(); }
    public ItemStack watched() { return watched.copy(); }
    public void watched(ItemStack stack) { watched=stack.copy(); }
    public boolean watchingFluid() { return watchingFluid; }
    public void watchingFluid(boolean value) { watchingFluid=value; }
    public boolean paused() { return paused; }
    public void paused(boolean value) { paused=value; }
    public boolean inactive() { return inactive; }
    public void inactive(boolean value) { inactive=value; }
    public boolean workEnabled() { return !paused && !inactive; }
    public void configure(GolemConfiguration configuration) {
        config=Objects.requireNonNull(configuration); filters.configure(config); changed();
    }
    public void changed() { if (!loading) changed.run(); }
    public void save(CompoundTag tag) {
        config.save(tag); filters.save(tag);
        tag.putInt("HomeX",home.getX()); tag.putInt("HomeY",home.getY()); tag.putInt("HomeZ",home.getZ()); tag.putByte("HomeFacing",(byte)homeFacing.get3DDataValue());
        if(homeAnchor!=null)tag.put("HomeAnchor",homeAnchor.save());else tag.remove("HomeAnchor");
        tag.putString("Owner",owner); tag.put("ItemCarried",carried.save(new CompoundTag())); tag.put("Markers",markers.save());
        if ((config.core()==GolemCoreType.LIQUID || config.customTask()!=null) && !fluid.isEmpty()) fluid.writeToNBT(tag);
        if ((config.core()==GolemCoreType.ESSENTIA || config.customTask()!=null) && essentia!=null && essentiaAmount>0) {
            tag.putString("essentia",essentia.tag()); tag.putInt("essentiaAmount",essentiaAmount);
        }
    }
    public void load(CompoundTag tag) {
        loading=true;
        try {
            config=GolemConfiguration.load(tag);
            home=new BlockPos(tag.getInt("HomeX"),tag.getInt("HomeY"),tag.getInt("HomeZ"));
            int face=tag.getByte("HomeFacing"); homeFacing=face>=0 && face<6?Direction.from3DDataValue(face):Direction.UP;
            homeAnchor=thaumcraft.api.golems.GolemAnchor.load(tag.getCompound("HomeAnchor"));
            owner=tag.getString("Owner"); carried=ItemStack.of(tag.getCompound("ItemCarried"));
            filters.load(tag,config); markers.load(tag.getList("Markers",Tag.TAG_COMPOUND));
            fluid=(config.core()==GolemCoreType.LIQUID || config.customTask()!=null)?FluidStack.loadFluidStackFromNBT(tag):FluidStack.EMPTY;
            essentia=(config.core()==GolemCoreType.ESSENTIA || config.customTask()!=null)?TCAspects.catalog().find(tag.getString("essentia")).orElse(null):null;
            essentiaAmount=essentia==null?0:Math.max(0,tag.getInt("essentiaAmount"));
            watched=ItemStack.EMPTY; watchingFluid=false; paused=false; inactive=false;
        } finally { loading=false; }
        changed();
    }
    /** Rendering never fabricates an ItemStack from a fluid registry number. */
    public CompoundTag visualData() {
        var tag=new CompoundTag(); config.save(tag); filters.save(tag); tag.putString("Owner",owner);
        tag.put("ItemCarried",carried.save(new CompoundTag()));
        if (!fluid.isEmpty()) tag.put("FluidCarried",fluid.writeToNBT(new CompoundTag()));
        if (essentia!=null) { tag.putString("essentia",essentia.tag()); tag.putInt("essentiaAmount",essentiaAmount); }
        return tag;
    }
    public void applyVisualData(CompoundTag tag) {
        loading=true;
        try {
            config=GolemConfiguration.load(tag); filters.load(tag,config); owner=tag.getString("Owner");
            carried=ItemStack.of(tag.getCompound("ItemCarried")); fluid=FluidStack.loadFluidStackFromNBT(tag.getCompound("FluidCarried"));
            essentia=TCAspects.catalog().find(tag.getString("essentia")).orElse(null); essentiaAmount=Math.max(0,tag.getInt("essentiaAmount"));
        } finally { loading=false; }
    }
}
