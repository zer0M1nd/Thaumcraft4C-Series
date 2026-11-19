package temodern.crafting;

import appeng.api.inventories.InternalInventory;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.slot.AppEngSlot;
import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

/** Native ME browsing above the legacy terminal's real crafting grid and result preview. */
public class ArcaneTerminalMenu extends MEStorageMenu {
    public static final SlotSemantic WAND=SlotSemantics.register("thaumicenergistics_wand",false);
    public static final SlotSemantic ARMOR=SlotSemantics.register("thaumicenergistics_armor",false);
    public static final SlotSemantic WIRELESS_UPGRADES=SlotSemantics.UPGRADE;
    public static final SlotSemantic SINGULARITY=SlotSemantics.register("thaumicenergistics_singularity",false);
    private static final EquipmentSlot[] ARMOR_TYPES={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private final WorkHost machine;
    private final AppEngInternalInventory result=new AppEngInternalInventory(1);
    private final AppEngSlot resultSlot;
    @GuiSync(200) public String visCosts="";
    @GuiSync(201) public boolean useInventoryWands;
    @GuiSync(202) public boolean canPayVis;

    public ArcaneTerminalMenu(int id,Inventory inventory,FriendlyByteBuf data){this(id,inventory,MenuLocators.readFromPacket(data).locate(inventory.player,WorkHost.class));}
    public ArcaneTerminalMenu(int id,Inventory inventory,WorkHost host){
        super(WorkMenu.TERMINAL.get(),id,inventory,host);machine=host;setLocator(host.locator());
        InternalInventory slots=new InternalInventory(){
            public int size(){return host.workstation().inventory.getContainerSize();}
            public ItemStack getStackInSlot(int slot){return host.workstation().inventory.getItem(slot);}
            public void setItemDirect(int slot,ItemStack stack){host.workstation().inventory.setItem(slot,stack);}
            public boolean isItemValid(int slot,ItemStack stack){
                if(slot<9)return !stack.is(temodern.TE.MATERIALS.get("wireless_arcane_terminal").get());
                if(slot==9)return stack.getItem() instanceof tc4revival.content.CastingWandItem wand&&!wand.isStaff(stack);
                return slot>=18&&slot<22&&stack.canEquip(ARMOR_TYPES[slot-18],inventory.player);
            }
            public int getSlotLimit(int slot){return slot==9||slot>=18?1:64;}
            public void sendChangeNotification(int slot){host.workstation().inventory.setChanged();}
        };
        for(int i=0;i<9;i++)addSlot(new AppEngSlot(slots,i),SlotSemantics.CRAFTING_GRID);
        addSlot(new AppEngSlot(slots,9),WAND);
        if(!wireless())for(int i=0;i<4;i++)addSlot(new AppEngSlot(slots,18+i),ARMOR);
        if(host instanceof WirelessArcaneHost wireless){
            // MEStorageMenu already exposes the host's upgrades in AE2's native side panel.
            var singularity=new AppEngSlot(wireless.singularity(),0){
                @Override public boolean mayPlace(ItemStack stack){return stack.is(appeng.core.definitions.AEItems.QUANTUM_ENTANGLED_SINGULARITY.asItem());}
                @Override public int getMaxStackSize(){return 1;}
            };
            singularity.setEmptyTooltip(()->java.util.List.of(net.minecraft.network.chat.Component.translatable("gui.thaumicenergistics.quantum_singularity")));
            addSlot(singularity,SINGULARITY);
        }
        resultSlot=new AppEngSlot(result,0){
            @Override public boolean mayPlace(ItemStack stack){return false;}
            @Override public boolean mayPickup(Player player){return false;}
            @Override public boolean isDraggable(){return false;}
        };
        addSlot(resultSlot,SlotSemantics.CRAFTING_RESULT);
        registerClientAction("te_take_result",String.class,this::takeResult);
        registerClientAction("te_clear",Boolean.class,this::clearGrid);
        registerClientAction("te_swap_armor",this::swapArmor);
        registerClientAction("te_inventory_wands",Boolean.class,this::inventoryWands);
        // Return finished crafts held in the six output slots used by previous versions.
        if(!isClientSide())for(int i=12;i<18;i++){
            var old=host.workstation().inventory.removeItemNoUpdate(i);
            if(!old.isEmpty())host.markWorkChanged();
            if(!inventory.add(old)&&!old.isEmpty())inventory.player.drop(old,false);
        }
        updatePreview();
    }
    public static void open(ServerPlayer player,WorkHost machine){NetworkHooks.openScreen(player,new SimpleMenuProvider((id,inv,p)->new ArcaneTerminalMenu(id,inv,machine),machine.getMainMenuIcon().getHoverName()),buf->MenuLocators.writeToPacket(buf,machine.locator()));}
    public ItemStack wand(){return machine.workstation().inventory.getItem(9);}
    public boolean wireless(){return machine instanceof WirelessArcaneHost;}
    public boolean usesItem(ItemStack stack){return machine instanceof WirelessArcaneHost host&&host.getItemStack()==stack;}
    @Override public appeng.api.networking.IGridNode getNetworkNode(){return machine==null?super.getNetworkNode():machine.getActionableNode();}
    public void inventoryWands(boolean enabled){
        if(isClientSide()){sendClientAction("te_inventory_wands",enabled);return;}
        if(stillValid(getPlayer())&&machine instanceof WirelessArcaneHost host){host.inventoryWands(enabled);broadcastChanges();}
    }
    public AppEngSlot resultSlot(){return resultSlot;}
    private void updatePreview(){
        if(machine==null||isClientSide())return;
        useInventoryWands=machine.inventoryWands();
        var preview=machine.workstation().manualPreview((ServerPlayer)getPlayer());
        result.setItemDirect(0,preview.output());
        canPayVis=!preview.output().isEmpty();
        visCosts=preview.costs().entrySet().stream().map(e->e.getKey().tag()+":"+e.getValue()).collect(java.util.stream.Collectors.joining(";"));
    }
    @Override public void broadcastChanges(){updatePreview();super.broadcastChanges();}
    public void takeResult(String mode){
        if(isClientSide()){sendClientAction("te_take_result",mode);return;}
        if(!stillValid(getPlayer())||!java.util.Set.of("one","stack","inventory").contains(mode))return;
        updatePreview();var expected=result.getStackInSlot(0).copy();if(expected.isEmpty())return;
        boolean toInventory=mode.equals("inventory");
        // Shift crafts successive recipes into inventory, including unstackable outputs.
        // Bound a click even when ME storage can continuously refill the crafting grid.
        int times=mode.equals("one")?1:toInventory?64:Math.max(1,expected.getMaxStackSize()/expected.getCount());
        var player=(ServerPlayer)getPlayer();
        for(int i=0;i<times;i++)if(!machine.workstation().takeManualResult(player,expected,
                output->fits(output,toInventory),output->{
                    if(toInventory)player.getInventory().add(output);
                    else if(getCarried().isEmpty())setCarried(output);
                    else getCarried().grow(output.getCount());
                }))break;
        broadcastChanges();
    }
    private boolean fits(ItemStack output,boolean toInventory){
        if(!toInventory)return getCarried().isEmpty()||ItemStack.isSameItemSameTags(getCarried(),output)
                &&getCarried().getCount()+output.getCount()<=output.getMaxStackSize();
        var copy=new net.minecraft.world.SimpleContainer(36);
        for(int i=0;i<36;i++)copy.setItem(i,getPlayerInventory().getItem(i).copy());
        return net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(new net.minecraftforge.items.wrapper.InvWrapper(copy),output.copy(),true).isEmpty();
    }
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        if(resultSlot!=null&&slot==resultSlot.index){
            if(type==ClickType.PICKUP)takeResult("one");
            else if(type==ClickType.QUICK_MOVE)takeResult("inventory");
            return;
        }
        super.clicked(slot,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int slot){
        if(slot==resultSlot.index){takeResult("inventory");return ItemStack.EMPTY;}
        return super.quickMoveStack(player,slot);
    }
    public void swapArmor(){
        if(isClientSide()){sendClientAction("te_swap_armor");return;}
        if(!stillValid(getPlayer())||wireless())return;
        var player=getPlayer();var inv=machine.workstation().inventory;
        for(int i=0;i<4;i++){
            var equipped=player.getItemBySlot(ARMOR_TYPES[i]);var stored=inv.getItem(18+i);
            if(!player.isCreative()&&net.minecraft.world.item.enchantment.EnchantmentHelper.hasBindingCurse(equipped))continue;
            if(!stored.isEmpty()&&!stored.canEquip(ARMOR_TYPES[i],player))continue;
            player.setItemSlot(ARMOR_TYPES[i],stored);inv.setItem(18+i,equipped);
        }
        player.getInventory().setChanged();broadcastChanges();
    }
    public void clearGrid(boolean toPlayer){
        if(isClientSide()){sendClientAction("te_clear",toPlayer);return;}
        if(!stillValid(getPlayer()))return;
        var inv=machine.workstation().inventory;
        for(int i=0;i<9;i++){
            var stack=inv.getItem(i).copy();
            if(!toPlayer&&storage!=null&&!stack.isEmpty()){
                long moved=appeng.api.storage.StorageHelper.poweredInsert(powerSource,storage,appeng.api.stacks.AEItemKey.of(stack),stack.getCount(),getActionSource());
                stack.shrink((int)moved);
            }
            if(toPlayer)getPlayer().getInventory().add(stack);
            inv.setItem(i,stack);
        }
        broadcastChanges();
    }
    @Override public boolean stillValid(Player player){return !machine.removed()&&player.level()==machine.workLevel()&&player.distanceToSqr(machine.workPosition().getCenter())<=64&&super.stillValid(player);}
    /** Transfer actual owned items only; JEI alternatives are never installed as ghost items. */
    public void fillCraftingGrid(java.util.List<net.minecraft.world.item.crafting.Ingredient> ingredients){
        if(isClientSide()||ingredients.size()!=9||!stillValid(getPlayer())||!machine.active())return;
        clearGrid(false);clearGrid(true);
        var inv=machine.workstation().inventory;var player=getPlayer();
        for(int slot=0;slot<9;slot++){
            if(!inv.getItem(slot).isEmpty())continue;
            var needed=ingredients.get(slot);if(needed.isEmpty())continue;
            for(int i=0;i<36;i++){
                var item=player.getInventory().getItem(i);
                if(needed.test(item)&&!item.is(temodern.TE.MATERIALS.get("wireless_arcane_terminal").get())){inv.setItem(slot,player.getInventory().removeItem(i,1));break;}
            }
            if(!inv.getItem(slot).isEmpty()||storage==null)continue;
            for(var entry:storage.getAvailableStacks())if(entry.getKey() instanceof appeng.api.stacks.AEItemKey key&&entry.getLongValue()>0&&needed.test(key.toStack())
                    &&!key.toStack().is(temodern.TE.MATERIALS.get("wireless_arcane_terminal").get())){
                if(appeng.api.storage.StorageHelper.poweredExtraction(powerSource,storage,key,1,getActionSource())==1){inv.setItem(slot,key.toStack());break;}
            }
        }
        if(wand().isEmpty())for(int i=0;i<player.getInventory().getContainerSize();i++){
            var stack=player.getInventory().getItem(i);
            if(stack.getItem() instanceof tc4revival.content.CastingWandItem wand&&!wand.isStaff(stack)){
                inv.setItem(9,player.getInventory().removeItem(i,1));break;
            }
        }
        player.getInventory().setChanged();broadcastChanges();
    }
}
