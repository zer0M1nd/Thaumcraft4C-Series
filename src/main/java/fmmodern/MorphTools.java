package fmmodern;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.*;

public final class MorphTools {
    private static final ThreadLocal<Boolean> MINING = ThreadLocal.withInitial(()->false);
    public static int phase(ItemStack stack) { return stack.hasTag()?Math.floorMod(stack.getTag().getInt("phase"),3):0; }
    public static void cycle(ItemStack stack) {
        var tag=stack.getOrCreateTag(); int old=phase(stack),next=(old+1)%3;
        if(tag.contains("Enchantments",Tag.TAG_LIST))tag.put("enchants"+old,tag.getList("Enchantments",Tag.TAG_COMPOUND).copy());else tag.remove("enchants"+old);
        var display=tag.getCompound("display");
        if(display.contains("Name",Tag.TAG_STRING))display.putString("Name"+old,display.getString("Name"));else display.remove("Name"+old);
        if(tag.contains("enchants"+next,Tag.TAG_LIST))tag.put("Enchantments",tag.getList("enchants"+next,Tag.TAG_COMPOUND).copy());else tag.remove("Enchantments");
        if(display.contains("Name"+next,Tag.TAG_STRING))display.putString("Name",display.getString("Name"+next));else display.remove("Name");
        if(!display.isEmpty())tag.put("display",display);else tag.remove("display");tag.putInt("phase",next);
    }
    public static InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(!player.isShiftKeyDown()||stack.getMaxDamage()-stack.getDamageValue()<=5)return InteractionResultHolder.pass(stack);
        if(!level.isClientSide){cycle(stack);stack.hurtAndBreak(5,player,p->p.broadcastBreakEvent(hand));level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,net.minecraft.sounds.SoundSource.PLAYERS,.2f,.3f);}
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    public static void tick(ItemStack stack,Level level,Entity entity) {
        if(!level.isClientSide&&entity instanceof LivingEntity&&entity.tickCount%10==0&&stack.isDamaged()&&DarkEnchantments.level("voidtouched",stack)>0)stack.setDamageValue(stack.getDamageValue()-1);
    }
    public static void impact(ItemStack stack,BlockPos center,Player player) {
        if(!(player instanceof ServerPlayer server)||MINING.get()||DarkEnchantments.level("impact",stack)==0)return;
        var level=server.serverLevel();var state=level.getBlockState(center);
        if(!stack.isCorrectToolForDrops(state)||!level.mayInteract(player,center)||!player.mayBuild())return;
        var hit=player.pick(player.getBlockReach(),0,false);if(!(hit instanceof BlockHitResult blockHit)||!blockHit.getBlockPos().equals(center))return;
        // Verify the center's protection event before mining any adjacent blocks.
        if(net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level,server.gameMode.getGameModeForPlayer(),server,center)==-1)return;
        Direction.Axis axis=blockHit.getDirection().getAxis();MINING.set(true);
        try { for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++) {
            if(a==0&&b==0)continue;
            BlockPos target=switch(axis){case X->center.offset(0,a,b);case Y->center.offset(a,0,b);case Z->center.offset(a,b,0);};
            if(stack.isEmpty()||!level.isLoaded(target))continue;
            var adjacent=level.getBlockState(target);
            if(adjacent.getDestroySpeed(level,target)>=0&&stack.isCorrectToolForDrops(adjacent)&&level.mayInteract(player,target))server.gameMode.destroyBlock(target);
        }} finally {MINING.remove();}
    }
    public interface Morph extends IRepairable,IWarpingGear {
        @Override default int getWarp(ItemStack stack,Player player){return DarkEnchantments.level("voidtouched",stack)>0?1:0;}
    }
    public static class Pick extends PickaxeItem implements Morph {
        public Pick(){super(ToolTiers.MORPH,1,-2.8f,new Properties().rarity(Rarity.EPIC));}
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){return MorphTools.use(l,p,h);}
        @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean selected){tick(s,l,e);}
        @Override public boolean onBlockStartBreak(ItemStack s,BlockPos p,Player player){impact(s,p,player);return false;}
        @Override public boolean isValidRepairItem(ItemStack s,ItemStack ingredient){return ingredient.is(Content.item("envy_shard"))||super.isValidRepairItem(s,ingredient);}
    }
    public static class Spade extends ShovelItem implements Morph {
        public Spade(){super(tc4revival.content.ElementalToolTier.INSTANCE,1.5f,-3,new Properties().rarity(Rarity.EPIC));}
        // Shift phase switching excludes vanilla interactions in every phase and stance.
        @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){return InteractionResult.PASS;}
        @Override public boolean canPerformAction(ItemStack stack,net.minecraftforge.common.ToolAction action){return action==net.minecraftforge.common.ToolActions.SHOVEL_DIG;}
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){return MorphTools.use(l,p,h);}
        @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean selected){tick(s,l,e);}
        @Override public boolean onBlockStartBreak(ItemStack s,BlockPos p,Player player){impact(s,p,player);return false;}
        @Override public boolean isValidRepairItem(ItemStack s,ItemStack ingredient){return ingredient.is(Content.item("envy_shard"))||super.isValidRepairItem(s,ingredient);}
    }
    public static class Axe extends AxeItem implements Morph {
        public Axe(){super(tc4revival.content.ElementalToolTier.INSTANCE,6,-3,new Properties().rarity(Rarity.EPIC));}
        @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){return InteractionResult.PASS;}
        @Override public boolean canPerformAction(ItemStack stack,net.minecraftforge.common.ToolAction action){return action==net.minecraftforge.common.ToolActions.AXE_DIG;}
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){return MorphTools.use(l,p,h);}
        @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean selected){tick(s,l,e);}
        @Override public boolean isValidRepairItem(ItemStack s,ItemStack ingredient){return ingredient.is(Content.item("envy_shard"))||super.isValidRepairItem(s,ingredient);}
    }
    public static class Sword extends SwordItem implements Morph {
        public Sword(){super(tc4revival.content.ElementalToolTier.INSTANCE,3,-2.4f,new Properties().rarity(Rarity.EPIC));}
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){return MorphTools.use(l,p,h);}
        @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean selected){tick(s,l,e);}
        @Override public boolean isValidRepairItem(ItemStack s,ItemStack ingredient){return ingredient.is(Content.item("envy_shard"))||super.isValidRepairItem(s,ingredient);}
    }
}
