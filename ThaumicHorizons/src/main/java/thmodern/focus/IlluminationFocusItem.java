package thmodern.focus;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thmodern.Horizons;
public final class IlluminationFocusItem extends thaumcraft.api.wands.ItemFocusBasic {
    public static final List<String> COLORS=List.of("black","red","green","brown","blue","purple","cyan","light_gray","gray","pink","lime","yellow","light_blue","magenta","orange","white");
    private static final int[] RGB={1973019,11743532,3887386,5320730,2437522,8073150,2651799,11250603,4408131,14188952,4312372,14602026,6719955,12801229,15435844,15790320};
    public final int shade;
    public IlluminationFocusItem(int shade){this.shade=shade;}
    public static int rgb(int shade){return RGB[Math.max(0,Math.min(15,shade))];}
    @Override public int color(ItemStack focus){return rgb(shade);}
    @Override public String sortingPrefix(){return "I"+shade;}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){return Map.of(TCAspects.catalog().require("ignis"),100,TCAspects.catalog().require("aer"),100);}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){return rank<1||rank>5?List.of():rank==3?List.of(FocusUpgrade.FRUGAL,FocusUpgrade.require("thaumichorizons:solar")):List.of(FocusUpgrade.FRUGAL);}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        if(hit==null||hit.getType()!=HitResult.Type.BLOCK||!(stack.getItem() instanceof CastingWandItem wand))return InteractionResultHolder.pass(stack);
        if(level.isClientSide)return InteractionResultHolder.success(stack);
        var pos=hit.getBlockPos().relative(hit.getDirection());var focus=WandFocusItem.attached(stack);
        if(!(player instanceof ServerPlayer)||player.getItemInHand(hand)!=stack||!player.mayBuild()||player.distanceToSqr(pos.getCenter())>64||!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.isEmptyBlock(pos)||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),stack)||!wand.consumeForPlayer(stack,player,costs(focus),false,false,false))return InteractionResultHolder.fail(stack);
        var snapshot=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos);
        var state=(getUpgradeLevel(focus,"thaumichorizons:solar")>0?Horizons.SOLAR_LIGHT:Horizons.LIGHT).get().defaultBlockState().setValue(LightBlock.SHADE,shade);
        if(!level.setBlock(pos,state,3))return InteractionResultHolder.fail(stack);
        if(net.minecraftforge.event.ForgeEventFactory.onBlockPlace(player,snapshot,hit.getDirection())||player.getItemInHand(hand)!=stack||!level.getBlockState(pos).equals(state)||!wand.consumeForPlayer(stack,player,costs(focus),false,false,true)){snapshot.restore(true,false);return InteractionResultHolder.fail(stack);}
        level.playSound(null,pos,TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.PLAYERS,1,1);player.swing(hand);return InteractionResultHolder.consume(stack);
    }
}
