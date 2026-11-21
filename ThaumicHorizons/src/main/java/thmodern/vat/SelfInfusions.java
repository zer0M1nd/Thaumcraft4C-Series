package thmodern.vat;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thmodern.Horizons;

/** Player body data is separate from creature augmentations and does not survive ordinary death. */
@Mod.EventBusSubscriber(modid=Horizons.ID)
public final class SelfInfusions {
    public static final String DATA="thaumichorizons:self_infusions";
    public static final Set<String> TYPES=Set.of("quicksilver_limbs","awake_blood","diamond_skin","silverwood_heart","photosynthetic_skin","amphibious","spider_climb","chameleon_skin","warped_tumor","morphic_fingers");
    public static Set<String> read(CompoundTag tag){
        var result=new LinkedHashSet<String>();for(var value:tag.getList("Applied",Tag.TAG_STRING)){String key=value.getAsString();if(TYPES.contains(key))result.add(key);}return result;
    }
    public static CompoundTag write(Collection<String> types){
        var tag=new CompoundTag();var list=new ListTag();types.stream().filter(TYPES::contains).distinct().sorted().forEach(key->list.add(StringTag.valueOf(key)));tag.put("Applied",list);return tag;
    }
    public static boolean has(Player player,String key){return read(player.getPersistentData().getCompound(DATA)).contains(key);}
    /** Soul transfer supplies the new body's complete set, never a union with the old body. */
    public static void replace(Player player,Collection<String> types){
        WarpTumor.release(player);
        var old=player.getPersistentData().getCompound(DATA);
        for(var value:old.getList("Effects",Tag.TAG_STRING)){
            var type=net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(net.minecraft.resources.ResourceLocation.tryParse(value.getAsString()));
            var effect=type==null?null:player.getEffect(type);
            if(effect!=null&&effect.isInfiniteDuration()&&effect.getAmplifier()==0&&effect.isAmbient())player.removeEffect(type);
        }
        player.getPersistentData().put(DATA,write(types));refresh(player);BodyNetwork.sync(player);
    }
    private static void effect(Player player,MobEffect type){
        var old=player.getEffect(type);if(old!=null&&(old.getAmplifier()>0||old.isInfiniteDuration()))return;
        var value=new MobEffectInstance(type,-1,0,true,false);value.setCurativeItems(List.of());player.addEffect(value);
        var data=player.getPersistentData().getCompound(DATA);var owned=data.getList("Effects",Tag.TAG_STRING);var key=StringTag.valueOf(net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(type).toString());if(!owned.contains(key))owned.add(key);data.put("Effects",owned);
    }
    public static void refresh(Player player){
        WarpTumor.absorb(player);
        if(has(player,"quicksilver_limbs")){effect(player,MobEffects.JUMP);effect(player,MobEffects.MOVEMENT_SPEED);}
        if(has(player,"awake_blood"))effect(player,MobEffects.REGENERATION);
        if(has(player,"diamond_skin"))effect(player,MobEffects.DAMAGE_RESISTANCE);
        if(has(player,"chameleon_skin")&&!player.getPersistentData().getCompound(DATA).getBoolean("InvisibleDisabled"))effect(player,MobEffects.INVISIBILITY);
    }
    public static boolean toggle(Player player,int action){
        if(player.level().isClientSide||!player.isAlive()||player.isSpectator())return false;
        if(action==2){
            if(!(player instanceof net.minecraft.server.level.ServerPlayer server)||!has(player,"morphic_fingers")||player.containerMenu!=player.inventoryMenu)return false;
            net.minecraftforge.network.NetworkHooks.openScreen(server,new net.minecraft.world.SimpleMenuProvider((id,inventory,viewer)->new MorphicFingersMenu(id,inventory),net.minecraft.network.chat.Component.translatable("container.thaumichorizons.fingers")),buffer->{});return true;
        }
        String key=action==0?"spider_climb":action==1?"chameleon_skin":"";if(!has(player,key))return false;
        var data=player.getPersistentData().getCompound(DATA);String field=action==0?"ClimbDisabled":"InvisibleDisabled";data.putBoolean(field,!data.getBoolean(field));
        if(action==1){
            if(data.getBoolean(field)){var effect=player.getEffect(MobEffects.INVISIBILITY);if(effect!=null&&effect.isInfiniteDuration()&&effect.getAmplifier()==0&&effect.isAmbient())player.removeEffect(MobEffects.INVISIBILITY);}
            else effect(player,MobEffects.INVISIBILITY);
        }
        BodyNetwork.sync(player);return true;
    }
    public static void climb(Player player){
        if(!player.isAlive()||player.isSpectator()||!has(player,"spider_climb")||player.getPersistentData().getCompound(DATA).getBoolean("ClimbDisabled"))return;
        double speed;
        if(player.horizontalCollision)speed=player.isShiftKeyDown()?0:.2;
        else {
            double radius=player.getBbWidth()/1.5;var box=new net.minecraft.world.phys.AABB(player.getX()-radius,player.getY(),player.getZ()-radius,player.getX()+radius,player.getY()+player.getBbHeight()*.9,player.getZ()+radius);
            if(!player.level().getBlockCollisions(player,box).iterator().hasNext())return;
            speed=player.isShiftKeyDown()?0:-.15;
        }
        var movement=player.getDeltaMovement();player.setDeltaMovement(movement.x,speed,movement.z);player.fallDistance=0;
    }
    public static void tick(Player player){
        climb(player);
        if(player.level().isClientSide||!player.isAlive())return;
        if(has(player,"silverwood_heart"))for(var value:List.copyOf(player.getActiveEffects())){
            var key=net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(value.getEffect());
            boolean reduced=value.getEffect()==MobEffects.POISON||value.getEffect()==MobEffects.WITHER||key!=null&&key.getNamespace().equals("thaumcraft")&&Set.of("infectious_vis_exhaust","vis_exhaust","thaumarhia","flux_taint").contains(key.getPath());
            if(reduced&&!value.isInfiniteDuration()){
                player.removeEffect(value.getEffect());
                if(value.getDuration()>1){var shortened=new MobEffectInstance(value.getEffect(),value.getDuration()-1,0,false,value.isVisible(),value.showIcon());shortened.setCurativeItems(value.getCurativeItems());player.addEffect(shortened);}
            }
        }
        if(has(player,"photosynthetic_skin")&&player.tickCount%200==0&&player.level().isDay()&&player.level().canSeeSky(player.blockPosition()))player.getFoodData().eat(1,0);
        if(has(player,"amphibious")&&player.isInWater())player.setAirSupply(300);
        if(player.tickCount%30==0)refresh(player);
    }
    @SubscribeEvent public static void livingTick(LivingEvent.LivingTickEvent event){if(event.getEntity() instanceof Player player)tick(player);}
    @SubscribeEvent public static void taintDamage(net.minecraftforge.event.entity.living.LivingHurtEvent event){
        if(event.getEntity() instanceof Player player&&has(player,"silverwood_heart")&&event.getSource().is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,net.minecraft.resources.ResourceLocation.parse("thaumcraft:taint")))){event.setAmount(0);event.setCanceled(true);}
    }
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event){
        // End return creates a replacement player too, but is not a death.
        if(!event.isWasDeath()){
            event.getEntity().getPersistentData().put(DATA,event.getOriginal().getPersistentData().getCompound(DATA).copy());refresh(event.getEntity());
        }
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void died(net.minecraftforge.event.entity.living.LivingDeathEvent event){if(event.getEntity() instanceof Player player&&!player.level().isClientSide)replace(player,Set.of());}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){BodyNetwork.sync(event.getEntity());}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event){BodyNetwork.sync(event.getEntity());}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){BodyNetwork.sync(event.getEntity());}
}
