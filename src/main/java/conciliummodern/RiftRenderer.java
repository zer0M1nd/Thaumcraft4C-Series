package conciliummodern;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import tc4revival.client.TCRenderTypes;

/** Original six-sided ANGLE-joined animated polycone and spherical end shader. */
public final class RiftRenderer extends EntityRenderer<RiftEntity> {
    private static final ResourceLocation STARS=new ResourceLocation("minecraft","textures/entity/end_portal.png");
    public RiftRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    @Override public ResourceLocation getTextureLocation(RiftEntity entity){return STARS;}
    @Override public void render(RiftEntity rift,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        var shape=rift.geometry();var points=shape.points();int count=points.size();if(count<3)return;
        boolean revealed=ConciliumRevealing.hasGoggles(Minecraft.getInstance().player);
        for(int shell=0;shell<4;shell++) {
            boolean inner=shell==3;var out=buffers.getBuffer(inner?ConciliumShaders.end():ConciliumShaders.endGlow(shell==0&&revealed));
            var animated=new java.util.ArrayList<Vec3>(count);double[] radii=new double[count];
            for(int i=0;i<count;i++) {
                float time=rift.tickCount+partial+(i<count/2?i*10:i>count/2?-i*10:0);
                animated.add(points.get(i).add(Math.sin(time/50)*.15,rift.centerOffset()+Math.sin(time/60)*.15,Math.sin(time/70)*.15));
                radii[i]=shape.widths().get(i)*(1-Math.sin(time/8)*.15)*(inner?1:1.25+.5*shell);
            }
            int segment=0;var matrix=pose.last().pose();
            for(var tube:RiftTube.build(animated,radii)){
                for(int side=0;side<6;side++){
                    int next=(side+1)%6;var quad=new Vec3[]{tube.front()[side],tube.back()[side],tube.back()[next],tube.front()[next]};
                    for(int v=0;v<4;v++){var p=quad[v];
                        // The source GLE uses integer j/ncp: U remains zero until the seam.
                        out.vertex(matrix,(float)p.x,(float)p.y,(float)p.z).uv(v<2?0:side==5?1:0,v==0||v==3?segment:segment+1).color(1f,1f,1f,1f).endVertex();
                    }
                }segment++;
            }
        }
        super.render(rift,yaw,partial,pose,buffers,light);
    }
}
