package conciliummodern;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Dimensional rifts are short-lived, omit natural terrain cutting, and retain their identity in Forge persistent data. */
public final class RiftTransit {
    public static final ResourceKey<Level> DIMENSION=ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse("thaumicconcilium:causal_bouillon"));
    private static final String FLAG="thaumicconcilium:interdimensional";
    public static void initialize(RiftEntity rift){rift.getPersistentData().putBoolean(FLAG,true);}
    public static boolean active(RiftEntity rift){return rift.getPersistentData().getBoolean(FLAG);}
    public static void tick(RiftEntity rift,int age){
        if(!active(rift))return;
        if(rift.riftSize()>=50||age>200)rift.collapseTo(0);
        var random=rift.level().random;
        if(random.nextInt(3)==0){var start=rift.position().add(.5+(random.nextFloat()-.5)*5,.5+(random.nextFloat()-.5)*5,.5+(random.nextFloat()-.5)*5);
            var points=rift.geometry().points();var end=points.get(random.nextInt(points.size()-1)).add(rift.position()).add(0,rift.centerOffset(),0);
            ConciliumArcNetwork.emit(rift.level(),start,end,0x1e001e,.3f);}
    }
    /** Called at the same physical contact points as the rift's normal interactions. */
    public static boolean contact(RiftEntity rift,Entity entity){
        if(!active(rift)||!(entity instanceof ServerPlayer player)||!player.isAlive()||player.isSpectator()||player.isPassenger()||player.isVehicle()||player.isOnPortalCooldown())return false;
        ItemStack gem=player.getMainHandItem();
        if(!(gem.getItem() instanceof DimensionalGem item)||!item.rift()||!item.awakened())return false;
        boolean entering=!player.level().dimension().equals(DIMENSION);
        var sample=GemCoordinates.selected(gem);var destination=entering?DIMENSION:sample==null?Level.OVERWORLD:sample.dimension();
        if(destination.equals(player.level().dimension()))return false;
        var target=player.server.getLevel(destination);if(target==null){failure(player,"thaumicconcilium.gem.missing_dimension");return false;}
        if(!net.minecraftforge.common.ForgeHooks.onTravelToDimension(player,destination))return false;
        double x=GemRules.coordinate(player.getX(),entering,GemCoordinates.reverse(gem)),z=GemRules.coordinate(player.getZ(),entering,GemCoordinates.reverse(gem));
        if(!GemRules.validCoordinate(x)||!GemRules.validCoordinate(z))return false;
        var column=BlockPos.containing(x,0,z);if(!target.getWorldBorder().isWithinBounds(column)){failure(player,"thaumicconcilium.gem.outside_border");return false;}
        // Destination loading is explicit travel work. No terrain is destroyed or cleared to create a landing.
        var requested=new Vec3(x,entering?5:player.getY(),z);var landing=findLanding(target,player,requested);
        if(landing==null){failure(player,"thaumicconcilium.gem.no_landing");return false;}
        player.teleportTo(target,landing.x,landing.y,landing.z,player.getYRot(),player.getXRot());
        if(player.level()!=target)return false;
        player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;player.setPortalCooldown();
        return true;
    }
    static Vec3 findLanding(ServerLevel target,ServerPlayer player,Vec3 requested){
        int min=target.getMinBuildHeight()+1,max=target.getMaxBuildHeight()-(int)Math.ceil(player.getBbHeight());
        int preferred=net.minecraft.util.Mth.clamp((int)Math.floor(requested.y),min,max);
        var footprint=player.getBoundingBox().move(requested.x-player.getX(),preferred-player.getY(),requested.z-player.getZ());
        int minChunkX=net.minecraft.util.Mth.floor(footprint.minX)>>4,maxChunkX=net.minecraft.util.Mth.floor(footprint.maxX)>>4;
        int minChunkZ=net.minecraft.util.Mth.floor(footprint.minZ)>>4,maxChunkZ=net.minecraft.util.Mth.floor(footprint.maxZ)>>4;
        for(int cx=minChunkX;cx<=maxChunkX;cx++)for(int cz=minChunkZ;cz<=maxChunkZ;cz++)target.getChunk(cx,cz);
        // Prefer exact height, then the closest safe vertical position within the same X/Z column.
        for(int offset=0;offset<=max-min;offset++)for(int sign:offset==0?new int[]{1}:new int[]{1,-1}){
            int y=preferred+offset*sign;if(y<min||y>max)continue;var pos=BlockPos.containing(requested.x,y,requested.z);
            var bounds=player.getBoundingBox().move(requested.x-player.getX(),y-player.getY(),requested.z-player.getZ());
            if(!target.getWorldBorder().isWithinBounds(bounds))continue;
            // Keep the original Y=5 arrival above its Y=3 floor; at most a one-block safe drop is accepted.
            for(int drop=0;drop<=1;drop++){
                var floor=pos.below(drop+1);var path=bounds.expandTowards(0,-drop,0);
                if(target.getBlockState(floor).isFaceSturdy(target,floor,Direction.UP)&&!target.containsAnyLiquid(path)&&target.noCollision(player,path))
                    return new Vec3(requested.x,y,requested.z);
            }
        }
        return null;
    }
    private static void failure(ServerPlayer player,String key){if(player.tickCount%20==0)player.displayClientMessage(net.minecraft.network.chat.Component.translatable(key),true);}
    private RiftTransit(){}
}
