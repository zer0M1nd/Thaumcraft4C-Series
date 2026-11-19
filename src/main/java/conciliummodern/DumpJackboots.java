package conciliummodern;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

public final class DumpJackboots extends ArmorItem {
    public DumpJackboots(){super(tc4revival.content.SpecialArmorMaterial.INSTANCE,Type.BOOTS,new Properties().rarity(Rarity.RARE));}
    @Override public String getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,String layer){return "thaumicconcilium:textures/models/armor/dump_jackboots.png";}
    @Override public void onArmorTick(ItemStack armor,Level level,Player wearer){
        if(!(wearer instanceof ServerPlayer player) || player.tickCount%2!=0 || !player.isShiftKeyDown() || player.isSpectator())return;
        var held=player.getMainHandItem();if(!(held.getItem() instanceof BlockItem))return;
        var pos=BlockPos.containing(player.getX(),player.getY(),player.getZ()).below();
        if(level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.getBlockState(pos).canBeReplaced() || !level.mayInteract(player,pos) || !player.mayUseItemAt(pos,Direction.UP,held))return;
        var hit=new BlockHitResult(Vec3.atCenterOf(pos).add(0,-.5,0),Direction.UP,pos.below(),false);
        var before=level.getBlockState(pos);
        var result=net.minecraftforge.common.ForgeHooks.onPlaceItemIntoWorld(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
        if(result.consumesAction() && !before.equals(level.getBlockState(pos))){player.setDeltaMovement(0,.5,0);player.fallDistance=0;player.hurtMarked=true;player.getInventory().setChanged();}
    }
}
