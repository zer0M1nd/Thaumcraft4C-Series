package gadomancymodern.familiar;

import gadomancymodern.item.EtherealFamiliarItem;
import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import tc4revival.aspect.TCAspects;
import tc4revival.content.*;

/** Server-authoritative original targeting, hundredth-vis costs, and 1/3 debit chance. */
public class FamiliarController {
    private static final Map<Player,FamiliarController> CONTROLLERS=new WeakHashMap<>();
    public int cooldown;
    public static void remove(Player player){CONTROLLERS.remove(player);}
    public static void tick(Player player,ItemStack familiar){CONTROLLERS.computeIfAbsent(player,p->new FamiliarController()).update(player,familiar);}
    public void update(Player player,ItemStack familiar){
        if(cooldown>0){cooldown--;return;}var augments=EtherealFamiliarItem.augments(familiar);
        if(augments.isEmpty()){attack(player,augments);cooldown=32;return;}
        if(!player.getAbilities().instabuild&&!consumeVis(player,FamiliarAugment.costs(augments),false))return;
        if(attack(player,augments)){cooldown=FamiliarAugment.delay(augments);if(!player.getAbilities().instabuild&&player.getRandom().nextInt(3)==0)consumeVis(player,FamiliarAugment.costs(augments),true);}
    }
    public static boolean consumeVis(Player player,Map<String,Integer> raw,boolean commit){
        if(raw.isEmpty())return false;var costs=new LinkedHashMap<tc4revival.aspect.Aspect,Integer>();raw.forEach((tag,n)->costs.put(TCAspects.catalog().require(tag),n));
        for(var stack:WandInventory.wornBaubles(player))if(stack.getItem() instanceof WandInventory.VisAmulet amulet&&amulet.consumeVis(stack,player,costs,commit))return true;
        var inventory=player.getInventory().items;for(int i=inventory.size()-1;i>=0;i--){var stack=inventory.get(i);if(stack.getItem() instanceof CastingWandItem wand&&wand.consumeForPlayer(stack,player,costs,false,true,commit))return true;}return false;
    }
    public static boolean attack(Player player,Map<String,Integer> augments){
        FamiliarAugment element=null;for(String id:augments.keySet()){var candidate=FamiliarAugment.REGISTRY.get(id);if(candidate!=null&&candidate.element){element=candidate;break;}}
        int n=element==null?0:augments.get(element.id);double damage=FamiliarAugment.damage(augments),range=FamiliarAugment.range(augments);int type=6,targets=1;
        if(element==FamiliarAugment.SHOCK){damage+=n*.8;type=1;}else if(element==FamiliarAugment.FIRE){damage+=n*.5;type=4;}else if(element==FamiliarAugment.POISON){range+=n*.6;type=3;}else if(element==FamiliarAugment.WEAKNESS){damage-=n*.3;type=5;if(n==3)targets=3;}
        var p=player.position();var candidates=new ArrayList<>(player.level().getEntitiesOfClass(LivingEntity.class,new AABB(p.x-.5,p.y-.5,p.z-.5,p.x+.5,p.y+.5,p.z+.5).inflate(range),entity->entity.isAlive()&&entity instanceof Enemy&&!(entity instanceof Player)));
        if(candidates.isEmpty())return false;var chosen=new ArrayList<LivingEntity>();if(candidates.size()<=targets)chosen.addAll(candidates);else for(int i=0;i<targets;i++)chosen.add(candidates.remove(player.getRandom().nextInt(candidates.size())));
        for(var target:chosen){
            target.hurt(player.damageSources().magic(),(float)damage);player.level().playSound(null,target.getX()+.5,target.getY()+.5,target.getZ()+.5,net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(new net.minecraft.resources.ResourceLocation("thaumcraft","zap")),net.minecraft.sounds.SoundSource.PLAYERS,.8f,1);
            FamiliarNetwork.bolt(player,target.position(),type);
            if(element==FamiliarAugment.SHOCK&&player.getRandom().nextBoolean()){var motion=target.position().subtract(player.position()).normalize().scale(.4*n);target.setDeltaMovement(target.getDeltaMovement().add(motion.x,Math.abs(motion.y),motion.z));target.hurtMarked=true;}
            if(element==FamiliarAugment.FIRE){target.setSecondsOnFire(14*(1<<Math.min(n,24)));if(n==3)target.addEffect(new MobEffectInstance(TCEffects.SUN_SCORNED.get(),1200,2));}
            if(element==FamiliarAugment.POISON){target.addEffect(new MobEffectInstance(MobEffects.POISON,600,n>1?1:0));if(n==3)target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,2400,3));}
            if(element==FamiliarAugment.WEAKNESS){target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,600,n==3?1:0));target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,600,n==3?1:0));}
        }return true;
    }
}
