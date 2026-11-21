package automagy.client;
import automagy.content.BoilerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class BoilerScreen extends AbstractContainerScreen<BoilerMenu>{
 private net.minecraftforge.fluids.FluidStack cachedFluid=net.minecraftforge.fluids.FluidStack.EMPTY;
 private int cachedVersion=-1;
 private tc4revival.aspect.AspectAmounts fluidAspects=new tc4revival.aspect.AspectAmounts();
 private String blockIdentity;
 private boolean aspectDataReady;
 private static final net.minecraft.resources.ResourceLocation TEXTURE=net.minecraft.resources.ResourceLocation.parse("automagy:textures/gui/boiler.png");
 public BoilerScreen(BoilerMenu m,Inventory i,Component t){super(m,i,t);imageHeight=175;}
 @Override protected void renderLabels(GuiGraphics g,int x,int y){}
 @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);if(menu.tile==null)return;var t=menu.tile;var s=t.process();int burn=s.currentBurn()==0?0:Math.min(20,s.burn()*20/s.currentBurn());tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+80,topPos+46-burn,176,20-burn,16,burn);int cook=Math.min(46,s.cook()*46/t.smeltTime());tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+100,topPos+69-cook,216,46-cook,9,cook);int stored=t.storedEssentia()*48/t.essentiaCapacity();tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+56,topPos+66-stored,200,48-stored,8,stored);int fluid=t.tank.getFluidAmount()*48/t.tank.getCapacity();if(fluid>0){var contents=t.tank.getFluid();var extension=net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(contents.getFluid());var sprite=minecraft.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(extension.getStillTexture(contents));int color=extension.getTintColor(contents);g.setColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,((color>>>24)&255)/255f);for(int dy=0;dy<fluid;dy+=16)tc4revival.client.LegacyGui.blit(g,leftPos+117,topPos+66-fluid+dy,0,8,Math.min(16,fluid-dy),sprite);g.setColor(1,1,1,1);}tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+55,topPos+14,232,0,10,55);tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+116,topPos+14,232,0,10,55);}
 private void updateFluidAspects(){
  if(menu.tile==null)return;
  var contents=menu.tile.tank.getFluid();int version=tc4revival.client.ClientItemAspects.version();
  if(cachedVersion==version&&cachedFluid.isFluidEqual(contents))return;
  cachedVersion=version;cachedFluid=contents.copy();fluidAspects=new tc4revival.aspect.AspectAmounts();blockIdentity=null;
  var data=tc4revival.client.ClientItemAspects.calculation();aspectDataReady=data!=null;
  if(!aspectDataReady||contents.isEmpty())return;
  fluidAspects=automagy.content.BoilerFluids.aspects(contents,data,data.blocks());
  var state=contents.getFluid().defaultFluidState().createLegacyBlock();
  var target=data.blocks().resolve(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString(),tc4revival.aspect.item.BlockScanTargets.properties(state));
  if(target.isPresent()&&target.get().legacyKey()!=null)blockIdentity=Integer.toString(data.identity().scanHash(target.get().legacyKey()));
  else if(!state.isAir()&&state.getBlock().asItem()!=net.minecraft.world.item.Items.AIR)blockIdentity=tc4revival.research.ScanIdentity.item(new net.minecraft.world.item.ItemStack(state.getBlock()),data);
 }
 private boolean cannotProcess(){return aspectDataReady&&!cachedFluid.isEmpty()&&fluidAspects.isEmpty();}
 private boolean scanned(net.minecraft.world.item.ItemStack stack,tc4revival.research.PlayerKnowledge known){
  return tc4revival.client.ClientItemAspects.resolve(stack).map(value->known.scanned(tc4revival.research.PlayerKnowledge.ScanKind.OBJECT,value.identity())).orElse(false);
 }
 private void renderFluidAspects(GuiGraphics g,int x,int y){
  var known=tc4revival.client.ClientKnowledge.snapshot().orElse(null);if(known==null||cachedFluid.isEmpty())return;
  boolean visible=blockIdentity!=null&&known.scanned(tc4revival.research.PlayerKnowledge.ScanKind.OBJECT,blockIdentity);
  visible|=scanned(net.minecraftforge.fluids.FluidUtil.getFilledBucket(cachedFluid),known);
  if(automagy.content.SoupFluids.MUSHROOM.source.get().isSame(cachedFluid.getFluid()))visible|=scanned(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MUSHROOM_STEW),known);
  if(automagy.content.SoupFluids.VISHROOM.source.get().isSame(cachedFluid.getFluid()))visible|=scanned(new net.minecraft.world.item.ItemStack(automagy.content.Content.VISHROOM_STEW.get()),known);
  if(!visible)return;
  var aspects=new java.util.ArrayList<>(fluidAspects.aspects());aspects.sort(java.util.Comparator.comparingInt(fluidAspects::amount).reversed());
  g.pose().pushPose();g.pose().translate(0,0,500);
  for(int i=0;i<aspects.size();i++){
   var aspect=aspects.get(i);int px=x+9+i*18,py=y-34;
   tc4revival.client.LegacyGui.blit(g,net.minecraft.resources.ResourceLocation.parse("thaumcraft:textures/aspects/_back.png"),px-2,py-2,0,0,20,20,20,20);
   tc4revival.client.AspectTooltips.draw(g,font,new tc4revival.client.AspectTooltips.Cell(known.knows(aspect)?aspect.tag():"",known.knows(aspect)?Integer.toString(fluidAspects.amount(aspect)):""),px,py);
  }
  g.pose().popPose();
 }
 @Override public void render(GuiGraphics g,int x,int y,float partial){
  updateFluidAspects();renderBackground(g);super.render(g,x,y,partial);
  if(cannotProcess())tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+82,topPos+14,178,82,12,12);
  renderTooltip(g,x,y);
  if(menu.tile==null)return;
  if(isHovering(116,14,10,56,x,y)&&!menu.tile.tank.isEmpty()){
   g.renderComponentTooltip(font,java.util.List.of(menu.tile.tank.getFluid().getDisplayName(),Component.translatable("Automagy.gui.boiler.liquidQty",menu.tile.tank.getFluidAmount())),x,y);
   renderFluidAspects(g,x,y);
  }else if(cannotProcess()&&isHovering(82,14,12,12,x,y)){
   var lines=java.util.Arrays.stream(net.minecraft.client.resources.language.I18n.get("Automagy.gui.boiler.cannotProcess").replace("\\n","\n").split("\n")).map(Component::literal).map(c->(Component)c).toList();
   g.renderComponentTooltip(font,lines,x,y);
  }
 }
}
