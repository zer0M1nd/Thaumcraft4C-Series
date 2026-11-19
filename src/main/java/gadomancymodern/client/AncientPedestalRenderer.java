package gadomancymodern.client;
import net.minecraft.client.renderer.blockentity.*;
import gadomancymodern.compressor.AncientPedestalEntity;
public class AncientPedestalRenderer implements BlockEntityRenderer<AncientPedestalEntity> {
    protected final tc4revival.client.ArcanePedestalRenderer renderer;
    public AncientPedestalRenderer(BlockEntityRendererProvider.Context context){renderer=new tc4revival.client.ArcanePedestalRenderer(context);}
    @Override public void render(AncientPedestalEntity tile,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay){renderer.render(tile,partial,pose,buffers,light,overlay);}
}
