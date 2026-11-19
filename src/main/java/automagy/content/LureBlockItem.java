package automagy.content;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
public final class LureBlockItem extends BlockItem {
 public LureBlockItem(Block block){super(block,new Item.Properties());}
 @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new automagy.client.LureItemRenderer();return renderer;}});}
}
