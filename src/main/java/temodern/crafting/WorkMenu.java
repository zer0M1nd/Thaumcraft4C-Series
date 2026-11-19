package temodern.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.*;
import temodern.TE;
import temodern.machine.NetworkMachine;

public final class WorkMenu extends AbstractContainerMenu {
    public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,TE.ID);
    public static final RegistryObject<MenuType<WorkMenu>> TYPE=MENUS.register("workstation",()->IForgeMenuType.create(WorkMenu::new));
    public static final RegistryObject<MenuType<ArcaneTerminalMenu>> TERMINAL=MENUS.register("arcane_terminal",()->IForgeMenuType.create(ArcaneTerminalMenu::new));
    public static final RegistryObject<MenuType<ArcaneAssemblerMenu>> ASSEMBLER=MENUS.register("arcane_assembler",()->IForgeMenuType.create(ArcaneAssemblerMenu::new));
    public static final RegistryObject<MenuType<InscriberMenu>> INSCRIBER=MENUS.register("knowledge_inscriber",()->IForgeMenuType.create(InscriberMenu::new));
    public static final RegistryObject<MenuType<GeneratorMenu>> GENERATOR=MENUS.register("vibration_chamber",()->IForgeMenuType.create(GeneratorMenu::new));
    private final BlockPos pos;
    public final Container inventory;
    public final String kind;
    private final int machineSlots;
    private final int[] storedVis=new int[6];
    public int storedVis(int index){return storedVis[index];}
    public WorkMenu(int id,Inventory player,FriendlyByteBuf buffer){this(id,player,new SimpleContainer(23),buffer.readBlockPos());}
    public WorkMenu(int id,Inventory player,Container inventory,BlockPos pos) {
        super(TYPE.get(),id);this.pos=pos;this.inventory=inventory;
        kind=player.player.level().getBlockEntity(pos) instanceof NetworkMachine machine?machine.kind():"";
        var primals=thaumcraft.api.aspects.AspectSelection.select(tc4revival.aspect.TCAspects.catalog(),thaumcraft.api.aspects.AspectSelection.LEGACY_SIX).stream().toList();
        for(int i=0;i<6;i++){final int n=i;addDataSlot(new DataSlot(){public int get(){return player.player.level().isClientSide?storedVis[n]:player.player.level().getBlockEntity(pos) instanceof NetworkMachine machine?machine.storedVis(primals.get(n)):0;}public void set(int value){storedVis[n]=value;}});}
        if(kind.equals("arcane_assembler")){
            addSlot(new Slot(inventory,10,187,8){public boolean mayPlace(ItemStack stack){return AutomationPatterns.accepted(stack);}public int getMaxStackSize(){return 1;}});
            for(int i=0;i<4;i++){final int armorIndex=i;addSlot(new Slot(inventory,18+i,210,26+i*18){public boolean mayPlace(ItemStack stack){return stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor&&armor.getEquipmentSlot()==net.minecraft.world.entity.EquipmentSlot.byName(new String[]{"head","chest","legs","feet"}[armorIndex]);}public int getMaxStackSize(){return 1;}});}
            addSlot(new Slot(inventory,22,187,26){public boolean mayPlace(ItemStack stack){return stack.is(appeng.core.definitions.AEItems.SPEED_CARD.asItem());}public int getMaxStackSize(){return 4;}});
        }else{
            addSlot(new Slot(inventory,0,26,30){public boolean mayPlace(ItemStack stack){return false;}public boolean mayPickup(Player player){return false;}});
            addSlot(new Slot(inventory,11,80,30){public boolean mayPlace(ItemStack stack){return stack.is(appeng.core.definitions.AEItems.BLANK_PATTERN.asItem());}});
        }
        for(int i=0;i<6;i++)addSlot(new Slot(inventory,12+i,187+i%3*18,136+i/3*18){public boolean mayPlace(ItemStack stack){return false;}});
        machineSlots=slots.size();
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(player,9+row*9+col,8+col*18,115+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(player,col,8+col*18,173));
    }
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        if(kind.equals("distillation_encoder")&&slot==0){
            if(stillValid(player)&&(type==ClickType.PICKUP||type==ClickType.QUICK_MOVE)){inventory.setItem(0,type==ClickType.QUICK_MOVE||button==1?ItemStack.EMPTY:getCarried().copyWithCount(getCarried().isEmpty()?0:1));broadcastChanges();}return;
        }
        super.clicked(slot,button,type,player);
    }
    @Override public boolean stillValid(Player player){return player.distanceToSqr(pos.getCenter())<=64&&player.level().getBlockEntity(pos) instanceof NetworkMachine machine&&machine.kind().equals(kind);}
    @Override public boolean clickMenuButton(Player player,int button) {
        if(!(player instanceof ServerPlayer server)||!stillValid(player)||!(player.level().getBlockEntity(pos) instanceof NetworkMachine machine)||!kind.equals("distillation_encoder")||button!=2)return false;
        boolean success=machine.workstation.encode(server,true);
        if(!success)player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.thaumicenergistics.action_failed"),true);
        broadcastChanges();return success;
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(!stillValid(player)||index<0||index>=slots.size()||kind.equals("distillation_encoder")&&index==0)return ItemStack.EMPTY;
        var slot=slots.get(index);var stack=slot.getItem();if(stack.isEmpty())return ItemStack.EMPTY;var copy=stack.copy();
        if(index<machineSlots){if(!moveItemStackTo(stack,machineSlots,slots.size(),true))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,kind.equals("distillation_encoder")?1:0,kind.equals("distillation_encoder")?2:6,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}
