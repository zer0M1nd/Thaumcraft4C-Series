package thaumicomniverse.mixin;
import com.simibubi.create.content.redstone.thresholdSwitch.*;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.api.aspects.EssentiaStorageApi;
import thaumicomniverse.compat.create.*;
@Mixin(value=ThresholdSwitchBlockEntity.class,remap=false)
public abstract class CreateEssentiaThresholdMixin extends SmartBlockEntity {
    @Shadow public float currentLevel,onWhenAbove,offWhenBelow;
    @Shadow private boolean redstoneState;
    @Shadow private FilteringBehaviour filtering;
    @Shadow protected abstract void scheduleBlockTick();
    protected CreateEssentiaThresholdMixin(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);}
    @Inject(method="updateCurrentLevel",at=@At("HEAD"),cancellable=true)
    private void omniverse$observe(CallbackInfo ci){
        if(!(level instanceof ServerLevel server)||!ArcaneContraptions.enabled())return;
        var target=worldPosition.relative(ThresholdSwitchBlock.getTargetDirection(getBlockState()));
        if(!level.hasChunkAt(target)||!EssentiaStorageApi.supports(level.getBlockEntity(target),null))return;
        var storage=EssentiaStorageApi.atInventory(server,target);var filter=filtering.getFilter();
        long amount=storage.contents().view().entrySet().stream().filter(e->EssentiaFilter.test(filter,e.getKey())).mapToLong(e->Math.max(0,e.getValue())).sum();
        float previous=currentLevel;boolean powered=redstoneState;
        currentLevel=net.minecraft.util.Mth.clamp((float)amount/Math.max(1,storage.totalCapacity()),0,1);
        if(redstoneState&&currentLevel<=offWhenBelow)redstoneState=false;
        else if(!redstoneState&&currentLevel>=onWhenAbove)redstoneState=true;
        int bar=currentLevel==0?0:Math.min(5,1+(int)(4*currentLevel));
        if(getBlockState().getValue(ThresholdSwitchBlock.LEVEL)!=bar)level.setBlock(worldPosition,getBlockState().setValue(ThresholdSwitchBlock.LEVEL,bar),powered!=redstoneState?3:2);
        if(powered!=redstoneState)scheduleBlockTick();
        if(previous!=currentLevel||powered!=redstoneState){com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock.notifyGatherers(level,worldPosition);notifyUpdate();}
        ci.cancel();
    }
}
