package thmodern.mixin;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=BlockRenderDispatcher.class,remap=false)
public abstract class LensBlockRenderMixin {
    // Forge's extended overload retains this name in both development and production.
    @Inject(method="renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;Lnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",at=@At("HEAD"),cancellable=true)
    private void th$mask(BlockState state,BlockPos pos,BlockAndTintGetter world,com.mojang.blaze3d.vertex.PoseStack pose,com.mojang.blaze3d.vertex.VertexConsumer output,boolean checkSides,net.minecraft.util.RandomSource random,net.minecraftforge.client.model.data.ModelData data,net.minecraft.client.renderer.RenderType type,CallbackInfo callback){if(thmodern.client.EarthLensMask.hidden(pos))callback.cancel();}
}
