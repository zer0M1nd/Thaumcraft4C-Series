package thaumicomniverse.mixin;

import com.simibubi.create.content.redstone.thresholdSwitch.*;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import thaumcraft.api.aspects.EssentiaStorageApi;
import thaumicomniverse.compat.create.*;

/** Keeps Create's filtering, GUI, hysteresis, delayed output and item/fluid observation. */
@Mixin(value=ThresholdSwitchBlockEntity.class,remap=false)
public abstract class CreateEssentiaThresholdMixin extends SmartBlockEntity {
    @Shadow public int currentMinLevel,currentLevel,currentMaxLevel,onWhenAbove,offWhenBelow;
    @Shadow private boolean redstoneState;
    @Shadow private FilteringBehaviour filtering;
    @Shadow protected abstract void scheduleBlockTick();
    protected CreateEssentiaThresholdMixin(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);}
    @Unique private BlockPos omniverse$target(){return worldPosition.relative(ThresholdSwitchBlock.getTargetDirection(getBlockState()));}
    @Unique private boolean omniverse$essentia(){
        if(level==null||!ArcaneContraptions.enabled())return false;
        var target=omniverse$target();
        return level.hasChunkAt(target)&&EssentiaStorageApi.supports(level.getBlockEntity(target),null);
    }
    @Inject(method="updateCurrentLevel",at=@At("HEAD"),cancellable=true)
    private void omniverse$observe(CallbackInfo ci){
        if(!(level instanceof ServerLevel server)||!omniverse$essentia())return;
        var storage=EssentiaStorageApi.atInventory(server,omniverse$target());
        int previous=currentLevel,previousMax=currentMaxLevel;boolean previousState=redstoneState;
        currentMinLevel=0;currentMaxLevel=Math.max(0,storage.totalCapacity());
        var filter=filtering.getFilter();
        long count=storage.contents().view().entrySet().stream()
            .filter(e->EssentiaFilter.test(filter,e.getKey())).mapToLong(e->Math.max(0,e.getValue())).sum();
        currentLevel=(int)Math.min(currentMaxLevel,count);
        if(redstoneState&&currentLevel<=offWhenBelow)redstoneState=false;
        else if(!redstoneState&&currentLevel>=onWhenAbove)redstoneState=true;
        int bar=currentLevel==0?0:Math.min(5,1+(int)(4d*currentLevel/Math.max(1,currentMaxLevel)));
        if(getBlockState().getValue(ThresholdSwitchBlock.LEVEL)!=bar)
            level.setBlock(worldPosition,getBlockState().setValue(ThresholdSwitchBlock.LEVEL,bar),previousState!=redstoneState?3:2);
        if(previousState!=redstoneState)scheduleBlockTick();
        if(previous!=currentLevel||previousMax!=currentMaxLevel||previousState!=redstoneState){
            DisplayLinkBlock.notifyGatherers(level,worldPosition);notifyUpdate();
        }
        ci.cancel();
    }
    @Inject(method="getTypeOfCurrentTarget",at=@At("HEAD"),cancellable=true)
    private void omniverse$type(CallbackInfoReturnable<ThresholdSwitchBlockEntity.ThresholdType> cir){
        if(omniverse$essentia())cir.setReturnValue(ThresholdSwitchBlockEntity.ThresholdType.CUSTOM);
    }
    @Inject(method="format",at=@At("HEAD"),cancellable=true)
    private void omniverse$format(int value,boolean stacks,CallbackInfoReturnable<MutableComponent> cir){
        if(omniverse$essentia())cir.setReturnValue(Component.translatable("thaumicomniverse.essentia_amount",value)
            .append(" ("+Math.round(100d*value/Math.max(1,currentMaxLevel))+"%)"));
    }
}
