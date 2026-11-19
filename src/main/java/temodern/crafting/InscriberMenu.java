package temodern.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import temodern.item.KnowledgeCore;
import temodern.machine.NetworkMachine;

/** The recipe matrix is a ghost inventory: it can neither consume nor produce held ingredients. */
public final class InscriberMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final Container inventory;
    private final Player owner;
    private final Container result=new SimpleContainer(1);
    private final ContainerData state=new SimpleContainerData(2);
    public InscriberMenu(int id,Inventory player,FriendlyByteBuf data){this(id,player,new SimpleContainer(23),data.readBlockPos());}
    public InscriberMenu(int id,Inventory player,Container inventory,BlockPos pos){
        super(WorkMenu.INSCRIBER.get(),id);this.pos=pos;this.inventory=inventory;this.owner=player.player;
        addSlot(new Slot(inventory,10,186,8){public boolean mayPlace(ItemStack stack){return stack.getItem() instanceof KnowledgeCore;}public int getMaxStackSize(){return 1;}});
        for(int i=0;i<9;i++)addSlot(new Slot(inventory,i,26+i%3*18,90+i/3*18){public boolean mayPlace(ItemStack stack){return false;}public boolean mayPickup(Player player){return false;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(player,9+row*9+col,8+col*18,162+row*18));
        for(int i=0;i<9;i++)addSlot(new Slot(player,i,8+i*18,220));
        addSlot(new Slot(result,0,116,108){public boolean mayPlace(ItemStack stack){return false;}public boolean mayPickup(Player player){return false;}});
        addDataSlots(state);
    }
    public ItemStack preview(){return result.getItem(0);}
    public int saveState(){return state.get(0);}
    public int saveRevision(){return state.get(1);}
    private void refreshState(){
        if(!(owner instanceof ServerPlayer server))return;
        var grid=new CraftGrid();for(int i=0;i<9;i++)grid.setItem(i,inventory.getItem(i));
        var recipe=owner.level().getRecipeManager().getAllRecipesFor(tc4revival.content.TCArcane.RECIPE_TYPE.get()).stream()
            .filter(r->r.matches(grid,owner.level())&&r.unlocked(grid,key->thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(server,key))).findFirst().orElse(null);
        var output=recipe==null?ItemStack.EMPTY:recipe.assemble(grid,owner.level().registryAccess());result.setItem(0,output);
        if(!(inventory.getItem(10).getItem() instanceof KnowledgeCore)){state.set(0,0);return;}
        if(output.isEmpty()){state.set(0,1);return;}
        boolean exists=KnowledgeCore.decoded(inventory.getItem(10),owner.level()).stream().anyMatch(p->p.getPrimaryOutput().what().equals(appeng.api.stacks.AEItemKey.of(output)));
        state.set(0,exists?4:patterns().size()>=KnowledgeCore.LIMIT?2:3);
    }
    @Override public void broadcastChanges(){refreshState();super.broadcastChanges();}
    public java.util.List<ItemStack> patterns(){return KnowledgeCore.patterns(inventory.getItem(10));}
    public void setGhostRecipe(java.util.List<ItemStack> inputs){for(int i=0;i<9;i++){var stack=inputs.get(i);inventory.setItem(i,stack.copyWithCount(stack.isEmpty()?0:1));}broadcastChanges();}
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        if(slot==46)return;
        if(slot>=1&&slot<=9){
            if(stillValid(player)&&(type==ClickType.PICKUP||type==ClickType.QUICK_MOVE)){
                inventory.setItem(slot-1,type==ClickType.QUICK_MOVE||button==1?ItemStack.EMPTY:getCarried().copyWithCount(getCarried().isEmpty()?0:1));broadcastChanges();
            }
            return;
        }
        super.clicked(slot,button,type,player);
    }
    @Override public boolean clickMenuButton(Player player,int button){
        if(!(player instanceof ServerPlayer server)||!stillValid(player))return false;
        var machine=(NetworkMachine)player.level().getBlockEntity(pos);
        if(button==22){for(int i=0;i<9;i++)inventory.setItem(i,ItemStack.EMPTY);broadcastChanges();return true;}
        if(button==0){refreshState();int before=saveState();if(before<3)return false;boolean saved=machine.workstation.inscribe(server);if(saved&&before==3)state.set(1,(state.get(1)+1)&32767);broadcastChanges();return saved;}
        var patterns=KnowledgeCore.decoded(inventory.getItem(10),player.level());
        int selected=button-1;if(selected<0||selected>=patterns.size())return false;
        var sample=patterns.get(selected).grid();for(int i=0;i<9;i++)inventory.setItem(i,sample.getItem(i));broadcastChanges();return true;
    }
    @Override public boolean stillValid(Player player){return player.distanceToSqr(pos.getCenter())<=64&&player.level().getBlockEntity(pos) instanceof NetworkMachine machine&&machine.kind().equals("knowledge_inscriber");}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<0||index>=slots.size()||index==46||index>=1&&index<=9)return ItemStack.EMPTY;
        var slot=slots.get(index);var stack=slot.getItem();var copy=stack.copy();
        if(index==0){if(!moveItemStackTo(stack,10,46,true))return ItemStack.EMPTY;}
        else if(stack.getItem() instanceof KnowledgeCore){if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;}
        else return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();return copy;
    }
}
