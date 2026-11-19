package thaumcraft.api.research;

import net.minecraft.server.level.ServerPlayer;
import tc4revival.research.*;
/** Posted on MinecraftForge.EVENT_BUS after committing a thaumometer scan. */
public  class ScanEvent extends net.minecraftforge.eventbus.api.Event {
    private final ServerPlayer player;
    private final ScanTarget target;
    private final ScanRules.Result result;
    public ScanEvent(ServerPlayer player,ScanTarget target,ScanRules.Result result) { this.player=player;this.target=target;this.result=result; }
    public ServerPlayer getPlayer() { return player; }
    public ScanTarget getTarget() { return target; }
    public ScanRules.Result getScanResult() { return result; }
}
