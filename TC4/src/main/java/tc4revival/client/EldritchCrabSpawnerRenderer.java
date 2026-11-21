package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import tc4revival.content.EldritchCrabSpawnerBlockEntity;

/** TC4's raised vent is drawn over the ordinary crust cube, not instead of it. */
public class EldritchCrabSpawnerRenderer implements BlockEntityRenderer<EldritchCrabSpawnerBlockEntity> {
    public static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/crabvent.png");
    public EldritchCrabSpawnerRenderer(BlockEntityRendererProvider.Context context) {}
    public static Quaternionf rotation(Direction facing) {
        return switch(facing) {
            case DOWN -> Axis.XP.rotationDegrees(90);
            case UP -> Axis.XP.rotationDegrees(-90);
            case NORTH -> Axis.YP.rotationDegrees(180);
            case SOUTH -> new Quaternionf();
            case WEST -> Axis.YP.rotationDegrees(-90);
            case EAST -> Axis.YP.rotationDegrees(90);
        };
    }
    public static void draw(Direction facing,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();
        pose.translate(.5,.5,.5);pose.mulPose(rotation(facing));
        EldritchMeshes.draw("crabvent","Vent",pose,buffers,RenderType.entityCutout(TEXTURE),light,overlay,1);
        pose.popPose();
    }
    @Override public void render(EldritchCrabSpawnerBlockEntity spawner,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        draw(spawner.facing(),pose,buffers,light,overlay);
    }
}
