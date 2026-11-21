package conciliummodern;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import tc4revival.content.TCEffects;
import tc4revival.research.PlayerKnowledge.WarpKind;

/** A saved, UUID-owned charge; transfer effects only after the recipient accepts them. */
public final class WarpCharge extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER=SynchedEntityData.defineId(WarpCharge.class,EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> FLAGS=SynchedEntityData.defineId(WarpCharge.class,EntityDataSerializers.INT);
    private int power=1,range,age;
    public WarpCharge(EntityType<? extends WarpCharge> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(){entityData.define(OWNER,Optional.empty());entityData.define(FLAGS,0);}
    public boolean ownedBy(Player player){return entityData.get(OWNER).filter(player.getUUID()::equals).isPresent();}
    public Player owner(){return entityData.get(OWNER).map(level()::getPlayerByUUID).orElse(null);}
    public boolean forced(){return (entityData.get(FLAGS)&4)!=0;}
    public boolean hysteria(){return (entityData.get(FLAGS)&1)!=0;}
    public boolean flagellation(){return (entityData.get(FLAGS)&2)!=0;}
    public void initialize(ServerPlayer owner,int power,int range,boolean hysteria,boolean flagellation,boolean forced){
        entityData.set(OWNER,Optional.of(owner.getUUID()));this.power=Math.max(1,power);this.range=Math.max(0,Math.min(5,range));
        entityData.set(FLAGS,(hysteria?1:0)|(flagellation?2:0)|(forced?4:0));
    }
    @Override public boolean isPickable(){return true;}
    @Override public boolean isAttackable(){return false;}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
    @Override public void tick(){
        super.tick();age++;
        if(level().isClientSide){
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->WarpChargeEffects.tick(this));
            return;
        }
        var owner=owner();
        if(!(owner instanceof ServerPlayer player) || !owner.isAlive() || (forced() && age>200)){discard();return;}
        setDeltaMovement(getDeltaMovement().scale(.7));
        if(distanceToSqr(owner)>225)setPos(owner.position());
        if(distanceToSqr(owner)>(forced()?9:49))setDeltaMovement(owner.position().subtract(position()));
        if(!owner.getAbilities().flying)setDeltaMovement(getDeltaMovement().add(0,-.1,0));
        var next=net.minecraft.core.BlockPos.containing(position().add(getDeltaMovement()));
        if(level().hasChunkAt(next) && level().getWorldBorder().isWithinBounds(next))move(MoverType.SELF,getDeltaMovement());
        else setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        if(!forced() && age%120==1)level().playSound(null,blockPosition(),ConciliumSounds.WHISPERS.get(),net.minecraft.sounds.SoundSource.PLAYERS,.1f,1);
        if(age%10==0){
            int distance=5+4*range;
            if(hysteria()){
                var targets=level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(distance),e->e!=owner && e.isAlive() && !e.isAlliedTo(owner));
                Collections.shuffle(targets,new Random(random.nextLong()));
                outer:for(var effect:List.copyOf(owner.getActiveEffects()))if(!effect.getEffect().isBeneficial()){
                    for(var target:targets)if(transfer(owner,target,effect))break outer;
                }
            }
            if(flagellation())for(var nearby:level().getEntitiesOfClass(Player.class,getBoundingBox().inflate(distance),p->p!=owner && p.isAlive()))
                for(var effect:List.copyOf(nearby.getActiveEffects()))if(!effect.getEffect().isBeneficial() && transfer(nearby,owner,effect))break;
        }
        int temporary=ConciliumFocus.warp(player,WarpKind.TEMPORARY);
        if(!forced() && temporary==0){
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("TC.no_warp").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE),true);discard();return;
        }
        if(owner.hasEffect(TCEffects.WARP_WARD.get()))return;
        float damage=ConciliumFocus.warp(player,WarpKind.PERMANENT)/10f*power;
        for(var target:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox(),e->e.isAlive() && (forced() || e!=owner))){
            if(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).equals(net.minecraft.resources.ResourceLocation.parse("thaumicconcilium:crimson_pontifex")))continue;
            if(!forced() && temporary<=0)break;
            if(target.hurt(level().damageSources().indirectMagic(this,owner),damage) && !forced()){
                ConciliumFocus.changeWarp(player,WarpKind.TEMPORARY,-1);temporary--;
            }
        }
    }
    private static boolean transfer(LivingEntity from,LivingEntity to,MobEffectInstance effect){
        var current=to.getEffect(effect.getEffect());
        if(current!=null && (current.getAmplifier()>effect.getAmplifier() || current.getAmplifier()==effect.getAmplifier() && current.getDuration()>=effect.getDuration()))return false;
        if(!to.addEffect(new MobEffectInstance(effect)))return false;
        from.removeEffect(effect.getEffect());return true;
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){entityData.get(OWNER).ifPresent(id->tag.putUUID("Owner",id));tag.putInt("Flags",entityData.get(FLAGS));tag.putInt("Power",power);tag.putInt("Range",range);tag.putInt("Age",age);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){entityData.set(OWNER,tag.hasUUID("Owner")?Optional.of(tag.getUUID("Owner")):Optional.empty());entityData.set(FLAGS,tag.getInt("Flags")&7);power=Math.max(1,Math.min(1000,tag.getInt("Power")));range=Math.max(0,Math.min(5,tag.getInt("Range")));age=Math.max(0,tag.getInt("Age"));}
}
