package thmodern.vat;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** The idle vat matrix has the same inventory geometry as the original matrix. */
public final class VatMatrixItem extends BlockItem {
    public VatMatrixItem(Block block){super(block,new Properties());}
    @Override public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer){
        consumer.accept(new IClientItemExtensions(){
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return tc4revival.client.InfusionMatrixRenderer.itemRenderer();}
        });
    }
}
