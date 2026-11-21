package emtmodern.client;

import emtmodern.item.ElectricChainsaw;
import ic2.core.ref.Ic2SoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Original three chainsaws loop at the local player's hand, independent of their remaining EU. */
@Mod.EventBusSubscriber(modid="emt",value=Dist.CLIENT)
public class ChainsawAudio {
    private static Idle idle;
    private static Item lastItem;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();var player=mc.player;
        if(player==null){if(idle!=null)mc.getSoundManager().stop(idle);idle=null;lastItem=null;return;}
        Item item=player.getMainHandItem().getItem();
        if(idle!=null&&(item!=lastItem||idle.player!=player)){mc.getSoundManager().stop(idle);idle=null;mc.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(Ic2SoundEvents.ITEM_CHAINSAW_STOP,1,1));}
        if(item instanceof ElectricChainsaw&&idle==null){idle=new Idle(player);mc.getSoundManager().play(idle);}
        lastItem=item;
    }
    public static class Idle extends AbstractTickableSoundInstance {
        public final Player player;
        public Idle(Player player){super(Ic2SoundEvents.ITEM_CHAINSAW_IDLE,SoundSource.PLAYERS,RandomSource.create());this.player=player;looping=true;delay=0;volume=1;tick();}
        @Override public void tick(){if(player.isRemoved()||!(player.getMainHandItem().getItem() instanceof ElectricChainsaw)){stop();return;}x=player.getX();y=player.getEyeY();z=player.getZ();}
    }
}
