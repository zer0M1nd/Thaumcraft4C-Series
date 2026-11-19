package tc4revival.client;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;

/** Registry blocks without inventory items still have a real preview; failures are isolated per target. */
public class EditorIcons {
    private static final Set<String> broken=new HashSet<>();
    public static void draw(GuiGraphics g,EditorViewModel.Target target,int x,int y){
        if(target.kind().equals("entity")||broken.contains(target.key())){g.renderItem(new ItemStack(Items.BARRIER),x,y);return;}
        var mc=Minecraft.getInstance();boolean failed=false;
        // Give third-party item renderers a private pose stack: a thrown exception
        // between their push/pop calls must not corrupt the rest of this screen.
        var parent=g;g=new GuiGraphics(mc,parent.bufferSource());g.pose().mulPoseMatrix(parent.pose().last().pose());
        g.pose().pushPose();
        try{
            if(!target.stack().isEmpty()){g.renderItem(target.stack(),x,y);g.flush();return;}
            var state=target.state();
            if(state.is(Blocks.END_PORTAL)||state.is(Blocks.END_GATEWAY)){
                // These are shader-only, invisible block models. Use their actual portal texture.
                g.blit(new ResourceLocation("minecraft:textures/entity/end_portal.png"),x,y,16,16,0,0,256,256,256,256);return;
            }
            if(state.getRenderShape()==RenderShape.INVISIBLE&&!state.getFluidState().isEmpty()){
                var sprite=mc.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation(state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)?"minecraft:block/lava_still":"minecraft:block/water_still"));
                g.blit(x,y,0,16,16,sprite);return;
            }
            g.pose().translate(x+8,y+9,150);g.pose().scale(11,-11,11);
            g.pose().mulPose(Axis.XP.rotationDegrees(30));g.pose().mulPose(Axis.YP.rotationDegrees(225));g.pose().translate(-.5,-.5,-.5);
            if(state.getRenderShape()==RenderShape.MODEL)mc.getBlockRenderer().renderSingleBlock(state,g.pose(),g.bufferSource(),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            else if(state.getBlock() instanceof EntityBlock entityBlock){
                var entity=entityBlock.newBlockEntity(BlockPos.ZERO,state);if(entity==null)throw new IllegalStateException("No preview entity");
                entity.setLevel(mc.level);var renderer=mc.getBlockEntityRenderDispatcher().getRenderer(entity);
                if(renderer==null)throw new IllegalStateException("No preview renderer");
                renderer.render(entity,0,g.pose(),g.bufferSource(),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            }else throw new IllegalStateException("No visible block preview");
            g.flush();
        }catch(Throwable error){
            if(error instanceof VirtualMachineError fatal)throw fatal;
            failed=true;broken.add(target.key());
            tc4revival.TC4Revival.LOGGER.warn("Aspect editor preview failed for {}; using barrier",target.key(),error);
        }finally{
            // Discard the private pose regardless of how many frames a faulty renderer pushed.
            parent.setColor(1,1,1,1);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            RenderSystem.disableDepthTest();
        }
        if(failed)parent.renderItem(new ItemStack(Items.BARRIER),x,y);
    }
}
