package ttmodern;

import java.util.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import tc4revival.content.*;
import tc4revival.aspect.*;

public final class ShadowbeamFocus extends KamiFocus {
    public ShadowbeamFocus(){super("focus_shadowbeam","TTKSH",0x4b0053);}
    @Override public String ornamentTexture(){return Tinkerer.ID+":item/focus_shadowbeam_orn";}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){return Map.of(TCAspects.catalog().require("aer"),TinkererConfig.SHADOW_AIR.get(),TCAspects.catalog().require("ordo"),TinkererConfig.SHADOW_ORDER.get(),TCAspects.catalog().require("perditio"),TinkererConfig.SHADOW_ENTROPY.get());}
    @Override public boolean costPerTick(ItemStack focus){return true;}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){return rank>=1&&rank<=5?List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY):List.of();}
    static Vec3 reflect(Vec3 vector,net.minecraft.core.Direction face){var normal=Vec3.atLowerCornerOf(face.getNormal());return vector.subtract(normal.scale(2*vector.dot(normal)));}
    static java.util.List<Vec3> trace(net.minecraft.server.level.ServerLevel level,Player player,float potency){
        var position=player.getEyePosition().add(0,-.1,0);var velocity=player.getLookAngle().normalize().scale(.15);var points=new ArrayList<Vec3>();var hitEntities=new HashSet<Integer>();points.add(position);
        // Legacy Beam updates outside World ticking; only its own increment advances the 300-step lifetime.
        for(int step=0;step<300;step++){
            var end=position.add(velocity);if(!level.hasChunkAt(net.minecraft.core.BlockPos.containing(end))||!level.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(end)))break;
            var block=level.clip(new net.minecraft.world.level.ClipContext(position,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,player));boolean collided=block.getType()==HitResult.Type.BLOCK;var limit=collided?block.getLocation():end;
            var start=position;for(var entity:level.getEntities(player,new AABB(position,limit).inflate(.3),entity->entity.isAlive()&&!entity.isSpectator()&&entity.isPickable())){
                if(!thaumcraft.api.wands.WandActors.canTarget(player,entity)||hitEntities.contains(entity.getId())||entity instanceof Player other&&(!level.getServer().isPvpAllowed()||!player.canHarmPlayer(other)))continue;
                var box=entity.getBoundingBox().inflate(.3);if(box.contains(start)||box.clip(start,limit).isPresent()){hitEntities.add(entity.getId());entity.hurt(level.damageSources().playerAttack(player),(float)(TinkererConfig.SHADOW_DAMAGE.get()*(1+.2*potency)));}
            }
            if(collided){points.add(limit);velocity=reflect(velocity,block.getDirection());position=limit.add(Vec3.atLowerCornerOf(block.getDirection().getNormal()).scale(.001));}else position=end;
            if(step%2==0)points.add(position);
        }return points;
    }
    @Override public void usingTick(ItemStack stack,Player player,int remaining){if(!(player.level() instanceof net.minecraft.server.level.ServerLevel level)||!TinkererConfig.KAMI.get()||!player.isAlive()||!(stack.getItem() instanceof CastingWandItem wand)||!wand.consumeForPlayer(stack,player,costs(attached(stack)),false,false,true))return;
        var points=trace(level,player,wand.focusPotency(stack,player));SoulHearts.CHANNEL.send(net.minecraftforge.network.PacketDistributor.NEAR.with(()->new net.minecraftforge.network.PacketDistributor.TargetPoint(player.getX(),player.getY(),player.getZ(),64,level.dimension())),new ShadowTrail(player.getId(),points));
        if(level.random.nextInt(10)==0)level.playSound(null,player.blockPosition(),TCSounds.BRAIN.get(),net.minecraft.sounds.SoundSource.PLAYERS,.5f,1);
    }
    public record ShadowTrail(int caster,java.util.List<Vec3> points){
        static void encode(ShadowTrail packet,net.minecraft.network.FriendlyByteBuf buffer){buffer.writeVarInt(packet.caster);buffer.writeVarInt(packet.points.size());for(var p:packet.points)buffer.writeDouble(p.x).writeDouble(p.y).writeDouble(p.z);}
        static ShadowTrail decode(net.minecraft.network.FriendlyByteBuf buffer){int caster=buffer.readVarInt(),count=buffer.readVarInt();if(count<0||count>451)throw new IllegalArgumentException("Invalid shadow trail length");var points=new ArrayList<Vec3>(count);for(int i=0;i<count;i++)points.add(new Vec3(buffer.readDouble(),buffer.readDouble(),buffer.readDouble()));return new ShadowTrail(caster,points);}
    }
}
