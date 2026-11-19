package thmodern.client;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.Horse;
import thmodern.Horizons;
/** Vanilla undead skins use the current vanilla horse mesh. */
public final class InfusedHorseRenderer extends AbstractHorseRenderer<Horse,HorseModel<Horse>> {
    public InfusedHorseRenderer(EntityRendererProvider.Context context){super(context,new HorseModel<>(context.bakeLayer(ModelLayers.HORSE)),1);
        addLayer(new net.minecraft.client.renderer.entity.layers.HorseArmorLayer(this,context.getModelSet()){
            @Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,Horse horse,float swing,float amount,float partial,float age,float yaw,float pitch){
                if(horse.canWearArmor())super.render(pose,buffers,light,horse,swing,amount,partial,age,yaw,pitch);
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(Horse horse){
        return ResourceLocation.parse("minecraft:textures/entity/horse/horse_"+(horse.getType()==Horizons.UNDEAD_HORSE.get()?"zombie":"skeleton")+".png");
    }
}
