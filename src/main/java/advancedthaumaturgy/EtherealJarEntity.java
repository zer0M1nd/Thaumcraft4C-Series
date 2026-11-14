package advancedthaumaturgy;
import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.EssentiaJarBlockEntity;
public class EtherealJarEntity extends EssentiaJarBlockEntity {
    public EtherealJarEntity(BlockPos p,BlockState s){super(AdvancedThaumaturgy.JAR_ENTITY.get(),p,s);}
    @Override public int capacity(){return 256;}
    @Override public int minimumSuction(){return super.minimumSuction()+5;}
    @Override public ItemStack asItem(){var original=super.asItem();var result=new ItemStack(AdvancedThaumaturgy.ETHEREAL_JAR.get());if(original.hasTag())result.setTag(original.getTag().copy());return result;}
}
