package fmmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fmmodern.WrathCageEntity;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.registries.ForgeRegistries;

public final class CageRenderer implements BlockEntityRenderer<WrathCageEntity> {
    private final BlockEntityRendererProvider.Context context;
    private final WeakHashMap<WrathCageEntity,Entity> cache=new WeakHashMap<>();
    public CageRenderer(BlockEntityRendererProvider.Context context){this.context=context;}
    @Override public void render(WrathCageEntity cage,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(cage.mob()==null||cage.getLevel()==null)return;var type=ForgeRegistries.ENTITY_TYPES.getValue(cage.mob());if(type==null)return;
        var entity=cache.get(cage);if(entity==null||entity.getType()!=type){entity=type.create(cage.getLevel());if(entity==null)return;cache.put(cage,entity);}
        pose.pushPose();pose.translate(.5,.4,.5);pose.mulPose(Axis.YP.rotationDegrees((float)((cage.previousRotation+(cage.rotation-cage.previousRotation)*partial)*10)));pose.translate(0,-.2,0);pose.mulPose(Axis.XP.rotationDegrees(-30));
        float scale=.53125f/Math.max(1,Math.max(entity.getBbWidth(),entity.getBbHeight()));pose.scale(scale,scale,scale);
        context.getEntityRenderer().render(entity,0,0,0,0,partial,pose,buffers,light);pose.popPose();
    }
}
