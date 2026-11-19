package automagy.client;
import automagy.content.EssentiaLocusBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
public final class EssentiaLocusRenderer implements BlockEntityRenderer<EssentiaLocusBlockEntity>{
    private final InventariumRenderer brain=new InventariumRenderer(null);
    public EssentiaLocusRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(EssentiaLocusBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){brain.draw(tile.hasLevel()?(tile.getLevel().getGameTime()%10800)+partial:0,tile.hasLevel(),ResourceLocation.parse("automagy:textures/models/brain_water.png"),.4f,pose,buffers,light,overlay);}
}
