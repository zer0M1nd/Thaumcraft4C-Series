package gadomancymodern.item;
import gadomancymodern.Gadomancy;
import net.minecraft.world.item.*;
import net.minecraft.client.renderer.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
public class KnowledgeBookItem extends BlockItem {
    public KnowledgeBookItem(){super(Gadomancy.KNOWLEDGE_BOOK.get(),new Properties());}
    @Override public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer){consumer.accept(new IClientItemExtensions(){
        private BlockEntityWithoutLevelRenderer renderer;
        @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null){var mc=net.minecraft.client.Minecraft.getInstance();var book=new gadomancymodern.client.KnowledgeBookRenderer(null);renderer=new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(),mc.getEntityModels()){
            @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,com.mojang.blaze3d.vertex.PoseStack pose,MultiBufferSource buffers,int light,int overlay){pose.pushPose();if(context==ItemDisplayContext.GUI){pose.scale(1.4f,1.4f,1.4f);pose.translate(0,-.1,0);}else if(context==ItemDisplayContext.GROUND){pose.scale(2,2,2);pose.translate(-.5,-.5,-.5);}else if(context.firstPerson()){pose.translate(-.2,.4,0);pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(30));pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(55));}else if(context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND||context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND){pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(15));pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-50));pose.translate(-.4,0,-1.25);pose.scale(1.8f,1.8f,1.8f);}book.draw(0,0,0,pose,buffers,light,overlay);pose.popPose();}
        };}return renderer;}
    });}
}
