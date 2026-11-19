package thmodern.client;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;
import thmodern.Horizons;
import thmodern.creature.InfusedCat;
public final class InfusedCatRenderer extends CatRenderer {
    public InfusedCatRenderer(EntityRendererProvider.Context context){super(context);model=new FamiliarModel();layers.clear();shadowRadius=.5f;}
    @Override public ResourceLocation getTextureLocation(Cat entity){
        if(entity.getType()==Horizons.GRAVEKEEPER.get())return ResourceLocation.parse("thaumichorizons:textures/entity/gravekeeper.png");
        if(entity.getType()==Horizons.FAMILIAR.get())return ResourceLocation.parse("thaumichorizons:textures/entity/familiar.png");
        return ((InfusedCat)entity).ocelotAppearance()?ResourceLocation.parse("minecraft:textures/entity/cat/ocelot.png"):super.getTextureLocation(entity);
    }
    @Override protected void scale(Cat entity,com.mojang.blaze3d.vertex.PoseStack pose,float partial){super.scale(entity,pose,partial);shadowRadius=((InfusedCat)entity).panther()?1:.5f;if(((InfusedCat)entity).panther())pose.scale(2,2,2);}
}
