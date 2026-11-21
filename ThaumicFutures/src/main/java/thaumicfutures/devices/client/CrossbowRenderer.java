package thaumicfutures.devices.client;

import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import thaumicfutures.devices.AutomatedCrossbow;

public class CrossbowRenderer extends MobRenderer<AutomatedCrossbow,CrossbowModel> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumicfutures:textures/entity/crossbow.png");
    public CrossbowRenderer(EntityRendererProvider.Context context) { super(context,new CrossbowModel(context.bakeLayer(CrossbowModel.LAYER)),.5f); }
    @Override public ResourceLocation getTextureLocation(AutomatedCrossbow entity) { return TEXTURE; }
}
