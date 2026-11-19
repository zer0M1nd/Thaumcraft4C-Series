package thaumicomniverse.compat.create.airship;

import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.content.contraptions.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;

/** One glued Create structure; station and route nodes can never be captured. */
public final class AirshipStructure extends TranslatingContraption {
    @Override public ContraptionType getType(){return AirshipContent.STRUCTURE.get();}
    @Override protected boolean isAnchoringBlockAt(BlockPos pos){return false;}
    @Override public void readNBT(Level level,CompoundTag tag,boolean spawnData){
        super.readNBT(level,tag,spawnData);
        // Earlier ships were captured without Create's opens_controls tag.
        // Upgrade their saved shell too, so the actor supplies the only levers.
        getBlocks().replaceAll((pos,info)->info.state().is(AirshipContent.CONTROLS.get())
            ?new StructureBlockInfo(pos,info.state().setValue(ControlsBlock.OPEN,true).setValue(ControlsBlock.VIRTUAL,false),info.nbt()):info);
        for(var actor:getActors())if(actor.getLeft().state().is(AirshipContent.CONTROLS.get())){
            var info=getBlocks().get(actor.getLeft().pos());actor.setLeft(info);
            if(actor.getRight()!=null)actor.getRight().state=info.state();
        }
    }
    @Override public boolean assemble(Level level,BlockPos root)throws AssemblyException{
        if(!level.getBlockState(root).is(AirshipContent.CORE.get())||!searchMovedStructure(level,root,null))return false;
        if(getBlocks().values().stream().filter(b->b.state().is(AirshipContent.CORE.get())).count()!=1)throw new IllegalArgumentException("airship.error.cores");
        if(getBlocks().values().stream().noneMatch(b->b.state().is(AirshipContent.CONTROLS.get())))throw new IllegalArgumentException("airship.error.controls");
        if(getBlocks().values().stream().anyMatch(b->b.state().getBlock() instanceof AirshipBlocks.NodeBlock||b.state().is(AirshipContent.SIGNAL.get())))throw new IllegalArgumentException("airship.error.fixed_node");
        startMoving(level);return true;
    }
}
