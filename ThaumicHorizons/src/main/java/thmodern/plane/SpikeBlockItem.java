package thmodern.plane;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
public final class SpikeBlockItem extends BlockItem {
 public SpikeBlockItem(Block block){super(block,new Properties());}
 @Override public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer){consumer.accept(new IClientItemExtensions(){
  private thmodern.client.SpikeItemRenderer renderer;
  @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new thmodern.client.SpikeItemRenderer();return renderer;}
 });}
}
