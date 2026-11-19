package tc4revival.client;

import tc4revival.content.TaintSwarmEntity;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

/** The old swarm has no body model, shadow, or nameplate: its flies are the visible body. */
public  class TaintSwarmRenderer extends EntityRenderer<TaintSwarmEntity> {
    public TaintSwarmRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(TaintSwarmEntity entity) { return ResourceLocation.parse("thaumcraft:textures/misc/particles.png"); }
    @Override public void render(TaintSwarmEntity entity,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light) {}
}
