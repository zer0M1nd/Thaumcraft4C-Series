package automagy.mixin;

import automagy.content.NetherRuneBlock;
import automagy.content.RunedObsidianBlock;
import automagy.logic.RunicBeaconBase;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {
    // 1.20.1 official development name and verified SRG distribution name. No refmap is needed.
    @Inject(method={"updateBase(Lnet/minecraft/world/level/Level;III)I","m_155092_(Lnet/minecraft/world/level/Level;III)I"},at=@At("RETURN"),cancellable=true,remap=false,require=1)
    private static void automagy$runicFirstTier(Level level,int x,int y,int z,CallbackInfoReturnable<Integer> result){
        if(result.getReturnValueI()>=4)return;
        int levels=RunicBeaconBase.levels((dx,depth,dz)->{
            var pos=new BlockPos(x+dx,y-depth,z+dz);
            if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return new RunicBeaconBase.Cell(false,false,false);
            var state=level.getBlockState(pos);
            return new RunicBeaconBase.Cell(true,state.is(BlockTags.BEACON_BASE_BLOCKS),state.getBlock() instanceof RunedObsidianBlock||state.getBlock() instanceof NetherRuneBlock);
        });
        if(levels>result.getReturnValueI())result.setReturnValue(levels);
    }
}
