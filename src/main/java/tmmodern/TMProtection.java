package tmmodern;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.damagesource.*;
import thaumcraft.api.equipment.ArmorProtectionProviders;

public class TMProtection {
    public static boolean applies(LivingEntity wearer){for(var s:wearer.getArmorSlots())if(s.getItem() instanceof TMArmor.Boots||s.getItem() instanceof TMArmor.Fortress f&&f.shadow)return true;return false;}
    public static void setup(){
        ArmorProtectionProviders.register("taintedmagic:armor",100,
            stack->stack.getItem() instanceof TMArmor.Boots||stack.getItem() instanceof TMArmor.Fortress f&&f.shadow,
            context->{
                var stack=context.stack();var armor=(ArmorItem)stack.getItem();int defense=armor.getDefense();
                int priority=context.magic()||context.elemental()?1:0;double ratio;
                if(armor instanceof TMArmor.Boots)ratio=context.magic()||context.elemental()?defense/80d:context.bypass()?0:defense/90d;
                else ratio=(context.magic()?defense/35d:context.elemental()?defense/20d:context.bypass()?0:defense/25d)
                    *(.75+.15*context.fortressCount()+.05*context.masks());
                return new ArmorProtectionProviders.Protection(priority,ratio,Math.max(0,stack.getMaxDamage()+1-stack.getDamageValue()),context.source().is(DamageTypes.FALL),false);
            });
    }
    /** Compatibility entry point; all equipment still shares one host settlement. */
    public static float absorb(LivingEntity wearer,DamageSource source,float amount){return tc4revival.content.FortressProtection.absorb(wearer,source,amount);}
}
