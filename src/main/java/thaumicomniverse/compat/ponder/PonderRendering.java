package thaumicomniverse.compat.ponder;

import thaumcraft.api.client.DeviceRendering;
import tc4revival.content.*;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Environment selection is owned by the addon; TC4 has no Create/Ponder dependency. */
public final class PonderRendering {
    private PonderRendering() {}
    private static boolean ponder(BlockEntity entity){return entity.getLevel() instanceof net.createmod.ponder.api.level.PonderLevel;}
    public static void setup(){
        DeviceRendering.registerOverride("thaumicomniverse:ponder_aura",0,AuraNodeBlockEntity.class,PonderRendering::ponder,
            c->PonderNodeRenderers.draw(c.entity().aspects(),c.entity().nodeType(),false,c.pose(),c.buffers(),1));
        DeviceRendering.registerOverride("thaumicomniverse:ponder_energized",0,EnergizedNodeBlockEntity.class,PonderRendering::ponder,
            c->PonderNodeRenderers.draw(c.entity().aura(),c.entity().nodeType(),true,c.pose(),c.buffers(),1));
        DeviceRendering.registerOverride("thaumicomniverse:ponder_jar",0,NodeJarBlockEntity.class,PonderRendering::ponder,
            c->PonderNodeRenderers.draw(c.entity().contents().aspects(),c.entity().contents().type(),false,c.pose(),c.buffers(),.65f));
        DeviceRendering.registerOverride("thaumicomniverse:ponder_portal",0,EldritchPortalBlockEntity.class,PonderRendering::ponder,
            c->PonderNodeRenderers.drawPortal(c.entity(),c.partial(),c.pose(),c.buffers()));
        DeviceRendering.registerLayer("thaumicomniverse:ponder_relay",0,VisRelayBlockEntity.class,PonderRendering::ponder,c->{
            var tile=c.entity();if(tile.displayedParentPoint()==null)return;var origin=net.minecraft.world.phys.Vec3.atLowerCornerOf(tile.getBlockPos());
            PonderNodeRenderers.beam(tile.displayedParentPoint().subtract(origin),tile.beamPoint().subtract(origin),c.pose(),c.buffers());
        });
    }
}
