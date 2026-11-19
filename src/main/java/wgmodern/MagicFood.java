package wgmodern;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tc4revival.aspect.TCAspects;
import tc4revival.research.KnowledgeSavedData;

/** Original food values and 1–3 independent brain-jerky research-point rolls. */
public class MagicFood extends Item {
    public final String kind;
    public MagicFood(String kind,int nutrition,float saturation){super(new Properties().food(new FoodProperties.Builder().nutrition(nutrition).saturationMod(saturation).build()));this.kind=kind;}
    @Override public String getDescriptionId(){return "item.WG_MagicFood."+kind;}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity eater){
        if(kind.equals("brainjerky")&&eater instanceof ServerPlayer player){
            var store=KnowledgeSavedData.get(player.getServer());
            store.update(player.getUUID(),player.getRandom()::nextInt,knowledge->{
                int rolls=1+player.getRandom().nextInt(3);
                var primals=TCAspects.catalog().primals();
                for(int i=0;i<rolls;i++)knowledge.changePool(primals.get(player.getRandom().nextInt(primals.size())),(short)(1+player.getRandom().nextInt(2)));
                return true;
            });
        }
        return super.finishUsingItem(stack,level,eater);
    }
}
