package conciliummodern;

import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

/** Living vat output, settling into its redstone-sensitive block after release. */
public final class RedPoweredMind extends Mob {
    public RedPoweredMind(EntityType<? extends RedPoweredMind> type,Level level){super(type,level);setPersistenceRequired();}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,20);}
    @Override public void tick(){super.tick();if(level().isClientSide||!isAlive()||tickCount%20!=1)return;
        var pos=blockPosition();if(!level().getBlockState(pos).isAir()||!level().getWorldBorder().isWithinBounds(pos)||level().isOutsideBuildHeight(pos))return;
        if(!net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level(),this))return;
        var before=net.minecraftforge.common.util.BlockSnapshot.create(level().dimension(),level(),pos);
        if(level().setBlock(pos,ConciliumBlocks.RED_MIND.get().defaultBlockState(),3)){
            if(net.minecraftforge.event.ForgeEventFactory.onBlockPlace(this,before,net.minecraft.core.Direction.UP)){before.restore(true,false);return;}
            // Creature transformations preserve equipment; settling must not erase carried stacks.
            for(var slot:EquipmentSlot.values()){var stack=getItemBySlot(slot);if(!stack.isEmpty()){spawnAtLocation(stack.copy());setItemSlot(slot,net.minecraft.world.item.ItemStack.EMPTY);}}
            discard();
        }
    }
}
