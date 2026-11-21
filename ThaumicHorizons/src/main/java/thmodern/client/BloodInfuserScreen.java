package thmodern.client;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.aspect.TCAspects;
import thmodern.blood.*;

public class BloodInfuserScreen extends AbstractContainerScreen<BloodInfuserMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumichorizons:textures/gui/guibloodinfuser.png");
    private static final ResourceLocation UNKNOWN=ResourceLocation.parse("thaumcraft:textures/aspects/_unknown.png");
    private int offset,topOut=2,bottomOut=2;
    private String hoveredAspect="";
    public BloodInfuserScreen(BloodInfuserMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=176;imageHeight=219;}
    private List<Integer> known(){var list=new ArrayList<Integer>();for(int i=0;i<BloodInfuserMenu.ASPECTS.size();i++)if(menu.known(i))list.add(i);return list;}
    private List<Integer> selected(){var list=new ArrayList<Integer>();for(int i=0;i<BloodInfuserMenu.ASPECTS.size();i++)for(int j=0;j<menu.selected(i);j++)list.add(i);return list;}
    private BloodFormula.Result preview(){var amounts=new LinkedHashMap<String,Integer>();for(int i=0;i<BloodInfuserMenu.ASPECTS.size();i++)if(menu.selected(i)>0)amounts.put(BloodInfuserMenu.ASPECTS.get(i),menu.selected(i));return BloodInfuserEntity.FORMULA.calculate(amounts,key->TCAspects.catalog().require(key).color());}
    private void icon(GuiGraphics g,int index,int x,int y,boolean dim){
        var aspect=TCAspects.catalog().require(BloodInfuserMenu.ASPECTS.get(index));
        boolean known=menu.known(index);
        int color=!known&&dim?0x404040:aspect.color();
        // TC4 uses a neutral translucent silhouette for missing essentia.
        if(known&&dim)g.setColor(.1f,.1f,.1f,.8f);
        else g.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,known?1:.8f);
        tc4revival.client.LegacyGui.blit(g,known?aspect.texture():UNKNOWN,x,y,0,0,16,16,16,16);
        g.setColor(1,1,1,1);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        var known=known();offset=Math.min(offset,Math.max(0,known.size()-16));
        for(int i=0;i<16&&offset+i<known.size();i++)icon(g,known.get(offset+i),leftPos+14+18*(i/2),topPos+83+(i%2==0?18:0),false);
        var chosen=selected();var used=new HashMap<Integer,Integer>();
        for(int i=0;i<chosen.size();i++){int index=chosen.get(i);int number=used.merge(index,1,Integer::sum);icon(g,index,leftPos+54+17*(i%2),topPos+12+17*(i/2),menu.acquired(index)<number);}
        tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+111,topPos+75,177,offset>0?136:144,24,8);tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+135,topPos+75,201,offset+16<known.size()?136:144,24,8);
        if(menu.mode()>0)tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+38,topPos+(menu.mode()==1?57:70),178,130,6,6);
        hoveredAspect="";for(int i=0;i<16&&offset+i<known.size();i++)if(isHovering(14+18*(i/2),83+(i%2==0?18:0),16,16,mouseX,mouseY))hoveredAspect=BloodInfuserMenu.ASPECTS.get(known.get(offset+i));
        var effects=preview().effects();topOut=Math.max(2,Math.min(38,topOut+(effects.isEmpty()?-2:2)));
        bottomOut=Math.max(2,Math.min(38,bottomOut+(hoveredAspect.isEmpty()?-2:2)));
        tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+171,topPos+25,215-topOut,152,topOut,41);
        tc4revival.client.LegacyGui.blit(g,TEXTURE,leftPos+171,topPos+69,215-bottomOut,194,bottomOut,59);
        if(topOut==38)for(int i=0;i<Math.min(4,effects.size());i++)effectIcon(g,effects.get(i).id(),effects.get(i).amplifier()+1,171+18*(i%2),28+18*(i/2));
        if(bottomOut==38){int i=0;for(var e:BloodInfuserEntity.FORMULA.weights(hoveredAspect).entrySet()){if(i>=6)break;effectIcon(g,e.getKey(),e.getValue(),171+18*(i%2),72+18*(i/2));i++;}}
    }
    private void effectIcon(GuiGraphics g,String id,int amount,int x,int y){var effect=net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.parse(id));if(effect==null)return;
        var instance=new net.minecraft.world.effect.MobEffectInstance(effect,1,0);if(!net.minecraftforge.client.extensions.common.IClientMobEffectExtensions.of(effect).renderGuiIcon(instance,minecraft.gui,g,leftPos+x-3,topPos+y-3,0,1))tc4revival.client.LegacyGui.blit(g,leftPos+x,topPos+y,0,18,18,minecraft.getMobEffectTextures().get(effect));
        g.drawString(font,Integer.toString(amount),leftPos+x,topPos+y,0x808080,false);
    }

    @Override protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY){}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        renderBackground(g);super.render(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);
        var known=known();
        for(int i=0;i<16&&offset+i<known.size();i++)if(isHovering(14+18*(i/2),83+(i%2==0?18:0),16,16,mouseX,mouseY))g.renderTooltip(font,Component.literal(thaumcraft.api.aspects.AspectViews.display(TCAspects.catalog().require(BloodInfuserMenu.ASPECTS.get(known.get(offset+i)))).displayName()),mouseX,mouseY);
        if(isHovering(38,57,10,10,mouseX,mouseY))g.renderTooltip(font,Component.translatable("thaumichorizons.infuser.once"),mouseX,mouseY);
        if(isHovering(38,70,10,10,mouseX,mouseY))g.renderTooltip(font,Component.translatable("thaumichorizons.infuser.continuous"),mouseX,mouseY);
        var effects=preview().effects();
        for(int i=0;i<effects.size();i++)if(topOut==38&&isHovering(171+18*(i%2),28+18*(i/2),18,18,mouseX,mouseY)){
            var definition=effects.get(i);var effect=net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.parse(definition.id()));if(effect==null)continue;
            var lines=new ArrayList<Component>();var stack=new net.minecraft.world.item.ItemStack(thmodern.Horizons.INJECTION.get());
            net.minecraft.world.item.alchemy.PotionUtils.setCustomEffects(stack,List.of(new net.minecraft.world.effect.MobEffectInstance(effect,definition.duration(),definition.amplifier())));
            net.minecraft.world.item.alchemy.PotionUtils.addPotionTooltip(stack,lines,1);g.renderTooltip(font,lines,Optional.empty(),mouseX,mouseY);
        }
    }
    private void button(int value){if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,value);}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0){
            if(isHovering(38,57,10,10,x,y)){button(menu.mode()==1?0:1);return true;}
            if(isHovering(38,70,10,10,x,y)){button(menu.mode()==2?0:2);return true;}
            if(isHovering(111,75,24,8,x,y)){offset=Math.max(0,offset-2);return true;}
            var known=known();if(isHovering(135,75,24,8,x,y)){offset=Math.min(Math.max(0,known.size()-16),offset+2);return true;}
            for(int i=0;i<16&&offset+i<known.size();i++)if(isHovering(14+18*(i/2),83+(i%2==0?18:0),16,16,x,y)){button(3+known.get(offset+i));return true;}
            var selected=selected();for(int i=0;i<selected.size();i++)if(isHovering(54+17*(i%2),12+17*(i/2),16,16,x,y)){button(3+BloodInfuserMenu.ASPECTS.size()+selected.get(i));return true;}
        }
        return super.mouseClicked(x,y,button);
    }
}
