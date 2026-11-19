package thmodern.lens;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.research.*;
import thmodern.Horizons;
@Mod.EventBusSubscriber(modid=Horizons.ID)
public final class ScholarLens {
    private record Identity(Object dimension,Object continuity){}
    private static final Map<ServerPlayer,LensScanProgress> PROGRESS=new IdentityHashMap<>();
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.PlayerTickEvent event){if(event.phase==net.minecraftforge.event.TickEvent.Phase.END&&event.player instanceof ServerPlayer player)tick(player);}
    public static void tick(ServerPlayer player){
        if(!player.isAlive()||player.isSpectator()||!LensEquipment.active(player).equals("order_entropy")){if(PROGRESS.remove(player)!=null)LensNetwork.view(player,new LensNetwork.View(Component.empty(),Map.of(),0));return;}
        var timer=PROGRESS.computeIfAbsent(player,p->new LensScanProgress());var selection=ScanTargetSelector.select(player).orElse(null);long time=player.level().getGameTime();
        if(selection==null){timer.clear();if(time%5==0)LensNetwork.view(player,new LensNetwork.View(Component.empty(),Map.of(),0));return;}
        var store=KnowledgeSavedData.get(player.server);store.ensure(player.getUUID(),player.getRandom()::nextInt);var knowledge=store.snapshot(player.getUUID()).orElseThrow();var target=selection.target();
        boolean unknown=target.canTrack(knowledge,PlayerKnowledge.ScanMode.THAUMOMETER);
        if(timer.observe(unknown?new Identity(player.level().dimension(),target.continuity()):null,time)){
            var result=store.update(player.getUUID(),player.getRandom()::nextInt,next->target.completeWithClue(next,tc4revival.aspect.TCAspects.catalog(),PlayerKnowledge.ScanMode.THAUMOMETER,tc4revival.config.TCConfig.SERVER.aspectPoolSoftCap.get(),ResearchCatalogs.rules(),player.level().random::nextInt));
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new thaumcraft.api.research.ScanEvent(player,target,result.scan()));
            if(player.server.getPlayerList().getPlayer(player.getUUID())==player){tc4revival.network.TCNetwork.send(player,tc4revival.network.ScanFeedbackPacket.from(result.scan()));result.clue().ifPresent(key->tc4revival.network.TCNetwork.send(player,new tc4revival.network.CluePacket(key)));}
            knowledge=store.snapshot(player.getUUID()).orElseThrow();unknown=target.canTrack(knowledge,PlayerKnowledge.ScanMode.THAUMOMETER);
        }
        if(time%5==0){
            var aspects=new LinkedHashMap<String,Integer>();var values=selection.node().map(tc4revival.content.NodeJarData::aspects).orElseGet(target::aspects);
            if(!unknown)for(var aspect:values.aspects())if(knowledge.knows(aspect)&&aspects.size()<128)aspects.put(aspect.tag(),values.amount(aspect));
            var name=selection.name().copy();selection.node().ifPresent(node->{name.append(" (").append(Component.translatable("nodetype."+node.type().name()+".name"));if(node.modifier()!=null)name.append(", ").append(Component.translatable("nodemod."+node.modifier().name()+".name"));name.append(")");});
            LensNetwork.view(player,new LensNetwork.View(name,aspects,timer.progress()));
        }
        if(unknown&&timer.progress()>0&&timer.progress()%20==0)player.level().playSound(null,player.blockPosition(),tc4revival.content.TCSounds.CAMERA_TICKS.get(),net.minecraft.sounds.SoundSource.PLAYERS,.2f,.5f);
    }
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){PROGRESS.remove(event.getEntity());}
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){PROGRESS.clear();}
}
