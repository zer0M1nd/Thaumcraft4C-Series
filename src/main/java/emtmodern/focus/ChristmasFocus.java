package emtmodern.focus;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import tc4revival.aspect.*;
import tc4revival.content.*;
public class ChristmasFocus extends ChargeFocus {
    @Override public int color(ItemStack stack){return 65535;}
    @Override public String sortingPrefix(){return "CHRISTMAS";}
    @Override public java.util.Map<Aspect,Integer> costs(ItemStack stack){return java.util.Map.of(TCAspects.catalog().require("ordo"),500,TCAspects.catalog().require("aer"),500);}
    @Override public java.util.List<FocusUpgrade> upgradesForRank(ItemStack stack,int rank){return java.util.List.of();}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        if(hit!=null&&hit.getType()==HitResult.Type.BLOCK){if(!level.isClientSide&&stack.getItem() instanceof CastingWandItem wand&&(player.getAbilities().instabuild||wand.consumeForPlayer(stack,player,costs(stack),false,true,true))){var snowman=EntityType.SNOW_GOLEM.create(level);if(snowman!=null){var pos=hit.getBlockPos().above();snowman.setPos(pos.getX(),pos.getY(),pos.getZ());level.addFreshEntity(snowman);}}player.swing(hand);}
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
