package emtmodern.energy;
import emtmodern.ElectroMagicTools;
import ic2.api.energy.prefab.BasicSource;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.*;

public class EssentiaGeneratorBlockEntity extends BlockEntity {
    public final BasicSource energy=new BasicSource(this,1_000_000_000,3);
    private int tick;
    public EssentiaGeneratorBlockEntity(BlockPos pos,BlockState state){super(ElectroMagicTools.GENERATOR_BE.get(),pos,state);}
    public static void tick(Level level,BlockPos pos,BlockState state,EssentiaGeneratorBlockEntity generator){
        if(level.isClientSide)return;
        generator.energy.update();
        if(generator.tick--<=0){generator.tick=19;generator.generate();}
    }
    public int generate(){
        if(!(level instanceof ServerLevel server))return 0;
        var aspect=TCAspects.catalog().require(((EssentiaGeneratorBlock)getBlockState().getBlock()).aspect);
        int consumed=0;
        // Legacy asymmetric [-4,+3] cube; each eligible container supplies one unit per second.
        for(int x=-4;x<4;x++)for(int y=-4;y<4;y++)for(int z=-4;z<4;z++){
            BlockPos sourcePos=worldPosition.offset(x,y,z);
            if(!level.hasChunkAt(sourcePos))continue;
            if(level.getBlockEntity(sourcePos) instanceof tc4revival.content.EssentiaSource source&&source.contains(aspect,1)&&source.takeFromContainer(aspect,1)){
                energy.addEnergy(EmtEnergyConfig.essentiaOutput(aspect)/30.0);consumed++;
                tc4revival.network.EssentiaSourcePacket.send(server,worldPosition,sourcePos,aspect.color());
            }
        }
        boolean active=consumed>0;
        if(getBlockState().getValue(EssentiaGeneratorBlock.ACTIVE)!=active)level.setBlock(worldPosition,getBlockState().setValue(EssentiaGeneratorBlock.ACTIVE,active),3);
        if(active)setChanged();return consumed;
    }
    @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide)energy.onLoad();}
    @Override public void onChunkUnloaded(){energy.onChunkUnload();super.onChunkUnloaded();}
    @Override public void setRemoved(){energy.invalidate();super.setRemoved();}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);energy.writeToNBT(tag);tag.putInt("cycle",tick);}
    @Override public void load(CompoundTag tag){super.load(tag);energy.readFromNBT(tag);energy.setEnergyStored(Math.max(0,Math.min(1_000_000_000,energy.getEnergyStored())));tick=Math.max(0,Math.min(19,tag.getInt("cycle")));}
}
