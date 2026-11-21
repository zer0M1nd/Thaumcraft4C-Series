package conciliummodern;

import net.minecraft.client.Minecraft;

/** Original ClientProxy.warpchain and mass-hysteria fragments, driven only by synchronized charge state. */
final class WarpChargeEffects {
    static void tick(WarpCharge charge){
        var mc=Minecraft.getInstance();var owner=charge.owner();
        if(mc.level!=charge.level()||mc.isPaused()||owner==null)return;
        if(charge.tickCount%3==0)CrimsonDrainEffects.local(mc.level,
                owner.position().add(0,mc.level.random.nextDouble()-1,0),charge.position().add(0,.5,0),1,1,0x9929bd,.5f);
        if(charge.hysteria()&&mc.level.random.nextInt(8)==0&&mc.getConnection()!=null){
            var packet=new ConciliumArcNetwork.Taint(mc.level.dimension().location(),charge.position());
            for(int i=0;i<10;i++)ConciliumParticleEffects.taint(packet,mc.getConnection().getConnection());
        }
    }
    private WarpChargeEffects(){}
}
