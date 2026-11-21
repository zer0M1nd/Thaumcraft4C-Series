package thmodern.client;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Slime;
import thmodern.creature.PlanarSlime;
public final class PlanarSlimeRenderer extends SlimeRenderer {
 public PlanarSlimeRenderer(EntityRendererProvider.Context context){super(context);}
 @Override public ResourceLocation getTextureLocation(Slime slime){return ResourceLocation.parse("thaumichorizons:textures/entity/"+((PlanarSlime)slime).kind()+"slime.png");}
}
