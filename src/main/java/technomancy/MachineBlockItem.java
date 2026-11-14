package technomancy;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
/** Uses vanilla BlockEntityTag restoration; vessel loot preserves its contents. */
public class MachineBlockItem extends BlockItem {
    public MachineBlockItem(Block b,Properties p){super(b,p);}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return technomancy.client.MachineRenderer.itemRenderer();}
        });
    }
}
