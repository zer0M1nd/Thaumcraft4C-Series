package emtmodern.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.*;

public class PortableNodeEntity extends AuraNodeBlockEntity {
    public PortableNodeEntity(BlockPos pos,BlockState state){super(emtmodern.ElectroMagicTools.PORTABLE_NODE_BE.get(),pos,state);}
    @Override public AuraNodeType nodeType(){return AuraNodeType.UNSTABLE;}
    @Override public AuraNodeModifier nodeModifier(){return AuraNodeModifier.FADING;}
}
