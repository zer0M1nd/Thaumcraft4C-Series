package thaumicomniverse.compat.create;

import java.util.function.ToIntBiFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.*;
import tc4revival.aspect.item.ItemAspects;
import tc4revival.content.*;

/** Repair credit belongs to the device, never to an item that can be split or copied. */
public final class VisItemRepair {
    public static boolean needsRepair(ItemStack stack){return AuraFilling.repairable(stack)&&!costs(stack).isEmpty();}
    private static int tier(ItemStack stack){return stack.getEnchantmentLevel(TCEnchantments.REPAIR.get());}
    private static AspectAmounts costs(ItemStack stack){
        var base=ItemAspects.baseSession().base(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString()).orElse(null);
        return base==null?new AspectAmounts():VisRepair.costs(TCAspects.catalog(),base,tier(stack),stack.getDamageValue());
    }
    public static boolean repair(ItemStack stack,Level level,Player player,CompoundTag buffer,ToIntBiFunction<Aspect,Integer> source){
        if(!(level instanceof ServerLevel server)||level.getGameTime()%40!=0||!needsRepair(stack))return false;
        var costs=costs(stack);boolean ready=true;
        for(var cost:costs.view().entrySet()){
            String key=cost.getKey().tag();int stored=Math.max(0,buffer.getInt(key));
            stored+=source.applyAsInt(cost.getKey(),Math.max(0,cost.getValue()-stored));
            buffer.putInt(key,stored);ready&=stored>=cost.getValue();
        }
        int tier=tier(stack);
        if(!ready||stack.getItem() instanceof RepairableItem item
                &&!item.beforeVisRepair(stack,player==null?FakePlayerFactory.getMinecraft(server):player,tier))return false;
        costs.view().forEach((a,n)->buffer.putInt(a.tag(),buffer.getInt(a.tag())-n));
        stack.setDamageValue(Math.max(0,stack.getDamageValue()-tier));return true;
    }
    private VisItemRepair(){}
}
