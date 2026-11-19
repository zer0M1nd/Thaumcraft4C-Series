package tc4revival.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

/** Collects TC4 mining results, then routes each stack through the base's output side. */
public  class ArcaneBoreOutput {
    private ArcaneBoreOutput() {}
    public static List<ItemStack> collect(ServerLevel level,BlockPos target,ServerPlayer worker,ArcaneBoreEquipment equipment) {
        var state=level.getBlockState(target); var results=new ArrayList<ItemStack>();
        if (state.isAir()) return results;
        var tool=new ItemStack(Items.DIAMOND_PICKAXE);
        if (equipment.silk()) tool.enchant(Enchantments.SILK_TOUCH,1);
        else if (equipment.fortune()>0) tool.enchant(Enchantments.BLOCK_FORTUNE,equipment.fortune());
        results.addAll(Block.getDrops(state,level,target,level.getBlockEntity(target),worker,tool));
        for (var dropped:level.getEntitiesOfClass(ItemEntity.class,new AABB(target).inflate(1))) {
            results.add(dropped.getItem().copy()); dropped.discard();
        }
        if (!equipment.silk() && equipment.dowsing()) {
            for (int i=0;i<results.size();i++) results.set(i,SpecialMining.apply(results.get(i),equipment.fortune(),level.random));
        }
        return results;
    }
    public static void deliver(ServerLevel level,BlockPos bore,BlockPos base,Direction output,List<ItemStack> drops) {
        var destination=base.relative(output);
        var entity=level.hasChunkAt(destination)?level.getBlockEntity(destination):null;
        var handler=entity==null?null:entity.getCapability(ForgeCapabilities.ITEM_HANDLER,output.getOpposite()).orElse(null);
        for (var drop:drops) {
            var remainder=handler==null?drop.copy():insert(handler,drop);
            if (remainder.isEmpty()) continue;
            var item=new ItemEntity(level,bore.getX()+.5+output.getStepX()*.66,base.getY()+.5+output.getStepY()*.66,bore.getZ()+.5+output.getStepZ()*.66,remainder);
            item.setDeltaMovement(.075f*output.getStepX(),.025f+.075f*output.getStepY(),.075f*output.getStepZ()); level.addFreshEntity(item);
        }
    }
    private static ItemStack insert(IItemHandler inventory,ItemStack stack) {
        return net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(inventory,stack.copy(),false);
    }
    public static void damagePick(ItemStack pick,ServerLevel level) {
        if (!pick.isDamageableItem()) return;
        if (pick.hurt(1, level.random, null)) { pick.shrink(1); pick.setDamageValue(0); }
    }
}
