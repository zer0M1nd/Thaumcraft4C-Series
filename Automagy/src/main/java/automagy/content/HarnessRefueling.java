package automagy.content;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import tc4revival.content.*;
import tc4revival.aspect.*;

/** Writes a real removable jar through the main mod's existing public fuel methods. */
public final class HarnessRefueling {
    private HarnessRefueling() {}
    public static int insert(Player player,int maximum){
        if(maximum<=0||!player.isAlive()||player.isSpectator())return 0;
        var harness=player.getItemBySlot(EquipmentSlot.CHEST);
        if(!(harness.getItem() instanceof HoverHarnessItem))return 0;
        var jar=HoverFuel.jar(harness);
        if(!(jar.getItem() instanceof EssentiaJarItem container))return 0;
        var stored=container.storedAspects(jar);
        var potentia=TCAspects.catalog().require("potentia");
        // A player may edit item data: never mix fuel into a jar containing another aspect.
        if(stored.aspects().stream().anyMatch(a->!a.equals(potentia)))return 0;
        if(jar.hasTag()&&!jar.getTag().getString("AspectFilter").isEmpty()&&!jar.getTag().getString("AspectFilter").equals("potentia"))return 0;
        int accepted=Math.min(maximum,Math.max(0,64-stored.amount(potentia)));
        if(accepted<=0)return 0;
        stored.add(potentia,accepted);container.setStoredAspects(jar,stored);HoverFuel.setJar(harness,jar);
        player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();
        return accepted;
    }
}
