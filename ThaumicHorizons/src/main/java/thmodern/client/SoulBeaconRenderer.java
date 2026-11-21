package thmodern.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import thmodern.soul.SoulBeaconEntity;
public final class SoulBeaconRenderer implements BlockEntityRenderer<SoulBeaconEntity> {
    private static final ResourceLocation BEAM=ResourceLocation.parse("minecraft:textures/entity/beacon_beam.png");
    public SoulBeaconRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(SoulBeaconEntity beacon,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var level=beacon.getLevel();if(level==null||!level.canSeeSky(beacon.getBlockPos().above()))return;
        BeaconRenderer.renderBeaconBeam(pose,buffers,BEAM,partial,1,level.getGameTime(),0,256,new float[]{1,1,1},.2f,.25f);
    }
    @Override public boolean shouldRenderOffScreen(SoulBeaconEntity beacon){return true;}
    @Override public int getViewDistance(){return 256;}
}
