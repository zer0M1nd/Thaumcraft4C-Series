package gadomancymodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import gadomancymodern.item.EtherealFamiliarItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class FamiliarItemExtension implements IClientItemExtensions {
    protected Renderer renderer;
    @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new Renderer();return renderer;}
    @Override public boolean applyForgeHandTransform(PoseStack pose,net.minecraft.client.player.LocalPlayer player,net.minecraft.world.entity.HumanoidArm arm,ItemStack stack,float partial,float equip,float swing){return new tc4revival.client.WandItemExtension().applyForgeHandTransform(pose,player,arm,stack,partial,equip,swing);}
    public static class Renderer extends BlockEntityWithoutLevelRenderer {
        public Renderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            var aspect=tc4revival.aspect.TCAspects.catalog().find(EtherealFamiliarItem.aspect(stack));if(aspect.isEmpty())return;pose.pushPose();pose.translate(.5,.5,.5);
            float[] basis;
            if(context==ItemDisplayContext.GUI){
                // Forge 1.7.10 INVENTORY_BLOCK helper: 10 pixels versus modern 16-pixel units.
                pose.scale(.625f,-.625f,-.625f);pose.mulPose(Axis.XP.rotationDegrees(210));pose.mulPose(Axis.YP.rotationDegrees(-45));pose.translate(0,-.45,0);
                basis=new float[]{-.71445745f,.9573291f,.69967884f,-.20220716f,-.20647818f};
            }else if(context.firstPerson()){
                if(context==ItemDisplayContext.FIRST_PERSON_LEFT_HAND)pose.scale(-1,1,1);pose.translate(-.5,-.5,-.5);pose.translate(0,1,.8);
                basis=new float[]{-.4186075f,.99932945f,-.90816724f,.033253096f,-.015327567f};
            }else if(context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND||context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND){
                boolean left=context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND;pose.translate(left?1.0/16:-1.0/16,-.125,.625);pose.mulPose(Axis.YP.rotationDegrees(-180));pose.mulPose(Axis.XP.rotationDegrees(90));if(left)pose.scale(-1,1,1);
                pose.translate(-.0625,.4375,.0625);pose.translate(0,.1875,0);pose.scale(.625f,-.625f,.625f);pose.mulPose(Axis.XP.rotationDegrees(-100));pose.mulPose(Axis.YP.rotationDegrees(45));pose.translate(-.5,-.5,-.5);pose.translate(.5,.5,.7);
                basis=new float[]{.85535365f,.9868404f,-.51804453f,.083717324f,.13822734f};
            }else{pose.scale(.5f,.5f,.5f);pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());basis=new float[]{1,1,0,0,0};}
            pose.scale(1.3f,1.3f,1.3f);pose.translate(0,.45,0);int age=FamiliarVisuals.ticks,frame=Math.floorMod(age,16);float u=frame%4/4f,v=frame/4/4f;
            quad(buffers.getBuffer(tc4revival.client.TCRenderTypes.softParticles(FamiliarVisuals.WISP)),pose,1,u+.25f-.0000390625f,v,u,v+.25f-.0000390625f,aspect.get().color(),basis);
            quad(buffers.getBuffer(tc4revival.client.TCRenderTypes.softParticles(FamiliarVisuals.PARTICLES)),pose,.4f+(float)Math.sin(age/10f)*.1f,(frame+1)/16f,5/16f,frame/16f,6/16f,0xffffff,basis);pose.popPose();
        }
        public static void quad(VertexConsumer out,PoseStack pose,float size,float u1,float v1,float u2,float v2,int color,float[] basis){var h=new org.joml.Vector3f(basis[0],0,basis[2]).mul(size);var v=new org.joml.Vector3f(basis[3],basis[1],basis[4]).mul(size);vertex(out,pose,new org.joml.Vector3f(h).negate().sub(v),u1,v2,color);vertex(out,pose,new org.joml.Vector3f(h).negate().add(v),u1,v1,color);vertex(out,pose,new org.joml.Vector3f(h).add(v),u2,v1,color);vertex(out,pose,new org.joml.Vector3f(h).sub(v),u2,v2,color);}
        public static void vertex(VertexConsumer out,PoseStack pose,org.joml.Vector3f p,float u,float v,int color){out.vertex(pose.last().pose(),p.x(),p.y(),p.z()).color((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1).uv(u,v).uv2(240).endVertex();}
    }
}
