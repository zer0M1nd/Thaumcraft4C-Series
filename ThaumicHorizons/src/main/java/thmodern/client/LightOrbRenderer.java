package thmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.client.TCRenderTypes;
import thmodern.Horizons;
import thmodern.focus.IlluminationFocusItem;
import thmodern.focus.LightBlock;

@Mod.EventBusSubscriber(modid=Horizons.ID,value=Dist.CLIENT)
public final class LightOrbRenderer implements BlockEntityRenderer<LightBlock.Orb> {
    private static final ResourceLocation ATLAS=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private final BlockEntityRendererProvider.Context context;
    public LightOrbRenderer(BlockEntityRendererProvider.Context context){this.context=context;}

    // Already placed lights had no saved block entity. Discover them once when the
    // client loads a chunk; findBlocks skips sections whose palette contains no lights.
    @SubscribeEvent public static void onChunkLoad(ChunkEvent.Load event) {
        if(event.getLevel().isClientSide() && event.getChunk() instanceof LevelChunk chunk)
            chunk.findBlocks(state->state.getBlock() instanceof LightBlock,
                    (pos,state)->chunk.getBlockEntity(pos,LevelChunk.EntityCreationType.IMMEDIATE));
    }

    @Override public void render(LightBlock.Orb orb,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var state=orb.getBlockState();
        if(!(state.getBlock() instanceof LightBlock) || orb.getLevel()==null)return;
        int shade=state.getValue(LightBlock.SHADE), rgb=IlluminationFocusItem.rgb(shade);
        double phase=(orb.getLevel().getGameTime()%80+partial)*(2*Math.PI/80)
                +(state.getSeed(orb.getBlockPos())&1023)*(2*Math.PI/1024);
        float pulse=(float)Math.sin(phase),half=.20f+.018f*pulse;
        var type=shade==0?TCRenderTypes.softParticle(ATLAS):TCRenderTypes.runes(ATLAS);
        var vertices=TCRenderTypes.buffer(buffers,type);
        pose.pushPose();
        pose.translate(.5,.5,.5);
        pose.mulPose(context.getEntityRenderer().cameraOrientation());
        for(int i=0;i<4;i++){
            boolean left=i==0||i==3, bottom=i<2;
            vertices.vertex(pose.last().pose(),left?-half:half,bottom?-half:half,0)
                    .color(rgb>>16&255,rgb>>8&255,rgb&255,Math.round(193+12*pulse))
                    .uv(left?0:.125f,bottom?1:.875f).uv2(LightTexture.FULL_BRIGHT).endVertex();
        }
        pose.popPose();
    }
}
