package tc4revival.client;

/** The old projectile body was invisible; particles and its small shadow supply its appearance. */
public  class AlumentumRenderer extends net.minecraft.client.renderer.entity.EntityRenderer<tc4revival.content.AlumentumEntity> {
    public AlumentumRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) { super(context); shadowRadius=.1f; }
    @Override public net.minecraft.resources.ResourceLocation getTextureLocation(tc4revival.content.AlumentumEntity entity) { return net.minecraft.resources.ResourceLocation.parse("thaumcraft:textures/item/alumentum.png"); }
}
