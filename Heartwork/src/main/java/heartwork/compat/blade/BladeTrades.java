package heartwork.compat.blade;

import heartwork.Heartwork;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;

public final class BladeTrades {
    public static ItemStack blade(Entity trader,String name){return trader.level().registryAccess().registryOrThrow(SlashBladeDefinition.REGISTRY_KEY).get(BladeCompat.id(name)).getBlade();}
    public static void add(VillagerTradesEvent event){
        event.getTrades().get(1).add((e,r) -> new MerchantOffer(new ItemStack(Items.EMERALD,16),blade(e,"namedblade_basic"),12,2,0.05F));
        event.getTrades().get(1).add((e,r) -> new MerchantOffer(new ItemStack(Items.EMERALD,8),new ItemStack(Heartwork.item("black_feather"),4),12,2,0.05F));
        String[] names={"namedblade","namedblade1","namedblade2","namedblade3","namedblade4","template_re","template_re2"};
        for(int i=0;i<names.length;i++){int n=i;event.getTrades().get(2+i/2).add((e,r) -> {
            var cost=blade(e,names[n]);var tag=new net.minecraft.nbt.CompoundTag();var state=new net.minecraft.nbt.CompoundTag();
            state.putString("translationKey","item.heartwork."+names[n]);tag.put("bladeState",state);cost.setTag(tag);
            return new MerchantOffer(cost,new ItemStack(Items.EMERALD,n<5?10:6),12,10+n,0.05F);
        });}
    }
}
