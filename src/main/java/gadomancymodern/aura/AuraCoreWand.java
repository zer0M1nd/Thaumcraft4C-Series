package gadomancymodern.aura;
import gadomancymodern.Gadomancy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.*;
@Mod.EventBusSubscriber(modid="gadomancy")
public class AuraCoreWand {
    @SubscribeEvent(priority=EventPriority.HIGH) public static void rightClick(PlayerInteractEvent.RightClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer player)||!(event.getItemStack().getItem() instanceof CastingWandItem wand)||!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof CrystalClusterBlock cluster))return;
        if(!thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,"gadomancy:aura_core"))return;
        var drops=event.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(event.getPos()).inflate(10),entity->entity.isAlive()&&!(entity instanceof AuraCoreEntity)&&entity.getItem().is(Gadomancy.AURA_CORES.get("blank").get()));
        if(drops.isEmpty())return;
        var primals=cluster.type()==ShardType.BALANCED?AuraComposition.PRIMALS:java.util.List.of(AuraComposition.PRIMALS.get(cluster.type().metadata()));
        var costs=new java.util.LinkedHashMap<tc4revival.aspect.Aspect,Integer>();for(String a:primals)costs.put(tc4revival.aspect.TCAspects.catalog().require(a),primals.size()==1?84:14);
        if(!wand.consumeForPlayer(event.getItemStack(),player,costs,true,true,true))return;
        activate(event.getLevel(),event.getPos(),drops.get(0),primals);event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
    }
    public static AuraCoreEntity activate(Level world,BlockPos pos,ItemEntity drop,java.util.List<String> primals){
        tc4revival.network.FocusEffectPacket.burst(world,net.minecraft.world.phys.Vec3.atCenterOf(pos),world.random.nextInt(3)+1);
        world.explode(null,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,1.5f,Level.ExplosionInteraction.NONE);world.removeBlock(pos,false);
        var entity=new AuraCoreEntity(Gadomancy.AURA_ENTITY.get(),world);entity.activate(drop,pos,primals);world.addFreshEntity(entity);drop.discard();return entity;
    }
}
