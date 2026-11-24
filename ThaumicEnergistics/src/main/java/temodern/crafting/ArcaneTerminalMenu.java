package temodern.crafting;

import appeng.api.inventories.InternalInventory;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.slot.CraftingTermSlot;
import appeng.helpers.InventoryAction;
import appeng.util.inv.CarriedItemInventory;
import appeng.util.inv.PlayerInternalInventory;
import appeng.menu.slot.AppEngSlot;
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
public class ArcaneTerminalMenu extends CraftingTermMenu {
    public static final SlotSemantic WAND=SlotSemantics.register("thaumicenergistics_wand",false);
    public static final SlotSemantic ARMOR=SlotSemantics.register("thaumicenergistics_armor",false);
    public static final SlotSemantic WIRELESS_UPGRADES=SlotSemantics.UPGRADE;
    public static final SlotSemantic SINGULARITY=SlotSemantics.register("thaumicenergistics_singularity",false);
    private static final EquipmentSlot[] ARMOR_TYPES={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private final WorkHost machine;
    private final CraftingTermSlot resultSlot;
    @GuiSync(200) public String visCosts="";
    @GuiSync(201) public boolean useInventoryWands;
    @GuiSync(202) public boolean canPayVis;

    public ArcaneTerminalMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, MenuLocators.readFromPacket(data));
    }
    private ArcaneTerminalMenu(int id, Inventory inventory, appeng.menu.locator.MenuLocator locator) {
        this(id, inventory, locator.locate(inventory.player, WorkHost.class), locator);
    }
    public ArcaneTerminalMenu(int id,Inventory inventory,WorkHost host){
        this(id, inventory, host, host.locator());
    }
    private ArcaneTerminalMenu(int id, Inventory inventory, WorkHost host, appeng.menu.locator.MenuLocator locator) {
        super(WorkMenu.TERMINAL.get(), id, inventory, host, true);
        machine = host;
        setLocator(locator);
        InternalInventory slots=new InternalInventory(){
            public int size(){return host.workstation().inventory.getContainerSize();}
            public ItemStack getStackInSlot(int slot){return host.workstation().inventory.getItem(slot);}
            public void setItemDirect(int slot,ItemStack stack){host.workstation().inventory.setItem(slot,stack);}
            public boolean isItemValid(int slot,ItemStack stack){
                if(slot==9)return stack.getItem() instanceof tc4revival.content.CastingWandItem wand&&!wand.isStaff(stack);
                return slot>=18&&slot<22&&stack.canEquip(ARMOR_TYPES[slot-18],inventory.player);
            }
            public int getSlotLimit(int slot){return slot==9||slot>=18?1:64;}
            public void sendChangeNotification(int slot){host.workstation().inventory.setChanged();}
        };
        resultSlot=(CraftingTermSlot)getSlots(SlotSemantics.CRAFTING_RESULT).get(0);
        addSlot(new AppEngSlot(slots,9),WAND);
        if(!wireless())for(int i=0;i<4;i++)addSlot(new AppEngSlot(slots,18+i),ARMOR);
        if(host instanceof PortableArcaneHost wireless){
            // MEStorageMenu already exposes the host's upgrades in AE2's native side panel.
            var singularity=new AppEngSlot(wireless.singularity(),0){
                @Override public boolean mayPlace(ItemStack stack){return stack.is(appeng.core.definitions.AEItems.QUANTUM_ENTANGLED_SINGULARITY.asItem());}
                @Override public int getMaxStackSize(){return 1;}
            };
            singularity.setEmptyTooltip(()->java.util.List.of(net.minecraft.network.chat.Component.translatable("gui.thaumicenergistics.quantum_singularity")));
            addSlot(singularity,SINGULARITY);
        }
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
    public static void open(ServerPlayer player, WorkHost machine) { open(player, machine, machine.locator()); }
    public static void open(ServerPlayer player, WorkHost machine, appeng.menu.locator.MenuLocator locator) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inv, p) -> new ArcaneTerminalMenu(id, inv, machine, locator),
                machine.getMainMenuIcon().getHoverName()), buf -> MenuLocators.writeToPacket(buf, locator));
    }
    public ItemStack wand(){return machine.workstation().inventory.getItem(9);}
    public boolean wireless(){return machine instanceof PortableArcaneHost;}
    public boolean usesItem(ItemStack stack){return machine instanceof PortableArcaneHost host&&host.getItemStack()==stack;}
    @Override public appeng.api.networking.IGridNode getNetworkNode(){return machine==null?super.getNetworkNode():machine.getActionableNode();}
    public void inventoryWands(boolean enabled){
        if(isClientSide()){sendClientAction("te_inventory_wands",enabled);return;}
        if(stillValid(getPlayer())&&machine instanceof PortableArcaneHost host){host.inventoryWands(enabled);broadcastChanges();}
    }
    public AppEngSlot resultSlot(){return resultSlot;}
    private void updatePreview(){
        if(machine==null||isClientSide())return;
        super.slotsChanged(machine.workstation().inventory);
        useInventoryWands=machine.inventoryWands();
        // AE2 updates ordinary recipes locally when their matrix changes. Avoid a
        // research snapshot and a full arcane recipe search on every storage sync.
        if(getCurrentRecipe()!=null || getCraftingMatrix().isEmpty()){
            visCosts="";canPayVis=getCurrentRecipe()!=null && machine.active();
            if(!canPayVis)resultSlot.setDisplayedCraftingOutput(ItemStack.EMPTY);
            else if(!resultSlot.hasItem())resultSlot.setDisplayedCraftingOutput(getCurrentRecipe().assemble(machine.workstation().grid(),machine.workLevel().registryAccess()));
            return;
        }
        var preview=machine.workstation().manualPreview((ServerPlayer)getPlayer());
        if(!ItemStack.matches(resultSlot.getItem(),preview.output()))resultSlot.setDisplayedCraftingOutput(preview.output());
        canPayVis=!preview.output().isEmpty();
        visCosts=preview.costs().entrySet().stream().map(e->e.getKey().tag()+":"+e.getValue()).collect(java.util.stream.Collectors.joining(";"));
    }
    @Override public void broadcastChanges(){updatePreview();super.broadcastChanges();}
    @Override public void doAction(ServerPlayer player,InventoryAction action,int slot,long id){
        if(slot!=resultSlot.index){super.doAction(player,action,slot,id);return;}
        if(!stillValid(player)||!machine.active())return;
        updatePreview();
        var expected=resultSlot.getItem().copy();
        if(expected.isEmpty())return;
        if(getCurrentRecipe()!=null && !expected.is(tc4revival.content.TCItems.ADVANCED_ALCHEMICAL_CONSTRUCT.get())
                && !(expected.is(tc4revival.content.TCItems.JAR_LABEL.get())&&expected.hasTag())){
            super.doAction(player,action,slot,id);
            return;
        }
        var inventoryTarget=action==InventoryAction.CRAFT_SHIFT || action==InventoryAction.CRAFT_ALL;
        InternalInventory target=inventoryTarget?new PlayerInternalInventory(player.getInventory()):new CarriedItemInventory(this);
        int perStack=expected.getMaxStackSize()/expected.getCount();
        int crafts=switch(action){
            case CRAFT_ITEM -> 1;
            case CRAFT_STACK,CRAFT_SHIFT -> perStack;
            case CRAFT_ALL -> expected.getMaxStackSize()*Inventory.INVENTORY_SIZE/expected.getCount();
            default -> 0;
        };
        for(int i=0;i<crafts;i++){
            if(!machine.workstation().takeManualResult(player,expected,
                    output->target.simulateAdd(output).isEmpty(),
                    output->{var extra=target.addItems(output);if(!extra.isEmpty())player.drop(extra,false);}))break;
        }
        broadcastChanges();
    }
    public void takeResult(String mode){
        var action=switch(mode){
            case "one" -> InventoryAction.CRAFT_ITEM;
            case "stack" -> InventoryAction.CRAFT_STACK;
            case "inventory" -> InventoryAction.CRAFT_SHIFT;
            default -> null;
        };
        if(action!=null && getPlayer() instanceof ServerPlayer player)doAction(player,action,resultSlot.index,0);
    }
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        if(resultSlot!=null && slot==resultSlot.index){
            if(type==ClickType.PICKUP)takeResult(button==1?"stack":"one");
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
        fillCraftingGrid(ingredients,false);
    }
    public void fillCraftingGrid(java.util.List<net.minecraft.world.item.crafting.Ingredient> ingredients,boolean craftMissing){
        if(isClientSide()||ingredients.size()!=9||!stillValid(getPlayer())||!machine.active())return;
        var network=machine.grid();if(network==null || storage==null)return;
        var inv=machine.workstation().inventory;var player=getPlayer();
        boolean stored=false;
        for(int slot=0;slot<9;slot++){
            var current=inv.getItem(slot);
            if(current.isEmpty() || ingredients.get(slot).test(current))continue;
            var rest=current.copy();
            long moved=appeng.api.storage.StorageHelper.poweredInsert(powerSource,storage,appeng.api.stacks.AEItemKey.of(rest),rest.getCount(),getActionSource());
            stored|=moved>0;
            rest.shrink((int)moved);
            player.getInventory().add(rest);
            inv.setItem(slot,rest);
        }
        if(stored)network.getStorageService().invalidateCache();
        var available=network.getStorageService().getCachedInventory();
        var filter=appeng.items.storage.ViewCellItem.createItemFilter(getViewCells());
        var requests=new java.util.LinkedHashMap<appeng.api.stacks.AEItemKey,java.util.List<Integer>>();
        for(int slot=0;slot<9;slot++){
            var needed=ingredients.get(slot);
            if(!inv.getItem(slot).isEmpty()||needed.isEmpty())continue;
            var options=new java.util.LinkedHashSet<appeng.api.stacks.AEItemKey>();
            for(var sample:needed.getItems()){
                if(!getCraftingMatrix().isItemValid(slot,sample))continue;
                for(var entry:available.findFuzzy(appeng.api.stacks.AEItemKey.of(sample),appeng.api.config.FuzzyMode.IGNORE_ALL)){
                    if(entry.getLongValue()>0 && entry.getKey() instanceof appeng.api.stacks.AEItemKey key
                            && key.matches(needed) && (filter==null || filter.isListed(key)))options.add(key);
                }
            }
            for(var key:options){
                if(appeng.api.storage.StorageHelper.poweredExtraction(powerSource,storage,key,1,getActionSource())==1){
                    inv.setItem(slot,key.toStack());break;
                }
            }
            if(!inv.getItem(slot).isEmpty())continue;
            for(int i=0;i<Inventory.INVENTORY_SIZE;i++){
                var item=player.getInventory().getItem(i);
                if(!isPlayerInventorySlotLocked(i) && getCraftingMatrix().isItemValid(slot,item) && needed.test(item)){
                    inv.setItem(slot,player.getInventory().removeItem(i,1));break;
                }
            }
            if(craftMissing && inv.getItem(slot).isEmpty())for(var sample:needed.getItems()){
                if(!getCraftingMatrix().isItemValid(slot,sample))continue;
                var key=network.getCraftingService().getFuzzyCraftable(appeng.api.stacks.AEItemKey.of(sample),
                        k->k instanceof appeng.api.stacks.AEItemKey item && item.matches(needed));
                if(key instanceof appeng.api.stacks.AEItemKey item){
                    requests.computeIfAbsent(item,ignored->new java.util.ArrayList<>()).add(slot);break;
                }
            }
        }
        if(wand().isEmpty())for(int i=0;i<player.getInventory().getContainerSize();i++){
            var stack=player.getInventory().getItem(i);
            if(stack.getItem() instanceof tc4revival.content.CastingWandItem wand&&!wand.isStaff(stack)){
                inv.setItem(9,player.getInventory().removeItem(i,1));break;
            }
        }
        player.getInventory().setChanged();broadcastChanges();
        if(!requests.isEmpty()){
            network.getStorageService().invalidateCache();
            startAutoCrafting(requests.entrySet().stream().map(e->new AutoCraftEntry(e.getKey(),e.getValue())).toList());
        }
    }
}
