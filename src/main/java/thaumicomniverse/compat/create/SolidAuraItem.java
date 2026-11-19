package thaumicomniverse.compat.create;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.aspect.TCAspects;
import tc4revival.content.AspectOrbEntity;

public class SolidAuraItem extends Item {
    public static final int VIS=6;
    public SolidAuraItem(){super(new Properties().rarity(Rarity.UNCOMMON));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!thaumicomniverse.RevivalFeatures.Group.AURA.enabled())return InteractionResultHolder.pass(stack);
        if(!level.isClientSide){
            var primals=TCAspects.catalog().primals();
            for(int i=0;i<VIS;i++)level.addFreshEntity(new AspectOrbEntity(level,player.getX(),player.getY()+.5,player.getZ(),primals.get(level.random.nextInt(primals.size())),1));
            if(!player.getAbilities().instabuild)stack.shrink(1);player.getCooldowns().addCooldown(this,5);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
