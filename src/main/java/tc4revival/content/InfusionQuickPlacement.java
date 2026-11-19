package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Places owned materials only. JEI previews never authorize crafting or create items. */
public class InfusionQuickPlacement {
    private static void message(ServerPlayer player,String key) { player.displayClientMessage(Component.translatable("thaumcraft.infusion.place."+key),true); }
    public static void place(ServerPlayer player,ResourceLocation id,ItemStack requestedCenter) {
        if(player==null)return;
        if(player.containerMenu!=player.inventoryMenu)return;
        player.closeContainer();
        if(player.isSpectator()||!player.mayBuild()) {message(player,"denied");return;}
        if(requestedCenter.isEmpty()||requestedCenter.hasTag()&&requestedCenter.getTag().sizeInBytes()>16384) {message(player,"recipe");return;}
        var level=player.serverLevel();
        var recipe=level.getRecipeManager().byKey(id).filter(InfusionRecipe.class::isInstance).map(InfusionRecipe.class::cast).orElse(null);
        if(recipe==null||!recipe.acceptsCenter(requestedCenter)) {message(player,"recipe");return;}
        var matrices=new ArrayList<InfusionMatrixBlockEntity>();
        for(var p:BlockPos.betweenClosed(player.blockPosition().offset(-5,-5,-5),player.blockPosition().offset(5,5,5)))
            if(p.distToCenterSqr(player.position())<=25&&level.hasChunkAt(p)&&level.getBlockEntity(p) instanceof InfusionMatrixBlockEntity matrix)matrices.add(matrix);
        if(matrices.size()!=1) {message(player,matrices.isEmpty()?"none":"multiple");return;}
        var matrix=matrices.get(0);var origin=matrix.getBlockPos();
        if(matrix.crafting()||!matrix.validLocation()) {message(player,"busy");return;}
        var positions=InfusionLayout.scan(level,origin).pedestals();
        var all=new ArrayList<BlockPos>(positions);all.add(origin.below(2));
        if(all.stream().anyMatch(p->!level.mayInteract(player,p))) {message(player,"denied");return;}
        // The requested clean-up happens even when the new recipe lacks materials or space.
        for(var p:all) {
            var pedestal=(ArcanePedestalBlockEntity)level.getBlockEntity(p);
            var old=pedestal.getItem(0).copy();pedestal.clearContent();
            if(!old.isEmpty()&&!player.getInventory().add(old))player.drop(old,false);
        }
        var inventory=player.getInventory();
        for(int centerSlot=0;centerSlot<inventory.items.size();centerSlot++) {
            var center=inventory.getItem(centerSlot);
            if(!ItemStack.isSameItemSameTags(center,requestedCenter)||!recipe.acceptsCenter(center))continue;
            InfusionRecipe.Plan plan;
            try {plan=recipe.plan(center);}catch(ArithmeticException|IllegalArgumentException invalid) {message(player,"recipe");return;}
            if(plan.components().size()>positions.size()) {message(player,"pedestals");return;}
            var units=new ArrayList<Integer>();
            for(int slot=0;slot<inventory.items.size();slot++)for(int n=0;n<Math.min(plan.components().size(),inventory.getItem(slot).getCount()-(slot==centerSlot?1:0));n++)units.add(slot);
            int[] owner=new int[units.size()];Arrays.fill(owner,-1);
            boolean found=true;
            for(int component=0;component<plan.components().size();component++)if(!assign(component,plan.components(),units,owner,new boolean[units.size()],inventory)) {found=false;break;}
            if(!found)continue;
            var items=new ArrayList<ItemStack>(Collections.nCopies(plan.components().size(),ItemStack.EMPTY));
            for(int unit=0;unit<owner.length;unit++)if(owner[unit]>=0)items.set(owner[unit],inventory.getItem(units.get(unit)).copyWithCount(1));
            // Also enforce disabled recipes/configuration, without gating placement on research.
            if(!recipe.matches(new InfusionRecipe.Input(center,items,key->true),level)) {message(player,"recipe");return;}
            var selected=InfusionPlacementOrder.select(origin,positions,items.size(),player.getDirection());
            var ordered=InfusionPlacementOrder.counterclockwise(origin,selected);
            ((ArcanePedestalBlockEntity)level.getBlockEntity(origin.below(2))).setItem(0,inventory.removeItem(centerSlot,1));
            for(int unit=0;unit<owner.length;unit++)if(owner[unit]>=0)inventory.removeItem(units.get(unit),1);
            for(int i=0;i<ordered.size();i++) {
                // Browser components advance clockwise. Keep the top item and reverse the rest.
                int component=i==0?0:items.size()-i;
                ((ArcanePedestalBlockEntity)level.getBlockEntity(ordered.get(i))).setItem(0,items.get(component));
            }
            inventory.setChanged();player.inventoryMenu.broadcastChanges();matrix.refreshSurroundings();message(player,"done");return;
        }
        player.inventoryMenu.broadcastChanges();message(player,"materials");
    }
    private static boolean assign(int component,List<InfusionIngredient> ingredients,List<Integer> units,int[] owner,boolean[] seen,net.minecraft.world.entity.player.Inventory inventory) {
        for(int unit=0;unit<units.size();unit++) {
            if(seen[unit]||!ingredients.get(component).matches(inventory.getItem(units.get(unit))))continue;
            seen[unit]=true;
            if(owner[unit]<0||assign(owner[unit],ingredients,units,owner,seen,inventory)) {owner[unit]=component;return true;}
        }
        return false;
    }
}
