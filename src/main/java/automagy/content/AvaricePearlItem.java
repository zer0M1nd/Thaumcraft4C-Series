package automagy.content;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.*;

public final class AvaricePearlItem extends Item {
    private final boolean miser;
    public AvaricePearlItem(boolean miser){super(new Properties().stacksTo(16).rarity(miser?Rarity.UNCOMMON:Rarity.COMMON));this.miser=miser;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);
        if(player instanceof ServerPlayer server){
            if(miser&&!player.getAbilities().instabuild&&!ThaumcraftApiHelper.consumeVisFromInventory(server,new AspectList().add(Aspect.ORDER,10)))return InteractionResultHolder.fail(stack);
            var pearl=new AvaricePearlEntity(level,player,miser,hand==InteractionHand.MAIN_HAND?player.getInventory().selected:40);
            pearl.shootFromRotation(player,player.getXRot(),player.getYRot(),0,1.5f,1);
            if(level.addFreshEntity(pearl)&&!player.getAbilities().instabuild)stack.shrink(1);
            level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.SNOWBALL_THROW,net.minecraft.sounds.SoundSource.PLAYERS,.5f,.4f/(level.random.nextFloat()*.4f+.8f));
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
