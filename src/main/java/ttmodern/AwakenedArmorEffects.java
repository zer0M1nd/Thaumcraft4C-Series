package ttmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Tinkerer.ID)
public final class AwakenedArmorEffects {
    public static final String DISABLED="TTArmorDisabled";
    private static final String FLIGHT_OWNED="TTArmorFlightOwned";
    private static final UUID STEP=UUID.fromString("de4ad600-df24-497b-aee1-85f3059c6dc8");
    public static boolean active(Player player,EquipmentSlot slot){var stack=player.getItemBySlot(slot);return TinkererConfig.KAMI.get()&&!player.getPersistentData().getBoolean(DISABLED)&&stack.getItem() instanceof AwakenedArmorItem&&AwakenedArmorItem.enabled(stack);}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){if(event.phase==TickEvent.Phase.END)tickPlayer(event.player);}
    static void tickPlayer(Player player){
        boolean head=active(player,EquipmentSlot.HEAD),chest=active(player,EquipmentSlot.CHEST),legs=active(player,EquipmentSlot.LEGS),boots=active(player,EquipmentSlot.FEET);
        if(!player.level().isClientSide){
            updateFlight(player,chest&&TinkererConfig.FLIGHT.get());
            if(head){
                if(player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)){player.setAirSupply(player.getMaxAirSupply());effect(player,MobEffects.NIGHT_VISION,202,0);}
                if(player.isEyeInFluid(net.minecraft.tags.FluidTags.LAVA)){player.setAirSupply(player.getMaxAirSupply());effect(player,MobEffects.BLINDNESS,202,0);}
                int food=player.getFoodData().getFoodLevel();if(food>0&&food<18&&player.getHealth()<player.getMaxHealth()&&player.tickCount%80==0)player.heal(1);
            }
            if(chest)DeflectionFocus.protect(player,0);
            if(legs&&player.tickCount%5==0){if(player.isOnFire())player.heal(.5f);effect(player,MobEffects.FIRE_RESISTANCE,119,0);if(TinkererConfig.KAMI_LEG_LIGHT.get())light(player);}
            if(boots){effect(player,MobEffects.DIG_SPEED,2,1);if(TinkererConfig.KAMI_BOOT_GRASS.get())grass(player);}
        }
        var step=player.getAttribute(net.minecraftforge.common.ForgeMod.STEP_HEIGHT_ADDITION.get());
        if(step!=null){boolean high=boots&&!player.isShiftKeyDown();if(high&&!step.hasModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(STEP,"Ichor stepping",.4,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION)))step.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(STEP,"Ichor stepping",.4,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));else if(!high)step.removeModifier(STEP);}
        if(boots){
            // Keep descent distance for Slow Fall and other movement effects.
            // The LivingFallEvent handler below provides the boots' fall protection.
            // Locally controlled motion is predicted on the client; the dedicated server does not add it twice.
            if(player.level().isClientSide&&player.isLocalPlayer()){
                var stack=player.getItemBySlot(EquipmentSlot.FEET);double modifier=number(stack,"speed",1);
                var input=BootMotion.boosted(player.zza,player.xxa,stack.hasTag()&&stack.getTag().getBoolean("omni"));
                if(player.onGround()||player.onClimbable()||player.getAbilities().flying){float bonus=(float)(.075*modifier*(player.isInWater()?.25:1)*(player.isShiftKeyDown()?.5:1));if(input.forward()!=0)player.moveRelative(bonus,new net.minecraft.world.phys.Vec3(0,0,input.forward()));if(input.strafe()!=0)player.moveRelative(bonus,new net.minecraft.world.phys.Vec3(input.strafe(),0,0));}
                if(player.zza==0&&player.xxa==0&&player.getAbilities().flying&&stack.hasTag()&&stack.getTag().getBoolean("inertiacanceling")){var motion=player.getDeltaMovement();player.setDeltaMovement(motion.x*.5,motion.y,motion.z*.5);}
                if(!player.onGround()&&!player.getAbilities().flying)player.moveRelative(player.isSprinting()?.03f:.02f,new net.minecraft.world.phys.Vec3(player.xxa,0,player.zza));
            }
        }
    }
    static void updateFlight(Player player,boolean wanted){
        var tag=player.getPersistentData();var abilities=player.getAbilities();boolean owned=tag.getBoolean(FLIGHT_OWNED);
        if(wanted&&!abilities.mayfly){tag.putBoolean(FLIGHT_OWNED,true);abilities.mayfly=true;syncAbilities(player);}
        else if(!wanted&&owned){tag.remove(FLIGHT_OWNED);if(!player.isCreative()&&!player.isSpectator()){abilities.mayfly=false;abilities.flying=false;syncAbilities(player);}}
    }
    private static void syncAbilities(Player player){if(!(player instanceof net.minecraftforge.common.util.FakePlayer))player.onUpdateAbilities();}
    private static void effect(Player player,MobEffect effect,int duration,int amplifier){var old=player.getEffect(effect);if(old==null||old.getAmplifier()<amplifier||old.getAmplifier()==amplifier&&old.getDuration()<duration)player.addEffect(new MobEffectInstance(effect,duration,amplifier,true,false));}
    private static double number(net.minecraft.world.item.ItemStack stack,String key,double fallback){double value=stack.hasTag()&&stack.getTag().contains(key)?stack.getTag().getDouble(key):fallback;return Double.isFinite(value)?Math.max(0,Math.min(10,value)):fallback;}
    private static void grass(Player player){var world=player.level();var pos=player.blockPosition().below();if(world.getBlockState(pos).is(Blocks.DIRT))place(player,pos,Blocks.GRASS_BLOCK.defaultBlockState());}
    private static void light(Player player){
        var world=player.level();
        PersonalLight.place(world,player,player.blockPosition().above(),1);
        // Yaw, rather than the flattened look vector, also lights ahead when looking straight up/down.
        double yaw=Math.toRadians(player.getYRot());
        var ahead=new net.minecraft.world.phys.Vec3(-Math.sin(yaw),0,Math.cos(yaw));
        for(int i=1;i<=5;i++)PersonalLight.place(world,player,BlockPos.containing(player.position().add(ahead.scale(i)).add(0,1,0)),0);
    }
    private static void place(Player player,BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
        var world=player.level();if(!world.hasChunkAt(pos)||!world.mayInteract(player,pos)||!world.getWorldBorder().isWithinBounds(pos))return;
        var before=world.getBlockState(pos);var snapshot=net.minecraftforge.common.util.BlockSnapshot.create(world.dimension(),world,pos);
        if(!world.setBlock(pos,state,2))return;
        if(net.minecraftforge.event.ForgeEventFactory.onBlockPlace(player,snapshot,Direction.UP)){world.setBlock(pos,before,2);return;}
        world.updateNeighborsAt(pos,state.getBlock());
    }
    @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent event){if(event.getEntity() instanceof Player player&&active(player,EquipmentSlot.FEET))player.setDeltaMovement(player.getDeltaMovement().add(0,.3*number(player.getItemBySlot(EquipmentSlot.FEET),"jump",1),0));}
    // Preserve the travelled distance for landing effects such as Shockwave.
    @SubscribeEvent public static void fall(LivingFallEvent event){if(event.getEntity() instanceof Player player&&active(player,EquipmentSlot.FEET))event.setDamageMultiplier(0);}
    private AwakenedArmorEffects(){}
}
