package tc4revival.content;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public  class ThaumatoriumMenu extends AbstractContainerMenu {
    private final ThaumatoriumBlockEntity controller;
    private final Container contents;
    private final Player player;
    public ThaumatoriumMenu(int id,Inventory inventory,FriendlyByteBuf buffer) {
        this(id,inventory,clientController(inventory,buffer),new SimpleContainer(1));
    }
    private static ThaumatoriumBlockEntity clientController(Inventory inventory,FriendlyByteBuf buffer) {
        var entity=inventory.player.level().getBlockEntity(buffer.readBlockPos()); return entity instanceof ThaumatoriumBlockEntity core ? core.controller() : null;
    }
    public ThaumatoriumMenu(int id,Inventory inventory,ThaumatoriumBlockEntity controller) { this(id,inventory,controller,controller); }
    private ThaumatoriumMenu(int id,Inventory inventory,ThaumatoriumBlockEntity controller,Container contents) {
        super(TCArcane.THAUMATORIUM_MENU.get(),id); this.player=inventory.player; this.controller=controller; this.contents=contents;
        addSlot(new Slot(contents,0,48,16));
        for (int row=0;row<3;row++) for (int col=0;col<9;col++) addSlot(new Slot(inventory,9+row*9+col,8+col*18,84+row*18));
        for (int col=0;col<9;col++) addSlot(new Slot(inventory,col,8+col*18,142));
    }
    public ThaumatoriumProcess process() { return controller==null ? null : controller.process(); }
    private Set<String> known() {
        if (player instanceof net.minecraft.server.level.ServerPlayer server)
            return tc4revival.research.KnowledgeSavedData.get(server.getServer()).snapshot(server.getUUID()).map(tc4revival.research.PlayerKnowledge::researchMarkers).orElse(Set.of());
        return tc4revival.client.ClientKnowledge.snapshot().map(tc4revival.research.PlayerKnowledge::researchMarkers).orElse(Set.of());
    }
    public List<CrucibleRecipe> choices() {
        var selected=process()==null ? List.<net.minecraft.resources.ResourceLocation>of() : process().selected(); var known=known();
        return player.level().getRecipeManager().getAllRecipesFor(TCArcane.CRUCIBLE_RECIPE_TYPE.get()).stream()
                .filter(recipe->selected.contains(recipe.getId()) || known.contains(recipe.research()) && recipe.catalystMatches(contents.getItem(0)))
                .sorted(Comparator.comparingInt(CrucibleRecipe::legacyOrder).thenComparing(recipe->recipe.getId().toString())).toList();
    }
    @Override public boolean clickMenuButton(Player player,int button) {
        if (player.level().isClientSide || controller==null || !stillValid(player) || player.containerMenu!=this) return false;
        var choices=choices(); if (button<0 || button>=choices.size()) return false;
        return controller.process().toggle(choices.get(button),player.getGameProfile().getName(),controller);
    }
    @Override public boolean stillValid(Player player) { return player.level().isClientSide || controller!=null && controller.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if (index<0 || index>=slots.size()) return ItemStack.EMPTY;
        var slot=slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        var input=slot.getItem(); var before=input.copy();
        if (!moveItemStackTo(input,index==0 ? 1 : 0,index==0 ? 37 : 1,false) || input.getCount()==before.getCount()) return ItemStack.EMPTY;
        if (input.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged(); slot.onTake(player,input); return before;
    }
}
