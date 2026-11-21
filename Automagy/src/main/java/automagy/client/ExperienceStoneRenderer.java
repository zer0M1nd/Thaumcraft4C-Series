package automagy.client;
import automagy.content.ExperienceStoneBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
public final class ExperienceStoneRenderer implements BlockEntityRenderer<ExperienceStoneBlockEntity>{
 public ExperienceStoneRenderer(BlockEntityRendererProvider.Context context){}
 @Override public void render(ExperienceStoneBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){float f=tile.experience()/(float)tile.capacity();if(f<=0)return;float alpha=f<.2f?.17f+.03f*f/.2f:f;ExperienceRender.box(pose,buffers,net.minecraft.resources.ResourceLocation.parse("automagy:textures/block/experience_stone_overlay.png"),-.001f,-.001f,1.001f,1.001f,alpha,tile.getLevel()==null?0:tile.getLevel().getGameTime(),overlay);}
}
