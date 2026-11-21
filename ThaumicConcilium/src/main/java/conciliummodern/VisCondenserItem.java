package conciliummodern;

public final class VisCondenserItem extends net.minecraft.world.item.BlockItem {
    public VisCondenserItem(){super(ConciliumBlocks.CONDENSER.get(),new Properties());}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return VisCondenserRenderer.itemRenderer();}
        });
    }
}
