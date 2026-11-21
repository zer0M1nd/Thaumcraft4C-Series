package dyesmodern.client;

import com.mojang.blaze3d.vertex.*;
import java.util.*;
import dyesmodern.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;

/** Original visible part graph, with modern entity poses and uncached equipment-dependent details. */
public final class DyesArmorModel extends HumanoidModel<LivingEntity> {
    private final String kind;
    private final Map<String,ModelPart> parts;
    private ResourceLocation translucent;
    public DyesArmorModel(String kind,boolean outer){this(kind,DyesGeometry.bake(kind,outer));}
    private DyesArmorModel(String kind,DyesGeometry.Baked baked){super(baked.root());this.kind=kind;parts=baked.parts();}
    public void prepare(LivingEntity wearer,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original) {
        net.minecraftforge.client.ForgeHooksClient.copyModelProperties(original,this);
        setAllVisible(false);head.visible=hat.visible=slot==EquipmentSlot.HEAD;
        body.visible=slot==EquipmentSlot.CHEST||slot==EquipmentSlot.LEGS;
        rightArm.visible=leftArm.visible=slot==EquipmentSlot.CHEST;
        // Primal boots have no separate geometry in 1.10.5; their presence completes the visible set.
        rightLeg.visible=leftLeg.visible=slot==EquipmentSlot.LEGS;
        var armor=(DyesArmorItem)stack.getItem();
        var helmet=wearer.getItemBySlot(EquipmentSlot.HEAD);
        if(kind.equals("Fortress")||kind.equals("IronFortress")) {
            int count=0;for(var s:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS}) {
                var piece=wearer.getItemBySlot(s);
                if(piece.getItem() instanceof DyesArmorItem item&&item.family.equals(armor.family))count++;
            }
            boolean matching=helmet.getItem() instanceof DyesArmorItem item&&item.family.equals(armor.family);
            visible("Goggles",matching&&DyesArmorItem.goggles(helmet));
            for(int i=0;i<3;i++)visible("Mask_"+i,matching&&DyesArmorItem.mask(helmet)==i);
            for(String name:DyesGeometry.FORTRESS_THREE)visible(name,count>=3);
            for(String name:DyesGeometry.FORTRESS_TWO)visible(name,count>=2);
        } else if(kind.equals("Ranger")) {
            int mask=helmet.getItem() instanceof DyesArmorItem item&&item.family.equals(armor.family)?DyesArmorItem.mask(helmet):-1;
            for(int i=0;i<2;i++)visible("Mask_"+i,mask==i);
        } else if(kind.equals("Ranger2")) {visible("Mask_0",false);visible("Mask_1",true);}
        translucent=null;
        if(kind.equals("RobesSpecial")) {
            int count=0;for(ItemStack piece:wearer.getArmorSlots())if(piece.getItem() instanceof DyesArmorItem item&&item.primal())count++;
            for(String name:DyesGeometry.PRIMAL_SET)visible(name,count==4);
            for(String name:DyesGeometry.PRIMAL_HIDDEN)visible(name,false);
            if(!DyesArmorItem.hardened(stack))translucent=ResourceLocation.parse(ExternalResources.armorTexture(armor.family,stack,wearer,slot,null));
        }
        float partial=Minecraft.getInstance().getFrameTime(),phase=wearer.walkAnimation.position(partial),amount=wearer.isPassenger()?0:Math.min(1,wearer.walkAnimation.speed(partial));
        if(wearer.isBaby())phase*=3;
        float a=Mth.cos(phase*.6662f)*1.4f*amount,b=Mth.cos(phase*.6662f+Mth.PI)*1.4f*amount,c=Math.min(a,b);
        for(String side:new String[]{"L","R"}) {
            rotate("Frontcloth"+side+"1",c-.1047198f);rotate("Frontcloth"+side+"2",c-.3316126f);
            rotate("ClothBack"+side+"1",-c+.1047198f);rotate("ClothBack"+side+"2",-c+.2268928f);rotate("ClothBack"+side+"3",-c+.2268928f);
        }
        if(kind.equals("Knight")){rotate("Frontcloth1",c-.1047198f);rotate("Frontcloth2",c-.3316126f);}
        if(kind.equals("Paladin")||kind.equals("Praetor")){rotate("LegClothR",a-.10472f);rotate("LegClothL",b-.10472f);}
        if(kind.equals("Knight")||kind.equals("Paladin")||kind.equals("Praetor")) {
            rotate("Cloak1",-c/2+.1396263f);rotate("Cloak2",-c/2+.3069452f);rotate("Cloak3",-c/2+.4465716f);
        }
        if(kind.equals("RobesSpecial"))rotate("Cloak1",.2f+Mth.sin((wearer.tickCount+partial)/7)*.08f);
    }
    private void visible(String name,boolean value){var part=parts.get(name);if(part!=null)part.visible=value;}
    private void rotate(String name,float value){var part=parts.get(name);if(part!=null)part.xRot=value;}
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,float r,float g,float b,float a) {
        if(translucent!=null)out=Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.entityTranslucent(translucent));
        pose.pushPose();if(young){pose.scale(.75f,.75f,.75f);pose.translate(0,1,0);}else pose.scale(1.01f,1.01f,1.01f);
        head.render(pose,out,light,overlay,r,g,b,a);pose.popPose();
        pose.pushPose();if(young){pose.scale(.5f,.5f,.5f);pose.translate(0,1.5,0);}
        for(var part:new ModelPart[]{body,rightArm,leftArm,rightLeg,leftLeg,hat})part.render(pose,out,light,overlay,r,g,b,a);
        pose.popPose();
    }
}
