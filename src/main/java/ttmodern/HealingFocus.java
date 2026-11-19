package ttmodern;

import java.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.content.*;
import tc4revival.aspect.Aspect;

public final class HealingFocus extends TinkererFocus {
    private final Map<Player,Integer> progress=new WeakHashMap<>();
    public HealingFocus(){super("focus_healing","TTHE",0xdf60df);}
    @Override public String ornamentTexture(){return null;}
    @Override public String depthTexture(){return Tinkerer.ID+":item/focus_healing_depth";}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){return cost("terra",45,"aqua",45);}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){return rank>=1&&rank<=5?List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY):List.of();}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){player.startUsingItem(hand);return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);}
    @Override public void usingTick(ItemStack stack,Player player,int remaining){
        if(!(player.level() instanceof net.minecraft.server.level.ServerLevel level)||!player.isAlive()||player.getHealth()>=player.getMaxHealth())return;
        var wand=(CastingWandItem)stack.getItem();var costs=costs(attached(stack));if(!wand.consumeForPlayer(stack,player,costs,false,false,false))return;
        float potency=wand.focusPotency(stack,player);int ticks=progress.getOrDefault(player,0)+1;
        dust(level,player.position().add(0,.5,0),0xdf60df,1,.5);
        if(ticks>=Math.max(1,30-potency*10/3)){
            if(wand.consumeForPlayer(stack,player,costs,false,false,true)){player.heal(1+potency);level.playSound(null,player.blockPosition(),TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.PLAYERS,.5f,1);}ticks=0;
        }
        progress.put(player,ticks);
    }
    @Override public void release(ItemStack stack,Level level,Player player,int remaining){progress.remove(player);}
}
