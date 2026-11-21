package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.Aspect;

public  class ArcaneBoreBaseBlockEntity extends BlockEntity implements EssentiaTransport,ArcaneBoreBaseTransport.Host {
    private final ArcaneBoreBaseTransport transport=new ArcaneBoreBaseTransport(this);
    public ArcaneBoreBaseBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.ARCANE_BORE_BASE_ENTITY.get(),pos,state); }
    public ArcaneBoreBaseTransport transport() { return transport; }
    @Override public boolean renderExtendedTube() { return true; }
    @Override public Direction output() { return getBlockState().getValue(ArcaneBoreBlock.FACING); }
    @Override public EssentiaTransport neighbor(Direction side) {
        var pos=worldPosition.relative(side);
        return level!=null && level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof EssentiaTransport source?source:null;
    }
    @Override public boolean connectable(Direction side) { return transport.connectable(side); }
    @Override public boolean canInput(Direction side) { return true; }
    @Override public boolean canOutput(Direction side) { return false; }
    @Override public Aspect suctionType(Direction side) { return transport.suctionType(side); }
    @Override public int suction(Direction side) { return transport.suction(side); }
    @Override public int minimumSuction() { return 0; }
    @Override public Aspect essentiaType(Direction side) { return null; }
    @Override public int essentiaAmount(Direction side) { return 0; }
    @Override public int takeEssentia(Aspect aspect,int amount,Direction side) { return 0; }
    @Override public int addEssentia(Aspect aspect,int amount,Direction side) { return 0; }
}
