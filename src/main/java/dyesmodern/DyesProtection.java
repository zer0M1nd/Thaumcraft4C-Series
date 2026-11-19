package dyesmodern;

import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import thaumcraft.api.equipment.ArmorProtectionProviders;

/** Each family contributes only its own slot; mixed sets share the host's armor stage. */
public final class DyesProtection {
    private DyesProtection(){}
    public static boolean applies(LivingEntity wearer){if(!(wearer instanceof Player))return false;for(var stack:wearer.getArmorSlots())if(stack.getItem() instanceof DyesArmorItem item&&item.specialArmor())return true;return false;}
    public static void setup(){
        ArmorProtectionProviders.register("thaumicdyes:armor",100,stack->stack.getItem() instanceof DyesArmorItem item&&item.specialArmor(),context->{
            var stack=context.stack();var dyes=(DyesArmorItem)stack.getItem();int defense=dyes.getDefense();
            int priority=0;double ratio=context.bypass()?0:defense/25d;
            if(dyes.runic()){
                if(!dyes.primal()&&dyes.countUpgrade(stack,5)>0){priority=1;ratio=dyes.upgradeSlots()==2?defense+.04:defense*2/25d;}
            }else if(dyes.fortress()){
                boolean magical=context.magic()&&!dyes.family.equals("IronFortressArmor");priority=magical||context.elemental()?1:0;
                ratio=(magical?defense/35d:context.elemental()?defense/20d:ratio)*context.fortressMultiplier();
            }else if(context.magic()){priority=1;ratio=defense/35d;}
            return new ArmorProtectionProviders.Protection(priority,ratio,Math.max(0,stack.getMaxDamage()+1-stack.getDamageValue()),dyes.runic()||context.source().is(DamageTypes.FALL),false);
        });
    }
    public static float absorb(LivingEntity wearer,DamageSource source,float damage){return tc4revival.content.FortressProtection.absorb(wearer,source,damage);}
}
