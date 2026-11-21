package thaumicfutures.devices.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumicfutures.devices.*;

public class PatternCrafterRenderer implements BlockEntityRenderer<PatternCrafter> {
    private static final ResourceLocation MODES=ResourceLocation.parse("thaumicfutures:textures/block/tc5/pattern_crafter_modes.png");
    static final ResourceLocation GEAR_MODEL=ResourceLocation.parse("thaumicfutures:item/tc5_brass_gear");
    public PatternCrafterRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(PatternCrafter tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();pose.translate(.5,.75,.5);
        pose.mulPose(Axis.YP.rotationDegrees(switch(tile.getBlockState().getValue(PatternCrafterBlock.FACING)){case WEST->270;case NORTH->180;case EAST->90;default->0;}));
        if(!tile.custom() && !tile.busy())DeviceDraw.texture(pose,buffers,MODES,-.25f,-.25f,.25f,.25f,.501f,tile.preset()/10f,0,(tile.preset()+1)/10f,1,light);
        else {
            DeviceDraw.color(pose,buffers,-.25f,-.25f,.25f,.25f,.502f,0x16110e);
            for(int slot=0;slot<9;slot++) {
                float x=-.20f+(slot%3)*.14f,y=.20f-(slot/3)*.14f;
                if(tile.cursor()==slot)DeviceDraw.color(pose,buffers,x-.016f,y-.114f,x+.114f,y+.016f,.504f,0xa2f7ff);
                DeviceDraw.color(pose,buffers,x,y-.098f,x+.098f,y,.506f,CraftingPatterns.enabled(tile.previewMask(),slot)?0xffc966:0x373031);
            }
        }
        float rot=Mth.lerp(partial,tile.previousRotation,tile.rotation);
        for(int side:new int[]{-1,1}) {
            pose.pushPose();pose.translate(side*.2,-.40625,.05);pose.mulPose(Axis.ZP.rotationDegrees(rot*side));
            pose.scale(.5f,.5f,1.6f);
            var mc=net.minecraft.client.Minecraft.getInstance();
            mc.getItemRenderer().render(new net.minecraft.world.item.ItemStack(thaumicfutures.LegacyContent.BRASS.get()),net.minecraft.world.item.ItemDisplayContext.NONE,false,pose,buffers,light,overlay,mc.getModelManager().getModel(GEAR_MODEL));
            pose.popPose();
        }
        pose.popPose();
    }
}
