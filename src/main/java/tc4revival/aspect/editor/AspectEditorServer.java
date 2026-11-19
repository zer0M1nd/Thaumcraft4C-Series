package tc4revival.aspect.editor;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import tc4revival.network.*;

@Mod.EventBusSubscriber(modid="thaumcraft")
public class AspectEditorServer {
    private static final Map<UUID,Session> sessions=new HashMap<>();
    private static boolean reloading;
    public static class Session {
        public UUID token=UUID.randomUUID();
        public long last=System.currentTimeMillis();
        public EditorUpload.Receiver receiver=new EditorUpload.Receiver();
        public int bytes;
        public boolean saving;
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("thaumcraft").then(Commands.literal("aspecteditor")
                .requires(s->s.hasPermission(2)).executes(c->{open(c.getSource().getPlayerOrException());return 1;})));
    }
    public static boolean authorized(ServerPlayer player){return player!=null&&player.hasPermissions(2);}
    public static void open(ServerPlayer player){
        if(!authorized(player))return;
        try{
            var session=new Session();sessions.put(player.getUUID(),session);
            String name=EditorPackStore.nextName(player.server.getWorldPath(LevelResource.DATAPACK_DIR),player.getGameProfile().getName());
            TCNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new AspectEditorOpen(session.token,name));
        }catch(Exception e){player.sendSystemMessage(net.minecraft.network.chat.Component.literal("无法打开要素编辑器："+e.getMessage()));}
    }
    public static void save(ServerPlayer player,AspectEditorSave frame){
        if(!authorized(player))return;
        var session=sessions.get(player.getUUID());
        if(session==null||!session.token.equals(frame.token()))return;
        if(session.saving)return;
        try{
            long now=System.currentTimeMillis();
            if(frame.index()>0&&now-session.last>30000)throw new IllegalArgumentException("上传超时，请重新保存");
            if(reloading)throw new IllegalArgumentException("另一位管理员正在保存，请稍后重试");
            if(frame.count()<1||frame.count()>192||frame.bytes().length>24576)throw new IllegalArgumentException("编辑内容过大");
            if(frame.index()==0){session.receiver=new EditorUpload.Receiver();session.bytes=0;}
            session.last=now;session.bytes+=frame.bytes().length;
            if(session.bytes>4*1024*1024)throw new IllegalArgumentException("编辑内容超过 4 MiB");
            var complete=session.receiver.accept(frame.index(),frame.count(),frame.bytes());
            if(complete.isEmpty())return;
            var json=JsonParser.parseString(complete.get()).getAsJsonObject();
            var edits=EditorDefinitions.decode(json.get("data"));EditorResources.validate(edits);
            if(edits.entries().isEmpty())throw new IllegalArgumentException("没有需要保存的修改");
            String name=json.get("name").getAsString();if(name.length()>64)throw new IllegalArgumentException("数据包名称过长");
            var server=player.server;var root=server.getWorldPath(LevelResource.DATAPACK_DIR);
            var saved=EditorPackStore.write(root,name,player.getGameProfile().getName(),edits,EditorResources.server().order());
            var old=new ArrayList<>(server.getPackRepository().getSelectedIds());
            server.getPackRepository().reload();
            var selected=new ArrayList<>(old);selected.add(saved.packId());session.saving=true;reloading=true;
            server.reloadResources(selected).whenComplete((unused,error)->server.execute(()->{
                if(error==null){
                    session.saving=false;reloading=false;
                    try{reply(player,session,true,"已保存并生效："+saved.displayName(),EditorPackStore.nextName(root,player.getGameProfile().getName()));}
                    catch(Exception e){reply(player,session,true,"已保存并生效："+saved.displayName(),"");}
                }else{
                    tc4revival.TC4Revival.LOGGER.error("Aspect editor datapack reload failed",error);
                    server.reloadResources(old).whenComplete((u,rollback)->server.execute(()->{
                        reloading=false;session.saving=false;
                        reply(player,session,false,"数据包已写入，但重载失败；请检查服务端日志。草稿保留。","");
                    }));
                }
            }));
        }catch(Exception e){if(session.saving){session.saving=false;reloading=false;}session.receiver=new EditorUpload.Receiver();session.bytes=0;reply(player,session,false,"保存失败："+e.getMessage(),"");}
    }
    private static void reply(ServerPlayer player,Session session,boolean ok,String message,String next){
        if(message.length()>1000)message=message.substring(0,1000);
        TCNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new AspectEditorResult(session.token,ok,message,next));
    }
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){sessions.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent e){sessions.clear();reloading=false;}
}
