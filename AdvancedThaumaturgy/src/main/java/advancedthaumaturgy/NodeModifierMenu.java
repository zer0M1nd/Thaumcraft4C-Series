package advancedthaumaturgy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public class NodeModifierMenu extends AbstractContainerMenu {
    public final NodeModifierEntity entity;
    public NodeModifierMenu(int id,Inventory inventory,FriendlyByteBuf data){this(id,inventory,(NodeModifierEntity)inventory.player.level().getBlockEntity(data.readBlockPos()));}
    public NodeModifierMenu(int id,Inventory inventory,NodeModifierEntity entity){super(AdvancedThaumaturgy.NODE_MENU.get(),id);this.entity=entity;}
    @Override public boolean stillValid(Player player){return entity!=null&&!entity.isRemoved()&&player.level()==entity.getLevel()&&player.distanceToSqr(entity.getBlockPos().getCenter())<=64;}
    @Override public boolean clickMenuButton(Player player,int id){if(!stillValid(player)||player.level().isClientSide)return false;return NodeOperation.byId(id).map(op->entity.start(op,player)).orElse(false);}
    @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
}
