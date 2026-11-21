package conciliummodern;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import tc4revival.content.CastingWandItem;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class BlusteringLightRing extends Item implements ICurioItem,thaumcraft.api.IRunicArmor,thaumcraft.api.IWarpingGear {
    private static final String[] PRIMALS={"aer","terra","ignis","aqua","ordo","perditio"};
    public BlusteringLightRing(){super(new Properties().stacksTo(1).rarity(Rarity.EPIC));}
    @Override public int getRunicCharge(ItemStack stack){return 50;}
    @Override public int getWarp(ItemStack stack,net.minecraft.world.entity.player.Player player){return 3;}
    @Override public void curioTick(SlotContext context,ItemStack stack){
        if(!(context.entity() instanceof ServerPlayer player) || player.tickCount%3!=0)return;
        var aspect=tc4revival.aspect.TCAspects.catalog().require(PRIMALS[player.getRandom().nextInt(PRIMALS.length)]);
        // Legacy addVis uses whole vis; the modern receive API uses centivis.
        for(int i=0;i<9;i++){
            var wandStack=player.getInventory().getItem(i);
            if(wandStack.getItem() instanceof CastingWandItem wand && wand.receiveVis(wandStack,aspect,100,true)>0){
                wand.receiveVis(wandStack,aspect,100,false);player.getInventory().setChanged();break;
            }
        }
    }
}
