package ttmodern;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.*;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID)
public final class ElementalEffects {
    public static final DeferredRegister<MobEffect> REGISTRY=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,Tinkerer.ID);
    public static final Map<String,RegistryObject<MobEffect>> EFFECTS=create();
    private static Map<String,RegistryObject<MobEffect>> create() {
        var result=new LinkedHashMap<String,RegistryObject<MobEffect>>();for(String aspect:List.of("aer","ignis","terra","aqua"))result.put(aspect,REGISTRY.register("imbued_"+aspect,()->new MobEffect(MobEffectCategory.HARMFUL,switch(aspect){case "aer"->0xffff7e;case "ignis"->0xff5a01;case "terra"->0x56c000;default->0x3cd4fc;}){}));return Map.copyOf(result);
    }
    @SubscribeEvent public static void attack(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if(!(event.getEntity().level() instanceof ServerLevel world)||!(event.getSource().getDirectEntity() instanceof Player player))return;
        var target=event.getEntity();var data=target.getPersistentData();long now=world.getGameTime();
        if(player.hasEffect(EFFECTS.get("aer").get()))data.putLong("ttmodern_air_until",now+20);
        if(player.hasEffect(EFFECTS.get("ignis").get()))data.putLong("ttmodern_fire_until",now+6000);
        if(player.hasEffect(EFFECTS.get("terra").get())) {
            boolean xAxis=Math.abs(target.getZ()-player.getZ())<Math.abs(target.getX()-player.getX());var center=BlockPos.containing((target.getX()+player.getX())/2,player.getY()+2,(target.getZ()+player.getZ())/2);
            for(int i=-2;i<=2;i++)for(int j=-2;j<=2;j++) {var pos=center.offset(xAxis?0:j,i,xAxis?j:0);if(!world.hasChunkAt(pos)||!world.isEmptyBlock(pos)||!world.mayInteract(player,pos))continue;
                var snapshot=net.minecraftforge.common.util.BlockSnapshot.create(world.dimension(),world,pos);world.setBlock(pos,Tinkerer.FORCEFIELD.get().defaultBlockState(),3);
                if(net.minecraftforge.event.ForgeEventFactory.onBlockPlace(player,snapshot,net.minecraft.core.Direction.UP)){snapshot.restore(true,false);continue;}
                // The original forcefield is invisible and has no end-rod particles.
            }
        }
    }
    @SubscribeEvent public static void livingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        LivingEntity entity=event.getEntity();if(!(entity.level() instanceof ServerLevel world))return;long now=world.getGameTime();var tag=entity.getPersistentData();
        for(String kind:List.of("air","fire")) {String key="ttmodern_"+kind+"_until";if(!tag.contains(key))continue;if(!entity.isAlive()||now>tag.getLong(key)){tag.remove(key);continue;}if(now%5!=0)continue;
            if(kind.equals("air")){entity.setDeltaMovement(world.random.nextFloat()-.5,world.random.nextFloat(),world.random.nextFloat()-.5);entity.hurtMarked=true;tc4revival.network.FocusEffectPacket.burst(world,entity.position(),.5f);}
            else {entity.setSecondsOnFire(6);thaumcraft.api.fx.LegacyEffects.wisp(world,entity.position().add(0,1,0),0xee3333,.1f,30,2.5f,1);}
        }
    }
    @SubscribeEvent public static void playerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END||!(event.player.level() instanceof ServerLevel world)||!event.player.hasEffect(EFFECTS.get("aqua").get()))return;
        var player=event.player;for(int x=(int)Math.floor(player.getX()-2);x<player.getX()+2;x++)for(int y=(int)Math.floor(player.getY()-2);y<player.getY()+2;y++)for(int z=(int)Math.floor(player.getZ()-2);z<player.getZ()+2;z++) {var pos=new BlockPos(x,y,z);if(world.hasChunkAt(pos)&&world.getBlockState(pos).is(Blocks.LAVA)&&world.mayInteract(player,pos)){world.setBlock(pos,Blocks.OBSIDIAN.defaultBlockState(),3);}}
    }
}
