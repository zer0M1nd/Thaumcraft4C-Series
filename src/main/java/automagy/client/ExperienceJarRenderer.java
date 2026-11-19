package automagy.client;
import automagy.content.ExperienceJarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
public final class ExperienceJarRenderer implements BlockEntityRenderer<ExperienceJarBlockEntity>{
 public ExperienceJarRenderer(BlockEntityRendererProvider.Context context){}
 @Override public void render(ExperienceJarBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){ExperienceRender.jar(tile.experience(),pose,buffers,tile.getLevel()==null?0:tile.getLevel().getGameTime(),overlay);}
}
