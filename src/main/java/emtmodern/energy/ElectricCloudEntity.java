package emtmodern.energy;
import emtmodern.ElectroMagicTools;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public class ElectricCloudEntity extends BlockEntity {
    public ElectricCloudEntity(BlockPos pos,BlockState state){super(ElectroMagicTools.CLOUD_BE.get(),pos,state);}
}
