package thaumicomniverse.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import slimeknights.tconstruct.library.tools.definition.module.aoe.AreaOfEffectIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.BoxAOEIterator;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveToolHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import thaumicomniverse.compat.tinkers.BedrockMining;
import java.util.function.Predicate;
import net.minecraft.world.item.context.UseOnContext;

/** Keep the native iterator's shape, expansion levels and sneaking behavior. */
@Mixin(value=BoxAOEIterator.class,remap=false)
public abstract class TinkersBedrockAreaMixin {
    @Redirect(method="calculate",at=@At(value="INVOKE",target="Lslimeknights/tconstruct/library/tools/definition/module/aoe/AreaOfEffectIterator;defaultBlockPredicate(Lslimeknights/tconstruct/library/tools/nbt/IToolStackView;Lnet/minecraft/world/item/context/UseOnContext;Lslimeknights/tconstruct/library/tools/definition/module/aoe/AreaOfEffectIterator$AOEMatchType;)Ljava/util/function/Predicate;"),remap=false)
    private static Predicate<BlockPos> omniverse$bedrock(IToolStackView tool,UseOnContext context,AreaOfEffectIterator.AOEMatchType match) {
        var original=AreaOfEffectIterator.defaultBlockPredicate(tool,context,match);var level=context.getLevel();
        if(match==AreaOfEffectIterator.AOEMatchType.TRANSFORM || !BedrockMining.dimension(level) || !BedrockMining.pick(tool))return original;
        return pos->{
            if(!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos))return false;
            var state=level.getBlockState(pos);
            return state.is(Blocks.BEDROCK)?BedrockMining.allowed(tool,level,pos)
                    :!state.isAir() && state.getDestroySpeed(level,pos)>=0 && IsEffectiveToolHook.isEffective(tool,state);
        };
    }
}
