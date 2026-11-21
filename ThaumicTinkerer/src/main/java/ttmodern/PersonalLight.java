package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Shared temporary-light lifecycle for Bright Nitor and awakened leggings. */
final class PersonalLight {
    static boolean supportedBy(Player player) {
        if (player.isSpectator() || !player.isAlive()) return false;
        return player.getInventory().contains(new net.minecraft.world.item.ItemStack(Tinkerer.BRIGHT_NITOR.get()))
                || top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                    .map(c -> c.findFirstCurio(Tinkerer.BRIGHT_NITOR.get()).isPresent()).orElse(false)
                || TinkererConfig.KAMI_LEG_LIGHT.get() && AwakenedArmorEffects.active(player,EquipmentSlot.LEGS);
    }

    static void place(Level level,Entity source,BlockPos pos,int power) {
        if (level.isClientSide || level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)
                || !level.getWorldBorder().isWithinBounds(pos)) return;
        var before=level.getBlockState(pos);
        if (!before.is(Blocks.AIR) && !before.is(Tinkerer.NITOR_GAS.get())) return;
        // A carried nitor must not downgrade the stronger centre of leggings light.
        if (before.is(Tinkerer.NITOR_GAS.get()) && before.getValue(GasBlock.POWER)>=power) return;
        if (source instanceof Player player && !level.mayInteract(player,pos)) return;
        var snapshot=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos);
        if (!level.setBlock(pos,Tinkerer.NITOR_GAS.get().defaultBlockState().setValue(GasBlock.POWER,power),2)) return;
        if (source instanceof Player player && net.minecraftforge.event.ForgeEventFactory.onBlockPlace(player,snapshot,Direction.UP)) {
            level.setBlock(pos,before,2);
            return;
        }
        level.updateNeighborsAt(pos,Tinkerer.NITOR_GAS.get());
    }
    private PersonalLight() {}
}
