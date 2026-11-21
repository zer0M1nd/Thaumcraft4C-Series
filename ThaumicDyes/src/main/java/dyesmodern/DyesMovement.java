package dyesmodern;

import java.util.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ThaumicDyes.ID)
public final class DyesMovement {
    private static final Map<Player,Float> PREVIOUS_STEP=new WeakHashMap<>();
    private DyesMovement(){}
    public static boolean wearing(Player player){return player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof DyesArmorItem item&&item.traveller();}
    public static void tick(DyesArmorItem item,Player player){
        boolean runic=item.family.equals("ItemRunicBootsTraveller"),thaumium=item.family.equals("ThaumiumBoots");
        if(!player.getAbilities().flying&&player.zza>0){
            if(player.level().isClientSide&&!player.isShiftKeyDown()){PREVIOUS_STEP.putIfAbsent(player,player.maxUpStep());player.setMaxUpStep(1);}
            if(player.onGround()){float speed=runic?.055f:thaumium?.085f:.1f;if(player.isInWater())speed/=runic?4:3;player.moveRelative(speed,new Vec3(0,0,1));}
        }
        if(player.fallDistance>0)player.fallDistance-=runic?.25f:thaumium?.35f:.5f;
    }
    public static float airSpeed(float original,Player player){return wearing(player)&&!player.getAbilities().flying&&player.zza>0&&!player.onGround()?tc4revival.content.HoverFlight.active(player)?.03f:.05f:original;}
    @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent event){
        if(event.getEntity() instanceof Player player
                && player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof DyesArmorItem item
                && item.traveller()){
            double bonus=item.family.equals("VoidBoots")||item.family.equals("ItemRunicArmorPrimalBoots")
                    ?.35D:.2750000059604645D;
            player.setDeltaMovement(player.getDeltaMovement().add(0,bonus,0));
        }
    }
    @SubscribeEvent public static void restoreStep(LivingEvent.LivingTickEvent event){
        if(event.getEntity() instanceof Player player&&player.level().isClientSide&&(player.isShiftKeyDown()||!wearing(player))){var old=PREVIOUS_STEP.remove(player);if(old!=null)player.setMaxUpStep(old);}
    }
}
