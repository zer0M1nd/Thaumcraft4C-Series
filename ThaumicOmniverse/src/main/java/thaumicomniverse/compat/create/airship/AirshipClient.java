package thaumicomniverse.compat.create.airship;

import java.util.*;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class AirshipClient {
    private static final Map<Integer,CompoundTag> statuses=new HashMap<>();
    private record DisplayCurve(String key,FlightCurve curve,int color,boolean emphasized){}
    private static final Map<Integer,List<DisplayCurve>> drivingCurves=new HashMap<>();
    private static final net.createmod.catnip.animation.LerpedFloat displayedSpeed=net.createmod.catnip.animation.LerpedFloat.linear();
    private static final net.createmod.catnip.animation.LerpedFloat promptSize=net.createmod.catnip.animation.LerpedFloat.linear();
    private static boolean showRoutes;
    private static int drivingEntity=-1;
    private static Component prompt;
    private static AirshipNetwork.ResumeControls pendingControls;
    private static int pendingControlTicks;
    public static void resumeControls(AirshipNetwork.ResumeControls packet){pendingControls=packet;pendingControlTicks=200;}
    private static void resumePendingControls(Minecraft mc){
        if(pendingControls==null)return;
        if(mc.getConnection()==null||--pendingControlTicks<=0){pendingControls=null;return;}
        if(mc.level==null||mc.player==null||!mc.level.dimension().location().equals(pendingControls.dimension()))return;
        if(!(mc.level.getEntity(pendingControls.entity()) instanceof AirshipEntity ship)||ship.isRemoved()
            ||!ship.getUUID().equals(pendingControls.ship())||ship.getContraption()==null)return;
        var info=ship.getContraption().getBlocks().get(pendingControls.local());
        if(info==null||!info.state().is(AirshipContent.CONTROLS.get())){pendingControls=null;return;}
        // The destination spawn / contraption data may arrive after the dimension change.
        ControlsHandler.currentlyPressed.clear();
        ControlsHandler.startControlling(ship,pendingControls.local());pendingControls=null;
    }
    private static final List<net.createmod.catnip.data.IntAttached<String>> scheduleStations=new ArrayList<>();
    public static List<net.createmod.catnip.data.IntAttached<String>> scheduleStations(){return List.copyOf(scheduleStations);}
    public static void scheduleStations(CompoundTag data){scheduleStations.clear();if(data!=null)for(var n:data.getList("Stations",Tag.TAG_COMPOUND)){var tag=(CompoundTag)n;scheduleStations.add(net.createmod.catnip.data.IntAttached.with(tag.getInt("Distance"),tag.getString("Name")));}}
    private record Anchor(BlockPos pos,net.minecraft.core.Direction facing){}
    private static Anchor highlight;private static int highlightTicks;
    private static final List<FlightGraph.Route> highlightedRoutes=new ArrayList<>();
    private static final Map<UUID,Anchor> pendingAnchors=new HashMap<>();
    public static void register(IEventBus bus){
        bus.addListener((net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers e)->e.registerEntityRenderer(AirshipContent.ENTITY.get(),com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer::new));
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e)->e.enqueueWork(()->dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer.builder(AirshipContent.ENTITY.get()).factory(com.simibubi.create.content.contraptions.render.ContraptionVisual::new).neverSkipVanillaRender().apply()));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(AirshipClient::tick);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(AirshipClient::hud);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(AirshipClient::key);
    }
    private static void key(net.minecraftforge.client.event.InputEvent.Key e){
        if(e.getKey()==org.lwjgl.glfw.GLFW.GLFW_KEY_TAB&&e.getAction()==org.lwjgl.glfw.GLFW.GLFW_PRESS&&Minecraft.getInstance().screen==null&&ControlsHandler.getContraption() instanceof AirshipEntity)showRoutes=!showRoutes;
    }
    public static void view(AirshipNetwork.View p){
        if(p.data()==null)return;var mc=Minecraft.getInstance();
        if(p.highlight()){
            highlight=new Anchor(BlockPos.of(p.data().getLong("Anchor")),net.minecraft.core.Direction.from3DDataValue(p.data().getInt("Facing")));highlightTicks=120;
            highlightedRoutes.clear();var id=p.data().getUUID("NodeId");
            for(var n:p.data().getList("Routes",Tag.TAG_COMPOUND)){var r=FlightData.route((CompoundTag)n);if(id.equals(r.from())||id.equals(r.to()))highlightedRoutes.add(r);}return;
        }
        if(mc.screen instanceof AirshipStationScreen screen&&screen.pos.equals(p.pos()))screen.update(p.data());
        else if(mc.screen instanceof AirshipScreen screen&&screen.pos.equals(p.pos()))screen.update(p.data());
        else if(p.open())mc.setScreen(new AirshipStationScreen(p.pos(),p.data()));
    }
    public static void status(AirshipNetwork.Status p){if(p.data()!=null){
        if(p.data().contains("DisplayRoutes")){
            var curves=new ArrayList<DisplayCurve>();
            for(var n:p.data().getList("DisplayRoutes",Tag.TAG_COMPOUND)){var route=FlightData.route((CompoundTag)n);curves.add(new DisplayCurve("driving:"+route.id(),route.curve(),route.color(),((CompoundTag)n).getBoolean("Emphasized")));}
            for(var n:p.data().getList("DisplayBends",Tag.TAG_COMPOUND)){var bend=FlightData.bend((CompoundTag)n);curves.add(new DisplayCurve("driving:"+bend.id(),bend.curve(),bend.color(),((CompoundTag)n).getBoolean("Emphasized")));}
            drivingCurves.put(p.entity(),List.copyOf(curves));
        }
        statuses.put(p.entity(),p.data());
    }}
    /** Shift retains its vertical-flight binding in free mode. */
    public static boolean shiftStopsControlling(AirshipEntity ship){
        var status=statuses.get(ship.getId());return status!=null&&!status.getBoolean("Free");
    }
    public static void pose(AirshipNetwork.Pose p){var level=Minecraft.getInstance().level;if(level!=null&&level.getEntity(p.entity()) instanceof AirshipEntity ship)ship.receivePose(new FlightPoseTimeline.Pose(p.tick(),p.position(),p.yaw(),p.speed()));}
    private static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();resumePendingControls(mc);if(mc.level==null){statuses.clear();drivingCurves.clear();scheduleStations.clear();pendingAnchors.clear();highlight=null;drivingEntity=-1;showRoutes=false;return;}
        if(ControlsHandler.getContraption() instanceof AirshipEntity ship){
            if(drivingEntity!=ship.getId()){drivingEntity=ship.getId();showRoutes=false;displayedSpeed.startWithValue(0);promptSize.startWithValue(0);}
            var status=statuses.get(ship.getId());if(status!=null){
                double value=net.minecraft.util.Mth.clamp(ship.displayedSpeed()*20/Math.max(.1,status.getDouble("MaxSpeed"))+.05,0,1);
                displayedSpeed.chase((int)(value*18)/18d,.5,net.createmod.catnip.animation.LerpedFloat.Chaser.EXP);displayedSpeed.tickChaser();
                prompt=prompt(status);promptSize.chase(prompt==null?0:mc.font.width(prompt)+17,.5,net.createmod.catnip.animation.LerpedFloat.Chaser.EXP);promptSize.tickChaser();
                if(showRoutes)for(var curve:drivingCurves.getOrDefault(ship.getId(),List.of()))showCurve(curve.key,curve.curve,curve.color,curve.emphasized);
            }
        }else {drivingEntity=-1;showRoutes=false;prompt=null;}
        if(highlight!=null&&highlightTicks-->0){
            showAnchor("highlight",highlight.pos,highlight.facing);
            for(var route:highlightedRoutes){var curve=route.curve();var previous=curve.start;int segments=Math.max(32,Math.min(2048,(int)Math.ceil(curve.length()*2)));
                for(int i=1;i<=segments;i++){var next=curve.position(curve.length()*i/segments);line("route:"+route.id()+":"+i,previous,next,net.minecraft.world.item.DyeColor.byId(route.color()).getTextColor(),.045f);previous=next;}
            }
        }
        if(mc.player!=null&&mc.level.getGameTime()%20==0){
            pendingAnchors.clear();
            // Unconfirmed anchors stay visible in the world, even after closing configuration.
            var at=mc.player.blockPosition();for(var p:BlockPos.betweenClosed(at.offset(-8,-8,-8),at.offset(8,8,8)))if(mc.level.getBlockEntity(p) instanceof AirshipBlocks.NodeTile tile&&!tile.confirmed)
                pendingAnchors.put(tile.nodeId,new Anchor(tile.anchor,tile.facing));
            statuses.keySet().removeIf(id->mc.level.getEntity(id)==null);
            drivingCurves.keySet().removeIf(id->mc.level.getEntity(id)==null);
        }
        pendingAnchors.forEach((id,a)->showAnchor(id,a.pos,a.facing));
    }
    private static void line(Object key,net.minecraft.world.phys.Vec3 a,net.minecraft.world.phys.Vec3 b,int color,float width){
        net.createmod.catnip.outliner.Outliner.getInstance().showLine(key,a,b).colored(color).lineWidth(width);
    }
    private static void showCurve(String key,FlightCurve curve,int color,boolean emphasized){
        int rgb=net.minecraft.world.item.DyeColor.byId(color).getTextColor();if(!emphasized)rgb=((rgb>>16&255)/4<<16)|((rgb>>8&255)/4<<8)|(rgb&255)/4;
        var previous=curve.start;int segments=Math.max(32,Math.min(2048,(int)Math.ceil(curve.length()*2)));
        for(int i=1;i<=segments;i++){var next=curve.position(curve.length()*i/segments);line(key+":"+i,previous,next,rgb,emphasized?.065f:.025f);previous=next;}
    }
    public static void showAnchor(Object key,BlockPos pos,net.minecraft.core.Direction facing){
        var out=net.createmod.catnip.outliner.Outliner.getInstance();out.showAABB("airship:"+key,new AABB(pos)).colored(0xB18AE8).lineWidth(.06f);
        var direction=net.minecraft.world.phys.Vec3.atLowerCornerOf(facing.getNormal());var side=new net.minecraft.world.phys.Vec3(-direction.z,0,direction.x);
        var center=pos.getCenter().add(0,.58,0);var tip=center.add(direction.scale(.8));
        line("airship:"+key+":shaft",center.subtract(direction.scale(.4)),tip,0xF5D778,.07f);
        line("airship:"+key+":left",tip,tip.subtract(direction.scale(.35)).add(side.scale(.25)),0xF5D778,.07f);
        line("airship:"+key+":right",tip,tip.subtract(direction.scale(.35)).subtract(side.scale(.25)),0xF5D778,.07f);
    }
    private static void hud(net.minecraftforge.client.event.RenderGuiEvent.Post e){
        if(!(ControlsHandler.getContraption() instanceof AirshipEntity entity))return;
        var status=statuses.get(entity.getId());if(status==null)return;var mc=Minecraft.getInstance();if(mc.options.hideGui)return;var g=e.getGuiGraphics();
        nativeHud(g,status);
        int x=12,y=32,w=240;var choices=status.getList("Choices",Tag.TAG_COMPOUND);int rows=Math.min(12,choices.size());
        g.fill(x-5,y-6,x+w,y+30+rows*12,0xD8201830);
        g.drawString(mc.font,Component.translatable(status.getBoolean("Free")?"airship.mode.free":status.getBoolean("Locked")?"airship.hud.locked":"airship.hud.choose"),x,y,0xBFA4DF);y+=14;
        int selected=0;for(int i=0;i<choices.size();i++)if(choices.getCompound(i).getBoolean("Selected"))selected=i;
        int start=Math.max(0,Math.min(selected-rows/2,choices.size()-rows));
        for(int i=start;i<Math.min(choices.size(),start+rows);i++){var r=choices.getCompound(i);String text=(r.getBoolean("Selected")?"> ":"  ")+choiceLabel(r);g.drawString(mc.font,mc.font.plainSubstrByWidth(text,w-5),x,y,r.getBoolean("Disabled")?0x5D5668:r.getBoolean("Selected")?0xFFFFFF:0xAAA1B8);y+=12;}
        if(!status.getString("Message").isEmpty())g.drawString(mc.font,Component.translatable(status.getString("Message")),x,y+2,0xF3C389);
    }
    private static String enterLabel(String key,String destination){
        if(destination.isEmpty()&&key.equals("airship.choice.portal"))destination=Component.translatable("airship.portal.unpaired").getString();
        return Component.translatable(key).getString()+(destination.isEmpty()?"":" → "+destination);
    }
    private static String choiceLabel(CompoundTag choice){
        if(choice.getBoolean("Docking"))return enterLabel(choice.getString("LabelKey"),choice.getString("Destination"));
        String name=choice.getString("Name");if(name.isEmpty())name=Component.translatable("airship.route.default_name",choice.getInt("Sequence")).getString();
        return (choice.getBoolean("UTurn")?Component.translatable("airship.choice.uturn").getString()+" ":"")+choice.getInt("Sequence")+". "+name+" → "+choice.getString("Destination");
    }
    private static void nativeHud(net.minecraft.client.gui.GuiGraphics g,CompoundTag status){
        var mc=Minecraft.getInstance();int width=mc.getWindow().getGuiScaledWidth(),height=mc.getWindow().getGuiScaledHeight();
        var frame=com.simibubi.create.foundation.gui.AllGuiTextures.TRAIN_HUD_FRAME;
        var bg=com.simibubi.create.foundation.gui.AllGuiTextures.TRAIN_HUD_SPEED_BG;
        var speed=com.simibubi.create.foundation.gui.AllGuiTextures.TRAIN_HUD_SPEED;
        int x=width/2-91,y=height-29;frame.render(g,x-2,y+1);bg.render(g,x,y);
        int filled=(int)(speed.getWidth()*displayedSpeed.getValue(mc.getFrameTime()));
        g.blit(speed.location,x,y,speed.getStartX(),speed.getStartY(),filled,speed.getHeight());
        int size=(int)promptSize.getValue(mc.getFrameTime());if(size<2)return;
        int left=width/2-size/2,top=height-56;
        g.pose().pushPose();g.pose().translate(0,0,100);
        g.fill(left-3,top,left+size+3,top+17,0xFF382547);
        g.fill(left-2,top+1,left+size+2,top+16,0xFF795399);
        g.fill(left,top+2,left+size,top+15,0xFF503563);
        if(prompt!=null&&mc.font.width(prompt)<size-10)g.drawString(mc.font,prompt,width/2-mc.font.width(prompt)/2,top+4,0xF0DFFF,false);g.pose().popPose();
    }
    private static Component prompt(CompoundTag status){
        var mc=Minecraft.getInstance();
        if(status.getInt("ModeTicks")>0){
            boolean targetFree=status.getBoolean("ModeLatched")?status.getBoolean("Free"):!status.getBoolean("Free");
            int filled=Math.min(30,status.getInt("ModeTicks")*30/40);
            return Component.translatable(targetFree?"airship.hud.switch_free":"airship.hud.switch_route").append(" ")
                .append(Component.literal("|".repeat(filled)).withStyle(s->s.withColor(0xE7BDFF)))
                .append(Component.literal("|".repeat(30-filled)).withStyle(s->s.withColor(0x9B7CAC)));
        }
        if(status.getBoolean("Free"))return null;
        if(status.getBoolean("Detached")||status.getBoolean("Rejoining"))return Component.translatable("airship.hud.return",mc.options.keyJump.getTranslatedKeyMessage());
        if(status.getBoolean("Dock")&&status.getBoolean("DockHeld")&&status.getDouble("DockTotal")>0){
            double f=status.getDouble("DockRemaining")/status.getDouble("DockTotal");
            int remaining=(int)(net.minecraft.util.Mth.clamp(1-(1-f)*(1-f),0,1)*30);
            int mixed=net.createmod.catnip.theme.Color.mixColors(0xE7BDFF,0xB881E0,remaining/30f);
            return Component.literal("|".repeat(30-remaining)).withStyle(s->s.withColor(mixed)).append(Component.literal("|".repeat(remaining)).withStyle(s->s.withColor(remaining==0?0xE7BDFF:0x9B7CAC)));
        }
        return status.getBoolean("Approach")?Component.translatable("airship.hud.enter",mc.options.keyJump.getTranslatedKeyMessage(),enterLabel(status.getString("EnterKey"),status.getString("EnterDestination"))):null;
    }

}
