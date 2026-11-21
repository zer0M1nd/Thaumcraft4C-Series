package timodern.mixin;

import net.minecraft.core.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.common.IPlantable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import timodern.*;

@Mixin(value=BlockBehaviour.BlockStateBase.class,remap=false)
public abstract class InfusedStateMixin {
    @Inject(method={"getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;","m_60742_"},at=@At("HEAD"),cancellable=true)
    private void tiCollision(BlockGetter w,BlockPos p,CollisionContext context,CallbackInfoReturnable<VoxelShape> c){if(InfusionData.has(w,p,"spiritus"))c.setReturnValue(Shapes.empty());}
    @Inject(method={"getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;","m_60812_"},at=@At("HEAD"),cancellable=true)
    private void tiCachedCollision(BlockGetter w,BlockPos p,CallbackInfoReturnable<VoxelShape> c){if(InfusionData.has(w,p,"spiritus"))c.setReturnValue(Shapes.empty());}
    @Inject(method={"isSuffocating","m_60828_"},at=@At("HEAD"),cancellable=true)
    private void tiSuffocation(BlockGetter w,BlockPos p,CallbackInfoReturnable<Boolean> c){if(InfusionData.has(w,p,"spiritus"))c.setReturnValue(false);}
    @Inject(method={"getLightBlock","m_60739_"},at=@At("HEAD"),cancellable=true)
    private void tiOpacity(BlockGetter w,BlockPos p,CallbackInfoReturnable<Integer> c){if(InfusionData.has(w,p,"vitreus"))c.setReturnValue(0);}
    @Inject(method={"isValidSpawn","m_60643_"},at=@At("HEAD"),cancellable=true)
    private void tiSpawn(BlockGetter w,BlockPos p,EntityType<?> type,CallbackInfoReturnable<Boolean> c){if(InfusionData.has(w,p,"bestia")||InfusionData.has(w,p,"humanus"))c.setReturnValue(false);}
    @Inject(method={"getDestroySpeed","m_60800_"},at=@At("HEAD"),cancellable=true)
    private void tiHardness(BlockGetter w,BlockPos p,CallbackInfoReturnable<Float> c){if(InfusionData.has(w,p,"humanus"))c.setReturnValue(-1f);}
    @Inject(method={"getSignal","m_60746_"},at=@At("HEAD"),cancellable=true)
    private void tiSignal(BlockGetter w,BlockPos p,Direction d,CallbackInfoReturnable<Integer> c){int s=InfusionRuntime.signal(w,p);if(s>=0)c.setReturnValue(s);}
    @Inject(method={"getDirectSignal","m_60775_"},at=@At("HEAD"),cancellable=true)
    private void tiDirectSignal(BlockGetter w,BlockPos p,Direction d,CallbackInfoReturnable<Integer> c){if(InfusionData.has(w,p,"potentia"))c.setReturnValue(15);}
    public boolean shouldCheckWeakPower(net.minecraft.world.level.SignalGetter w,BlockPos p,Direction d){var state=(BlockState)(Object)this;return InfusionRuntime.signal(w,p)<0&&state.getBlock().shouldCheckWeakPower(state,w,p,d);}
    // Position-sensitive Forge extension methods. Defining them on the state supersedes the
    // interface defaults without changing or replacing any underlying block implementation.
    public int getLightEmission(BlockGetter w,BlockPos p){var state=(BlockState)(Object)this;return InfusionData.has(w,p,"lux")?14:state.getBlock().getLightEmission(state,w,p);}
    public float getEnchantPowerBonus(LevelReader w,BlockPos p){var state=(BlockState)(Object)this;return InfusionData.has(w,p,"cognitio")?1:state.getBlock().getEnchantPowerBonus(state,w,p);}
    public boolean canSustainPlant(BlockGetter w,BlockPos p,Direction d,IPlantable plant){var state=(BlockState)(Object)this;return InfusionData.has(w,p,"messis")||state.getBlock().canSustainPlant(state,w,p,d,plant);}
    public boolean isFertile(BlockGetter w,BlockPos p){var state=(BlockState)(Object)this;return InfusionData.has(w,p,"messis")||state.getBlock().isFertile(state,w,p);}
    public boolean canEntityDestroy(BlockGetter w,BlockPos p,Entity entity){var state=(BlockState)(Object)this;return !InfusionData.has(w,p,"humanus")&&state.getBlock().canEntityDestroy(state,w,p,entity);}
    public boolean isFireSource(LevelReader w,BlockPos p,Direction d){var state=(BlockState)(Object)this;return InfusionData.has(w,p,"ignis")||state.getBlock().isFireSource(state,w,p,d);}
    public int getFlammability(BlockGetter w,BlockPos p,Direction d){var state=(BlockState)(Object)this;return InfusionData.has(w,p,"ignis")?0:state.getBlock().getFlammability(state,w,p,d);}
    public boolean isFlammable(BlockGetter w,BlockPos p,Direction d){var state=(BlockState)(Object)this;return InfusionData.has(w,p,"ignis")||state.getBlock().isFlammable(state,w,p,d);}
    public int getFireSpreadSpeed(BlockGetter w,BlockPos p,Direction d){var state=(BlockState)(Object)this;return InfusionData.has(w,p,"ignis")?4096:state.getBlock().getFireSpreadSpeed(state,w,p,d);}
    public boolean canConnectRedstone(BlockGetter w,BlockPos p,Direction d){var state=(BlockState)(Object)this;return InfusionRuntime.signal(w,p)>=0||state.getBlock().canConnectRedstone(state,w,p,d);}
}
