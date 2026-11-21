package emtmodern.item;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public class BrokenThorHammer extends ThorHammer {
    public static final double[][] BOMB_OFFSETS={{8,0,-8},{-8,0,8},{-8,0,-8},{8,0,8},{0,4,0},{0,8,0}};
    public BrokenThorHammer(){super(1000);}
    @Override protected float followupDamage(){return 2;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);player.swing(hand);
        if(!level.isClientSide){for(var offset:BOMB_OFFSETS){var alumentum=new tc4revival.content.AlumentumEntity(tc4revival.content.TCEntities.ALUMENTUM.get(),level);alumentum.setPos(player.getX()+offset[0],player.getY()+offset[1],player.getZ()+offset[2]);level.addFreshEntity(alumentum);}if(!player.getAbilities().instabuild)stack.hurtAndBreak(20,player,p->p.broadcastBreakEvent(hand));}
        return InteractionResultHolder.success(stack);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags){
        lines.add(Component.translatable("tooltip.EMT.hammer.broken.thor"));lines.add(Component.translatable("tooltip.EMT.hammer.broken.danger"));
        if(net.minecraftforge.fml.loading.FMLEnvironment.dist==net.minecraftforge.api.distmarker.Dist.CLIENT)emtmodern.client.HammerTooltips.broken(lines);
        else lines.add(Component.translatable("tooltip.EMT.hammer.broken.plsNoRightClick"));
    }
}
