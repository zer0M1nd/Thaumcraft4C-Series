package thaumicomniverse.compat.create;

import java.util.*;
import thaumicomniverse.mixin.*;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.transmission.sequencer.*;
import com.simibubi.create.foundation.gui.*;
import com.simibubi.create.foundation.gui.widget.*;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

/** Create's sequencer widgets and artwork, with extra rows and paging on small windows. */
public final class FocusProgramScreen extends AbstractSimiScreen implements ThaumicCreateScreen {
    private final InteractionHand hand;
    private final Vector<Instruction> instructions;
    private final int limit;
    private int first,visibleRows;
    public FocusProgramScreen(InteractionHand hand,net.minecraft.nbt.ListTag program,int limit){
        super(Component.translatable("item.thaumicomniverse.focus_kinetic"));
        this.hand=hand;this.limit=limit;instructions=Instruction.deserializeAll(program);
    }
    @Override protected void init(){
        visibleRows=Math.min(limit,Math.max(3,(height-66)/22));
        first=Math.min(first,Math.max(0,limit-visibleRows));
        setWindowSize(AllGuiTextures.SEQUENCER.getWidth(),AllGuiTextures.SEQUENCER.getHeight()+(visibleRows-5)*22);
        setWindowOffset(-20,0);super.init();
        for(int row=first;row<Math.min(first+visibleRows,instructions.size());row++)addRow(row);
        var confirm=new IconButton(guiLeft+windowWidth-33,guiTop+windowHeight-24,AllIcons.I_CONFIRM);
        confirm.withCallback(this::onClose);addRenderableWidget(confirm);
        if(visibleRows<limit){
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("▲"),b->{first=Math.max(0,first-1);rebuildWidgets();})
                    .bounds(guiLeft+8,guiTop+windowHeight-24,20,20).build());
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("▼"),b->{first=Math.min(limit-visibleRows,first+1);rebuildWidgets();})
                    .bounds(guiLeft+30,guiTop+windowHeight-24,20,20).build());
        }
    }
    private void addRow(int row){
        int x=guiLeft+30,y=guiTop+20+22*(row-first);
        var instruction=(CreateInstructionAccess)(Object)instructions.get(row);var type=instruction.omniverse$type();var def=(CreateInstructionDefinitionAccess)(Object)type;
        addRenderableWidget(new SelectionScrollInput(x,y,50,18).forOptions(CreateInstructionDefinitionAccess.omniverse$options())
                .setState(type.ordinal()).titled(CreateLang.translateDirect("gui.sequenced_gearshift.instruction"))
                .calling(state->{
                    instruction.omniverse$type(SequencerInstructions.values()[state]);instruction.omniverse$value(((CreateInstructionDefinitionAccess)(Object)instruction.omniverse$type()).omniverse$defaultValue());
                    if(instruction.omniverse$type()==SequencerInstructions.END)while(instructions.size()>row+1)instructions.remove(instructions.size()-1);
                    else if(row+1==instructions.size()&&instructions.size()<limit)instructions.add(new Instruction(SequencerInstructions.END));
                    rebuildWidgets();
                }));
        if(def.omniverse$hasValue()){
            var value=new ScrollInput(x+58,y,28,18).withRange(1,def.omniverse$max()+1)
                    .titled(CreateLang.translateDirect(def.omniverse$parameter())).withShiftStep(def.omniverse$shift())
                    .setState(instruction.omniverse$value()).calling(state->instruction.omniverse$value(state));
            if(type==SequencerInstructions.DELAY)value.withStepFunction(c->{int v=c.currentValue-(c.forward?0:1);return v<20?(c.shift?20:1):(c.shift?100:20);});
            addRenderableWidget(value);
        }
        if(def.omniverse$hasSpeed())addRenderableWidget(new SelectionScrollInput(x+88,y,28,18)
                .forOptions(CreateInstructionSpeedAccess.omniverse$options()).setState(instruction.omniverse$modifier().ordinal())
                .titled(CreateLang.translateDirect("gui.sequenced_gearshift.speed"))
                .calling(state->instruction.omniverse$modifier(InstructionSpeedModifiers.values()[state])));
    }
    @Override protected void renderWindow(GuiGraphics graphics,int mouseX,int mouseY,float partial){
        var bg=AllGuiTextures.SEQUENCER;int extra=(visibleRows-5)*22;
        // Repeat the native row artwork; retain the original header and footer without stretching pixels.
        graphics.enableScissor(guiLeft,guiTop,guiLeft+windowWidth,guiTop+16);
        bg.render(graphics,guiLeft,guiTop);graphics.disableScissor();
        for(int view=0;view<visibleRows;view++){
            int row=first+view,y=guiTop+16+view*22;
            if(row>=instructions.size()){AllGuiTextures.SEQUENCER_EMPTY.render(graphics,guiLeft,y);continue;}
            var instruction=(CreateInstructionAccess)(Object)instructions.get(row);var type=instruction.omniverse$type();var def=(CreateInstructionDefinitionAccess)(Object)type;
            def.omniverse$background().render(graphics,guiLeft,y);
            graphics.drawString(font,Component.literal(Integer.toString(row+1)),guiLeft+12,y+9,0xFFFFEE,false);
            label(graphics,36,y,CreateLang.translateDirect(def.omniverse$title()));
            if(def.omniverse$hasValue()){var value=Component.literal(def.omniverse$format(instruction.omniverse$value()));label(graphics,102-font.width(value)/2,y,value);}
            if(def.omniverse$hasSpeed())label(graphics,127,y,((CreateInstructionSpeedAccess)(Object)instruction.omniverse$modifier()).omniverse$label());
        }
        graphics.enableScissor(guiLeft,guiTop+16+visibleRows*22,guiLeft+windowWidth,guiTop+windowHeight);
        bg.render(graphics,guiLeft,guiTop+extra);graphics.disableScissor();
        graphics.drawCenteredString(font,title,guiLeft+(windowWidth-8)/2,guiTop+4,5841956);
        graphics.drawCenteredString(font,Component.translatable("thaumicomniverse.focus.await_click"),guiLeft+windowWidth/2,guiTop+windowHeight+5,0xD6B6FF);
        GuiGameElement.of(new net.minecraft.world.item.ItemStack(ArcaneCreateContent.KINETIC_FOCUS.get())).scale(4).at(guiLeft+windowWidth+6,guiTop+windowHeight-56,100).render(graphics);
    }
    private void label(GuiGraphics g,int x,int y,Component text){g.drawString(font,text,guiLeft+x,y+9,0xFFFFEE,false);}
    @Override public void removed(){CreateFocusNetwork.save(hand,Instruction.serializeAll(instructions));}
    @Override public boolean isPauseScreen(){return false;}
}
