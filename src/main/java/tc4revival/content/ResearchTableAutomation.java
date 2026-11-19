package tc4revival.content;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import tc4revival.aspect.TCAspects;
import tc4revival.config.TCConfig;
import tc4revival.research.*;

/** All optional table actions are validated and committed on the server thread. */
public  class ResearchTableAutomation {
    private ResearchTableAutomation() {}
    public static boolean unlocked(ServerPlayer player) {
        return ResearchTableAccess.server(player).refill();
    }
    public static boolean learn(ServerPlayer player,ResearchTableBlockEntity table) {
        var stack=table.items.getStackInSlot(1);
        if(!ResearchNotesItem.learn(player,stack))return false;
        table.items.setStackInSlot(1,stack.isEmpty()?ItemStack.EMPTY:stack);return true;
    }
    public static void refill(ServerPlayer player,ResearchTableBlockEntity table,ResearchTableMenu menu) {
        if(table.items.getStackInSlot(1).isEmpty())for(var stack:player.getInventory().items) {
            if(!stack.is(TCItems.RESEARCH_NOTES.get()))continue;
            var note=ResearchNoteNbt.read(stack.getTag(),TCAspects.catalog());if(!note.problems().isEmpty()||note.note().isEmpty()||note.note().get().complete())continue;
            table.items.setStackInSlot(1,stack.split(1));player.getInventory().setChanged();break;
        }
        var tools=table.items.getStackInSlot(0);
        if(ScribingToolsItem.ink(tools)>0)return;
        if(tools.isEmpty()) {
            for(var stack:player.getInventory().items)if(ScribingToolsItem.ink(stack)>0) { table.items.setStackInSlot(0,stack.split(1));player.getInventory().setChanged();return; }
            return;
        }
        if(!(tools.getItem() instanceof ScribingToolsItem))return;
        var found=player.serverLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("thaumcraft:refill_scribing_tools"));
        if(found.isEmpty()||found.get().getType()!=RecipeType.CRAFTING)return;
        @SuppressWarnings("unchecked") var recipe=(Recipe<net.minecraft.world.inventory.CraftingContainer>)(Recipe<?>)found.get();
        var grid=new TransientCraftingContainer(menu,3,3);grid.setItem(0,tools.copy());
        for(var ingredient:player.getInventory().items) {
            if(ingredient.isEmpty())continue;var one=ingredient.copy();one.setCount(1);grid.setItem(1,one);
            if(!recipe.matches(grid,player.level()))continue;var result=recipe.assemble(grid,player.level().registryAccess());
            if(ScribingToolsItem.ink(result)<=0||result.getCount()!=1)continue;
            var remainders=recipe.getRemainingItems(grid);ingredient.shrink(1);table.items.setStackInSlot(0,result);
            for(var remainder:remainders)if(!remainder.isEmpty()&&!player.getInventory().add(remainder))player.drop(remainder,false);
            player.getInventory().setChanged();return;
        }
    }
    public static boolean random(ServerPlayer player,ResearchTableBlockEntity table) {
        if(!ResearchTableAccess.server(player).random()||!table.valid(player))return false;
        boolean paper=false,ink=false;var excluded=new HashSet<String>();
        for(var stack:player.getInventory().items) {
            paper|=stack.is(Items.PAPER);ink|=ScribingToolsItem.ink(stack)>0;
            if(stack.getItem() instanceof ResearchNotesItem)ResearchNoteNbt.read(stack.getTag(),TCAspects.catalog()).note().ifPresent(note->excluded.add(note.key()));
        }
        ResearchNoteNbt.read(table.items.getStackInSlot(1).getTag(),TCAspects.catalog()).note().ifPresent(note->excluded.add(note.key()));
        if(!paper||!ink)return false;
        var knowledge=KnowledgeSavedData.get(player.server).snapshot(player.getUUID()).orElseThrow();
        var rules=ResearchCatalogs.rules();var actions=new ResearchBookActions(TCAspects.catalog(),ResearchCatalogs.catalog(),ResearchCatalogs::enabled);int difficulty=TCConfig.SERVER.researchDifficulty.get();
        var choices=ResearchCatalogs.catalog().all().stream().filter(ResearchCatalogs::enabled)
                .filter(entry->!entry.has(ResearchDefinition.Flag.HIDDEN)&&!entry.has(ResearchDefinition.Flag.LOST))
                .filter(entry->!excluded.contains(entry.key())&&!rules.completed(knowledge,entry.key())&&rules.prerequisites(knowledge,entry.key())&&actions.visible(knowledge,entry))
                .filter(entry->ResearchBookActions.route(entry,difficulty)==ResearchBookActions.Route.NOTE).toList();
        if(choices.isEmpty())return false;
        var selected=choices.get(player.getRandom().nextInt(choices.size()));
        var result=ResearchBookInventory.requestNote(player,knowledge,selected.key(),difficulty);
        if(result.status()!=ResearchBookInventory.Status.CREATED)return false;
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tc.research.popup",net.minecraft.network.chat.Component.translatable("tc.research_name."+selected.key())),true);return true;
    }
}
