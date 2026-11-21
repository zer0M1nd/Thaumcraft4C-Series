package thaumicomniverse.compat.create;

import com.simibubi.create.content.equipment.armor.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.*;

public class VoidBacktank extends BacktankItem implements RepairableItem,RunicGear,WarpingGear {
    public VoidBacktank(){super(((ArmorItem)TCItems.voidArmor(VoidEquipment.Armor.CHESTPLATE)).getMaterial(),new Item.Properties().rarity(Rarity.UNCOMMON),ResourceLocation.parse("thaumicomniverse:void_backtank"),ArcaneCreateContent.BACKTANK_PLACEABLE::get);}
    @Override public boolean canBeDepleted(){return true;}
    @Override public int warp(ItemStack stack,Player player){return 1;}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean held){if(ArcaneContraptions.enabled()&&!level.isClientSide&&!(entity instanceof Player p&&p.getItemBySlot(EquipmentSlot.CHEST)==stack))VoidEquipment.repair(stack,entity);}
    @Override public void onArmorTick(ItemStack stack,Level level,Player player){if(ArcaneContraptions.enabled()&&!level.isClientSide)VoidEquipment.repair(stack,player);}
    public static class Block extends BacktankBlock {
        public Block(){super(Properties.copy(Blocks.IRON_BLOCK).strength(3).noOcclusion());}
        @Override public BlockEntityType<? extends BacktankBlockEntity> getBlockEntityType(){return ArcaneCreateContent.BACKTANK_TILE.get();}
    }
    public static class Tile extends BacktankBlockEntity {
        public Tile(BlockPos pos,BlockState state){super(ArcaneCreateContent.BACKTANK_TILE.get(),pos,state);}
        @Override public net.minecraft.network.chat.Component getName(){return getCustomName()!=null?getCustomName():net.minecraft.network.chat.Component.translatable("item.thaumicomniverse.void_backtank");}
    }
}
