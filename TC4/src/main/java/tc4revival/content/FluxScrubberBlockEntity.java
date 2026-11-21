package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.*;

/** Searches a shuffled 33-cube sixteen cells per tick, removing at most one flux quantum. */
public  class FluxScrubberBlockEntity extends BlockEntity implements EssentiaTransport {
    private int essentia,charges,power,phase;
    private final java.util.ArrayDeque<BlockPos> checklist=new java.util.ArrayDeque<>();
    public FluxScrubberBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.FLUX_SCRUBBER_ENTITY.get(),pos,state); }
    public Direction facing() { return getBlockState().getValue(FluxScrubberBlock.FACING); }
    public int power() { return power; } public int charges() { return charges; } public int phase() { return phase; }
    public int uncheckedCount() { return checklist.size(); }
    public void tick() {
        if (level==null) return;
        if (phase==0) phase=level.random.nextInt(1000);
        if (level.isClientSide) return;
        if (charges>=4) { charges-=4; if (level.random.nextInt(4)==0) essentia=Math.min(4,essentia+1); setChanged(); }
        if (power<5) { int drawn=VisNetwork.drain(level,worldPosition,TCAspects.catalog().require("aer"),10); if (drawn>0) { power+=drawn;setChanged(); } }
        if (power>=5) checkFlux();
    }
    private void checkFlux() {
        if (checklist.isEmpty()) {
            var cells=new java.util.ArrayList<BlockPos>(35937);
            for (int x=-16;x<=16;x++) for (int y=-16;y<=16;y++) for (int z=-16;z<=16;z++) cells.add(worldPosition.offset(x,y,z));
            for (int i=cells.size();i>1;i--) java.util.Collections.swap(cells,i-1,level.random.nextInt(i));
            checklist.addAll(cells);
        }
        for (int i=0;i<16&&!checklist.isEmpty();i++) {
            var target=checklist.removeFirst();
            if (worldPosition.distSqr(target)>=256 || level.isOutsideBuildHeight(target) || !level.hasChunkAt(target)) continue;
            var state=level.getBlockState(target);
            if (!state.is(TCArcane.FLUX_GOO.get())&&!state.is(TCArcane.FLUX_GAS.get())) continue;
            int quanta=state.getValue(FiniteFluidBlock.QUANTA);
            var next=quanta>1 ? state.setValue(FiniteFluidBlock.QUANTA,quanta-1) : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            if (!level.setBlock(target,next,3)) continue;
            power-=5;charges++;setChanged();
            var at=net.minecraft.world.phys.Vec3.atLowerCornerOf(target);
            tc4revival.network.TCNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.NEAR.with(()->new net.minecraftforge.network.PacketDistributor.TargetPoint(at.x,at.y,at.z,32,level.dimension())),new tc4revival.network.FocusEffectPacket(level.dimension().location(),tc4revival.network.FocusEffectPacket.Kind.BLOCK_SPARKLE,at,at,1,0xDD00FF));
            return;
        }
    }
    @Override public boolean connectable(Direction face) { return face==facing(); }
    @Override public boolean canInput(Direction face) { return false; }
    @Override public boolean canOutput(Direction face) { return face==facing(); }
    @Override public Aspect suctionType(Direction face) { return null; }
    @Override public int suction(Direction face) { return 0; }
    @Override public int minimumSuction() { return 0; }
    @Override public Aspect essentiaType(Direction face) { return TCAspects.catalog().require("praecantatio"); }
    @Override public int essentiaAmount(Direction face) { return essentia; }
    @Override public int takeEssentia(Aspect aspect,int amount,Direction face) { if(!canOutput(face) || !essentiaType(face).equals(aspect))return 0; int taken=Math.max(0,Math.min(essentia,amount)); if (taken>0) { essentia-=taken;setChanged(); } return taken; }
    @Override public int addEssentia(Aspect aspect,int amount,Direction face) { return 0; }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag);tag.putInt("charges",charges);tag.putInt("power",power);tag.putInt("essentia",essentia); }
    @Override public void load(CompoundTag tag) { super.load(tag);charges=Math.max(0,tag.getInt("charges"));power=Math.max(0,tag.getInt("power"));essentia=Math.max(0,tag.getInt("essentia"));checklist.clear();phase=0; }
}
