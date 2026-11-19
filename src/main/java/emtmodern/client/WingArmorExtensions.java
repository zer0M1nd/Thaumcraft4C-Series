package emtmodern.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class WingArmorExtensions implements IClientItemExtensions {
    protected WingModel model;
    @Override public Model getGenericArmorModel(LivingEntity wearer,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original){
        if(model==null)model=new WingModel();model.prepare(wearer,stack);return model;
    }
    public static class WingModel extends Model {
        protected final ModelPart right,left,center;
        public WingModel(){
            super(net.minecraft.client.renderer.RenderType::entityCutoutNoCull);
            var mesh=new MeshDefinition();var root=mesh.getRoot();
            root.addOrReplaceChild("right",CubeListBuilder.create().texOffs(24,0).addBox(-11,-1,-1.5f,11,13,1),PartPose.offset(-2,0,3));
            root.addOrReplaceChild("left",CubeListBuilder.create().texOffs(0,0).addBox(0,-1,-1.5f,11,13,1),PartPose.offset(2,0,3));
            root.addOrReplaceChild("center",CubeListBuilder.create().texOffs(0,14).addBox(0,0,-.5f,8,14,1),PartPose.offset(-4,0,3));
            // Legacy ModelBox captures the model's 64x64 UV size at addBox; the later
            // ModelRenderer.setTextureSize(64,32) does not change existing cube UVs.
            var baked=LayerDefinition.create(mesh,64,64).bakeRoot();right=baked.getChild("right");left=baked.getChild("left");center=baked.getChild("center");
        }
        public void prepare(LivingEntity entity,ItemStack stack){
            // GTNH's reset fixes shared-model pose leaking after jumping or sneaking.
            boolean jump=stack.hasTag()&&stack.getTag().getBoolean("isJumping");right.yRot=jump?.5f:.1570796f;left.yRot=-right.yRot;
            right.xRot=left.xRot=center.xRot=entity.isShiftKeyDown()?.5f:0;
        }
        @Override public void renderToBuffer(PoseStack pose,VertexConsumer vertex,int light,int overlay,float red,float green,float blue,float alpha){right.render(pose,vertex,light,overlay,red,green,blue,alpha);left.render(pose,vertex,light,overlay,red,green,blue,alpha);center.render(pose,vertex,light,overlay,red,green,blue,alpha);}
    }
}
