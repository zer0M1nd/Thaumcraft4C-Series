package com.ilya3point999k.thaumicconcilium.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** Post-completion notification. Server bridge delivers committed additions at the end of the player's tick. */
public class ThaumcraftResearchCompletedEvent extends PlayerEvent {
    private final String researchKey;
    public ThaumcraftResearchCompletedEvent(Player player,String key){super(player);researchKey=java.util.Objects.requireNonNull(key);}
    public String getUnlockedResearch(){return researchKey;}
    public static void fireResearchCompletedEvent(Player player,String key){net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new ThaumcraftResearchCompletedEvent(player,key));}
}
