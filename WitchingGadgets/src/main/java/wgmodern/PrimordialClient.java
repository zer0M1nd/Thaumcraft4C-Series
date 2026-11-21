package wgmodern;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;

@Mod.EventBusSubscriber(modid="witchinggadgets",value=Dist.CLIENT)
public class PrimordialClient {
 public static List<KeyMapping> ABILITIES=List.of();
 @Mod.EventBusSubscriber(modid="witchinggadgets",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
 public static class PrimordialRegistration {
  @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){ABILITIES=List.of(new KeyMapping("Primordial: cycle held tool",-1,"key.categories.misc"),new KeyMapping("Primordial: cycle helmet",-1,"key.categories.misc"),new KeyMapping("Primordial: cycle cuirass",-1,"key.categories.misc"),new KeyMapping("Primordial: cycle greaves",-1,"key.categories.misc"),new KeyMapping("Primordial: cycle boots",-1,"key.categories.misc"));ABILITIES.forEach(e::register);}
  @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->MenuScreens.register(PrimordialContent.MENU.get(),GloveScreen::new));}
  @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item e){for(var item:PrimordialContent.WEAPONS.values())e.register((s,t)->t==1?PrimordialGear.color(s):0xffffff,item.get());for(var item:PrimordialContent.ARMOR.values())e.register((s,t)->t==0?PrimordialGear.color(s):0xffffff,item.get());}
  @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent e){e.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)m->PrimordialArmorModel.clear());}
 }
 @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGH) public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.screen!=null)return;for(int i=0;i<ABILITIES.size();i++)while(ABILITIES.get(i).consumeClick())WGNetwork.CHANNEL.sendToServer(new PrimordialNetwork.Action(2,i));var focus=tc4revival.client.WandKeys.CHANGE_FOCUS;if(mc.player.getMainHandItem().getItem() instanceof PrimordialGlove)while(focus.consumeClick()){if(mc.player.isShiftKeyDown())WGNetwork.CHANNEL.sendToServer(new PrimordialNetwork.Action(1,0));else mc.setScreen(new Radial());}}
 public static class GloveScreen extends AbstractContainerScreen<PrimordialMenu> {
  static final ResourceLocation BACK=new ResourceLocation("witchinggadgets","textures/gui/primordialglove.png");
  public GloveScreen(PrimordialMenu m,Inventory inv,Component title){super(m,inv,title);imageWidth=176;imageHeight=166;}
  @Override protected void renderLabels(GuiGraphics g,int x,int y){}
  @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){tc4revival.client.LegacyGui.blit(g,BACK,leftPos,topPos,0,0,176,166);if(menu.glove.hasTag()&&menu.glove.getTag().contains("storedNode")){g.pose().pushPose();g.pose().translate(leftPos+88,topPos+39,100);g.pose().scale(120,-120,120);PrimordialRenderer.node(menu.glove,g.pose(),g.bufferSource(),true);g.flush();g.pose().popPose();}}
  @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);super.render(g,mx,my,partial);renderTooltip(g,mx,my);if(mx>=leftPos+75&&mx<leftPos+101&&my>=topPos+26&&my<topPos+52&&menu.glove.hasTag()&&menu.glove.getTag().contains("storedNode")){var tag=menu.glove.getTag().getCompound("storedNode");var aspects=PrimordialGlove.stored(menu.glove);var tips=new ArrayList<Component>();var type=tc4revival.content.AuraNodeType.fromId(tag.getInt("type"));var modifier=tag.getInt("modifier")<0?null:tc4revival.content.AuraNodeModifier.fromId(tag.getInt("modifier"));tips.add(Component.translatable("nodetype."+type.name()+".name").append(modifier==null?Component.empty():Component.literal(", ").append(Component.translatable("nodemod."+modifier.name()+".name"))));tips.add(Component.translatable("wg.gui.visSize").append(": "+aspects.total()).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));if(hasShiftDown())aspects.view().forEach((a,n)->tips.add(Component.literal(thaumcraft.api.aspects.AspectViews.display(a).displayName()+" "+n)));else tips.add(Component.translatable("wg.gui.shiftForAspectList"));g.renderComponentTooltip(font,tips,mx,my);}}
 }
 public static class Radial extends Screen {
  private float progress;private ItemStack glove=ItemStack.EMPTY;private boolean held=true,locked,closing;
  public Radial(){super(Component.literal("Primordial gems"));}
  @Override protected void init(){glove=minecraft.player.getMainHandItem();}
  @Override public boolean isPauseScreen(){return false;}
  @Override public void tick(){
   if(minecraft.player==null||!(minecraft.player.getMainHandItem().getItem() instanceof PrimordialGlove)){minecraft.setScreen(null);return;}
   glove=minecraft.player.getMainHandItem();float step=WGConfig.RADIAL_SPEED.get().floatValue();
   if(closing||!held&&!locked){progress=Math.max(0,progress-step);if(progress==0)minecraft.setScreen(null);}
   else if(!locked){progress=Math.min(1,progress+step);locked=progress>=1;}
  }
  @Override public void onClose(){closing=true;}
  @Override public boolean keyPressed(int key,int scan,int mods){if(tc4revival.client.WandKeys.CHANGE_FOCUS.matches(key,scan)){if(!held&&locked)closing=true;held=true;return true;}return super.keyPressed(key,scan,mods);}
  @Override public boolean keyReleased(int key,int scan,int mods){if(tc4revival.client.WandKeys.CHANGE_FOCUS.matches(key,scan)){held=false;return true;}return super.keyReleased(key,scan,mods);}
  public static int selected(double x,double y){return PrimordialRules.radialSlot(x,y);}
  @Override public boolean mouseClicked(double x,double y,int button){if(button==0&&locked&&!closing){int slot=selected(x-width/2d,y-height/2d);WGNetwork.CHANNEL.sendToServer(new PrimordialNetwork.Action(0,slot));PrimordialGlove.select(glove,slot);onClose();return true;}return super.mouseClicked(x,y,button);}
  @Override public void render(GuiGraphics g,int mx,int my,float partial){float radius=50*progress;g.pose().pushPose();g.pose().translate(width/2f,height/2f,100);g.pose().mulPose(Axis.ZP.rotationDegrees(180+180*progress));RenderSystem.enableBlend();tc4revival.client.LegacyGui.blit(g,new ResourceLocation("witchinggadgets","textures/gui/gauntletradial0.png"),(int)-radius,(int)-radius,(int)(radius*2),(int)(radius*2),0,0,256,256,256,256);g.pose().mulPose(Axis.ZP.rotationDegrees((System.currentTimeMillis()%10000)*.036f));tc4revival.client.LegacyGui.blit(g,new ResourceLocation("witchinggadgets","textures/gui/gauntletradial1.png"),(int)-radius,(int)-radius,(int)(radius*2),(int)(radius*2),0,0,256,256,256,256);g.pose().mulPose(Axis.ZP.rotationDegrees(-(System.currentTimeMillis()%10000)*.036f));var gems=PrimordialGlove.gems(glove);int sel=selected(mx-width/2d,my-height/2d);for(int i=0;i<5;i++)if(!gems.get(i).isEmpty()){int x=(int)((i==0?-54:i==1?13:i==3?-22:i==2?-76:35)/256f*radius*2),y=(int)((i<2?-64:i==3?36:-6)/256f*radius*2);g.flush();float tint=sel==i?1:.5625f;g.setColor(tint,tint,tint,1);g.renderItem(gems.get(i),x,y);g.flush();g.setColor(1,1,1,1);}g.pose().popPose();}
 }
}
