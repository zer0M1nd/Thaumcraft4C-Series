package conciliummodern;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

/** The original uses an ordinary 64x32 biped; its unused fortress overlay stays absent. */
public final class ThaumaturgeRenderer extends HumanoidMobRenderer<Thaumaturge,HumanoidModel<Thaumaturge>> {
    public ThaumaturgeRenderer(EntityRendererProvider.Context context){
        super(context,new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE,0),64,32).bakeRoot()),.5f);
    }
    @Override public ResourceLocation getTextureLocation(Thaumaturge entity){return new ResourceLocation(ThaumicConcilium.ID,"textures/entity/thaumaturge.png");}
}

