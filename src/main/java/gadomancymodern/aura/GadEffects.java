package gadomancymodern.aura;
import gadomancymodern.Gadomancy;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraftforge.registries.*;

public class GadEffects {
    public static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,Gadomancy.MOD_ID);
    public static final RegistryObject<MobEffect> LUCK=EFFECTS.register("mining_luck",()->new AspectEffect("lucrum","potion.luck"));
    public static final RegistryObject<MobEffect> ELDRITCH=EFFECTS.register("eldritch",()->new AspectEffect("alienis","potion.eldritch"));
    public static final RegistryObject<MobEffect> VIS_AFFINITY=EFFECTS.register("vis_affinity",()->new AspectEffect("praecantatio","potion.visdiscount"));
    public static final RegistryObject<MobEffect> GOLEM=EFFECTS.register("golem_buff",GolemBuff::new);
    public static final RegistryObject<MobEffect> ACHROMATIC=EFFECTS.register("achromatic",Achromatic::new);
    public static class AspectEffect extends MobEffect {
        public final String aspect,translation;
        public AspectEffect(String aspect,String translation){super(MobEffectCategory.BENEFICIAL,tc4revival.aspect.AspectDefinitions.bundled().require(aspect).color());this.aspect=aspect;this.translation=translation;}
        @Override public String getDescriptionId(){return translation;}
        // Forge invokes this from MobEffect's constructor, before aspect is assigned.
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientMobEffectExtensions> consumer){consumer.accept(new gadomancymodern.client.AspectEffectIcon(()->aspect));}
    }
    public static class Achromatic extends AspectEffect {
        public Achromatic(){super("vitreus","potion.achromatic");}
        @Override public boolean isDurationEffectTick(int duration,int amplifier){return true;}
        @Override public void applyEffectTick(LivingEntity entity,int amplifier){if(entity.level().isClientSide)return;var bounds=new net.minecraft.world.phys.AABB(entity.getX()-4,entity.getY()-4,entity.getZ()-4,entity.getX()+3,entity.getY()+3,entity.getZ()+3);for(var projectile:entity.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class,bounds,other->other instanceof net.minecraft.world.entity.projectile.AbstractArrow||other instanceof net.minecraft.world.entity.projectile.ThrowableProjectile)){var displacement=projectile.getDeltaMovement().normalize().scale(projectile.distanceTo(entity)*2);projectile.setPos(projectile.position().add(displacement));projectile.hasImpulse=true;}}
    }
    public static class GolemBuff extends AspectEffect {
        protected static final java.util.Map<Attribute,AttributeModifier> MODIFIERS=java.util.Map.of(Attributes.MOVEMENT_SPEED,new AttributeModifier(java.util.UUID.fromString("9f76335f-2952-46e9-b91a-c555ca983ada"),"POTION_GOLEM_SPEED",.4,AttributeModifier.Operation.MULTIPLY_BASE),Attributes.MAX_HEALTH,new AttributeModifier(java.util.UUID.fromString("fafcbdc2-9c24-45a1-b832-4b6c4a5df9c7"),"POTION_GOLEM_HEALTH",.5,AttributeModifier.Operation.MULTIPLY_BASE),Attributes.ATTACK_DAMAGE,new AttributeModifier(java.util.UUID.fromString("6e562261-dccf-4421-ac6f-c742989c7dfb"),"POTION_GOLEM_DAMAGE",1,AttributeModifier.Operation.MULTIPLY_BASE));
        public GolemBuff(){super("fabrico","potion.buffGolem");}
        @Override public void addAttributeModifiers(LivingEntity entity,AttributeMap attributes,int amplifier){super.addAttributeModifiers(entity,attributes,amplifier);if(entity instanceof tc4revival.content.GolemEntity)MODIFIERS.forEach((type,modifier)->{var attribute=attributes.getInstance(type);if(attribute!=null){attribute.removeModifier(modifier);attribute.addPermanentModifier(modifier);}});}
        @Override public void removeAttributeModifiers(LivingEntity entity,AttributeMap attributes,int amplifier){super.removeAttributeModifiers(entity,attributes,amplifier);if(entity instanceof tc4revival.content.GolemEntity)MODIFIERS.forEach((type,modifier)->{var attribute=attributes.getInstance(type);if(attribute!=null)attribute.removeModifier(modifier);});}
    }
}
