package thaumicomniverse.compat.create;

import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingEvent;
import tc4revival.content.RepairableItem;

public class MagicHand extends ExtendoGripItem implements RepairableItem {
    private static final java.util.UUID BONUS=java.util.UUID.fromString("d0dce6d3-fd4a-44bc-a904-dc555c6609e7");
    public MagicHand(){super(new Properties());}
    @Override public int getEnchantmentValue(){return 15;}
    @Override public boolean isEnchantable(ItemStack stack){return stack.getCount()==1;}
    public static boolean matches(ItemStack stack){return ArcaneContraptions.enabled()&&stack.getItem() instanceof MagicHand;}
    public static void tick(LivingEvent.LivingTickEvent event){
        var entity=event.getEntity();if(!(entity instanceof net.minecraft.world.entity.player.Player))return;
        var range=entity.getAttribute(ForgeMod.BLOCK_REACH.get());if(range==null)return;
        int count=(matches(entity.getMainHandItem())?1:0)+(matches(entity.getOffhandItem())?1:0);
        var old=range.getModifier(BONUS);double value=count*.5;
        if(old!=null&&old.getAmount()==value)return;
        if(old!=null)range.removeModifier(BONUS);
        if(count>0)range.addTransientModifier(new AttributeModifier(BONUS,"Arcane grip reach",value,AttributeModifier.Operation.ADDITION));
    }
}
