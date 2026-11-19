package thmodern.node;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class DeviceBlockItem extends BlockItem{
    public DeviceBlockItem(Block block){super(block,new Properties());}
    @Override public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer){consumer.accept(new IClientItemExtensions(){
        private thmodern.client.DeviceItemRenderer renderer;
        @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new thmodern.client.DeviceItemRenderer();return renderer;}
    });}
}
