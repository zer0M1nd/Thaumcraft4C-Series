package thmodern.client;

import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cow;

public final class ChocolateCowRenderer extends CowRenderer {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumichorizons:textures/entity/chocolatecow.png");
    public ChocolateCowRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public ResourceLocation getTextureLocation(Cow entity){return TEXTURE;}
}
