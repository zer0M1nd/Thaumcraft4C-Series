package tc4revival.content;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;

/** World adapter: transport, wand port selection, persistence and client animation. */
public  class EssentiaTubeBlockEntity extends BlockEntity implements EssentiaTransport,RevealedAspectContainer,WandInteractable,EssentiaTubeFlow.Host {
    private final EssentiaTubeFlow flow;
    private final RandomSource detachedRandom=RandomSource.create();
    private float rotation;
    private int clientVent,ventColor=0xaaaaaa;
    public EssentiaTubeBlockEntity(BlockPos pos,BlockState state) {
        this(TCArcane.ESSENTIA_TUBE_ENTITY.get(),pos,state);
    }
    protected EssentiaTubeBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state) {
        super(type,pos,state);
        flow=new EssentiaTubeFlow(((EssentiaTubeBlock)state.getBlock()).kind(),this);
    }
    public EssentiaTubeFlow flow() { return flow; }
    public float rotation() { return rotation; }
    public int clientVent() { return clientVent; }
    public int ventColor() { return ventColor; }
    public void tick() {
        if (level==null) return;
        if (!level.isClientSide) flow.tick();
        else {
            if (clientVent>0) clientVent--;
            if (flow.kind()==EssentiaTubeFlow.Kind.VALVE) rotation=flow.allowFlow() ? Math.max(0,rotation-20) : Math.min(360,rotation+20);
        }
    }
    public BlockEntity adjacent(Direction side) {
        if (level==null || side==null) return null;
        var pos=worldPosition.relative(side); return level.hasChunkAt(pos) ? level.getBlockEntity(pos) : null;
    }
    public boolean hasTransport(Direction side) {
        var neighbor=adjacent(side);return thaumcraft.api.transport.EssentiaConnections.connects(neighbor,side==null?null:side.getOpposite(),neighbor instanceof EssentiaTransport);
    }
    @Override public EssentiaTransport neighbor(Direction side) {
        return hasTransport(side)&&adjacent(side) instanceof EssentiaTransport other && other.connectable(side.getOpposite()) ? other : null;
    }
    @Override public RandomSource random() { return level==null ? detachedRandom : level.random; }
    @Override public boolean powered() { return level!=null && level.hasNeighborSignal(worldPosition); }
    @Override public int bellows() { return level==null ? 0 : BellowsBlockEntity.count(level,worldPosition,Arrays.asList(Direction.values())); }
    @Override public void changed() {
        setChanged();
        if (level!=null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
            if (flow.kind()==EssentiaTubeFlow.Kind.BUFFER) level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());
        }
    }
    private void sound(net.minecraft.sounds.SoundEvent sound,float volume,float pitch) {
        if (level!=null && !level.isClientSide) level.playSound(null,worldPosition,sound,SoundSource.BLOCKS,volume,pitch);
    }
    @Override public void valveSound() { sound(TCSounds.SQUEEK.get(),.7f,.9f+random().nextFloat()*.2f); }
    @Override public void creak() { if (level!=null) level.blockEvent(worldPosition,getBlockState().getBlock(),0,0); }
    @Override public void vent(Aspect type) { if (level!=null) level.blockEvent(worldPosition,getBlockState().getBlock(),1,type==null?0:tc4revival.aspect.TCAspects.catalog().all().indexOf(type)+1); }
    @Override public boolean triggerEvent(int id,int data) {
        if (id!=0 && id!=1) return super.triggerEvent(id,data);
        if (level!=null && level.isClientSide) {
            if (id==0) level.playLocalSound(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,TCSounds.CREAK.get(),SoundSource.BLOCKS,1,1.3f+random().nextFloat()*.2f,false);
            else {
                if (clientVent<=0) level.playLocalSound(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.1f,1+random().nextFloat()*.1f,false);
                clientVent=50; var types=tc4revival.aspect.TCAspects.catalog().all(); ventColor=data>0 && data<=types.size() ? types.get(data-1).color() : 0xaaaaaa;
            }
        }
        return true;
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.merge(flow.save(true)); }
    @Override public void load(CompoundTag tag) { super.load(tag); flow.load(tag,level==null || !level.isClientSide); }
    @Override public CompoundTag getUpdateTag() { return flow.save(false); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public AspectAmounts revealedAspects() {
        return flow.kind()==EssentiaTubeFlow.Kind.FILTER ? flow.filter()==null ? new AspectAmounts() : new AspectAmounts().add(flow.filter(),-1) : flow.kind()==EssentiaTubeFlow.Kind.BUFFER ? flow.contents() : new AspectAmounts();
    }
    @Override public boolean connectable(Direction side) { return flow.connectable(side); }
    @Override public boolean canInput(Direction side) { return flow.canInput(side); }
    @Override public boolean canOutput(Direction side) { return flow.canOutput(side); }
    @Override public Aspect suctionType(Direction side) { return flow.suctionType(side); }
    @Override public int suction(Direction side) { return flow.suction(side); }
    @Override public int minimumSuction() { return flow.minimumSuction(); }
    @Override public Aspect essentiaType(Direction side) { return flow.essentiaType(side); }
    @Override public int essentiaAmount(Direction side) { return flow.essentiaAmount(side); }
    @Override public int takeEssentia(Aspect aspect,int amount,Direction side) { return flow.takeEssentia(aspect,amount,side); }
    @Override public int addEssentia(Aspect aspect,int amount,Direction side) { return flow.addEssentia(aspect,amount,side); }
    @Override public InteractionResult onWandUseFirst(ItemStack stack,UseOnContext context) {
        var player=context.getPlayer(); if (player==null || level==null) return InteractionResult.PASS;
        if (!level.mayInteract(player,worldPosition) || !player.mayUseItemAt(worldPosition,context.getClickedFace(),stack)) return InteractionResult.FAIL;
        int part=EssentiaTubeBlock.tracePart(this,player);
        if (part<0) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (part<6) {
            var side=Direction.from3DDataValue(part);
            if (flow.kind()==EssentiaTubeFlow.Kind.BUFFER && player.isShiftKeyDown()) {
                flow.cycleChoke(side); sound(TCSounds.SQUEEK.get(),.6f,1.1f+random().nextFloat()*.2f);
            } else {
                var adjacent=adjacent(side);
                if (adjacent instanceof EssentiaTubeBlockEntity tube && (flow.kind()==EssentiaTubeFlow.Kind.BUFFER || tube.flow.kind()!=EssentiaTubeFlow.Kind.BUFFER)) {
                    var pos=tube.getBlockPos();
                    if (!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,side.getOpposite(),stack)) return InteractionResult.FAIL;
                    tube.flow.setOpen(side.getOpposite(),!flow.open(side));
                }
                flow.setOpen(side,!flow.open(side)); sound(TCSounds.TOOL.get(),.5f,.9f+random().nextFloat()*.2f);
            }
        } else if (flow.kind()!=EssentiaTubeFlow.Kind.BUFFER) {
            for (int index=flow.facing().get3DDataValue()+1;index<20;index++) {
                var next=Direction.from3DDataValue(index%6);
                boolean allowed=flow.kind()==EssentiaTubeFlow.Kind.VALVE ? !hasTransport(next) : hasTransport(next.getOpposite()) && flow.connectable(next.getOpposite());
                if (allowed) { flow.face(next); break; }
            }
            sound(TCSounds.TOOL.get(),.5f,.9f+random().nextFloat()*.2f);
        }
        return InteractionResult.CONSUME;
    }
    @Override public Optional<ItemStack> onWandUse(Level level,ItemStack stack,Player player) { return Optional.empty(); }
    @Override public void onWandUseTick(ItemStack stack,Player player,int remaining) {}
    @Override public void onWandRelease(ItemStack stack,Level level,Player player,int remaining) {}
}
