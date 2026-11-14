package thaumicexploration;

import java.util.UUID;
import com.google.common.collect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class ExplorationCurio extends Item implements ICurioItem,thaumcraft.api.IVisDiscountGear {
    public final String kind,aspect;
    public ExplorationCurio(String kind,String aspect){super(new Properties().stacksTo(1));this.kind=kind;this.aspect=aspect;}
    @Override public int getVisDiscount(ItemStack stack,Player player,thaumcraft.api.aspects.Aspect a){return a!=null&&a.getTag().equals(aspect)?3:0;}
    @Override public Multimap<Attribute,AttributeModifier> getAttributeModifiers(SlotContext context,UUID uuid,ItemStack stack){
        var result=HashMultimap.<Attribute,AttributeModifier>create();
        if(kind.equals("belt"))result.put(Attributes.KNOCKBACK_RESISTANCE,new AttributeModifier(uuid,"Thaumostatic stabilizer",1,AttributeModifier.Operation.ADDITION));
        return result;
    }
}
