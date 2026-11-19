package thaumcraft.api.fx;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.PacketDistributor;
import tc4revival.network.LegacyEffectPacket;
import tc4revival.network.TCNetwork;

/** Shared, cosmetic-only effects. Safe on either logical side; call once on the authoritative side. */
public class LegacyEffects {
    public static void emit(Level level,String style,Vec3 from,Vec3 to,int color,float scale,int life,int count,float spread,float gravity) {
        if(level==null)return;
        var packet=new LegacyEffectPacket(level.dimension().location(),style,from,to,color,scale,life,count,spread,gravity);
        if(level.isClientSide)DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->tc4revival.client.LegacyAddonEffects.emit(packet));
        else TCNetwork.CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(from.x,from.y,from.z,64,level.dimension())),packet);
    }
    public static void sparkle(Level level,Vec3 at,int color,float size,int count,float spread) {emit(level,"sparkle",at,Vec3.ZERO,color,size,18,count,spread,0);}
    public static void wisp(Level level,Vec3 at,int color,float size,int count,float spread,float gravity) {emit(level,"wisp",at,Vec3.ZERO,color,size,40,count,spread,gravity);}
    public static void beam(Level level,Vec3 from,Vec3 to,int color,float width,int life) {emit(level,"beam",from,to,color,width,life,1,0,0);}
    public static void wandBeam(net.minecraft.world.entity.player.Player player,Vec3 end,int type,int color,float endScale,boolean reverse){
        var actor=thaumcraft.api.wands.WandActors.resolve(player);
        emit(actor.level(),"wand:"+type+":"+actor.getId()+":"+(reverse?1:0),actor.position(),end,color,Math.max(.0001f,endScale),8,1,0,0);
    }
    public static void smokeSpiral(Level level,Vec3 at,float radius,double floor,int color,int count) {emit(level,"spiral",at,new Vec3(radius,floor,0),color,1,25,count,0,0);}
}
