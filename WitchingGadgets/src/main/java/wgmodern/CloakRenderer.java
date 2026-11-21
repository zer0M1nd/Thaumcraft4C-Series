package wgmodern;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Original curved cloak mesh (15 x 8 panels) and animated kama panels. */
public class CloakRenderer implements ICurioRenderer {
    private static final double[] CIRCLE={.5,.49039,.46194,.41573,.35355,.27779,.19134,.09755,0,-.09755,-.19134,-.27779,-.35355,-.41573,-.46194,-.49039,-.5,-.49039,-.46194,-.41573,-.35355,-.27779,-.19134,-.09755,0,.09755,.19134,.27779,.35355,.41573,.46194,.49039};
    private static final double[] DIVIDER={.3,.725,.75,.8,.825,.9,1,1.1};
    @Override public <T extends LivingEntity,M extends EntityModel<T>> void render(ItemStack stack,SlotContext context,PoseStack pose,RenderLayerParent<T,M> renderer,MultiBufferSource buffers,int light,float limb,float amount,float partial,float age,float headYaw,float pitch){if(WGMagicClient.hidden(stack,context.entity()))return;if(net.minecraftforge.fml.ModList.get().isLoaded("botania")&&BotaniaCosmeticRenderer.render(stack,context,pose,renderer,buffers,light,limb,amount,partial,age,headYaw,pitch))return;if(!(renderer.getModel() instanceof HumanoidModel<?> human)||!(stack.getItem() instanceof CloakItem item))return;int color=item.getColor(stack);float alpha=context.entity().hasEffect(MobEffects.INVISIBILITY)?.1001f:1;if(alpha<1)color=0xffffff;String variant=item.kind.equals("wolf")?"wolf":item.kind.equals("raven")?"raven":"";var texture=new ResourceLocation("witchinggadgets",item.kind.equals("plain")?"textures/tg/cloak.png":"textures/models/cloak"+variant+".png");pose.pushPose();if(item.kama){if(human.crouching)pose.translate(0,.2,0);}else human.body.translateAndRotate(pose);if(!item.kama&&WGConfig.CLOAK_ANIMATION.get()==1){if(human.leftLeg.xRot>0)pose.mulPose(Axis.XP.rotation(human.leftLeg.xRot*.15f));if(human.rightLeg.xRot>0)pose.mulPose(Axis.XP.rotation(human.rightLeg.xRot*.15f));}pose.translate(0,1.45,0);pose.scale(1,-1,1);var out=buffers.getBuffer(RenderType.entityTranslucent(texture));if(item.kama)kama(pose,out,buffers,light,color,alpha,human,context.entity().isSprinting());else cloak(pose,out,light,color,alpha,human);pose.popPose();}
    private static void cloak(PoseStack p,VertexConsumer v,int light,int color,float alpha,HumanoidModel<?> h){double d0p=CIRCLE[0],d1p=CIRCLE[1],d2p=CIRCLE[24],d3p=CIRCLE[25];double angle=Math.toDegrees(Math.max(h.leftLeg.xRot,h.rightLeg.xRot));double stretch=WGConfig.CLOAK_ANIMATION.get()==2&&angle>1?1+.3*angle/90:1;for(int i=0;i<15;i++){int a=i,b=i+1,c=i+24;if(c>31)c-=31;int d=c+1;if(d>31)d-=31;for(int j=0;j<8;j++){double h0=CIRCLE[j]*CIRCLE[j]*7,h1=CIRCLE[j+1]*CIRCLE[j+1]*7;double d0=CIRCLE[a]*1.5*DIVIDER[j],d1=CIRCLE[b]*1.5*DIVIDER[j],d2=CIRCLE[c]*1.5*DIVIDER[j],d3=CIRCLE[d]*1.5*DIVIDER[j];if(j==2)h0*=.975;if(j==1){h1*=.975;h0*=.9;}if(j==0){d0=d1=d2=d3=d0p=d1p=d2p=d3p=0;h0*=.9;h1*=.9;}d2*=stretch;d3*=stretch;quad(p,v,light,color,alpha,0,1,0,new double[][]{{d0p,h0,d2p,i/16d,j/8d},{d0,h1,d2,i/16d,(j+1)/8d},{d1,h1,d3,(i+1)/16d,(j+1)/8d},{d1p,h0,d3p,(i+1)/16d,j/8d}});d0p=d0;d1p=d1;d2p=d2;d3p=d3;}}}
    private static void kama(PoseStack p,VertexConsumer v,MultiBufferSource buffers,int light,int color,float alpha,HumanoidModel<?> human,boolean sprint){double ap=.8125,h=.5625,w=.038,limit=sprint?60:70,l=Math.max(0,Math.toDegrees(human.leftLeg.xRot)/limit)*.4375,r=Math.max(0,Math.toDegrees(human.rightLeg.xRot)/limit)*.4375,dl=.25+l,dr=.25+r;
        quad(p,v,light,color,alpha,1,0,0,new double[][]{{.4375,ap-h+.1,-.125,0,1},{.25+w,ap,-.125,0,0},{.25+w,ap,.125+w,.25,0},{.375,ap-h+l,dl,.25,1}});
        quad(p,v,light,color,alpha,0,0,1,new double[][]{{.375,ap-h+l,dl,.25,1},{0,ap-h-.1+l,dl,.515625,1},{-.03125,ap,.125+w,.515625,0},{.25+w,ap,.125+w,.25,0}});
        quad(p,v,light,color,alpha,0,0,1,new double[][]{{-.0376,ap-h+r,dr,.515625,1},{-.0376,ap,.125+w,.515625,0},{-.0376,ap,.125+w,.484375,0},{-.0376,ap-h+l,dl,.484375,1}});
        quad(p,v,light,color,alpha,0,0,1,new double[][]{{.0376,ap-h+l,dl,.515625,1},{.0376,ap,.125+w,.515625,0},{.0376,ap,.125+w,.484375,0},{.0376,ap-h+l,dl,.484375,1}});
        quad(p,v,light,color,alpha,0,0,1,new double[][]{{-.375,ap-h+r,dr,.75,1},{-.25-w,ap,.125+w,.75,0},{.03125,ap,.125+w,.484375,0},{0,ap-h-.1+r,dr,.484375,1}});
        quad(p,v,light,color,alpha,-1,0,0,new double[][]{{-.375,ap-h+r,dr,.75,1},{-.25-w,ap,.125+w,.75,0},{-.25-w,ap,-.125,1,0},{-.4375,ap-h+.1,-.125,1,1}});
        v=buffers.getBuffer(RenderType.entityTranslucent(new ResourceLocation("witchinggadgets","textures/models/magicalbaubles.png")));double[][] corners={{-.25-w,.125+w},{-.25-w,-.125-w},{.25+w,-.125-w},{.25+w,.125+w}};double[] uv={0,.0625,.1875,.25,.375};for(int i=0;i<4;i++){var a=corners[i];var b=corners[(i+1)%4];quad(p,v,light,0xffffff,alpha,i==0?-1:i==2?1:0,0,i==1?-1:i==3?1:0,new double[][]{{a[0],ap-.125,a[1],uv[i],1},{a[0],ap+.125,a[1],uv[i],.875},{b[0],ap+.125,b[1],uv[i+1],.875},{b[0],ap-.125,b[1],uv[i+1],1}});}}
    private static void quad(PoseStack p,VertexConsumer v,int light,int color,float alpha,float nx,float ny,float nz,double[][] points){for(var a:points)v.vertex(p.last().pose(),(float)a[0],(float)a[1],(float)a[2]).color((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,alpha).uv((float)a[3],(float)a[4]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),nx,ny,nz).endVertex();}
}
