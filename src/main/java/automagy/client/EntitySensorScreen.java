package automagy.client;
import automagy.content.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class EntitySensorScreen extends AbstractContainerScreen<EntitySensorMenu>{
 private static final ResourceLocation TEXTURE=LegacyControls.texture("entity_signal");private Button adults,children,specific;private boolean ages,player;private Component target=Component.empty();
 public EntitySensorScreen(EntitySensorMenu m,Inventory i,Component t){super(m,i,t);}
 private void send(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
 @Override protected void init(){super.init();adults=addRenderableWidget(LegacyControls.checkbox(leftPos+40,topPos+55,TEXTURE,()->menu.value(0)==1,r->send(0)));children=addRenderableWidget(LegacyControls.checkbox(leftPos+40,topPos+65,TEXTURE,()->menu.value(1)==1,r->send(1)));specific=addRenderableWidget(LegacyControls.checkbox(leftPos+40,topPos+60,TEXTURE,()->menu.value(2)==1,r->send(2)));refresh();}
 private void refresh(){var eye=menu.getSlot(0).getItem();ages=false;player=false;target=Component.empty();if(EntityEyeItem.linked(eye)){var tag=eye.getTag();player=tag.hasUUID("Player");if(player)target=Component.literal(tag.getString("PlayerName"));else{var type=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse(tag.getString("EntityType")));if(type!=null){target=type.getDescription();ages=tag.getBoolean("HasAge");}}}adults.visible=children.visible=ages;specific.visible=player;}
 @Override protected void containerTick(){super.containerTick();refresh();}
 @Override protected void renderBg(GuiGraphics g,float p,int x,int y){tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos,topPos,0,0,176,166);LegacyControls.text(g,player?Component.translatable("Automagy.gui.entitySignal.players"):target,leftPos+42,topPos+36,.75f,0x00aaaa,true);
  if(ages){LegacyControls.text(g,Component.translatable("Automagy.gui.entitySignal.includeAdults"),leftPos+53,topPos+57,.75f,0xffffff,true);LegacyControls.text(g,Component.translatable("Automagy.gui.entitySignal.includeChildren"),leftPos+53,topPos+67,.75f,0xffffff,true);}
  if(player){LegacyControls.text(g,Component.translatable("Automagy.gui.entitySignal.specificPlayer.1"),leftPos+53,topPos+57,.75f,0xffffff,true);LegacyControls.text(g,Component.translatable("Automagy.gui.entitySignal.specificPlayer.2",net.minecraft.ChatFormatting.AQUA+target.getString()+net.minecraft.ChatFormatting.GRAY),leftPos+53,topPos+67,.75f,0xffffff,true);}
 }
 @Override protected void renderLabels(GuiGraphics g,int x,int y){}
 @Override public void render(GuiGraphics g,int x,int y,float p){renderBackground(g);super.render(g,x,y,p);renderTooltip(g,x,y);}
}
