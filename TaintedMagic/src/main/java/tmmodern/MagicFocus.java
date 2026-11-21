package tmmodern;

import java.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import tc4revival.aspect.Aspect;
import tc4revival.content.*;

/** Original-author 8.1.1 costs are centivis, including diffusion's 90 percent discount. */
public class MagicFocus extends thaumcraft.api.wands.ItemFocusBasic {
    public final String kind;
    public MagicFocus(String kind){this.kind=kind;}
    public static Aspect aspect(String tag){return tc4revival.aspect.TCAspects.catalog().require(tag);}
    public boolean upgraded(ItemStack focus,String key){return upgradeLevel(focus,FocusUpgrade.require("taintedmagic:"+key).id)>0;}
    @Override public int color(ItemStack focus){return switch(kind){case "taint_swarm"->0x9929BD;case "dark_matter"->0x000018;case "mage_mace"->0xACA9B5;case "shockwave"->0xB0B7C4;case "vis_shard"->0x9929BD;default->0xC9D2F8;};}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){var out=new LinkedHashMap<Aspect,Integer>();
        switch(kind){case "taint_swarm"->{out.put(aspect("terra"),50);out.put(aspect("aqua"),50);if(upgraded(focus,"antibody"))out.put(aspect("ordo"),10);}
        case "dark_matter"->{out.put(aspect("perditio"),150);out.put(aspect("ignis"),100);if(upgraded(focus,"sanity"))out.put(aspect("ordo"),50);if(upgraded(focus,"corrosive"))out.put(aspect("aqua"),50);if(upgraded(focus,"diffusion"))out.replaceAll((a,n)->(int)(n*.1));}
        case "mage_mace"->out.put(aspect("perditio"),10);case "shockwave"->{out.put(aspect("aer"),150);out.put(aspect("perditio"),100);}
        case "vis_shard"->{out.put(aspect("ignis"),10);out.put(aspect("perditio"),10);out.put(aspect("aer"),10);if(upgraded(focus,"persistent"))out.put(aspect("aqua"),10);}
        case "lumos"->{out.put(aspect("aer"),10);out.put(aspect("ignis"),25);}}
        return out;}
    @Override public boolean costPerTick(ItemStack focus){return kind.equals("dark_matter")&&upgraded(focus,"diffusion");}
    @Override public Animation animation(ItemStack focus){return kind.equals("dark_matter")&&upgraded(focus,"diffusion")?Animation.CHARGE:Animation.WAVE;}
    @Override public int cooldownMillis(ItemStack focus){return switch(kind){case "taint_swarm"->3000;case "shockwave"->10000;case "vis_shard"->300;case "mage_mace"->-1;case "dark_matter"->upgraded(focus,"diffusion")?-1:1000;default->1000;};}
    @Override public String sortingPrefix(){return "TM"+kind;}
    @Override public String depthTexture(){return java.util.List.of("taint_swarm","dark_matter","shockwave").contains(kind)?"taintedmagic:items/itemfocus"+kind.replace("_","")+"_depth":null;}
    @Override public String ornamentTexture(){return switch(kind){case "taint_swarm"->"thaumcraft:item/focus_whatever_orn";case "dark_matter"->"thaumcraft:item/focus_warding_orn";case "mage_mace"->"taintedmagic:item/itemfocusmagemace_orn";default->null;};}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){var out=new ArrayList<FocusUpgrade>();out.add(FocusUpgrade.FRUGAL);if(kind.equals("lumos"))return out;if(!kind.equals("lumos"))out.add(FocusUpgrade.POTENCY);if(kind.equals("dark_matter")||kind.equals("shockwave"))out.add(FocusUpgrade.ENLARGE);if(rank==3){if(kind.equals("taint_swarm"))out.add(FocusUpgrade.require("taintedmagic:antibody"));if(kind.equals("dark_matter")){out.add(FocusUpgrade.require("taintedmagic:sanity"));out.add(FocusUpgrade.require("taintedmagic:corrosive"));}}if(rank==5&&kind.equals("vis_shard"))out.add(FocusUpgrade.require("taintedmagic:persistent"));if(rank==5&&kind.equals("dark_matter"))out.add(FocusUpgrade.require("taintedmagic:diffusion"));return out;}
    @Override public boolean canApplyUpgrade(ItemStack focus,Player p,FocusUpgrade upgrade,int rank){if(!upgrade.key.equals("taintedmagic:diffusion")||p.isCreative())return true;if(p instanceof net.minecraft.server.level.ServerPlayer s)return tc4revival.research.KnowledgeSavedData.get(s.server).snapshot(p.getUUID()).map(k->k.researchMarkers().contains("taintedmagic:DIFFUSIONUPGRADE")).orElse(false);return tc4revival.client.ClientKnowledge.snapshot().map(k->k.researchMarkers().contains("taintedmagic:DIFFUSIONUPGRADE")).orElse(false);}
    public static boolean pay(ItemStack wand,Player p,Map<Aspect,Integer> cost){return ((CastingWandItem)wand.getItem()).consumeForPlayer(wand,p,cost,false,false,true);}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack wand,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        ItemStack focus=attached(wand);if(kind.equals("mage_mace"))return InteractionResultHolder.pass(wand);
        if(kind.equals("dark_matter")&&upgraded(focus,"diffusion")){player.startUsingItem(hand);return InteractionResultHolder.consume(wand);}
        if(kind.equals("lumos")&&(hit==null||!level.getBlockState(lumosPosition(level,hit)).canBeReplaced()))return InteractionResultHolder.pass(wand);
        Entity target=FocusTargeting.pointed(player,0,32,1.1,e->e instanceof LivingEntity);if((kind.equals("vis_shard")||kind.equals("taint_swarm"))&&target==null)return InteractionResultHolder.pass(wand);
        if(level.isClientSide)return InteractionResultHolder.success(wand);if(!pay(wand,player,costs(focus)))return InteractionResultHolder.fail(wand);
        float potency=((CastingWandItem)wand.getItem()).focusPotency(wand,player);int enlarge=((CastingWandItem)wand.getItem()).focusUpgrade(wand,FocusUpgrade.ENLARGE);

        switch(kind){case "taint_swarm"->{var type=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:taint_swarm"));var swarm=(TaintSwarmEntity)type.create(level);swarm.setPos(player.getEyePosition().add(player.getLookAngle().scale(.5)));swarm.summoned(true);swarm.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(5+potency);if(target instanceof LivingEntity living)swarm.setTarget(living);level.addFreshEntity(swarm);if(!upgraded(focus,"antibody")&&level.random.nextInt(3)==0)TMRuntime.effect(player,"taint_poison",40,2);}
        case "dark_matter"->{var orb=new DarkMatterProjectile(TaintedMagic.DARK.get(),level);orb.damage=16+potency;orb.radius=1.5f+enlarge*.5f;orb.corrosive=upgraded(focus,"corrosive");launch(orb,player,hand,1,1);level.addFreshEntity(orb);if(!upgraded(focus,"sanity")&&level.random.nextInt(20)==0)TMRuntime.warp(player,1);}
        case "shockwave"->shockwave(player,15+enlarge,2+potency,5+potency,1.5+.1*potency,true);
        case "vis_shard"->{var shard=new HomingShardProjectile(TaintedMagic.SHARD.get(),level);shard.strength=potency;shard.persistent=upgraded(focus,"persistent");shard.target=target==null?null:target.getUUID();shard.targetType=target==null?null:net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(target.getType());launch(shard,player,hand,.5f,0);shard.setPos(player.getEyePosition().add(player.getLookAngle().scale(.5)));float ry=(player.getYRot()+(level.random.nextFloat()-level.random.nextFloat())*60)*(float)Math.PI/180,rp=(player.getXRot()+(level.random.nextFloat()-level.random.nextFloat())*60)*(float)Math.PI/180;shard.setDeltaMovement(-Math.sin(ry)*Math.cos(rp)*.5,-Math.sin(rp)*.5,Math.cos(ry)*Math.cos(rp)*.5);level.addFreshEntity(shard);TMRuntime.sound(level,shard.blockPosition(),"taintedmagic:shard",.3f,1.1f+level.random.nextFloat()*.1f);TMEffects.sparkles(level,shard.position(),18,0,1.75f,.1f);}
        case "lumos"->{var pos=lumosPosition(level,hit);if(level.mayInteract(player,pos)){level.setBlock(pos,TaintedMagic.block("lumos").defaultBlockState(),3);TMRuntime.sound(level,pos,"thaumcraft:ice",.5f,1);TMEffects.sparkles(level,Vec3.atCenterOf(pos),9,6,1.75f,.1f);}}}
        return InteractionResultHolder.success(wand);}
    public static net.minecraft.core.BlockPos lumosPosition(Level level,BlockHitResult hit){return level.getBlockState(hit.getBlockPos()).canBeReplaced()?hit.getBlockPos():hit.getBlockPos().relative(hit.getDirection());}
    @Override public void usingTick(ItemStack wand,Player player,int remaining){if(player.level().isClientSide)return;var focus=attached(wand);if(!kind.equals("dark_matter")||!upgraded(focus,"diffusion"))return;if(!pay(wand,player,costs(focus))){player.stopUsingItem();return;}var casting=(CastingWandItem)wand.getItem();float potency=casting.focusPotency(wand,player);int enlarge=casting.focusUpgrade(wand,FocusUpgrade.ENLARGE);int count=FocusPotency.discrete(2+potency,player.getRandom());for(int i=0;i<count;i++){var orb=new DarkMatterProjectile(TaintedMagic.DIFFUSION.get(),player.level());orb.damage=12+potency;orb.radius=1;orb.corrosive=upgraded(focus,"corrosive");launch(orb,player,player.getUsedItemHand(),1,enlarge>0?12+enlarge:9);player.level().addFreshEntity(orb);}if(player.tickCount%15==0)TMRuntime.sound(player.level(),player.blockPosition(),"thaumcraft:wind",.1f,1);if(!upgraded(focus,"sanity")&&player.getRandom().nextInt(1000)==0)TMRuntime.warp(player,1);}
    public static void launch(net.minecraft.world.entity.projectile.Projectile entity,Player owner,InteractionHand hand,float speed,float scatter){entity.setOwner(owner);float yaw=owner.getYRot()*(float)Math.PI/180;float side=(hand==InteractionHand.MAIN_HAND?owner.getMainArm():owner.getMainArm().getOpposite())==HumanoidArm.RIGHT?-.16f:.16f;entity.setPos(owner.getX()+Math.cos(yaw)*side,owner.getEyeY()-.1,owner.getZ()+Math.sin(yaw)*side);var look=owner.getLookAngle();entity.shoot(look.x,look.y,look.z,speed,scatter);}
    public static void shockwave(Player p,double range,float damage,double force,double rise,boolean closeOnly){for(var e:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(range))){if(e==p||!e.isAlive()||e.isInvulnerable()||!thaumcraft.api.wands.WandActors.canTarget(p,e))continue;if(!closeOnly||p.distanceTo(e)<7)e.hurt(p.damageSources().magic(),damage);var direction=e.position().subtract(p.position()).multiply(1,0,1).normalize();e.push(direction.x*force,rise,direction.z*force);e.hurtMarked=true;TMEffects.lightning(p,e);}TMRuntime.sound(p.level(),p.blockPosition(),"taintedmagic:shockwave",5,1.5f*p.getRandom().nextFloat());}
}
