package thaumicomniverse.compat.ponder;

import java.util.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import com.simibubi.create.infrastructure.ponder.PonderIndex;
import com.simibubi.create.foundation.ponder.ui.PonderUI;
import com.simibubi.create.foundation.gui.ScreenOpener;
import tc4revival.research.*;
import thaumcraft.api.client.ResearchBookTutorials;

/** Book return history is retained by Catnip while the server book session remains open. */
public final class BookPonderBridge implements ResearchBookTutorials.Handler {
    private static Screen bookReturn;
    private static boolean listening;
    private static boolean leavingTutorial;
    private static final Map<String,String> STRUCTURES=Map.ofEntries(
        Map.entry("InfusionAltar","thaumcraft:infusion_matrix"),Map.entry("InfernalFurnace","thaumcraft:infernal_furnace"),
        Map.entry("Thaumatorium","thaumcraft:thaumatorium"),Map.entry("AdvAlchemyFurnace","thaumcraft:advanced_alchemy_furnace"),
        Map.entry("NodeJar","thaumcraft:node_jar"),Map.entry("Crucible","thaumcraft:crucible"),
        Map.entry("ResTable","thaumcraft:research_table"),Map.entry("ArcTable","thaumcraft:arcane_workbench"));
    public static void register(){
        ResearchBookTutorials.register(new BookPonderBridge());
        if(!listening){
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(BookPonderBridge::opening);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(BookPonderBridge::closing);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(BookPonderBridge::tick);
            listening=true;
        }
    }
    private static void opening(net.minecraftforge.client.event.ScreenEvent.Opening event){
        if(bookReturn==null)return;
        var next=event.getNewScreen();
        if(next==bookReturn||next==null||!next.getClass().getName().startsWith("com.simibubi.create.foundation.ponder."))bookReturn=null;
    }
    private static void closing(net.minecraftforge.client.event.ScreenEvent.Closing event){
        if(bookReturn!=null&&event.getScreen().getClass().getName().startsWith("com.simibubi.create.foundation.ponder."))leavingTutorial=true;
    }
    private static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END||!leavingTutorial)return;
        leavingTutorial=false;
        var mc=net.minecraft.client.Minecraft.getInstance();
        if(bookReturn==null)return;
        if(mc.player==null||!tc4revival.client.ClientBookSession.active()){bookReturn=null;return;}
        // Forge does not fire Opening when setScreen(null) closes a screen. Catnip
        // also clears its history on Esc/X, so restore only after Closing completes.
        if(mc.screen==null){var target=bookReturn;bookReturn=null;ScreenOpener.clearStack();mc.setScreen(target);}
    }
    private static boolean exists(ResourceLocation id){return com.simibubi.create.foundation.ponder.PonderRegistry.ALL.containsKey(id);}
    @Override public boolean has(ItemStack stack){return !stack.isEmpty()&&exists(ForgeRegistries.ITEMS.getKey(stack.getItem()));}
    @Override public Optional<ItemStack> structure(String research,ResearchPage page,long cycle) {
        if(page.type()!=ResearchPage.Type.COMPOUND_CRAFTING)return Optional.empty();
        if(page.output().isPresent()) {
            var output=ResearchDisplayItems.bundled().resolve(page.output().get(),cycle).filter(this::has);
            if(output.isPresent())return output;
        }
        var mapped=page.recipe() instanceof ResearchPage.Reference reference?STRUCTURES.get(reference.displayKey(cycle)):null;
        if(mapped!=null){var item=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(mapped));if(item!=null){var stack=new ItemStack(item);if(has(stack))return Optional.of(stack);}}
        return Optional.empty();
    }
    @Override public Optional<ResourceLocation> structureId(String research,ResearchPage page,long cycle){
        var item=ResearchBookTutorials.Handler.super.structureId(research,page,cycle);
        if(item.isPresent()||page.type()!=ResearchPage.Type.COMPOUND_CRAFTING)return item;
        var mapped=page.recipe() instanceof ResearchPage.Reference reference?STRUCTURES.get(reference.displayKey(cycle)):null;
        return Optional.ofNullable(mapped).map(ResourceLocation::parse).filter(BookPonderBridge::exists);
    }
    @Override public boolean open(ResourceLocation id,Screen returnTo){
        if(!exists(id))return false;
        bookReturn=returnTo;
        ScreenOpener.open(returnTo,PonderUI.of(id));return true;
    }
    @Override public boolean open(ItemStack stack,Screen returnTo){
        if(!has(stack))return false;
        bookReturn=returnTo;
        ScreenOpener.open(returnTo,PonderUI.of(stack));return true;
    }
}
