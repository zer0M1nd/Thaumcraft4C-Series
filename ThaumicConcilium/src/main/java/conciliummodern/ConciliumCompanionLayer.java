package conciliummodern;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;

/** Original author-name companion easter egg, using existing biped and host armor models. */
public final class ConciliumCompanionLayer extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    private final HumanoidModel<AbstractClientPlayer> companion=new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE,0),64,32).bakeRoot());
    private final HumanoidModel<AbstractClientPlayer> armor=new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(.5f),0),64,32).bakeRoot());
    public ConciliumCompanionLayer(RenderLayerParent<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> parent){super(parent);}
    @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,AbstractClientPlayer player,float walk,float amount,float partial,float age,float yaw,float pitch){
        String name=player.getGameProfile().getName();boolean loot=name.equals("Lootram");String skin=switch(name){case "Excsi"->"excsi";case "krumplerban"->"krump";case "Fantasy5hark"->"shark";case "RA3DUPLYATOR","BreadX22"->"thaumaturge";case "Lootram"->"loot";default->name.equalsIgnoreCase("Ilya3000")?"thaumaturge":null;};
        var viewer=net.minecraft.client.Minecraft.getInstance().player;if(skin==null||player.isInvisible()||viewer!=null&&player.isInvisibleTo(viewer))return;
        companion.young=false;companion.riding=false;companion.crouching=player.isCrouching();companion.hat.visible=!loot;companion.attackTime=0;
        companion.setupAnim(player,player.xo==player.getX()&&player.zo==player.getZ()?2.4f:walk,.625f,age,0,pitch);
        for(int side:new int[]{-1,1}){
            pose.pushPose();pose.translate(side,0,1);companion.renderToBuffer(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse("thaumicconcilium:textures/entity/"+skin+".png"))),light,OverlayTexture.NO_OVERLAY,1,1,1,1);
            if(loot)for(var slot:java.util.List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
                String part=switch(slot){case HEAD->"helm";case CHEST->"chest";case LEGS->"legs";default->"boots";};var stack=CrimsonContent.item("thaumic_tinkerer:awakened_ichorcloth_"+part);if(!(stack.getItem() instanceof ArmorItem item))continue;
                net.minecraftforge.client.ForgeHooksClient.copyModelProperties(companion,armor);armor.setAllVisible(false);armor.head.visible=slot==EquipmentSlot.HEAD;armor.body.visible=slot==EquipmentSlot.CHEST;armor.leftArm.visible=armor.rightArm.visible=slot==EquipmentSlot.CHEST;armor.leftLeg.visible=armor.rightLeg.visible=slot==EquipmentSlot.LEGS||slot==EquipmentSlot.FEET;
                var model=net.minecraftforge.client.extensions.common.IClientItemExtensions.of(item).getGenericArmorModel(player,stack,slot,armor);String texture=item.getArmorTexture(stack,player,slot,null);if(texture!=null)model.renderToBuffer(pose,buffers.getBuffer(RenderType.armorCutoutNoCull(ResourceLocation.parse(texture))),light,OverlayTexture.NO_OVERLAY,1,1,1,1);
            }
            pose.popPose();
        }
    }
}
