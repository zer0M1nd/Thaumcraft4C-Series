package timodern.mixin;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import timodern.*;

@Mixin(value=ModelBlockRenderer.class,remap=false)
public class InfusedRendererMixin {
    @Inject(method="tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",at=@At("HEAD"),cancellable=true)
    private void tiModel(BlockAndTintGetter level,BakedModel model,BlockState state,BlockPos pos,PoseStack pose,VertexConsumer out,boolean checkSides,RandomSource random,long seed,int overlay,ModelData data,RenderType type,CallbackInfo ci){
        if(model instanceof InfusedModel)return;var e=InfusionData.CLIENT.get(pos.asLong());if(e==null||!e.has("sensus")&&!e.has("fabrico"))return;
        ((ModelBlockRenderer)(Object)this).tesselateBlock(level,new InfusedModel(model,e),state,pos,pose,out,checkSides,random,seed,overlay,data,type);ci.cancel();
    }
}
