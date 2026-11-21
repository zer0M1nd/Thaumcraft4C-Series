package thaumicomniverse.compat.railcraft;

import mods.railcraft.world.inventory.ManipulatorMenu;
import mods.railcraft.world.level.block.entity.manipulator.ManipulatorBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;

public class EssentiaManipulatorMenu extends ManipulatorMenu<EssentiaManipulator> {
    public EssentiaManipulatorMenu(int id,Inventory inventory,EssentiaManipulator block){super(ArcaneRail.MENU.get(),id,inventory,block);}
    public EssentiaManipulatorMenu(int id,Inventory inventory,FriendlyByteBuf data){this(id,inventory,(EssentiaManipulator)inventory.player.level().getBlockEntity(data.readBlockPos()));}
    @Override public boolean clickMenuButton(Player player,int button){
        if(!stillValid(player)||button<0||button>=ManipulatorBlockEntity.RedstoneMode.values().length)return false;
        getManipulator().setRedstoneMode(ManipulatorBlockEntity.RedstoneMode.values()[button]);getManipulator().setChanged();getManipulator().syncToClient();return true;
    }
}
