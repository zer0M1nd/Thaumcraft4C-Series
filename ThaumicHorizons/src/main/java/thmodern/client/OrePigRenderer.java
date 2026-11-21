package thmodern.client;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Pig;
public final class OrePigRenderer extends PigRenderer {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumichorizons:textures/entity/oreboar.png");
    public OrePigRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.7f;}
    @Override public ResourceLocation getTextureLocation(Pig pig){return TEXTURE;}
}
