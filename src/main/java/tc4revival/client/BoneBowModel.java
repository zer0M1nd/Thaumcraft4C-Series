package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraftforge.client.model.BakedModelWrapper;

/** Preserve the bow's baked display transforms for every pull variant. */
 class BoneBowModel extends BakedModelWrapper<BakedModel> {
    private final java.util.Map<BakedModel,BakedModel> variants=new java.util.IdentityHashMap<>();
    BoneBowModel(BakedModel original) { super(original); }
    @Override public ItemOverrides getOverrides() {
        return new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel model,ItemStack stack,ClientLevel level,LivingEntity entity,int seed) {
                var selected=originalModel.getOverrides().resolve(originalModel,stack,level,entity,seed);
                return selected==null || selected==originalModel?BoneBowModel.this:variants.computeIfAbsent(selected,BoneBowModel::new);
            }
        };
    }
    @Override public BakedModel applyTransform(ItemDisplayContext context,PoseStack pose,boolean left) {
        originalModel.applyTransform(context,pose,left);
        return this;
    }
}
