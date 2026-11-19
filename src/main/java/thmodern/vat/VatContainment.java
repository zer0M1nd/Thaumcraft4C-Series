package thmodern.vat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thmodern.Horizons;

/** The synchronized vat owns the restraint. No persistent player flags survive release or removal. */
@Mod.EventBusSubscriber(modid=Horizons.ID)
public class VatContainment {
    public static VatEntity containing(Player player) {
        if(!player.isAlive()||player.isSpectator())return null;
        var base=player.blockPosition();
        // The occupant's feet are two blocks below the controller. Allow the entry/exit boundary too.
        for(int dy=1;dy<=3;dy++) {
            var pos=base.above(dy);
            if(player.level().hasChunkAt(pos)&&player.level().getBlockEntity(pos) instanceof VatEntity vat&&vat.holds(player))return vat;
        }
        return null;
    }
    public static boolean restrain(Player player) {
        var vat=containing(player);if(vat==null)return false;
        var pos=vat.anchor();
        player.setPos(pos.x,pos.y,pos.z);
        player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
        return true;
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if(restrain(event.player)) {
            // Keep camera interpolation stable even if another effect added velocity during the tick.
            event.player.xo=event.player.xOld=event.player.getX();
            event.player.yo=event.player.yOld=event.player.getY();
            event.player.zo=event.player.zOld=event.player.getZ();
        }
    }
    /** Detached subjects are NBT snapshots, not entities receiving normal interpolation updates. */
    public static void settlePreview(LivingEntity entity) {
        entity.xo=entity.xOld=entity.getX();entity.yo=entity.yOld=entity.getY();entity.zo=entity.zOld=entity.getZ();
        entity.xRotO=entity.getXRot();entity.yRotO=entity.getYRot();
        entity.yBodyRotO=entity.yBodyRot;entity.yHeadRotO=entity.yHeadRot;
        entity.setDeltaMovement(Vec3.ZERO);entity.walkAnimation.setSpeed(0);
        entity.hurtTime=0;entity.deathTime=0;
    }
}
