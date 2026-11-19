package tc4revival.content;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.golems.GolemCropRegistry;

/** AgriCraft 4 stores maturity and genes in its block entity, not an age property. */
final class AgriCraftCropHarvest {
    private record Api(Class<?> crop,Method mature,Method harvest) {}
    private static final class Holder {
        static final Optional<Api> API=load();
        private static Optional<Api> load() {
            if(!net.minecraftforge.fml.ModList.get().isLoaded("agricraft"))return Optional.empty();
            try {
                var crop=Class.forName("com.agricraft.agricraft.api.crop.AgriCrop");
                return Optional.of(new Api(crop,crop.getMethod("canBeHarvested"),crop.getMethod("harvest",Consumer.class,LivingEntity.class)));
            }catch(ReflectiveOperationException e){com.mojang.logging.LogUtils.getLogger().warn("AgriCraft crop API unavailable",e);return Optional.empty();}
        }
    }
    static Optional<Boolean> mature(ServerLevel level,BlockPos pos) {
        var entity=level.getBlockEntity(pos);
        return Holder.API.filter(api->api.crop.isInstance(entity)).map(api->{
            try{return Boolean.TRUE.equals(api.mature.invoke(entity));}
            catch(ReflectiveOperationException e){throw new IllegalStateException("AgriCraft maturity check failed",e);}
        });
    }
    static Optional<Boolean> harvest(GolemCropRegistry.Context context) {
        var entity=context.level().getBlockEntity(context.pos());
        return Holder.API.filter(api->api.crop.isInstance(entity)).map(api->{
            if(!context.allowed(context.pos()))return false;
            Consumer<ItemStack> drops=stack->Block.popResource(context.level(),context.pos(),stack);
            try{return Boolean.TRUE.equals(api.harvest.invoke(entity,drops,context.player()));}
            catch(ReflectiveOperationException e){throw new IllegalStateException("AgriCraft harvest failed",e);}
        });
    }
    private AgriCraftCropHarvest() {}
}
