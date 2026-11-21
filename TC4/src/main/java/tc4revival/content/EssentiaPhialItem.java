package tc4revival.content;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;

public  class EssentiaPhialItem extends Item implements StoredAspectItem {
    private final boolean filled;
    public EssentiaPhialItem(boolean filled) { super(new Properties()); this.filled=filled; }
    public static ItemStack filled(Aspect aspect) {
        var result=new ItemStack(TCItems.ESSENTIA_PHIAL.get()); TCItems.ESSENTIA_PHIAL.get().setStoredAspects(result,new AspectAmounts().add(aspect,8)); return result;
    }
    public int tint(ItemStack stack) { var stored=storedAspects(stack); return stored.isEmpty() ? 0xffffff : stored.aspects().get(0).color(); }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context) {
        var level=context.getLevel(); var pos=context.getClickedPos(); var player=context.getPlayer();
        var stored=storedAspects(stack);
        var vessel=thaumcraft.api.aspects.AspectSourceHelper.phialVessel(level.getBlockEntity(pos),filled&&stored.size()==1?stored.aspects().get(0):null);
        if (player==null || vessel==null) return InteractionResult.PASS;
        if (!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),stack)) return InteractionResult.FAIL;
        Aspect selected=filled ? stored.size()==1 ? stored.aspects().get(0) : null : vessel.aspect();
        boolean possible=selected!=null && (filled ? vessel.acceptsPhials() && vessel.amount()<=vessel.capacity()-8 && vessel.accepts(selected) && (vessel.amount()==0 || selected.equals(vessel.aspect())) : vessel.contains(selected,8));
        if (!possible) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (filled ? vessel.addToContainer(selected,8)!=0 : !vessel.takeFromContainer(selected,8)) return InteractionResult.PASS;
        stack.shrink(1); var returned=filled ? new ItemStack(TCItems.EMPTY_PHIAL.get()) : filled(selected);
        if (!player.getInventory().add(returned)) level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,returned));
        level.playSound(null,player,net.minecraft.sounds.SoundEvents.GENERIC_SWIM,net.minecraft.sounds.SoundSource.PLAYERS,.25f,1);
        player.containerMenu.broadcastChanges(); return InteractionResult.CONSUME;
    }
}
