package thaumicomniverse.compat.create;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thaumcraft.api.wands.ItemFocusBasic;

public class RelocationFocusItem extends ItemFocusBasic {
    @Override public int color(ItemStack focus){return 0x9266CF;}
    @Override public String sortingPrefix(){return "RELOCATION";}
    @Override public String ornamentTexture(){return "thaumicomniverse:item/focus_relocation_orn";}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){
        return Map.of(TCAspects.catalog().require("aer"),2,TCAspects.catalog().require("terra"),2,
                TCAspects.catalog().require("ordo"),2);
    }
    @Override public boolean costPerTick(ItemStack focus){return true;}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){
        if(rank==1 && upgradeLevel(focus,FocusUpgrade.ARCHITECT.id)==0)
            return List.of(FocusUpgrade.FRUGAL,FocusUpgrade.ARCHITECT);
        return upgradeLevel(focus,FocusUpgrade.ARCHITECT.id)>0?List.of(FocusUpgrade.FRUGAL,FocusUpgrade.ENLARGE)
                :List.of(FocusUpgrade.FRUGAL);
    }
    public static int boxSize(ItemStack focus) {
        return 8+8*((RelocationFocusItem)focus.getItem()).upgradeLevel(focus,FocusUpgrade.ENLARGE.id);
    }
    public static boolean architect(ItemStack focus) {
        return focus.getItem() instanceof RelocationFocusItem item && item.upgradeLevel(focus,FocusUpgrade.ARCHITECT.id)>0;
    }
    public static boolean physics(ItemStack focus){
        return focus.getItem() instanceof RelocationFocusItem item
                &&item.upgradeLevel(focus,FocusUpgrade.require("thaumicomniverse:relocation_physics").id)>0;
    }
    @Override public boolean swing(ItemStack wand,LivingEntity entity){return true;}
    @Override public boolean startBreak(ItemStack wand,BlockPos pos,Player player){return true;}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack wand,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        if(!ArcaneContraptions.enabled())return InteractionResultHolder.fail(wand);
        if(!level.isClientSide && !RelocationSessions.begin(player,hand,wand))return InteractionResultHolder.fail(wand);
        player.startUsingItem(hand);return InteractionResultHolder.consume(wand);
    }
    @Override public void usingTick(ItemStack wand,Player player,int remaining){
        if(!player.level().isClientSide&&!RelocationSessions.charge(player,wand))player.stopUsingItem();
    }
    @Override public void release(ItemStack wand,Level level,Player player,int remaining){
        if(level.isClientSide)return;
        // Vanilla also releases when opening a screen. The dedicated C2S message
        // distinguishes a physical mouse release from that cancellation.
        if(player instanceof net.minecraftforge.common.util.FakePlayer)RelocationSessions.releaseByPlayer(player);
        else RelocationSessions.awaitReleasePacket(player);
    }
}
