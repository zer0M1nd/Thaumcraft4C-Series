package fmmodern;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

/** Retained entity ID loads old saves; AI, leveling, restocking and profession now use vanilla rules. */
public class Heretic extends Villager {
    public Heretic(EntityType<? extends Villager> type,Level level) {super(type,level);}
}
