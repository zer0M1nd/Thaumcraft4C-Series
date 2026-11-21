package thmodern.client;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thmodern.Horizons;
import thmodern.lens.LensEquipment;
/** Immutable renderer-only mask. No block state, collision or block entity is replaced. */
@Mod.EventBusSubscriber(modid=Horizons.ID,value=Dist.CLIENT)
public final class EarthLensMask {
    private static volatile Set<BlockPos> hidden=Set.of();
    private static net.minecraft.client.multiplayer.ClientLevel world;
    public static boolean hidden(BlockPos pos){return hidden.contains(pos);}
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(world!=mc.level){hidden=Set.of();world=mc.level;}var next=new HashSet<BlockPos>();
        if(mc.level!=null&&mc.player!=null&&mc.player.isAlive()&&LensEquipment.active(mc.player).equals("earth")&&mc.hitResult instanceof BlockHitResult hit&&hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK){
            var center=hit.getBlockPos();var axis=hit.getDirection().getAxis();
            for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++){
                var pos=axis==Direction.Axis.Y?center.offset(a,0,b):axis==Direction.Axis.Z?center.offset(a,b,0):center.offset(0,a,b);
                if(!mc.level.hasChunkAt(pos))continue;var state=mc.level.getBlockState(pos);
                if(!state.isAir()&&!state.hasBlockEntity()&&state.getLightEmission(mc.level,pos)==0&&!state.getCollisionShape(mc.level,pos).isEmpty())next.add(pos.immutable());
            }
        }
        if(next.equals(hidden))return;var changed=new HashSet<>(hidden);changed.addAll(next);hidden=Set.copyOf(next);
        if(mc.level!=null)for(var pos:changed)mc.levelRenderer.setBlocksDirty(pos.getX()-1,pos.getY()-1,pos.getZ()-1,pos.getX()+1,pos.getY()+1,pos.getZ()+1);
    }
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){hidden=Set.of();world=null;}
}
