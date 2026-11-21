package tc4revival.content;

public  class EssentiaReservoirItem extends net.minecraft.world.item.BlockItem {
    public EssentiaReservoirItem() { super(TCArcane.ESSENTIA_RESERVOIR.get(),new Properties()); }
    @Override public net.minecraft.world.InteractionResult place(net.minecraft.world.item.context.BlockPlaceContext context) {
        var pos=context.getClickedPos(); var result=super.place(context);
        if (result.consumesAction() && !context.getLevel().isClientSide
                && context.getLevel().getBlockEntity(pos) instanceof EssentiaReservoirBlockEntity reservoir)
            reservoir.face(context.getClickedFace().getOpposite());
        return result;
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() { return tc4revival.client.EssentiaReservoirRenderer.itemRenderer(); }
        });
    }
}
