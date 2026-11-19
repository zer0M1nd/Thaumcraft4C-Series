package ttmodern;

import net.minecraft.client.*;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Tinkerer.ID,value=Dist.CLIENT)
public final class AwakenedArmorClient {
    static KeyMapping TOGGLE;
    private static boolean disabled;
    private static net.minecraft.network.Connection connection;
    public static void receive(boolean value,net.minecraft.network.Connection source){disabled=value;connection=source;}
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.getConnection()==null)return;
        if(mc.getConnection().getConnection()==connection)mc.player.getPersistentData().putBoolean(AwakenedArmorEffects.DISABLED,disabled);
        while(TOGGLE.consumeClick())if(mc.screen==null)SoulHearts.CHANNEL.sendToServer(new SoulHearts.ArmorToggle());
    }
    @Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class Registration {@SubscribeEvent public static void keys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event){event.register(TOGGLE = new KeyMapping("ttmisc.toggleArmor",-1,"ttmisc.keyCategory"));}}
    public static final class Extensions implements net.minecraftforge.client.extensions.common.IClientItemExtensions {
        private Wings wings;
        @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original){
            if(slot!=EquipmentSlot.CHEST)return original;if(wings==null)wings=new Wings();
            wings.head.copyFrom(original.head);wings.hat.copyFrom(original.hat);wings.body.copyFrom(original.body);wings.leftArm.copyFrom(original.leftArm);wings.rightArm.copyFrom(original.rightArm);wings.leftLeg.copyFrom(original.leftLeg);wings.rightLeg.copyFrom(original.rightLeg);
            wings.young=original.young;wings.crouching=original.crouching;wings.riding=original.riding;wings.attackTime=original.attackTime;
            float angle=entity instanceof net.minecraft.world.entity.player.Player player&&player.getAbilities().flying?(float)((Math.sin(entity.tickCount)+1)*Math.PI/180*15-.6108652):-.6108652f;
            wings.body.getChild("left_wing").yRot=angle;wings.body.getChild("right_wing").yRot=-angle;return wings;
        }
    }
    private static final class Wings extends HumanoidModel<LivingEntity> {
        Wings(){super(layer());}
        private static ModelPart layer(){var mesh=HumanoidModel.createMesh(new CubeDeformation(1),0);var body=mesh.getRoot().getChild("body");
            body.addOrReplaceChild("left_wing",CubeListBuilder.create().texOffs(16,-12).addBox(0,0,0,0,7,12),PartPose.offsetAndRotation(-2,1,2,0,-.6108652f,0));
            body.addOrReplaceChild("right_wing",CubeListBuilder.create().texOffs(16,-12).addBox(.1f,0,0,0,7,12),PartPose.offsetAndRotation(2,1,2,0,.4468043f,0));return LayerDefinition.create(mesh,64,32).bakeRoot();
        }
    }
}
