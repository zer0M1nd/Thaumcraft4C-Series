package conciliummodern;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.AuraNodeBlockEntity;

@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID)
public final class RiftSpawning {
    private record BrokenNode(ServerLevel level,BlockPos pos,AuraNodeBlockEntity original,int size){}
    private static final List<BrokenNode> PENDING=new ArrayList<>();
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void broken(BlockEvent.BreakEvent event){
        if(event.getPlayer()==null || !(event.getLevel() instanceof ServerLevel level) || !(level.getBlockEntity(event.getPos()) instanceof AuraNodeBlockEntity node))return;
        long amount=node.aspects().total()/2;
        if(amount<5 || level.random.nextInt(100)>=ConciliumMobConfig.RIFT_CHANCE.get())return;
        PENDING.add(new BrokenNode(level,event.getPos().immutable(),node,(int)Math.min(80,amount)));
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        // Observe actual removal after all break listeners, never spawn for a denied break.
        for(var broken:PENDING) {
            var level=broken.level();var pos=broken.pos();if(level.getServer()!=event.getServer() || !level.hasChunkAt(pos) || level.getBlockEntity(pos)==broken.original())continue;
            var rift=ConciliumEntities.RIFT.get().create(level);if(rift==null)continue;
            rift.setPos(pos.getX(),pos.getY(),pos.getZ());rift.initialize(level.random.nextInt(),broken.size());level.addFreshEntity(rift);
        }
        PENDING.clear();
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){PENDING.clear();}
}
