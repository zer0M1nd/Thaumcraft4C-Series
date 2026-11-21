package thaumicomniverse.compat.create.airship;

import java.util.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Station workflow and a terrain-aware 3D route editor. All edits are explicit server transactions. */
public final class AirshipScreen extends Screen {
    public final BlockPos pos;
    private CompoundTag data;
    private final Map<UUID,FlightGraph.Node> nodes=new LinkedHashMap<>();
    private final Map<UUID,FlightGraph.Route> routes=new LinkedHashMap<>();
    private int nodeScroll,routeScroll;
    private final Map<UUID,FlightBend> bends=new LinkedHashMap<>();
    private UUID selectedNode,selectedRoute,secondRoute,selectedBend;
    private BlockPos candidate;private Direction facing;
    private final List<Vec3> controls=new ArrayList<>();
    private FlightCurve preview;
    private int color,dragging=-1;
    private EditBox name;
    private final AirshipTerrain terrain;
    private static int terrainPrecision=4;
    private double zoom=1.5,viewYaw=.7,viewPitch=.6,panX,panZ;
    private boolean cameraDragging,lockEndDirections=true;
    private double bendEntry,bendExit;
    private int draggingEnd=-1;
    private float uiScale=1;
    private long viewRevision;
    private int dragAxis;
    private String error="";
    public AirshipScreen(BlockPos p,CompoundTag tag){super(Component.translatable("airship.title"));pos=p;data=tag;candidate=BlockPos.of(tag.getLong("Anchor"));facing=Direction.from3DDataValue(tag.getInt("Facing"));terrain=new AirshipTerrain(p);readGraph();}
    public void update(CompoundTag tag){data=tag;error=tag.getString("Error");candidate=BlockPos.of(tag.getLong("Anchor"));facing=Direction.from3DDataValue(tag.getInt("Facing"));viewRevision++;readGraph();loadSelection();rebuildWidgets();}
    private void readGraph(){
        nodes.clear();routes.clear();bends.clear();
        for(var n:data.getList("Nodes",Tag.TAG_COMPOUND)){var node=FlightData.node((CompoundTag)n);nodes.put(node.id(),node);}
        for(var n:data.getList("Routes",Tag.TAG_COMPOUND)){var route=FlightData.route((CompoundTag)n);routes.put(route.id(),route);}
        for(var n:data.getList("Bends",Tag.TAG_COMPOUND)){var bend=FlightData.bend((CompoundTag)n);bends.put(bend.id(),bend);}
        if(selectedNode==null||!nodes.containsKey(selectedNode))selectedNode=data.getUUID("NodeId");
        if(selectedRoute!=null&&!routes.containsKey(selectedRoute)){selectedRoute=secondRoute=selectedBend=null;}
        if(secondRoute!=null&&!routes.containsKey(secondRoute)){secondRoute=selectedBend=null;}
        if(selectedBend!=null&&!bends.containsKey(selectedBend))selectedBend=null;
    }
    private boolean related(FlightGraph.Route route){var id=data.getUUID("NodeId");return route.from().equals(id)||route.to().equals(id);}
    private List<FlightBend> pairBends(){return secondRoute==null?List.of():bends.values().stream().filter(b->b.pair(selectedRoute,secondRoute)).toList();}
    private FlightCurve selectedCurve(){var b=bends.get(selectedBend);if(b!=null)return b.curve();var r=secondRoute==null?routes.get(selectedRoute):null;return r==null?null:r.curve();}
    private void loadSelection(){var curve=selectedCurve();controls.clear();if(curve!=null)controls.addAll(curve.controls);preview=null;var b=bends.get(selectedBend);var r=routes.get(selectedRoute);color=b!=null?b.color():r!=null?r.color():0;lockEndDirections=b!=null?b.lockEndDirections():r==null||r.lockEndDirections();bendEntry=b==null?0:b.entry();bendExit=b==null?0:b.exit();dragging=draggingEnd=-1;constrain();}
    private FlightCurve preview(){
        var original=selectedCurve();if(original==null)return null;
        if(preview==null){var b=bends.get(selectedBend);preview=new FlightCurve(b!=null&&b.passing()?routes.get(b.incoming()).leaving(b.node()).position(bendEntry):original.start,
            controls,b!=null?routes.get(b.outgoing()).leaving(b.node()).position(bendExit):original.end);}return preview;
    }
    private Vec3 endDirection(boolean start){
        var b=bends.get(selectedBend);
        if(b!=null)return start?routes.get(b.incoming()).leaving(b.node()).tangent(bendEntry).scale(-1):routes.get(b.outgoing()).leaving(b.node()).tangent(bendExit);
        var r=routes.get(selectedRoute);return r==null?null:FlightEditing.direction(nodes.get(start?r.from():r.to()));
    }
    private void constrain(){
        preview=null;var c=preview();if(c==null||!lockEndDirections)return;
        var b=bends.get(selectedBend);if(b!=null&&b.pivot())return;
        preview=b==null?FlightEditing.alignAxes(c,endDirection(true),endDirection(false)):FlightEditing.align(c,endDirection(true),endDirection(false));controls.clear();controls.addAll(preview.controls);
    }
    private void moveEnd(int which,double distance){
        var before=preview();var b=bends.get(selectedBend);if(b==null||b.pivot()||which==0&&!b.passing())return;
        if(which==0)bendEntry=distance;else bendExit=distance;preview=null;var after=preview();
        if(!controls.isEmpty()){int index=which==0?0:controls.size()-1;var delta=which==0?after.start.subtract(before.start):after.end.subtract(before.end);controls.set(index,controls.get(index).add(delta));}constrain();
    }
    private double nearestDistance(FlightCurve curve,double x,double y){
        double best=Double.POSITIVE_INFINITY,result=0;var previous=project(curve.start);
        for(int i=1;i<=512;i++){var next=project(curve.position(curve.length()*i/512d));double dx=next[0]-previous[0],dy=next[1]-previous[1];double t=Math.max(0,Math.min(1,((x-previous[0])*dx+(y-previous[1])*dy)/Math.max(1e-12,dx*dx+dy*dy)));double d=Math.hypot(x-previous[0]-t*dx,y-previous[1]-t*dy);if(d<best){best=d;result=curve.length()*(i-1+t)/512d;}previous=next;}return result;
    }
    private void rebuildDraft(){String draft=name==null?"":name.getValue();rebuildWidgets();name.setValue(draft);}
    private void back(){if(selectedBend!=null)selectedBend=null;else if(secondRoute!=null)secondRoute=null;else selectedRoute=null;loadSelection();rebuildWidgets();}
    private Component tr(String key){return Component.translatable("airship."+key);}
    private Button button(int x,int y,int w,String key,Runnable action){return addRenderableWidget(Button.builder(tr(key),b->action.run()).bounds(x,y,w,20).build());}
    private void send(String action,CompoundTag tag){AirshipNetwork.send(pos,action,tag);}
    private void send(String action){send(action,new CompoundTag());}
    @Override protected void init(){
        int actualWidth=minecraft.getWindow().getGuiScaledWidth(),actualHeight=minecraft.getWindow().getGuiScaledHeight();
        uiScale=Math.min(1,Math.min(actualWidth/640f,actualHeight/360f));width=Math.round(actualWidth/uiScale);height=Math.round(actualHeight/uiScale);
        viewRevision++;
        button(12,8,120,"tab.station",()->minecraft.setScreen(new AirshipStationScreen(pos,data)));
        terrain.precision(terrainPrecision);
        addRenderableWidget(Button.builder(terrainLabel(),b->{terrainPrecision=switch(terrainPrecision){case 1->2;case 2->4;case 4->8;case 8->0;default->1;};terrain.precision(terrainPrecision);b.setMessage(terrainLabel());viewRevision++;}).bounds(150,8,170,20).build());
        button(width-66,8,54,"close",this::onClose);
        routeWidgets();
    }
    private Component terrainLabel(){return terrainPrecision==0?tr("terrain.off"):Component.translatable("airship.terrain.precision",terrainPrecision,terrainPrecision,terrainPrecision);}
    private EditBox name(int x,int y,int width,String initial){var box=new EditBox(font,x,y,width,20,tr("name"));box.setMaxLength(64);box.setValue(initial);addRenderableWidget(box);return box;}
    private List<FlightGraph.Route> visibleRoutes(){var current=data.getUUID("NodeId");return routes.values().stream().filter(r->related(r)&&(selectedNode==null||selectedNode.equals(current)||r.from().equals(selectedNode)||r.to().equals(selectedNode))).sorted(Comparator.comparingInt(FlightGraph.Route::sequence)).toList();}
    private String routeName(FlightGraph.Route r){return r.name().isEmpty()?Component.translatable("airship.route.default_name",r.sequence()).getString():r.name();}
    private void selectRoute(UUID id){
        if(!related(routes.get(id)))return;
        if(hasShiftDown()&&selectedRoute!=null){secondRoute=id;selectedBend=null;}
        else{selectedRoute=id;secondRoute=selectedBend=null;}
        loadSelection();rebuildWidgets();
    }
    private String bendName(FlightBend b){
        if(!b.name().isEmpty())return b.name();
        return tr(b.passing()?"bend.passing":"bend.departure").getString()+" "+routes.get(b.incoming()).sequence()+" → "+routes.get(b.outgoing()).sequence();
    }
    private void routeWidgets(){
        int x=width-166;var route=secondRoute==null?routes.get(selectedRoute):null;var bend=bends.get(selectedBend);boolean editable=selectedCurve()!=null;
        name=name(x,55,154,bend!=null?bend.name():route==null?"":route.name());name.active=editable;
        if(editable)name.setHint(Component.literal(bend!=null?bendName(bend):routeName(route)));
        button(x,80,154,"route.add",()->{var n=new CompoundTag();n.putUUID("From",data.getUUID("NodeId"));n.putUUID("To",selectedNode);send("connect",n);}).active=selectedNode!=null&&!selectedNode.equals(data.getUUID("NodeId"))&&secondRoute==null;
        button(x,106,74,"route.delete",()->{var n=new CompoundTag();n.putUUID("Id",selectedRoute);send("delete_route",n);}).active=route!=null;
        button(x+80,106,74,"route.color",()->color=(color+1)%16).active=editable;
        button(x,132,74,"apply",()->{var n=new CompoundTag();n.putUUID("Id",bend!=null?selectedBend:selectedRoute);n.putString("Name",name.getValue());n.putInt("Color",color);n.putBoolean("LockEndDirections",lockEndDirections);if(bend!=null){n.putDouble("Entry",bendEntry);n.putDouble("Exit",bendExit);}n.put("Controls",FlightData.controls(controls));send(bend!=null?"edit_bend":"edit_route",n);}).active=editable;
        button(x+80,132,74,"cancel",()->{loadSelection();rebuildWidgets();}).active=editable;
        button(x,158,74,"control.remove",()->{if(!controls.isEmpty()){controls.remove(controls.size()/2);constrain();rebuildDraft();}}).active=editable&&controls.size()>(lockEndDirections?2:0);
        button(x+80,158,74,"control.add",()->{var elevated=preview().elevated();controls.clear();controls.addAll(elevated.controls);constrain();rebuildDraft();}).active=editable&&controls.size()<16;
        var lock=new Checkbox(x,181,154,20,tr("route.lock_end_directions"),lockEndDirections){@Override public void onPress(){super.onPress();lockEndDirections=selected();constrain();rebuildDraft();}};
        lock.active=editable&&(bend==null||!bend.pivot());addRenderableWidget(lock);
        addRenderableWidget(Button.builder(axisLabel(),b->{dragAxis=(dragAxis+1)%4;b.setMessage(axisLabel());}).bounds(x,height-54,154,20).build()).active=editable;
    }
    private Component axisLabel(){return Component.translatable("airship.route.drag_axis",new String[]{"XZ","Y","X","Z"}[dragAxis]);}
    private List<FlightGraph.Node> visibleNodes(){return nodes.values().stream().sorted(Comparator.comparing(FlightGraph.Node::station).reversed().thenComparing(FlightGraph.Node::name).thenComparing(n->n.id().toString())).toList();}
    private String nodeName(FlightGraph.Node n){return n.name().isEmpty()?n.anchor().toShortString():n.name();}
    private void drawNodes(GuiGraphics g){
        var labels=new ArrayList<int[]>();var ordered=visibleNodes();var selected=nodes.get(selectedNode);
        // Reserve the selected label first; paint its marker last so coincident nodes cannot hide it.
        if(selected!=null){var p=project(selected.point());if(inMap(p[0],p[1]))labels.add(labelBounds(p,nodeName(selected)));}
        for(int pass=0;pass<2;pass++)for(var node:ordered){
            boolean chosen=node.id().equals(selectedNode);if(chosen!=(pass==1))continue;
            var p=project(node.point());int c=chosen?0xFFFFFFFF:node.station()?0xFFD0A4FF:0xFF83DBD4;
            g.fill((int)p[0]-3,(int)p[1]-3,(int)p[0]+4,(int)p[1]+4,c);
            if(!inMap(p[0],p[1]))continue;
            String text=font.plainSubstrByWidth(nodeName(node),110);var box=labelBounds(p,text);
            if(!chosen&&labels.stream().anyMatch(b->box[0]<b[2]&&box[2]>b[0]&&box[1]<b[3]&&box[3]>b[1]))continue;
            if(!chosen)labels.add(box);
            g.fill(box[0]-1,box[1]-1,box[2]+1,box[3]+1,chosen?0xE0261D33:0xA0131621);
            g.drawString(font,text,box[0],box[1],c);
        }
    }
    private int[] labelBounds(double[] point,String text){int w=font.width(font.plainSubstrByWidth(text,110));int x=Math.max(mapX()+1,Math.min(mapX()+mapWidth()-w-2,(int)point[0]+6));int y=Math.max(41,Math.min(height-50,(int)point[1]-4));return new int[]{x,y,x+w,y+9};}
    private double[] project(Vec3 at){
        double dx=at.x-pos.getX()-panX,dz=at.z-pos.getZ()-panZ,dy=at.y-candidate.getY();
        double a=dx*Math.cos(viewYaw)+dz*Math.sin(viewYaw),b=-dx*Math.sin(viewYaw)+dz*Math.cos(viewYaw);
        return new double[]{mapX()+mapWidth()/2d+a*zoom,54+(height-104)/2d+(b*Math.sin(viewPitch)-dy*Math.cos(viewPitch))*zoom,b*Math.cos(viewPitch)+dy*Math.sin(viewPitch)};
    }
    private int mapX(){return 150;}private int mapWidth(){return Math.max(60,width-324);}
    private boolean inMap(double x,double y){return x>=mapX()&&x<mapX()+mapWidth()&&y>=40&&y<height-40;}
    private static void line(GuiGraphics g,double[] a,double[] b,int color,int width){
        double dx=b[0]-a[0],dy=b[1]-a[1];g.pose().pushPose();g.pose().translate(a[0],a[1],0);g.pose().mulPose(com.mojang.math.Axis.ZP.rotation((float)Math.atan2(dy,dx)));g.fill(0,-width/2,(int)Math.ceil(Math.hypot(dx,dy)),(width+1)/2,color);g.pose().popPose();
    }
    private void drawCurve(GuiGraphics g,FlightCurve curve,int color,int width){var previous=project(curve.start);for(int i=1;i<=96;i++){var next=project(curve.position(curve.length()*i/96d));line(g,previous,next,color,width);previous=next;}}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        mx=Math.round(mx/uiScale);my=Math.round(my/uiScale);g.pose().pushPose();g.pose().scale(uiScale,uiScale,1);
        renderBackground(g);g.fill(4,4,width-4,height-4,0xF0261D33);g.fill(5,34,width-5,36,0xFF71518C);
            g.fill(mapX(),40,mapX()+mapWidth(),height-40,0xFF131621);g.enableScissor(Math.round(mapX()*uiScale),Math.round(40*uiScale),Math.round((mapX()+mapWidth())*uiScale),Math.round((height-40)*uiScale));
            terrain.render(g,this::project,viewRevision,mapX(),40,mapX()+mapWidth(),height-40);
            for(var r:routes.values())drawCurve(g,r.curve(),(related(r)?0xFF000000:0x40000000)|DyeColor.byId(r.color()).getTextColor(),r.id().equals(selectedRoute)||r.id().equals(secondRoute)?2:1);
            for(var bend:pairBends())if(!bend.id().equals(selectedBend))drawCurve(g,bend.curve(),0xFF000000|DyeColor.byId(bend.color()).getTextColor(),2);
            var curve=preview();if(curve!=null){
                drawCurve(g,curve,0xFF000000|DyeColor.byId(color).getTextColor(),3);
                var previous=curve.start;for(var point:controls){line(g,project(previous),project(point),0x88C4A1EC,1);previous=point;}line(g,project(previous),project(curve.end),0x88C4A1EC,1);
                for(int i=0;i<controls.size();i++){var q=project(controls.get(i));g.fill((int)q[0]-4,(int)q[1]-4,(int)q[0]+5,(int)q[1]+5,0xFFF6CFFF);g.drawString(font,Integer.toString(i+1),(int)q[0]+6,(int)q[1],0xFFF6CFFF);}
            }
            drawNodes(g);
            var selected=bends.get(selectedBend);if(selected!=null&&!selected.pivot()){
                var c=preview();for(int i=selected.passing()?0:1;i<2;i++){var q=project(i==0?c.start:c.end);g.fill((int)q[0]-5,(int)q[1]-5,(int)q[0]+6,(int)q[1]+6,0xFF66F5D0);g.fill((int)q[0]-2,(int)q[1]-2,(int)q[0]+3,(int)q[1]+3,0xFF163D36);}
            }
            g.disableScissor();
            g.drawString(font,tr("nodes"),12,44,0xD5C1E8);var list=visibleNodes();int count=Math.max(1,(height-110)/22);
            for(int i=nodeScroll;i<Math.min(list.size(),nodeScroll+count);i++){var n=list.get(i);int y=64+(i-nodeScroll)*22;if(n.id().equals(selectedNode))g.fill(8,y-3,144,y+17,0xFF604777);g.drawString(font,font.plainSubstrByWidth(n.name().isEmpty()?n.anchor().toShortString():n.name(),126),12,y,n.station()?0xE3CDFF:0x9CDCD7);}
            int right=width-166,rows=Math.max(1,(height-284)/22);
            if(secondRoute!=null){var listBends=pairBends();for(int i=0;i<listBends.size()&&i<rows;i++){var b=listBends.get(i);int y=230+i*22;if(b.id().equals(selectedBend))g.fill(right-2,y-3,width-10,y+17,0xFF604777);g.drawString(font,font.plainSubstrByWidth(bendName(b),150),right,y,0xFF000000|DyeColor.byId(b.color()).getTextColor());}}
            else {var routeList=visibleRoutes();for(int i=routeScroll;i<Math.min(routeList.size(),routeScroll+rows);i++){var r=routeList.get(i);int y=230+(i-routeScroll)*22;if(r.id().equals(selectedRoute))g.fill(right-2,y-3,width-10,y+17,0xFF604777);g.drawString(font,font.plainSubstrByWidth(routeName(r),150),right,y,0xD8C5ED);}}
            if(selectedCurve()!=null)g.drawString(font,Component.translatable("airship.control.count",controls.size()),right,208,0xD8C5ED);
            else if(secondRoute!=null)g.drawWordWrap(font,tr(pairBends().isEmpty()?"bend.none":"bend.choose"),right,208,150,0xD8C5ED);
            g.drawString(font,tr("map.help"),mapX(),height-32,0xB6A6C9);
            if(curve!=null){
                double radius=curve.minimumRadius();String value=Double.isInfinite(radius)?"∞":String.format(Locale.ROOT,"%.2f",radius);
                g.drawString(font,Component.translatable("airship.curve.metrics",String.format(Locale.ROOT,"%.2f",curve.length()),value),mapX(),height-18,0xE7C7FF);
            }else g.drawString(font,tr("map.drag_help"),mapX(),height-18,0xB6A6C9);
        super.render(g,mx,my,partial);
        if(mx<146&&my>=60&&my<height-40){var tooltipNodes=visibleNodes();int i=nodeScroll+(my-60)/22;if(i<tooltipNodes.size())g.renderTooltip(font,Component.literal(nodeName(tooltipNodes.get(i))),mx,my);}
        if(!error.isEmpty()){var message=Component.translatable(error);int textWidth=Math.min(width-24,font.width(message));g.fill(width/2-textWidth/2-4,37,width/2+textWidth/2+4,51,0xEE261D33);g.drawCenteredString(font,message,width/2,40,0xFF9A9A);}
        g.pose().popPose();
    }
    private double hit(FlightCurve curve,double x,double y){
        double best=Double.POSITIVE_INFINITY;var prev=project(curve.start);
        for(int i=1;i<=128;i++){var next=project(curve.position(curve.length()*i/128d));double dx=next[0]-prev[0],dy=next[1]-prev[1];double t=Math.max(0,Math.min(1,((x-prev[0])*dx+(y-prev[1])*dy)/Math.max(1e-12,dx*dx+dy*dy)));best=Math.min(best,Math.hypot(x-prev[0]-t*dx,y-prev[1]-t*dy));prev=next;}
        return best;
    }
    private void selectNode(UUID id){selectedNode=id;selectedRoute=secondRoute=selectedBend=null;routeScroll=0;loadSelection();rebuildWidgets();}
    @Override public boolean mouseClicked(double x,double y,int button){
        x/=uiScale;y/=uiScale;
        if(button==1){back();return true;}
        if(inMap(x,y)&&button==2){cameraDragging=true;return true;}
        if(button==0){
            if(inMap(x,y)){
                var chosen=bends.get(selectedBend);
                if(chosen!=null&&!chosen.pivot()&&!hasShiftDown()){var c=preview();for(int i=chosen.passing()?0:1;i<2;i++){var q=project(i==0?c.start:c.end);if(Math.hypot(x-q[0],y-q[1])<8){draggingEnd=i;return true;}}}
                if(selectedCurve()!=null&&!hasShiftDown())for(int i=0;i<controls.size();i++){var q=project(controls.get(i));if(Math.hypot(x-q[0],y-q[1])<8){dragging=i;return true;}}
                if(secondRoute!=null){FlightBend nearest=null;double dist=7;for(var b:pairBends()){double d=hit(b.curve(),x,y);if(d<dist){dist=d;nearest=b;}}if(nearest!=null){selectedBend=nearest.id();loadSelection();rebuildWidgets();return true;}}
                FlightGraph.Node node=null;double nodeDist=64;for(var n:nodes.values()){var q=project(n.point());double d=(q[0]-x)*(q[0]-x)+(q[1]-y)*(q[1]-y);if(d<nodeDist){nodeDist=d;node=n;}}
                if(node!=null&&!hasShiftDown()){selectNode(node.id());return true;}
                FlightGraph.Route nearest=null;double dist=7;for(var r:routes.values())if(related(r)){double d=hit(r.curve(),x,y);if(d<dist){dist=d;nearest=r;}}
                if(nearest!=null){selectRoute(nearest.id());return true;}
            }else if(x<146&&y>=60&&y<height-40){var list=visibleNodes();int index=nodeScroll+(int)((y-60)/22);if(index<list.size()){selectNode(list.get(index).id());return true;}}
            else if(x>=width-168&&y>=226&&y<height-60){int index=(int)((y-226)/22);
                if(secondRoute!=null){var list=pairBends();if(index<list.size()){selectedBend=list.get(index).id();loadSelection();rebuildWidgets();return true;}}
                else {var list=visibleRoutes();index+=routeScroll;if(index<list.size()){selectRoute(list.get(index).id());return true;}}
            }
        }return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        x/=uiScale;y/=uiScale;dx/=uiScale;dy/=uiScale;
        if(cameraDragging&&button==2){viewYaw+=dx*.01;viewPitch=Math.max(.15,Math.min(1.4,viewPitch+dy*.01));viewRevision++;return true;}
        if(draggingEnd>=0&&button==0){var b=bends.get(selectedBend);var route=routes.get(draggingEnd==0?b.incoming():b.outgoing()).leaving(b.node());moveEnd(draggingEnd,nearestDistance(route,x,y));return true;}
        if(dragging>=0&&button==0){
            int axis=hasShiftDown()?1:hasControlDown()?2:hasAltDown()?3:dragAxis;
            Vec3 delta;if(axis==1)delta=new Vec3(0,-dy/zoom/Math.cos(viewPitch),0);
            else{double a=dx/zoom,b=dy/zoom/Math.sin(viewPitch);delta=new Vec3(a*Math.cos(viewYaw)-b*Math.sin(viewYaw),0,a*Math.sin(viewYaw)+b*Math.cos(viewYaw));if(axis==2)delta=new Vec3(delta.x,0,0);if(axis==3)delta=new Vec3(0,0,delta.z);}
            if(lockEndDirections&&(dragging==0||dragging==controls.size()-1)){
                boolean start=dragging==0;var direction=endDirection(start);if(direction!=null){var ray=direction.scale(start?1:-1);var endpoint=start?preview().start:preview().end;double length=controls.get(dragging).add(delta).subtract(endpoint).dot(ray);if(selectedBend!=null)length=Math.max(.001,length);else if(Math.abs(length)<.001)length=Math.copySign(.001,length);controls.set(dragging,endpoint.add(ray.scale(length)));}else controls.set(dragging,controls.get(dragging).add(delta));
            }else controls.set(dragging,controls.get(dragging).add(delta));constrain();return true;
        }return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button){dragging=draggingEnd=-1;cameraDragging=false;return super.mouseReleased(x/uiScale,y/uiScale,button);}
    @Override public boolean mouseScrolled(double x,double y,double scroll){
        x/=uiScale;y/=uiScale;
        if(inMap(x,y)){zoom=Math.max(.15,Math.min(16,zoom*Math.pow(1.15,scroll)));viewRevision++;return true;}
            if(x<146){nodeScroll=Math.max(0,Math.min(Math.max(0,nodes.size()-Math.max(1,(height-110)/22)),nodeScroll-(int)scroll));return true;}
            if(x>=width-168){routeScroll=Math.max(0,Math.min(Math.max(0,visibleRoutes().size()-Math.max(1,(height-284)/22)),routeScroll-(int)scroll));return true;}
        return super.mouseScrolled(x,y,scroll);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(!name.isFocused()){double delta=hasShiftDown()?16:4;if(key==GLFW.GLFW_KEY_W){panZ-=delta;viewRevision++;return true;}if(key==GLFW.GLFW_KEY_S){panZ+=delta;viewRevision++;return true;}if(key==GLFW.GLFW_KEY_A){panX-=delta;viewRevision++;return true;}if(key==GLFW.GLFW_KEY_D){panX+=delta;viewRevision++;return true;}}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public void tick(){super.tick();name.tick();terrain.tick();}
    @Override public boolean isPauseScreen(){return false;}
}
