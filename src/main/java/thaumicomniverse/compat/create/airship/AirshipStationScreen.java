package thaumicomniverse.compat.create.airship;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.slidingDoor.DoorControl;
import com.simibubi.create.content.trains.station.WideIconButton;
import com.simibubi.create.foundation.gui.*;
import com.simibubi.create.foundation.gui.widget.*;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import thaumicomniverse.compat.create.ThaumicCreateScreen;

/** Airship actions fit inside the centered native Create station / assembly panel. */
public final class AirshipStationScreen extends AbstractSimiScreen implements ThaumicCreateScreen {
    public final BlockPos pos;
    private CompoundTag data;
    private AllGuiTextures background;
    private EditBox name;
    private BlockPos anchor;private Direction facing;
    private int ticks;
    public AirshipStationScreen(BlockPos pos,CompoundTag data){super(Component.translatable("airship.title"));this.pos=pos;this.data=data;read();}
    private void read(){anchor=BlockPos.of(data.getLong("Anchor"));facing=Direction.from3DDataValue(data.getInt("Facing"));}
    private boolean assembly(){return !data.contains("Ship")&&(data.getBoolean("AssemblyAvailable")||data.getBoolean("Assembling")||!data.getBoolean("Confirmed"));}
    public void update(CompoundTag next){
        boolean rebuild=data.getBoolean("AssemblyAvailable")!=next.getBoolean("AssemblyAvailable")||data.getBoolean("Assembling")!=next.getBoolean("Assembling")||data.getBoolean("Confirmed")!=next.getBoolean("Confirmed")||!data.getCompound("Ship").equals(next.getCompound("Ship"));
        data=next;read();if(rebuild)rebuildWidgets();
    }
    private Component text(String key){return Component.translatable("airship."+key);}
    private void send(String action){AirshipNetwork.send(pos,action,new CompoundTag());}
    private void configure(boolean confirm){var n=new CompoundTag();n.putLong("Anchor",anchor.asLong());n.putInt("Facing",facing.get3DDataValue());n.putString("Name",name.getValue());n.putBoolean("Confirm",confirm);AirshipNetwork.send(pos,"configure",n);}
    private IconButton icon(int x,int y,AllIcons art,String tooltip,Runnable action){var button=new IconButton(x,y,art);button.setToolTip(text(tooltip));button.withCallback(action);return addRenderableWidget(button);}
    private WideIconButton wide(int x,int y,AllGuiTextures art,String tooltip,Runnable action){var button=new WideIconButton(x,y,art);button.setToolTip(text(tooltip));button.withCallback(action);return addRenderableWidget(button);}
    private net.minecraft.client.gui.components.Button button(int x,int y,int width,String key,Runnable action){return addRenderableWidget(net.minecraft.client.gui.components.Button.builder(text(key),b->action.run()).bounds(x,y,width,18).build());}
    private void centerName(){int width=Math.max(12,Math.min(background.getWidth()-46,font.width(name.getValue())+10));name.setWidth(width);name.setX(guiLeft+(background.getWidth()-width)/2);}
    @Override protected void init(){
        background=assembly()?AllGuiTextures.STATION_ASSEMBLING:AllGuiTextures.STATION;
        setWindowSize(background.getWidth(),background.getHeight());super.init();clearWidgets();
        int x=guiLeft,y=guiTop,w=background.getWidth(),bottom=y+background.getHeight()-24;
        icon(x+w-33,bottom,AllIcons.I_CONFIRM,"close",this::onClose);
        name=new EditBox(font,x+23,y+4,w-46,10,text("name"));name.setBordered(false);name.setMaxLength(64);name.setTextColor(0x59416B);name.setValue(data.getString("Name"));
        name.setResponder(s->centerName());centerName();addRenderableWidget(name);
        if(!data.getBoolean("Confirmed")){
            for(int axis=0;axis<3;axis++){
                final int a=axis;
                var minus=new IconButton(x+34,y+64+axis*24,(g,ix,iy)->g.fill(ix+4,iy+7,ix+12,iy+9,0xFFFFFFFF));
                minus.setToolTip(text("minus"));minus.withCallback(()->{anchor=anchor.offset(a==0?-1:0,a==1?-1:0,a==2?-1:0);configure(false);});addRenderableWidget(minus);
                icon(x+148,y+64+axis*24,AllIcons.I_ADD,"plus",()->{anchor=anchor.offset(a==0?1:0,a==1?1:0,a==2?1:0);configure(false);});
            }
            icon(x+14,bottom,AllIcons.I_ROTATE_CCW,"anchor.facing",()->{facing=facing.getCounterClockWise();configure(false);});
            button(x+72,bottom,86,"anchor.confirm",()->configure(true));
            return;
        }
        button(x+94,bottom,64,"tab.routes",()->{saveName();minecraft.setScreen(new AirshipScreen(pos,data));});
        var ship=data.getCompound("Ship");boolean present=data.contains("Ship");
        if(assembly()){
            icon(x+14,bottom,AllIcons.I_DISABLE,"assembly.cancel",()->send("assembly_mode")).active=!data.getBoolean("AssemblyAvailable");
            wide(x+87,y+117,AllGuiTextures.I_ASSEMBLE_TRAIN,"assemble",()->send("assemble")).active=data.getBoolean("Station");
        }else{
            if(present){
                boolean aligned=ship.getBoolean("Aligned"),aligning=ship.getBoolean("Aligning");
                wide(x+87,y+65,AllGuiTextures.I_DISASSEMBLE_TRAIN,aligning?"aligning":aligned?"disassemble":"align",()->send(aligned?"disassemble":"align"))
                    .active=!aligning&&(!aligned||!ship.getBoolean("Schedule"));
            }else wide(x+87,y+65,AllGuiTextures.I_NEW_TRAIN,"assembly.begin",()->send("assembly_mode")).active=data.getBoolean("Station");
            var values=DoorControl.values();var mode=values[Math.floorMod(data.getInt("DoorControl"),values.length)];
            var doors=DoorControl.createWidget(x+35,y+102,m->{var n=new CompoundTag();n.putInt("Mode",m.ordinal());AirshipNetwork.send(pos,"doors",n);},mode);
            addRenderableWidget(doors.getFirst());addRenderableWidget(doors.getSecond());
        }
    }
    private void saveName(){if(name==null||name.getValue().equals(data.getString("Name")))return;if(!data.getBoolean("Confirmed")){configure(false);return;}var n=new CompoundTag();n.putString("Name",name.getValue());AirshipNetwork.send(pos,"rename_node",n);}
    @Override public void onClose(){saveName();super.onClose();}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if((key==257||key==335)&&name.isFocused()){saveName();name.setFocused(false);return true;}return super.keyPressed(key,scan,modifiers);}
    @Override public boolean mouseClicked(double x,double y,int button){if(button==0&&x>=guiLeft+10&&x<guiLeft+background.getWidth()-10&&y>=guiTop&&y<guiTop+16){setFocused(name);name.setFocused(true);return true;}return super.mouseClicked(x,y,button);}
    @Override public void tick(){super.tick();name.tick();if(++ticks%20==0)send("refresh");AirshipClient.showAnchor("editor",anchor,facing);}
    @Override protected void renderWindow(GuiGraphics g,int mx,int my,float partial){
        int x=guiLeft,y=guiTop; background.render(g,x,y);
        if(!name.isFocused())AllGuiTextures.STATION_EDIT_NAME.render(g,x+background.getWidth()-25,y+1);
        boolean present=data.contains("Ship");
        if(!data.getBoolean("Confirmed")){
            g.drawCenteredString(font,text("anchor.configure"),x+100,y+45,0xDAC2EF);
            for(int i=0;i<3;i++)g.drawCenteredString(font,"XYZ".charAt(i)+": "+(i==0?anchor.getX():i==1?anchor.getY():anchor.getZ()),x+100,y+69+i*24,0xDAC2EF);
            g.drawCenteredString(font,Component.translatable("airship.direction."+facing.getName()),x+51,y+background.getHeight()-19,0xDAC2EF);
        }else if(assembly()){
            g.drawCenteredString(font,text("assemble"),x+100,y+47,0xDAC2EF);
            g.drawWordWrap(font,text("assembly.help"),x+28,y+67,138,0x7A7A7A);
        }else{
            if(present){
                AllGuiTextures.STATION_TEXTBOX_TOP.render(g,x+21,y+42);
                for(int i=60;i<86;i++)AllGuiTextures.STATION_TEXTBOX_MIDDLE.render(g,x+21,y+i);
                AllGuiTextures.STATION_TEXTBOX_BOTTOM.render(g,x+21,y+86);AllGuiTextures.STATION_TEXTBOX_SPEECH.render(g,x+94,y+38);
                g.drawCenteredString(font,text(data.getCompound("Ship").getBoolean("Aligned")?"docked_aligned":"docked"),x+100,y+47,0xD8C9E5);
                g.renderItem(new ItemStack(AirshipContent.CORE.get()),x+92,y+19);
            }else g.drawCenteredString(font,text("station.idle"),x+100,y+47,0x7A7A7A);
            g.renderItem(AllBlocks.TRAIN_DOOR.asStack(),x+14,y+103);
        }
        g.pose().pushPose();g.pose().translate(x-54,y+background.getHeight()+4,100);g.pose().scale(40,40,40);g.pose().mulPose(com.mojang.math.Axis.XP.rotationDegrees(-22));g.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(63));GuiGameElement.of(AirshipContent.STATION.get().defaultBlockState()).render(g);g.pose().popPose();
        if(!data.getString("Error").isEmpty())g.drawWordWrap(font,Component.translatable(data.getString("Error")),x,y+background.getHeight()+12,200,0xFF9999);
    }
    @Override public boolean isPauseScreen(){return false;}
}
