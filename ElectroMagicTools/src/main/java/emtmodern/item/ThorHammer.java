package emtmodern.item;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** The author inherits the diamond sword attack, then attempts a 12-point follow-up hit. */
public class ThorHammer extends SwordItem implements thaumcraft.api.IRepairable {
    public ThorHammer(){this(2000);}
    protected ThorHammer(int durability){super(Tiers.DIAMOND,3,-2.4f,new Properties().durability(durability));}
    public static void lightning(Level level,double x,double y,double z){var bolt=EntityType.LIGHTNING_BOLT.create(level);if(bolt!=null){bolt.moveTo(x,y,z);level.addFreshEntity(bolt);}}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);player.swing(hand);var hit=LightningSummoner.trace(player);
        if(hit.getType()==HitResult.Type.BLOCK&&!level.isClientSide&&level.mayInteract(player,hit.getBlockPos())){
            var pos=hit.getBlockPos();lightning(level,pos.getX(),pos.getY(),pos.getZ());if(!player.getAbilities().instabuild)stack.hurtAndBreak(20,player,p->p.broadcastBreakEvent(hand));
        }return InteractionResultHolder.success(stack);
    }
    protected float followupDamage(){return 12;}
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){
        if(!attacker.level().isClientSide){target.hurt(attacker instanceof Player player?attacker.damageSources().playerAttack(player):attacker.damageSources().mobAttack(attacker),followupDamage());stack.hurtAndBreak(1,attacker,p->p.broadcastBreakEvent(EquipmentSlot.MAINHAND));}return true;
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags){lines.add(Component.translatable("tooltip.EMT.hammer.broken.The Hammer of Thor"));}
}
