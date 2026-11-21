package thmodern.soul;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import tc4revival.content.*;

public final class MirrorAmuletItem extends Item implements top.theillusivec4.curios.api.type.capability.ICurioItem,thaumcraft.api.IRunicArmor {
    public MirrorAmuletItem(){super(new Properties().stacksTo(1).rarity(Rarity.RARE));}
    @Override public int getRunicCharge(ItemStack stack){return 0;}
    @Override public boolean isFoil(ItemStack stack){return ItemMirrorItem.destination(stack)!=null;}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){
        var level=context.getLevel();var player=context.getPlayer();var pos=context.getClickedPos();if(player==null||!(level.getBlockEntity(pos) instanceof ItemMirrorBlockEntity))return InteractionResult.PASS;
        if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),stack))return InteractionResult.FAIL;
        if(!level.isClientSide){ItemMirrorItem.writeDestination(stack.getOrCreateTag(),GlobalPos.of(level.dimension(),pos),level.dimension().location().toString());level.playSound(null,pos,TCSounds.JAR.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,2);player.displayClientMessage(Component.translatable("tc.handmirrorlinked"),false);player.containerMenu.broadcastChanges();}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flags){
        var target=ItemMirrorItem.destination(stack);if(target!=null){var pos=target.pos();lines.add(Component.translatable("tc.handmirrorlinkedto").append(" "+pos.getX()+", "+pos.getY()+", "+pos.getZ()+" — "+target.dimension().location()));}
    }
}
