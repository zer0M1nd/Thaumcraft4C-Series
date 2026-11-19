package ttmodern;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Replace only Ichor's armor contribution, leaving subsequent enchantments and effects intact. */
@Mod.EventBusSubscriber(modid=Tinkerer.ID)
public final class IchorArmorProtection {
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void hurt(LivingHurtEvent event){
        if(event.getEntity().level().isClientSide||event.getAmount()<=0||event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        float ratio=0,points=0;
        for(var stack:event.getEntity().getArmorSlots())if(stack.getItem() instanceof IchorArmorItem armor){ratio+=armor.absorption();points+=armor.getDefense();}
        if(points==0)return;
        float remaining=event.getAmount()*(1-Math.min(.95f,ratio));
        if(event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)){event.setAmount(remaining);return;}
        float total=event.getEntity().getArmorValue(),toughness=(float)event.getEntity().getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float target=CombatRules.getDamageAfterAbsorb(remaining,Math.max(0,total-points),toughness);
        // The modern damage pipeline will apply armor next; invert it to avoid double mitigation.
        float lo=0,hi=Math.max(event.getAmount(),target*5+1);
        for(int i=0;i<32;i++){float mid=(lo+hi)*.5f;if(CombatRules.getDamageAfterAbsorb(mid,total,toughness)<target)lo=mid;else hi=mid;}
        event.setAmount((lo+hi)*.5f);
    }
    private IchorArmorProtection(){}
}
