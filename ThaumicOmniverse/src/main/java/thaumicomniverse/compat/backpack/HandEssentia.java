package thaumicomniverse.compat.backpack;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.*;
import tc4revival.content.*;

/** Explicit container semantics: an arbitrary StoredAspectItem is not a refillable jar. */
final class HandEssentia {
    static boolean transfer(Player player, EssentiaTankUpgradeItem.Wrapper tank, boolean input) {
        for (var hand : InteractionHand.values()) {
            var held = player.getItemInHand(hand);
            ItemStack result = ItemStack.EMPTY;
            if (held.is(TCItems.ESSENTIA_PHIAL.get()) && input) {
                var contents = ((StoredAspectItem) held.getItem()).storedAspects(held);
                if (contents.size() == 1) {
                    var aspect = contents.aspects().get(0);
                    if (contents.amount(aspect) == 8 && tank.insert(aspect, 8, true) == 8) {
                        tank.insert(aspect, 8, false); result = new ItemStack(TCItems.EMPTY_PHIAL.get());
                    }
                }
            } else if (held.is(TCItems.EMPTY_PHIAL.get()) && !input && tank.aspect() != null && tank.amount() >= 8) {
                var aspect = tank.aspect(); tank.extract(aspect, 8, false); result = EssentiaPhialItem.filled(aspect);
            } else if (held.getItem() instanceof EssentiaJarItem jar) {
                var contents = jar.storedAspects(held);
                if (contents.size() > 1) continue;
                var aspect = input ? contents.isEmpty() ? null : contents.aspects().get(0) : tank.aspect();
                if (aspect == null) continue;
                int before = contents.amount(aspect);
                if (!contents.isEmpty() && !contents.aspects().get(0).equals(aspect)) continue;
                if (!input && held.hasTag() && held.getTag().contains("AspectFilter")
                        && !held.getTag().getString("AspectFilter").equals(aspect.tag())) continue;
                if (before < 0 || before > 64) continue;
                int count = input ? tank.insert(aspect, Math.min(8, before), true)
                        : tank.extract(aspect, Math.min(8, 64 - before), true);
                if (count == 0) continue;
                if (input) tank.insert(aspect, count, false); else tank.extract(aspect, count, false);
                int after = before + (input ? -count : count);
                boolean portable = after > 0 || held.hasTag() && held.getTag().contains("AspectFilter");
                boolean voidJar = jar.getBlock() == TCArcane.VOID_JAR.get();
                result = new ItemStack(voidJar ? portable ? TCItems.FILLED_VOID_JAR.get() : TCItems.VOID_JAR.get()
                        : portable ? TCItems.FILLED_ESSENTIA_JAR.get() : TCItems.ESSENTIA_JAR.get());
                if (held.hasTag()) result.setTag(held.getTag().copy());
                ((StoredAspectItem) result.getItem()).setStoredAspects(result,
                        after > 0 ? new AspectAmounts().add(aspect, after) : new AspectAmounts());
            }
            if (!result.isEmpty()) {
                if (held.getCount() == 1) player.setItemInHand(hand, result);
                else { held.shrink(1); if (!player.getInventory().add(result)) player.drop(result, false); }
                player.getInventory().setChanged(); player.containerMenu.broadcastChanges();
                return true;
            }
        }
        return false;
    }
}
