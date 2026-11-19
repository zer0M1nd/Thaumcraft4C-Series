package tc4revival.content;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

/** The gently floating item used by old wand transformations and other magical outputs. */
public class SpecialItemEntity extends ItemEntity {
    public SpecialItemEntity(EntityType<? extends ItemEntity> type, Level level) { super(type, level); }
    @Override public void tick() {
        var velocity = getDeltaMovement();
        double vertical = velocity.y > 0 ? velocity.y * (double) .9f : velocity.y;
        setDeltaMovement(velocity.x, vertical + (double) .04f, velocity.z);
        super.tick();
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        return !source.is(DamageTypeTags.IS_EXPLOSION) && super.hurt(source, amount);
    }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
}
