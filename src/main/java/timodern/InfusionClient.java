package timodern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import tc4revival.aspect.*;

@Mod.EventBusSubscriber(modid=ThaumicInfusion.ID,value=Dist.CLIENT)
public class InfusionClient {
    @SubscribeEvent public static void recolor(net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock e){if(!e.getLevel().isClientSide||!InfusionData.has(e.getLevel(),e.getPos(),"fabrico")||!(e.getItemStack().getItem() instanceof tc4revival.content.StoredAspectItem stored))return;var aspects=stored.storedAspects(e.getItemStack());if(aspects.isEmpty())return;var p=e.getPos();int color=aspects.aspects().get(0).color();var old=InfusionData.CLIENT.get(p.asLong());if(old!=null&&old.settings.getInt("tint")==color)return;e.getLevel().playLocalSound(p.getX()+.5,p.getY()+.5,p.getZ()+.5,net.minecraft.sounds.SoundEvents.GENERIC_SWIM,net.minecraft.sounds.SoundSource.BLOCKS,.5f,1+(e.getLevel().random.nextFloat()-e.getLevel().random.nextFloat())*.3f,false);for(int i=0;i<5;i++)thaumcraft.api.client.ThaumicParticles.bubble((net.minecraft.client.multiplayer.ClientLevel)e.getLevel(),new thaumcraft.api.client.ThaumicParticles.BubbleSpec(p.getX(),p.getY(),p.getZ(),1,color,0,0,1));}
    public static void receive(InfusionNetwork.Sync packet){var mc=Minecraft.getInstance();if(mc.level==null)return;if(packet.data()==null||!packet.data().contains("aspects"))InfusionData.CLIENT.remove(packet.pos().asLong());else InfusionData.CLIENT.put(packet.pos().asLong(),InfusionData.Entry.load(packet.data()));mc.level.getChunkSource().getLightEngine().checkBlock(packet.pos());mc.levelRenderer.setBlockDirty(packet.pos(),mc.level.getBlockState(packet.pos()),mc.level.getBlockState(packet.pos()));}
    public static void open(InteractionHand hand){Minecraft.getInstance().setScreen(new Selection(hand));}
    private static float tagScale;
    @SubscribeEvent public static void labels(RenderLevelStageEvent e){if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;var mc=Minecraft.getInstance();if(mc.player==null||!mc.player.isShiftKeyDown()||!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)||!(tc4revival.content.WandFocusItem.attached(mc.player.getMainHandItem()).getItem() instanceof InfusingFocus)){tagScale=0;return;}var entry=InfusionData.CLIENT.get(hit.getBlockPos().asLong());if(entry==null)return;if(tagScale<.5f)tagScale+=.031f-tagScale/10;var pose=e.getPoseStack();var buffers=mc.renderBuffers().bufferSource();var camera=e.getCamera().getPosition();var face=hit.getDirection();int i=0;for(var key:entry.aspects){var aspect=TCAspects.catalog().find(key).orElse(null);var effect=InfusionEffects.get(key);if(aspect==null||effect==null)continue;int row=i/5,col=i++%5,width=Math.min(5,entry.aspects.size()-row*5);var center=net.minecraft.world.phys.Vec3.atCenterOf(hit.getBlockPos()).add(face.getStepX()*(.5+tagScale*2),face.getStepY()*(.5+tagScale*2)+row*tagScale*1.05,face.getStepZ()*(.5+tagScale*2)).subtract(camera);pose.pushPose();pose.translate(center.x,center.y,center.z);/* Legacy labels stand away from the face and turn around Y only. */ float yaw=(float)Math.toDegrees(Math.atan2(camera.x-hit.getBlockPos().getX()-.5-face.getStepX()*.5,camera.z-hit.getBlockPos().getZ()-.5-face.getStepZ()*.5))+180;pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));pose.translate((col-width/2f+.5)*tagScale*tagScale*4,0,0);pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180));pose.scale(tagScale,tagScale,tagScale);int color=aspect.color();var out=buffers.getBuffer(tc4revival.client.TCRenderTypes.revealedAspect(aspect.texture()));var matrix=pose.last().pose();float r=(color>>16&255)/255f,g=(color>>8&255)/255f,b=(color&255)/255f;out.vertex(matrix,-.5f,-.5f,0).color(r,g,b,.75f).uv(0,0).uv2(220).endVertex();out.vertex(matrix,-.5f,.5f,0).color(r,g,b,.75f).uv(0,1).uv2(220).endVertex();out.vertex(matrix,.5f,.5f,0).color(r,g,b,.75f).uv(1,1).uv2(220).endVertex();out.vertex(matrix,.5f,-.5f,0).color(r,g,b,.75f).uv(1,0).uv2(220).endVertex();buffers.endBatch();pose.translate(.52,.5,.01);pose.scale(.04f,.04f,.04f);String count=Integer.toString(effect.cost());mc.font.drawInBatch(count,-mc.font.width(count),-mc.font.lineHeight,0xffffffff,true,pose.last().pose(),buffers,net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH,0,220);buffers.endBatch();pose.popPose();}}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){InfusionData.CLIENT.clear();}
    @SubscribeEvent public static void unload(net.minecraftforge.event.level.LevelEvent.Unload e){if(e.getLevel().isClientSide())InfusionData.CLIENT.clear();}
    @SubscribeEvent public static void unloadChunk(net.minecraftforge.event.level.ChunkEvent.Unload e){if(e.getLevel().isClientSide()){var c=e.getChunk().getPos();InfusionData.CLIENT.keySet().removeIf(key->new net.minecraft.world.level.ChunkPos(net.minecraft.core.BlockPos.of(key)).equals(c));}}
    @SubscribeEvent public static void overlay(RenderLevelStageEvent e){if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;var mc=Minecraft.getInstance();if(mc.player==null||!(tc4revival.content.WandFocusItem.attached(mc.player.getMainHandItem()).getItem() instanceof InfusingFocus))return;var buffers=mc.renderBuffers().bufferSource();var camera=e.getCamera().getPosition();var pose=e.getPoseStack();InfusionData.CLIENT.forEach((packed,entry)->{var pos=net.minecraft.core.BlockPos.of(packed);if(pos.distToCenterSqr(camera.x,camera.y,camera.z)>64*64||entry.aspects.isEmpty())return;int color=TCAspects.catalog().find(entry.aspects.iterator().next()).map(Aspect::color).orElse(0xffffff);java.util.function.Predicate<net.minecraft.core.BlockPos> connected=p->{var other=InfusionData.CLIENT.get(p.asLong());return other!=null&&other.aspects.equals(entry.aspects);};pose.pushPose();pose.translate(pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z);tc4revival.client.ConnectedFaces.render(pos,connected,connected,pose,type->buffers.getBuffer(InfusionOverlayRender.wrap(type)),(color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1f,false);pose.popPose();});buffers.endBatch();}
    @Mod.EventBusSubscriber(modid=ThaumicInfusion.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class Registration {
        @SubscribeEvent public static void additionalModels(ModelEvent.RegisterAdditional e){e.register(ResourceLocation.parse("thaumicinfusion:item/focus_infusion_orn"));}
        @SubscribeEvent public static void itemColors(RegisterColorHandlersEvent.Item event){event.register((stack,tint)->EssentiaBlock.color(EssentiaBlock.aspect(stack)),ThaumicInfusion.ESSENTIA.get(),ThaumicInfusion.BRICKS.get(),ThaumicInfusion.CHISELED.get());}
        @SubscribeEvent public static void blockColors(RegisterColorHandlersEvent.Block event){event.register((state,world,pos,tint)->world!=null&&pos!=null&&world.getBlockEntity(pos) instanceof EssentiaBlock.Tile t?EssentiaBlock.color(t.aspect):0xffffff,ThaumicInfusion.ESSENTIA.get(),ThaumicInfusion.BRICKS.get(),ThaumicInfusion.CHISELED.get());}
    }
    public static class Selection extends Screen {
        public static final ResourceLocation PANEL=new ResourceLocation("thaumicinfusion","textures/gui/gui_infusion.png");
        public static final ResourceLocation PAPER=new ResourceLocation("thaumcraft","textures/misc/parchment3.png");
        private final InteractionHand hand;
        private final List<Aspect> aspects=new ArrayList<>();
        private int left,top,page;
        private String selected="";
        private int textScroll;
        private String textAspect="",textLanguage="";
        private tc4revival.research.ResearchPages textPages;
        private Component description=Component.empty();
        public Selection(InteractionHand hand){super(Component.translatable("item.thaumicinfusion.focus_infusion"));this.hand=hand;}
        protected void init(){left=(width-105)/2;top=(height-113)/2;aspects.clear();var knowledge=tc4revival.client.ClientKnowledge.snapshot();for(var a:TCAspects.catalog().all())if(InfusionEffects.get(a.tag())!=null&&InfusionConfig.enabled(a.tag())&&(minecraft.player.isCreative()||knowledge.map(k->k.knows(a)).orElse(false)))aspects.add(a);var wand=minecraft.player.getItemInHand(hand);selected=wand.hasTag()?wand.getTag().getString("InfusionAspect"):"";}
        private void icon(GuiGraphics g,Aspect a,int x,int y,boolean active){g.flush();int c=a.color();g.setColor(active?(c>>16&255)/255f:.1f,active?(c>>8&255)/255f:.1f,active?(c&255)/255f:.1f,active?1:.8f);tc4revival.client.LegacyGui.blit(g,a.texture(),x,y,0,0,16,16,16,16);g.setColor(1,1,1,1);String cost=Integer.toString(InfusionEffects.get(a.tag()).cost());g.pose().pushPose();g.pose().translate(x+16,y+16,0);g.pose().scale(.5f,.5f,1);g.drawString(font,cost,-font.width(cost),-font.lineHeight,0xffffffff,true);g.pose().popPose();}
        public boolean isPauseScreen(){return false;}
        private Component description(){
            var pages=tc4revival.research.ResearchPages.bundled();
            String language=minecraft.getLanguageManager().getSelected();
            if(!selected.equals(textAspect)||!language.equals(textLanguage)||pages!=textPages){
                textAspect=selected;textLanguage=language;textPages=pages;textScroll=0;
                String research="thaumicinfusion:FOCUSINFUSION";
                String prefix=research+"/effect/"+InfusionEffects.local(selected);
                var result=Component.empty();
                for(var source:pages.entries().getOrDefault(research,List.of()))
                    if(source.id().equals(prefix+"/original")||source.id().equals(prefix+"/details"))
                        for(var part:tc4revival.client.BookAmendments.expand(source,research))
                            result.append(tc4revival.client.BookComponents.decode(part.component()));
                if(result.getSiblings().isEmpty())result.append(tc4revival.client.BookLegacyImport.prose(
                    Component.translatable("ti.effect_info."+InfusionEffects.local(selected).toUpperCase(Locale.ROOT)),research));
                // The selection parchment also uses real aspect components, with dark name text.
                var readable=Component.empty();
                result.visit((style,text)->{
                    var click=style.getClickEvent();var color=style.getColor();String insertion=style.getInsertion();
                    if(click!=null&&click.getValue().startsWith(tc4revival.client.BookComponents.LINK+"aspect/")
                        &&color!=null&&(insertion==null||!insertion.startsWith(tc4revival.client.BookComponents.ICON))){
                        int rgb=color.getValue();style=style.withColor(((int)((rgb>>16&255)*.4f)<<16)|((int)((rgb>>8&255)*.4f)<<8)|(int)((rgb&255)*.4f));
                    }
                    readable.append(Component.literal(text).withStyle(style));return Optional.empty();
                },net.minecraft.network.chat.Style.EMPTY);
                description=readable;
            }
            return description;
        }
        private void description(GuiGraphics g,int mx,int my){
            tc4revival.client.LegacyGui.blit(g,PAPER,left+110,top-20,0,0,150,150,256,256);
            var lines=font.split(description(),130);int shown=13;
            textScroll=Math.min(textScroll,Math.max(0,lines.size()-shown));
            net.minecraft.network.chat.Style hovered=null;
            g.enableScissor(left+120,top-10,left+250,top+107);
            try{for(int row=0;row<shown&&row+textScroll<lines.size();row++){
                var line=lines.get(row+textScroll);int y=top-10+row*font.lineHeight;
                g.drawString(font,line,left+120,y,1,false);
                float[] cursor={left+120};
                line.accept((index,style,code)->{
                    String insertion=style.getInsertion();
                    if(insertion!=null&&insertion.startsWith(tc4revival.client.BookComponents.ICON))
                        TCAspects.catalog().find(insertion.substring(tc4revival.client.BookComponents.ICON.length())).ifPresent(aspect->{
                            var appearance=thaumcraft.api.client.ClientAspectViews.view(aspect);int c=appearance.color();
                            g.setColor((c>>16&255)/255f,(c>>8&255)/255f,(c&255)/255f,1);
                            tc4revival.client.LegacyGui.blit(g,appearance.texture(),(int)cursor[0],y,9,9,0,0,16,16,16,16);g.setColor(1,1,1,1);
                        });
                    cursor[0]+=font.getSplitter().stringWidth(net.minecraft.network.chat.FormattedText.of(new String(Character.toChars(code)),style));return true;
                });
                if(mx>=left+120&&mx<left+250&&my>=y&&my<y+font.lineHeight)
                    hovered=font.getSplitter().componentStyleAtWidth(line,mx-left-120);
            }}finally{g.disableScissor();}
            if(lines.size()>shown)g.drawString(font,Component.translatable("ti.selection.scroll"),left+120,top+114,0x51452f,false);
            if(hovered!=null&&hovered.getHoverEvent()!=null){
                var tip=hovered.getHoverEvent().getValue(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT);
                if(tip!=null)g.renderTooltip(font,tip,mx,my);
            }
        }
        @Override public boolean mouseScrolled(double mx,double my,double amount){
            if(!selected.isEmpty()&&mx>=left+110&&mx<left+260&&my>=top-20&&my<top+130){
                int max=Math.max(0,font.split(description(),130).size()-13);
                textScroll=Math.max(0,Math.min(max,textScroll-(int)Math.signum(amount)*3));return true;
            }
            return super.mouseScrolled(mx,my,amount);
        }
        public void render(GuiGraphics g,int mx,int my,float delta){renderBackground(g);tc4revival.client.LegacyGui.blit(g,PANEL,left,top,0,0,105,113,256,256);if(!selected.isEmpty())description(g,mx,my);
            int rows=(int)Math.ceil(aspects.size()/4d);for(int col=0;col<4;col++)for(int row=0;row<4;row++){int index=col*rows+page*4+row;if(index>=aspects.size()||page*4+row>=rows)continue;var a=aspects.get(index);int x=left+14+col*20,y=top+14+row*20;icon(g,a,x,y,a.tag().equals(selected)||mx>=x&&mx<x+16&&my>=y&&my<y+16);}
            for(int col=0;col<4;col++)for(int row=0;row<4;row++){int index=col*rows+page*4+row,x=left+14+col*20,y=top+14+row*20;if(index<aspects.size()&&page*4+row<rows&&mx>=x&&mx<x+16&&my>=y&&my<y+16)g.renderTooltip(font,Component.literal(thaumcraft.api.aspects.AspectViews.display(aspects.get(index)).displayName()),mx,my);}
            if(page>0)tc4revival.client.LegacyGui.blit(g,PANEL,left+28,top+93,191,7,24,8,256,256);if((page+1)*4<rows)tc4revival.client.LegacyGui.blit(g,PANEL,left+52,top+93,215,7,24,8,256,256);
        }
        public boolean mouseClicked(double mx,double my,int button){int rows=(int)Math.ceil(aspects.size()/4d);if(mx>=left+28&&mx<left+76&&my>=top+93&&my<top+101){if(mx<left+52)page=Math.max(0,page-1);else page=Math.min(Math.max(0,(rows-1)/4),page+1);minecraft.player.playSound(tc4revival.content.TCSounds.KEY.get(),.3f,1);return true;}for(int col=0;col<4;col++)for(int row=0;row<4;row++){int index=col*rows+page*4+row,x=left+14+col*20,y=top+14+row*20;if(index<aspects.size()&&page*4+row<rows&&mx>=x&&mx<x+16&&my>=y&&my<y+16){String tag=aspects.get(index).tag();selected=tag.equals(selected)?"":tag;InfusionNetwork.CHANNEL.sendToServer(new InfusionNetwork.Select(hand,selected));return true;}}return super.mouseClicked(mx,my,button);}
    }
}
