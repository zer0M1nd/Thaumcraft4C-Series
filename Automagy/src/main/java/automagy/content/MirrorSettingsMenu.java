package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public class MirrorSettingsMenu extends AbstractContainerMenu {
 private final RequisitionTomeBlockEntity tile;private final Player owner;
 private final net.minecraft.world.SimpleContainer eyes=new net.minecraft.world.SimpleContainer(2);
 private final ContainerData status=new SimpleContainerData(3);
 public MirrorSettingsMenu(int id,Inventory inv,BlockPos pos){super(Content.MIRROR_SETTINGS_MENU.get(),id);owner=inv.player;tile=owner.level().getBlockEntity(pos) instanceof RequisitionTomeBlockEntity t?t:null;
  for(int i=0;i<2;i++){final int eyeSlot=i;addSlot(new Slot(eyes,i,16+i*88,30){public boolean isActive(){return eyeSlot==0||showOutput();}public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){return false;}});}
  for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inv,9+r*9+c,8+c*18,84+r*18));for(int c=0;c<9;c++)addSlot(new Slot(inv,c,8+c*18,142));addDataSlots(status);
 }
 public boolean showOutput(){return status.get(2)!=0;}
 public int status(int i){return status.get(i);}
 public boolean stillValid(Player p){return p==owner&&tile!=null&&!tile.isRemoved()&&tile.getLevel()==p.level()&&p.mayBuild()&&p.level().getBlockEntity(tile.getBlockPos())==tile&&p.distanceToSqr(tile.getBlockPos().getCenter())<=64&&(p.level().isClientSide||(tile.powered()&&tile.core()!=null));}
 public void broadcastChanges(){if(!owner.level().isClientSide&&tile!=null){status.set(2,tile.core()!=null&&tile.core().tracksDestinations()?1:0);for(int i=0;i<2;i++){eyes.setItem(i,tile.mirrorEye(i));status.set(i,tile.mirrorStatus(i));}}super.broadcastChanges();}
 public void clicked(int slot,int button,ClickType type,Player p){if(slot>=0&&slot<2){if(!p.level().isClientSide&&stillValid(p)&&(slot==0||tile.core().tracksDestinations())&&type==ClickType.PICKUP&&(button==0||button==1)){tile.configureEye(slot,button==1?ItemStack.EMPTY:getCarried(),p);broadcastChanges();}return;}super.clicked(slot,button,type,p);}
 public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
}
