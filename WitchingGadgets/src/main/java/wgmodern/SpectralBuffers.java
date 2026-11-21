package wgmodern;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

/** Uses the original layer's own texture, so skins, armor and held items fade together. */
public class SpectralBuffers implements MultiBufferSource {
    public interface Texture {java.util.Optional<ResourceLocation> wg$texture();}
    private final MultiBufferSource wrapped;private final float brightness,alpha;
    public SpectralBuffers(MultiBufferSource source,float brightness,float alpha){wrapped=source;this.brightness=brightness;this.alpha=alpha;}
    public static boolean unveiling(Player player){if(player==null)return false;var enchant=ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation("witchinggadgets","unveiling"));return enchant!=null&&EnchantmentHelper.getItemEnchantmentLevel(enchant,player.getItemBySlot(EquipmentSlot.HEAD))>0;}
    public static MultiBufferSource forEntity(MultiBufferSource original,LivingEntity entity){if(!(entity instanceof Player)||!CloakItem.spectral(entity))return original;var viewer=Minecraft.getInstance().player;if(entity==viewer)return new SpectralBuffers(original,.5f,.5f);if(unveiling(viewer))return new SpectralBuffers(original,1,.75f);if(viewer!=null&&tc4revival.content.RevealingGear.visible(viewer,true))return new SpectralBuffers(original,.25f,.5f);return new SpectralBuffers(original,1,0);}
    @Override public VertexConsumer getBuffer(RenderType type){var texture=type instanceof Texture access?access.wg$texture():java.util.Optional.<ResourceLocation>empty();RenderType effective=texture.isPresent()&&!type.isOutline()?RenderType.entityTranslucent(texture.get()):type;return new Tint(wrapped.getBuffer(effective),brightness,alpha);}
    private static class Tint implements VertexConsumer {
        private final VertexConsumer out;private final float brightness,opacity;
        Tint(VertexConsumer out,float brightness,float opacity){this.out=out;this.brightness=brightness;this.opacity=opacity;}
        public VertexConsumer vertex(double x,double y,double z){out.vertex(x,y,z);return this;}
        public VertexConsumer color(int r,int g,int b,int a){out.color(Math.round(r*brightness),Math.round(g*brightness),Math.round(b*brightness),Math.round(a*opacity));return this;}
        public VertexConsumer uv(float u,float v){out.uv(u,v);return this;}
        public VertexConsumer overlayCoords(int u,int v){out.overlayCoords(u,v);return this;}
        public VertexConsumer uv2(int u,int v){out.uv2(u,v);return this;}
        public VertexConsumer normal(float x,float y,float z){out.normal(x,y,z);return this;}
        public void endVertex(){out.endVertex();}public void defaultColor(int r,int g,int b,int a){out.defaultColor(Math.round(r*brightness),Math.round(g*brightness),Math.round(b*brightness),Math.round(a*opacity));}public void unsetDefaultColor(){out.unsetDefaultColor();}
    }
}
