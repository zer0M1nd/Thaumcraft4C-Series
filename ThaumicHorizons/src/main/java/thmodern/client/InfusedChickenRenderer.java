package thmodern.client;

import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Chicken;
import thmodern.creature.InfusedChicken;

public final class InfusedChickenRenderer extends ChickenRenderer {
    private static final ResourceLocation GOLD=ResourceLocation.parse("thaumichorizons:textures/entity/goldchicken.png"),SCHOLAR=ResourceLocation.parse("thaumichorizons:textures/entity/scholarchicken.png");
    public InfusedChickenRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.5f;}
    @Override public ResourceLocation getTextureLocation(Chicken entity){return ((InfusedChicken)entity).golden()?GOLD:SCHOLAR;}
}
