package emtmodern.focus;
import emtmodern.ElectroMagicTools;
import ic2.api.item.ElectricItem;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.Aspect;
import tc4revival.content.FocusUpgrade;
public class EnergyBallFocus extends ChargeFocus {
    @Override public int color(ItemStack stack){return 255;}
    @Override public String sortingPrefix(){return "ENERGYBALL";}
    @Override public java.util.Map<Aspect,Integer> costs(ItemStack stack){return java.util.Map.of();}
    @Override public java.util.List<FocusUpgrade> upgradesForRank(ItemStack stack,int rank){return java.util.List.of();}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        if(!level.isClientSide){var armor=player.getItemBySlot(EquipmentSlot.CHEST);if(!armor.isEmpty()&&ElectricItem.manager.discharge(armor,5120,4,true,false,false)>=5120){var ball=new EnergyBallEntity(ElectroMagicTools.ENERGY_BALL_ENTITY.get(),level);ball.fire(player);level.addFreshEntity(ball);}}
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
