package thaumicomniverse.mixin;

import com.blakebr0.mysticalagriculture.api.crop.ICropProvider;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="com.blakebr0.mysticalagriculture.block.MysticalCropBlock",remap=false)
public abstract class MysticalFeatureCropMixin {
    @Unique private boolean omniverse$disabled() {
        return ((ICropProvider)this).getCrop().getId().getNamespace().equals("thaumicomniverse")
                && !thaumicomniverse.RevivalFeatures.Group.CROPS.enabled();
    }
    @Inject(method={"randomTick","m_213898_"},at=@At("HEAD"),cancellable=true,remap=false)
    private void omniverse$growth(BlockState state,ServerLevel level,BlockPos pos,RandomSource random,CallbackInfo ci) {
        if(omniverse$disabled())ci.cancel();
    }
    @Inject(method={"performBonemeal","m_214148_"},at=@At("HEAD"),cancellable=true,remap=false)
    private void omniverse$bonemeal(ServerLevel level,RandomSource random,BlockPos pos,BlockState state,CallbackInfo ci) {
        if(omniverse$disabled())ci.cancel();
    }
    @Inject(method={"getDrops","m_49635_"},at=@At("HEAD"),cancellable=true,remap=false)
    private void omniverse$harvest(BlockState state,LootParams.Builder context,CallbackInfoReturnable<List<ItemStack>> ci) {
        // Recover the planted seed without producing essence or multiplying seeds.
        if(omniverse$disabled())ci.setReturnValue(List.of(new ItemStack(((ICropProvider)this).getCrop().getSeedsItem())));
    }
}
