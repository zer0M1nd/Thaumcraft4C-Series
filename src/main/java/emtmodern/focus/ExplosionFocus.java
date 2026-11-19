package emtmodern.focus;
import emtmodern.ElectroMagicTools;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.*;
import tc4revival.content.*;
public class ExplosionFocus extends ChargeFocus {
    @Override public int color(ItemStack stack){return 0x8b0000;}
    @Override public String sortingPrefix(){return "EXPLOSION";}
    @Override public java.util.Map<Aspect,Integer> costs(ItemStack stack){return java.util.Map.of(TCAspects.catalog().require("ignis"),200,TCAspects.catalog().require("perditio"),200);}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        if(!level.isClientSide&&stack.getItem() instanceof CastingWandItem wand&&(player.getAbilities().instabuild||wand.consumeForPlayer(stack,player,costs(stack),false,true,true))){var laser=new LaserEntity(ElectroMagicTools.LASER_ENTITY.get(),level);laser.fire(player);laser.strength=1+wand.focusPotency(stack,player)*.5f;level.addFreshEntity(laser);}
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
