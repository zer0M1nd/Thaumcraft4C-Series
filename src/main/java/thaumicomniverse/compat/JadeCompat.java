package thaumicomniverse.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.*;

@WailaPlugin
public final class JadeCompat implements IWailaPlugin {
    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new AspectProvider(), Block.class);
    }
    /** Read the exact same synchronized container and discovery state as TC4's floating HUD. */
    private static final class AspectProvider implements IBlockComponentProvider {
        @Override public ResourceLocation getUid() { return ResourceLocation.fromNamespaceAndPath("thaumicomniverse", "revealed_aspects"); }
        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, snownee.jade.api.config.IPluginConfig config) {
            var player = accessor.getPlayer();
            if (player == null || !tc4revival.content.RevealingGear.visible(player, false)
                    || !(accessor.getBlockEntity() instanceof tc4revival.content.RevealedAspectContainer container)) return;
            var amounts = container.revealedAspects();
            var knowledge = tc4revival.client.ClientKnowledge.snapshot();
            for (var aspect : amounts.aspects()) {
                boolean known = knowledge.map(k -> k.knows(aspect)).orElse(false);
                tooltip.add(new AspectIcon(aspect, known));
                var title = known ? net.minecraft.network.chat.Component.literal(thaumcraft.api.aspects.AspectViews.display(aspect).displayName())
                        : net.minecraft.network.chat.Component.literal("?");
                int quantity = amounts.amount(aspect);
                tooltip.append(title.append(quantity > 0 ? " × " + quantity : ""));
            }
        }
    }
    private static final class AspectIcon extends snownee.jade.api.ui.Element {
        private final tc4revival.aspect.Aspect aspect;
        private final boolean known;
        AspectIcon(tc4revival.aspect.Aspect aspect, boolean known) { this.aspect = aspect; this.known = known; }
        @Override public net.minecraft.world.phys.Vec2 getSize() { return new net.minecraft.world.phys.Vec2(11, 9); }
        @Override public void render(net.minecraft.client.gui.GuiGraphics graphics, float x, float y, float width, float height) {
            var texture = known ? thaumcraft.api.aspects.AspectViews.display(aspect).texture() : ResourceLocation.parse("thaumcraft:textures/aspects/_unknown.png");
            int color = thaumcraft.api.aspects.AspectViews.display(aspect).color();
            var previous = com.mojang.blaze3d.systems.RenderSystem.getShaderColor().clone();
            boolean blend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            try {
                graphics.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, previous[3]);
                graphics.blit(texture, (int) x, (int) y, 9, 9, 0, 0, 16, 16, 16, 16);
            } finally {
                graphics.setColor(previous[0], previous[1], previous[2], previous[3]);
                if (!blend) com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            }
        }
    }
}
