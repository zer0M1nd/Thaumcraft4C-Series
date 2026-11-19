package automagy.client;
import automagy.content.Content;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;
public final class ExperienceJarItemRenderer extends BlockEntityWithoutLevelRenderer {
 public ExperienceJarItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
 @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Content.EXPERIENCE_JAR.get().defaultBlockState(),pose,buffers,light,overlay);var tag=stack.getTagElement("BlockEntityTag");if(tag!=null)ExperienceRender.jar(tag.getInt("XP"),pose,buffers,Minecraft.getInstance().level==null?0:Minecraft.getInstance().level.getGameTime(),overlay);}
}
