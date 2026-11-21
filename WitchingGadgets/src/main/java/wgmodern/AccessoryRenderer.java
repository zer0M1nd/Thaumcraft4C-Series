package wgmodern;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;
public class AccessoryRenderer implements ICurioRenderer {
    private final java.util.Map<String,ModelPart> models=new java.util.HashMap<>();
    private static ModelPart make(String kind){var mesh=new MeshDefinition();var root=mesh.getRoot();boolean unarmored=kind.endsWith("_unarmored");kind=kind.replace("_unarmored","");boolean shoulder=kind.startsWith("shoulders");int u=kind.equals("shoulders_knockback")||kind.equals("vambrace_strength")||kind.endsWith("_base")?40:24,v=shoulder?16:24,y=shoulder?0:7;var deform=new CubeDeformation(shoulder?(unarmored?.5f:1.125f):.125f);root.addOrReplaceChild("right",CubeListBuilder.create().texOffs(u,v).addBox(-3,-2+y,-2,4,4,4,deform),PartPose.offset(-5,2,0));root.addOrReplaceChild("left",CubeListBuilder.create().texOffs(u,v).mirror().addBox(-1,-2+y,-2,4,4,4,deform),PartPose.offset(5,2,0));return LayerDefinition.create(mesh,64,32).bakeRoot();}
    @Override public <T extends LivingEntity,M extends EntityModel<T>> void render(ItemStack stack,SlotContext context,PoseStack p,RenderLayerParent<T,M> renderer,MultiBufferSource buffers,int light,float limb,float amount,float partial,float age,float yaw,float pitch){if(WGMagicClient.hidden(stack,context.entity()))return;if(net.minecraftforge.fml.ModList.get().isLoaded("botania")&&BotaniaCosmeticRenderer.render(stack,context,p,renderer,buffers,light,limb,amount,partial,age,yaw,pitch))return;if(!(renderer.getModel() instanceof HumanoidModel<?> human)||!(stack.getItem() instanceof WGAccessories.Gear gear)||!gear.kind.startsWith("shoulders")&&!gear.kind.startsWith("vambrace"))return;var model=models.computeIfAbsent(gear.kind+(context.entity().getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty()?"_unarmored":""),AccessoryRenderer::make);model.getChild("right").copyFrom(human.rightArm);model.getChild("left").copyFrom(human.leftArm);model.render(p,buffers.getBuffer(RenderType.entityCutoutNoCull(new ResourceLocation("witchinggadgets",gear.kind.endsWith("_base")?"textures/tg/tgtextures.png":"textures/models/magicalbaubles.png"))),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);}
}
