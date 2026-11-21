package thaumicomniverse.compat.create;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.content.contraptions.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
public class ArcaneMovingStructure extends TranslatingContraption {
    private boolean single;
    private BlockPos excluded;
    public ArcaneMovingStructure(){}
    public ArcaneMovingStructure(boolean single,BlockPos excluded){this.single=single;this.excluded=excluded;}
    protected java.util.Set<BlockPos> normalizeCapturedChests(){
        var changed=new java.util.HashSet<BlockPos>();
        var captured=getBlocks();
        captured.replaceAll((pos,info)->{
            var state=info.state();
            if(!(state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock)
                    ||state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE)==net.minecraft.world.level.block.state.properties.ChestType.SINGLE)return info;
            var partner=captured.get(pos.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(state)));
            if(partner!=null&&partner.state().is(state.getBlock())
                    &&partner.state().getValue(net.minecraft.world.level.block.ChestBlock.FACING)==state.getValue(net.minecraft.world.level.block.ChestBlock.FACING)
                    &&partner.state().getValue(net.minecraft.world.level.block.ChestBlock.TYPE)==state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE).getOpposite())return info;
            changed.add(pos);
            return new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(
                    info.pos(),state.setValue(net.minecraft.world.level.block.ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.SINGLE),info.nbt());
        });
        return changed;
    }
    @Override public void readNBT(Level level,net.minecraft.nbt.CompoundTag tag,boolean spawnData){
        super.readNBT(level,tag,spawnData);
        var changed=normalizeCapturedChests();
        // Saved structures may predate normalization. Older Create versions build render entities during readNBT.
        if(level.isClientSide)for(var pos:changed){
            var blockEntity=CreateClientStructures.blockEntity(this,pos);
            if(blockEntity!=null)blockEntity.setBlockState(getBlocks().get(pos).state());
        }
    }
    @Override public ContraptionType getType(){return ArcaneCreateContent.STRUCTURE.get();}
    @Override protected boolean isAnchoringBlockAt(BlockPos pos){return pos.equals(excluded);}
    @Override public boolean assemble(Level level,BlockPos pos)throws AssemblyException{
        if(single){anchor=pos;bounds=new AABB(BlockPos.ZERO);addBlock(level,pos,capture(level,pos));}
        else if(!searchMovedStructure(level,pos,null))return false;
        if(getBlocks().isEmpty())return false;normalizeCapturedChests();startMoving(level);return true;
    }
}
