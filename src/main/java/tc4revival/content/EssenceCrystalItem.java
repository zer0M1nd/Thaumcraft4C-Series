package tc4revival.content;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.aspect.*;

public  class EssenceCrystalItem extends Item implements StoredAspectItem {
    private final java.util.Random random=new java.util.Random();
    public EssenceCrystalItem() { super(new Properties()); }
    public ItemStack of(Aspect aspect,int count) { var stack=new ItemStack(this,count); setStoredAspects(stack,new AspectAmounts().add(aspect,1)); return stack; }
    private void initialize(ItemStack stack) {
        if (stack.hasTag()) return;
        var aspects=TCAspects.catalog().all(); if (!aspects.isEmpty()) setStoredAspects(stack,new AspectAmounts().add(aspects.get(random.nextInt(aspects.size())),1));
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected) { if (!level.isClientSide) initialize(stack); }
    @Override public void onCraftedBy(ItemStack stack,Level level,Player player) { initialize(stack); }
    public int tint(ItemStack stack) {
        var aspects=storedAspects(stack); if (!aspects.isEmpty()) return aspects.aspects().get(0).color();
        var all=TCAspects.catalog().all(); return all.isEmpty()?0xffffff:all.get(Math.floorMod(System.currentTimeMillis()/500,all.size())).color();
    }
}
