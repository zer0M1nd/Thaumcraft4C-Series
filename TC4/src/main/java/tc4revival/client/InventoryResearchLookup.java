package tc4revival.client;

import java.util.Optional;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.*;
import tc4revival.network.*;
import tc4revival.research.*;

/** A known-item shortcut, using the normal server-authorized book opening. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class InventoryResearchLookup {
    private static ItemStack item=ItemStack.EMPTY;
    private static String research;
    private static ResearchPages pages;
    private static long revision=-1;
    private static long recipeGeneration=-1;
    private static float held;
    private static boolean rendered,requested;
    private static Screen owner;
    private static void clear(){owner=null;item=ItemStack.EMPTY;research=null;held=0;revision=-1;pages=null;requested=false;}
    @SubscribeEvent public static void opened(ScreenEvent.Opening event){clear();}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){clear();}
    private static boolean hasBook(){
        var player=Minecraft.getInstance().player;
        if(player==null)return false;
        for(int i=0;i<player.getInventory().getContainerSize();i++)
            if(player.getInventory().getItem(i).getItem() instanceof ThaumonomiconItem)return true;
        return false;
    }
    public static Optional<String> find(ItemStack stack){
        var knowledge=ClientKnowledge.displaySnapshot().orElse(null);
        if(stack.isEmpty()||knowledge==null)return Optional.empty();
        var catalog=ResearchCatalogs.bundled();
        java.util.function.Predicate<String> known=key->knowledge.researchMarkers().contains(key)
                &&catalog.find(key).filter(ResearchCatalogs::enabled).filter(e->!e.category().equals("ELDRITCH")
                        ||knowledge.researchMarkers().contains("ELDRITCHMINOR")).isPresent();
        java.util.function.Predicate<ResearchDefinition.Stack> matches=display->{
            int count=ResearchDisplayItems.bundled().find(display).map(e->e.variants().size()).orElse(1);
            for(int variant=0;variant<count;variant++) {
                var resolved=ResearchDisplayItems.bundled().resolve(display,variant*1000L);
                if(resolved.isPresent()&&resolved.get().is(stack.getItem())) {
                    if(stack.getItem() instanceof CastingWandItem)
                        return BrowserQueryStacks.recipeKey(stack).equals(BrowserQueryStacks.recipeKey(resolved.get()));
                    return true;
                }
            }
            return false;
        };
        var recipe=new ResearchRecipeLinks(ResearchPages.bundled(),ResearchPageRecipes.bundled()).findCompleted(matches,known);
        if(recipe.isPresent())return Optional.of(recipe.get().research());
        // Modern recipe references carry their output in RecipeManager, not page.output.
        // Include ITEM pages used by integrations whose machines have no book renderer.
        for(var entry:ResearchPages.bundled().entries().entrySet())if(known.test(entry.getKey())) {
            for(var page:entry.getValue()) {
                if(LiveBookRecipes.supported(page.type()) && ClientLiveBookRecipes.get(page).stream()
                        .anyMatch(view->sameItem(stack,view.output())))return Optional.of(entry.getKey());
                var properties=page.properties();
                if(properties.has("items"))for(var value:properties.getAsJsonArray("items"))
                    if(sameItem(stack,BookComponents.item(value)))return Optional.of(entry.getKey());
            }
        }
        // Structures and functional items can have a research icon without a crafting output page.
        return catalog.all().stream().filter(e->known.test(e.key())&&!e.mirror()&&!e.tutorial())
                .filter(e->e.display().iconItem().filter(matches).isPresent() || e.display().icons().stream()
                        .anyMatch(icon->icon.item().filter(matches).isPresent())).map(ResearchDefinition::key).findFirst();
    }
    private static boolean sameItem(ItemStack input,ItemStack output) {
        return !output.isEmpty() && input.is(output.getItem()) && (!(input.getItem() instanceof CastingWandItem)
                || BrowserQueryStacks.recipeKey(input).equals(BrowserQueryStacks.recipeKey(output)));
    }
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event){
        if(event.phase==TickEvent.Phase.START)rendered=false;
        else if(!rendered)clear();
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void tooltip(RenderTooltipEvent.Pre event){
        var mc=Minecraft.getInstance();var current=event.getItemStack();
        if(!(mc.screen instanceof AbstractContainerScreen<?> screen)||current.isEmpty()||!hasBook()
                ||!screen.getMenu().getCarried().isEmpty()||PatchouliLookupCompat.hasLookup(current)){clear();return;}
        rendered=true;
        if(owner!=screen){clear();owner=screen;}
        if(!ItemStack.isSameItemSameTags(item,current)||pages!=ResearchPages.bundled()||revision!=ClientKnowledge.revision()
                ||recipeGeneration!=ClientLiveBookRecipes.generation()){
            held=0;requested=false;item=current.copy();pages=ResearchPages.bundled();revision=ClientKnowledge.revision();research=find(item).orElse(null);
            recipeGeneration=ClientLiveBookRecipes.generation();
        }
        if(research==null)return;
        boolean shift=PatchouliLookupCompat.shift(),down=shift?Screen.hasShiftDown():Screen.hasControlDown();
        float duration=PatchouliLookupCompat.duration();
        if(down)held=Math.min(duration,held+mc.getDeltaFrameTime());else{held=0;requested=false;}
        draw(event.getGraphics(),event.getX()-34,event.getY()-4,held/duration,shift);
        if(down&&held>=duration&&!requested&&find(item).filter(research::equals).isPresent()){
            requested=true;TCNetwork.CHANNEL.sendToServer(new BookRecipeRequest(research));
        }
    }
    /** Patchouli's tooltip-relative geometry and circular hold indicator, with our own book icon. */
    private static void draw(GuiGraphics g,int x,int y,float progress,boolean shift){
        var mc=Minecraft.getInstance();
        g.fill(x-4,y-4,x+20,y+26,0x44000000);
        g.fill(x-6,y-6,x+22,y+28,0x44000000);
        g.flush();RenderSystem.disableDepthTest();
        if(progress>0){
            RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShader(GameRenderer::getPositionColorShader);
            var buffer=Tesselator.getInstance().getBuilder();var pose=g.pose().last().pose();
            buffer.begin(VertexFormat.Mode.TRIANGLE_FAN,DefaultVertexFormat.POSITION_COLOR);
            float time=mc.player.tickCount+mc.getFrameTime();
            float alpha=.5f+.2f*((float)Math.cos(time/10f)*.5f+.5f);
            buffer.vertex(pose,x+8,y+8,0).color(0,.5f,0,alpha).endVertex();
            for(float angle=progress*360;angle>=0;angle=Math.max(-1,angle-1)){
                double radians=Math.toRadians(angle-90);
                buffer.vertex(pose,x+8+(float)Math.cos(radians)*12,y+8+(float)Math.sin(radians)*12,0).color(0f,1f,0f,1f).endVertex();
            }
            buffer.vertex(pose,x+8,y+8,0).color(0f,1f,0f,0f).endVertex();
            BufferUploader.drawWithShader(buffer.end());RenderSystem.disableBlend();
        }
        var book=new ItemStack(TCItems.THAUMONOMICON.get());
        g.pose().pushPose();
        try{
            g.pose().translate(0,0,300);g.renderItem(book,x,y);g.renderItemDecorations(mc.font,book,x,y);
            g.pose().translate(0,0,200);g.drawString(mc.font,"?",x+10,y+8,0xFFFFFFFF,true);
            g.pose().scale(.5f,.5f,1);
            var key=Component.literal(shift?"Shift":Minecraft.ON_OSX?"Cmd":"Ctrl").withStyle(ChatFormatting.BOLD);
            g.drawString(mc.font,key,(x+10)*2-16,(y+8)*2+20,0xFFFFFFFF,true);
        }finally{g.pose().popPose();g.flush();RenderSystem.enableDepthTest();}
    }
}
