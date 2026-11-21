package fmmodern;

import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.*;

/** Same commodities and price ranges, distributed over the five modern villager levels. */
@Mod.EventBusSubscriber(modid=ForbiddenMagic.ID)
public class HereticTrades {
    @SubscribeEvent public static void trades(VillagerTradesEvent event) {
        if(event.getType()!=Content.HERETIC_PROFESSION.get())return;
        var t=event.getTrades();
        t.get(1).add((e,r)->offer(new ItemStack(Items.EMERALD,1+r.nextInt(2)),new ItemStack(Content.item("greed_shard"),3+r.nextInt(2)),7,2));
        t.get(1).add((e,r)->offer(new ItemStack(Content.item("wrath_shard"),4+r.nextInt(6)),new ItemStack(Items.EMERALD),7,2));
        t.get(2).add((e,r)->offer(new ItemStack(Content.GLUTTONY.get(),4+r.nextInt(6)),new ItemStack(Items.EMERALD),7,10));
        t.get(2).add((e,r)->ForbiddenConfig.value(ForbiddenConfig.NO_LUST)?null:offer(new ItemStack(Content.item("lust_shard"),2+r.nextInt(4)),new ItemStack(Items.EMERALD),7,10));
        t.get(3).add((e,r)->offer(new ItemStack(Items.EMERALD,1+r.nextInt(3)),new ItemStack(Items.SKELETON_SKULL),7,10));
        t.get(3).add((e,r)->offer(new ItemStack(Items.EMERALD),new ItemStack(Items.GLOWSTONE_DUST,8+r.nextInt(9)),7,10));
        t.get(4).add((e,r)->offer(new ItemStack(Items.EMERALD,4+r.nextInt(4)),new ItemStack(Items.GHAST_TEAR),7,15));
        t.get(4).add((e,r)->new MerchantOffer(new ItemStack(Items.EMERALD,8+r.nextInt(5)),new ItemStack(Items.STICK),
                TCItems.CASTING_WAND.get().create(WandParts.Rod.require("forbidden_magic:profane"),WandParts.Cap.IRON,false,true),1,15,.05f));
        t.get(5).add((e,r)->new MerchantOffer(new ItemStack(Items.EMERALD,4+r.nextInt(5)),new ItemStack(Items.DIAMOND),
                new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:focus_fire"))),1,30,.05f));
    }
    private static MerchantOffer offer(ItemStack input,ItemStack output,int uses,int xp) {return new MerchantOffer(input,output,uses,xp,.05f);}
}
