package tbmodern;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraft.world.effect.*;
import tbmodern.api.RevolverUpgrades;
import tc4revival.content.*;

public final class RevolverBullet extends Projectile {
    private static final net.minecraft.network.syncher.EntityDataAccessor<ItemStack> WEAPON = net.minecraft.network.syncher.SynchedEntityData.defineId(RevolverBullet.class,net.minecraft.network.syncher.EntityDataSerializers.ITEM_STACK);
    private ItemStack weapon = ItemStack.EMPTY;
    private int life;
    public RevolverBullet(EntityType<? extends RevolverBullet> type, Level level) { super(type,level); }
    public RevolverBullet(Level level, Player shooter, ItemStack weapon) {
        this(Content.REVOLVER_BULLET.get(),level); setOwner(shooter); this.weapon=weapon.copy();entityData.set(WEAPON,this.weapon);
        setPos(shooter.getX(),shooter.getEyeY()-.1,shooter.getZ());
        shootFromRotation(shooter,shooter.getXRot(),shooter.getYRot(),0,(float)(4.5*RevolverUpgrades.speedMultiplier(weapon)),1);
    }
    @Override protected void defineSynchedData() {entityData.define(WEAPON,ItemStack.EMPTY);}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag);tag.put("Weapon",weapon.save(new CompoundTag()));tag.putInt("Life",life); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag);weapon=ItemStack.of(tag.getCompound("Weapon"));life=Math.max(0,tag.getInt("Life"));entityData.set(WEAPON,weapon); }
    private int upgrade(String name) { return RevolverUpgrades.level(entityData.get(WEAPON),name); }
    @Override public void tick() {
        super.tick();
        boolean primal=upgrade("primal")>0;
        boolean noclip=primal;
        Player shooter=getOwner() instanceof Player player?player:null;
        for(var active:RevolverUpgrades.behaviors(entityData.get(WEAPON)))noclip|=active.behavior().passesBlocks(shooter,entityData.get(WEAPON),active.level());
        int steps=primal?100:1;
        for(int step=0;step<steps && !isRemoved();step++) {
            if(++life>200) {discard();break;}
            Vec3 start=position(), velocity=getDeltaMovement(), end=start.add(velocity);
            var block=level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
            Vec3 entityEnd=!noclip && block.getType()!=HitResult.Type.MISS?block.getLocation():end;
            var entity=ProjectileUtil.getEntityHitResult(level(),this,start,entityEnd,getBoundingBox().expandTowards(velocity).inflate(1),e->e instanceof LivingEntity && e!=getOwner() && e.isAlive() && !e.isSpectator());
            if(!level().isClientSide && entity!=null) hit((LivingEntity)entity.getEntity());
            if(!level().isClientSide && !noclip && block.getType()!=HitResult.Type.MISS && level().getBlockState(block.getBlockPos()).isSolidRender(level(),block.getBlockPos()))discard();
            setPos(end);setDeltaMovement(velocity.scale(.99).add(0,primal?0:-.01,0));
            if(level().isClientSide){
                thaumcraft.api.fx.LegacyEffects.sparkle(level(),position(),0xee3333,1,1,0);
                thaumcraft.api.fx.LegacyEffects.sparkle(level(),position().subtract(velocity.scale(.05)),0xee3333,1,1,0);
            }
            if(primal)life++; // The old recursive update advanced its lifetime twice per movement step.
        }
    }
    private void hit(LivingEntity victim) {
        if(victim instanceof Player target && getOwner() instanceof Player shooter && !shooter.canHarmPlayer(target))return;
        float damage=14;
        damage*=Math.pow(1.1F,upgrade("power"));
        if(RevolverUpgrades.silverVulnerable(victim))damage*=Math.pow(1.2F,upgrade("silver"));
        if(victim.getMobType()==MobType.ARTHROPOD)damage*=Math.pow(1.2F,upgrade("bane_of_arthropods"));
        if(victim instanceof EldritchMob || victim instanceof net.minecraft.world.entity.monster.EnderMan)damage*=Math.pow(1.2F,upgrade("bane_of_eldritch"));
        if(victim instanceof Player)damage*=Math.pow(1.2F,upgrade("dueling"));
        if(upgrade("eldritch")>0) {
            boolean boss=victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || victim instanceof net.minecraft.world.entity.boss.wither.WitherBoss || victim instanceof EldritchWardenEntity || victim instanceof EldritchGolemEntity || victim.getType().is(net.minecraft.tags.TagKey.create(Registries.ENTITY_TYPE,new ResourceLocation("forge","bosses")));
            damage*=boss?Math.pow(1.3F,upgrade("eldritch")):1/1.2F;
        }
        if(upgrade("piercing")>0)damage*=1.1F;
        if(upgrade("tainted")>0)damage*=victim instanceof TaintedMob?1/1.6F:Math.pow(1.2F,upgrade("tainted"));
        Player shooter=getOwner() instanceof Player player?player:null;
        var behaviors=RevolverUpgrades.behaviors(weapon);
        for(var active:behaviors)damage=active.behavior().damageStart(victim,shooter,weapon,active.level(),damage);
        if(upgrade("primal")>0)damage*=2;
        for(var active:behaviors)damage=active.behavior().damageEnd(victim,shooter,weapon,active.level(),damage);
        if(!Float.isFinite(damage) || damage<0)damage=0;
        var key=ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("thaumicbases","revolver"));
        var source=new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key),this,getOwner());
        boolean accepted=victim.hurt(source,damage);
        boolean destroy=upgrade("piercing")==0;
        if(accepted) {
            if(upgrade("tainted")>0 && random.nextDouble()<=.1)victim.addEffect(new MobEffectInstance(TCEffects.FLUX_TAINT.get(),200,1,true,true));
            if(upgrade("poisoned")>0)victim.addEffect(new MobEffectInstance(MobEffects.POISON,200,1,true,true));
            if(upgrade("knowledge")>0 && !victim.isAlive() && getOwner() instanceof ServerPlayer player && random.nextInt(Math.max(1,7-upgrade("knowledge")))==0) {
                tc4revival.aspect.entity.EntityAspects.resolve(victim).ifPresent(result->{for(var aspect:result.aspects().aspects()) {
                    thaumcraft.api.ThaumcraftApiHelper.addResearchPoints(player,thaumcraft.api.aspects.Aspect.getAspect(aspect.tag()),(short)1); if(random.nextBoolean())break;
                }});
            }
            for(var active:behaviors)destroy &= active.behavior().afterHit(victim,shooter,weapon,active.level(),damage);
        }
        if(destroy)discard();
    }
}
