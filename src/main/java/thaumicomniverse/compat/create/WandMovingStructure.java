package thaumicomniverse.compat.create;

import com.simibubi.create.api.contraption.*;
import com.simibubi.create.content.contraptions.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Box capture deliberately omits glue: returning these blocks never bonds the selection. */
public class WandMovingStructure extends ArcaneMovingStructure {
    private BlockPos cornerA,cornerB;
    public WandMovingStructure(){}
    public WandMovingStructure(BlockPos a,BlockPos b){cornerA=a;cornerB=b;}
    @Override public ContraptionType getType(){return ArcaneCreateContent.WAND_STRUCTURE.get();}
    @Override public boolean assemble(Level level,BlockPos root)throws AssemblyException{
        if(cornerA==null)return super.assemble(level,root);
        anchor=root;bounds=new AABB(BlockPos.ZERO);
        for(var pos:BlockPos.betweenClosed(cornerA,cornerB)){
            if(!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos))return false;
            var state=level.getBlockState(pos);
            if(state.isAir())continue;
            if(state.getDestroySpeed(level,pos)<0||!BlockMovementChecks.isMovementAllowed(state,level,pos))return false;
            addBlock(level,pos,capture(level,pos));
            if(getBlocks().size()>com.simibubi.create.infrastructure.config.AllConfigs.server().kinetics.maxBlocksMoved.get())
                throw AssemblyException.structureTooLarge();
        }
        if(getBlocks().isEmpty())return false;
        normalizeCapturedChests();startMoving(level);return true;
    }
}
