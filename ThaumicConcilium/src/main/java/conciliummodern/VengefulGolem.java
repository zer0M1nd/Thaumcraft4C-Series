package conciliummodern;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.content.TCItems;

public final class VengefulGolem extends Monster implements RangedAttackMob {
    public VengefulGolem(EntityType<? extends VengefulGolem> type,Level level){super(type,level);setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(TCItems.GOLEM_BELL.get()));setDropChance(EquipmentSlot.MAINHAND,0);}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.FOLLOW_RANGE,32).add(Attributes.MOVEMENT_SPEED,.5).add(Attributes.ATTACK_DAMAGE,5);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new RangedAttackGoal(this,.5,20,40,24));goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(this,.4));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,Thaumaturge.class,true));
    }
    @Override public void performRangedAttack(LivingEntity target,float strength){
        if(target instanceof ServerPlayer player){
            var helmet=player.getItemBySlot(EquipmentSlot.HEAD);var key=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(helmet.getItem());
            boolean muffed=helmet.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,ResourceLocation.parse("thaumicconcilium:earmuffs")))||key!=null&&key.getNamespace().equals("witchery")&&key.getPath().equals("earmuffs");
            if(!muffed&&random.nextInt(10)>=2&&getSensing().hasLineOfSight(player)){
                swing(InteractionHand.MAIN_HAND);player.level().playSound(null,player.blockPosition(),SoundEvents.EXPERIENCE_ORB_PICKUP,SoundSource.HOSTILE,.7f,1+random.nextFloat()*.1f);
                if(random.nextInt(10)>5)player.sendSystemMessage(Component.translatable("tc.golem.taunt."+random.nextInt(4)).withStyle(ChatFormatting.DARK_PURPLE));
                CrimsonControl.enslave(player,this,60);
            }else CrimsonControl.release(player,this);
        }else if(target instanceof Mob mob&&random.nextFloat()>.1f)mob.getNavigation().moveTo(target.getX()+(random.nextDouble()-.5)*20,target.getY()+(random.nextDouble()-.5)*2,target.getZ()+(random.nextDouble()-.5)*20,.7);
    }
    @Override protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source,int looting,boolean recentlyHit){super.dropCustomDeathLoot(source,looting,recentlyHit);int amount=random.nextInt(2)+looting;if(amount>0)spawnAtLocation(new ItemStack(TCItems.KNOWLEDGE_FRAGMENT.get(),amount),1.5f);}
    @Override protected SoundEvent getAmbientSound(){return ConciliumGolems.GNAW.get();}
    @Override protected SoundEvent getDeathSound(){return ConciliumGolems.GNAW.get();}
    @Override public boolean canPickUpLoot(){return false;}
}
