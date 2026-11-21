package thmodern.vat;

import java.util.*;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonObject;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.effect.*;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thmodern.Horizons;

@Mod.EventBusSubscriber(modid=Horizons.ID)
public final class CreatureInfusions {
    public static final String DATA="thaumichorizons:infusions";
    public static final List<String> TYPES=List.of("quick_limbs","thaumic_claws","awake_blood","diamond_skin","ender_heart","shock_skin","instilled_loyalty","runic_hide","eldritch_fangs","portability");
    public static boolean has(LivingEntity entity,String key){return entity.getPersistentData().getCompound(DATA).getCompound("Applied").getBoolean(key);}
    public static boolean canApply(LivingEntity entity,JsonObject output){
        if(!(entity instanceof Mob mob)||!output.has("kind")||!output.get("kind").getAsString().equals("augmentation")||!output.has("id"))return false;
        String key=output.get("id").getAsString();return TYPES.contains(key)&&!has(entity,key)&&(!key.equals("instilled_loyalty")||!mob.goalSelector.getAvailableGoals().isEmpty());
    }
    public static boolean apply(LivingEntity entity,VatInfusionRecipe recipe,UUID owner){
        var output=recipe.output();if(!canApply(entity,output))return false;String key=output.get("id").getAsString();
        if(key.equals("instilled_loyalty")&&owner==null)return false;
        var data=entity.getPersistentData().getCompound(DATA);var applied=data.getCompound("Applied");applied.putBoolean(key,true);data.put("Applied",applied);
        var costs=data.getCompound("CloneCosts");recipe.aspects.forEach((aspect,count)->costs.putInt(aspect,Math.min(32763,Math.max(0,costs.getInt(aspect))+count)));data.put("CloneCosts",costs);
        if(key.equals("instilled_loyalty")){data.putUUID("Owner",owner);data.putBoolean("Sitting",false);}
        entity.getPersistentData().put(DATA,data);((Mob)entity).setPersistenceRequired();refresh(entity);return true;
    }
    private static void attribute(LivingEntity entity,String key,Attribute type,double amount){
        var instance=entity.getAttribute(type);if(instance==null)return;
        UUID id=UUID.nameUUIDFromBytes((Horizons.ID+":"+key).getBytes(StandardCharsets.UTF_8));
        if(instance.getModifier(id)==null)instance.addPermanentModifier(new AttributeModifier(id,key,amount,AttributeModifier.Operation.MULTIPLY_BASE));
    }
    public static void refresh(LivingEntity entity){
        if(has(entity,"quick_limbs")){attribute(entity,"quick_limbs",Attributes.MOVEMENT_SPEED,.3);effect(entity,MobEffects.JUMP);}
        if(has(entity,"thaumic_claws"))attribute(entity,"thaumic_claws",Attributes.ATTACK_DAMAGE,2);
        if(has(entity,"awake_blood"))effect(entity,MobEffects.REGENERATION);
        if(has(entity,"diamond_skin"))effect(entity,MobEffects.DAMAGE_RESISTANCE);
        if(has(entity,"eldritch_fangs"))attribute(entity,"eldritch_fangs",Attributes.ATTACK_DAMAGE,1);
        if(entity instanceof Mob mob&&has(entity,"instilled_loyalty"))LoyalCreatureGoals.install(mob);
    }
    private static void effect(LivingEntity entity,MobEffect effect){
        var old=entity.getEffect(effect);if(old!=null&&(old.getAmplifier()>0||old.isInfiniteDuration()))return;
        var value=new MobEffectInstance(effect,-1,0,true,false);value.setCurativeItems(List.of());entity.addEffect(value);
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event){var entity=event.getEntity();if(!entity.level().isClientSide&&!(entity instanceof net.minecraft.world.entity.player.Player)&&entity.tickCount%20==0)refresh(entity);}
    @SubscribeEvent public static void joined(net.minecraftforge.event.entity.EntityJoinLevelEvent event){if(!event.getLevel().isClientSide&&event.getEntity() instanceof Mob mob)refresh(mob);}
}
