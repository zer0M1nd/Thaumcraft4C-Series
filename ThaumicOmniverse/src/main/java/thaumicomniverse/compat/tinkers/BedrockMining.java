package thaumicomniverse.compat.tinkers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.*;

public final class BedrockMining {
    private BedrockMining() {}
    public static boolean pick(IToolStackView tool) {
        return ArcaneMaterials.enabled() && !tool.isBroken() && tool.getModifierLevel(ArcaneMaterials.DIVINE)>0
                && ModifierUtil.canPerformAction(tool,net.minecraftforge.common.ToolActions.PICKAXE_DIG);
    }
    public static boolean dimension(Level level) { return level.dimension().location().toString().equals("thaumic_tinkerer:bedrock"); }
    public static boolean allowed(IToolStackView tool,Level level,BlockPos pos) {
        return dimension(level) && pick(tool) && pos.getY()>=2 && pos.getY()<254 && level.hasChunkAt(pos);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void click(PlayerInteractEvent.LeftClickBlock event) {
        if(event.isCanceled() || event.getAction()!=PlayerInteractEvent.LeftClickBlock.Action.START || !(event.getEntity() instanceof ServerPlayer player)
                || !player.level().getBlockState(event.getPos()).is(Blocks.BEDROCK) || !player.getMainHandItem().is(slimeknights.tconstruct.common.TinkerTags.Items.HARVEST))return;
        var tool=ToolStack.from(player.getMainHandItem());
        if(!allowed(tool,player.level(),event.getPos()) || player.isSpectator() || !player.mayBuild()
                || player.distanceToSqr(event.getPos().getCenter())>Math.pow(player.getBlockReach()+1,2))return;
        event.setCanceled(true);
        // Native harvest dispatch performs protection checks, tool AOE, drops and per-block modifier callbacks.
        player.gameMode.destroyBlock(event.getPos());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void speed(PlayerEvent.BreakSpeed event) {
        var player=event.getEntity();
        if(event.isCanceled() || !dimension(player.level()) || !player.getMainHandItem().is(slimeknights.tconstruct.common.TinkerTags.Items.HARVEST))return;
        var tool=ToolStack.from(player.getMainHandItem());
        if(pick(tool) && slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveToolHook.isEffective(tool,event.getState()))
            event.setNewSpeed(Float.MAX_VALUE);
    }
}
