package tc4revival.compat;

import java.lang.reflect.*;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.*;

/** Slot/lifecycle adapter for TC4AutoResearch Reworked 1.0.6; the supplied JAR stays untouched. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public final class AutoResearchCompat {
    private static final Map<MinecraftServer,Map<UUID,Job>> JOBS=new WeakHashMap<>();
    private static final class Job {
        final ResearchTableMenu menu;
        final List<ItemStack> notes;
        final long started=System.currentTimeMillis();
        int index,passed,failed;
        long next;
        boolean waiting;
        Job(ResearchTableMenu menu,List<ItemStack> notes,long next){this.menu=menu;this.notes=notes;this.next=next;}
    }
    private static Map<UUID,Job> jobs(ServerPlayer player){return JOBS.computeIfAbsent(player.server,s->new HashMap<>());}
    public static boolean running(ServerPlayer player){return jobs(player).containsKey(player.getUUID());}
    public static int remaining(ServerPlayer player){var job=jobs(player).get(player.getUUID());return job==null?0:job.notes.size()-job.index;}
    private static void message(ServerPlayer player,String key,Object... args){player.sendSystemMessage(Component.translatable("tc4auto.msg.batch."+key,args));}
    private static boolean note(ItemStack stack){return stack.is(TCItems.RESEARCH_NOTES.get())||stack.is(TCItems.UNKNOWN_RESEARCH_NOTES.get());}
    public static int start(ServerPlayer player) {
        if(running(player)){message(player,"already_running");return 0;}
        if(!(player.containerMenu instanceof ResearchTableMenu menu)||menu.table()==null||!menu.stillValid(player)){message(player,"table_closed");return 0;}
        var notes=new ArrayList<ItemStack>();
        if(note(menu.getSlot(1).getItem()))notes.add(menu.getSlot(1).getItem().copy());
        // Inventory includes the selected hand and offhand exactly once.
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++) {
            var stack=player.getInventory().getItem(slot);if(note(stack))notes.add(stack.copy());
        }
        if(notes.isEmpty()){message(player,"no_notes");return 0;}
        jobs(player).put(player.getUUID(),new Job(menu,notes,player.server.getTickCount()+1));
        message(player,"started",notes.size());return notes.size();
    }
    public static void stop(ServerPlayer player) {
        if(jobs(player).remove(player.getUUID())!=null)Solver.evict(player.getUUID());
    }
    public static void done(ServerPlayer player,boolean success) {
        var job=jobs(player).get(player.getUUID());if(job==null||!job.waiting)return;
        job.waiting=false;if(success)job.passed++;else job.failed++;
        job.next=player.server.getTickCount()+1;
    }
    public static boolean advance(ServerPlayer player) {
        var job=jobs(player).get(player.getUUID());if(job==null||job.waiting)return false;
        if(player.containerMenu!=job.menu||!job.menu.stillValid(player)){message(player,"table_closed");stop(player);return false;}
        var table=job.menu.table();
        var present=table.items.getStackInSlot(1);
        // Only the first queued note may already occupy the table. Return every
        // completed/failed output before replacing it, without overwriting items.
        boolean first=job.index==0&&!job.notes.isEmpty()&&ItemStack.matches(present,job.notes.get(0));
        if(!present.isEmpty()&&!first) {
            // Inventory.add discards a full-inventory remainder in creative mode.
            // Preserve it so the completed note stays on the table in every mode.
            var remainder=net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(
                new net.minecraftforge.items.wrapper.PlayerMainInvWrapper(player.getInventory()),present.copy(),false);
            table.items.setStackInSlot(1,remainder);player.getInventory().setChanged();job.menu.broadcastChanges();
            if(!remainder.isEmpty()){player.sendSystemMessage(Component.translatable("thaumcraft.auto_research.inventory_full"));stop(player);return false;}
        }
        if(job.index>=job.notes.size()) {
            message(player,"finished",job.passed,job.failed,job.notes.size(),System.currentTimeMillis()-job.started);
            jobs(player).remove(player.getUUID());return false;
        }
        var wanted=job.notes.get(job.index++);ItemStack actual=first?present:ItemStack.EMPTY;
        if(!first)for(int slot=0;slot<player.getInventory().getContainerSize();slot++) {
            if(ItemStack.matches(player.getInventory().getItem(slot),wanted)){actual=player.getInventory().removeItemNoUpdate(slot);break;}
        }
        job.next=player.server.getTickCount()+1;
        if(actual.isEmpty()){job.failed++;return false;}
        table.items.setStackInSlot(1,actual);player.getInventory().setChanged();job.menu.broadcastChanges();
        job.waiting=true;message(player,"progress",job.index,job.notes.size(),job.passed,job.failed);
        if(!Solver.start(player))done(player,false);
        return true;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        var server=event.getServer();var jobs=JOBS.get(server);if(jobs==null||jobs.isEmpty())return;
        for(var id:List.copyOf(jobs.keySet())) {
            var player=server.getPlayerList().getPlayer(id);if(player==null){jobs.remove(id);Solver.evict(id);continue;}
            var job=jobs.get(id);
            if(player.containerMenu!=job.menu||!job.menu.stillValid(player)){stop(player);continue;}
            if(server.getTickCount()<job.next)continue;
            if(job.waiting) {
                // Both solving and applying are asynchronous. A failed solve or
                // aborted application must release the queue as well as success.
                if(!Solver.busy(id))done(player,job.menu.getSlot(1).getItem().is(TCItems.DISCOVERY.get()));
            }else advance(player);
        }
    }
    private static final class Solver {
        private static final Class<?> SERVICE=type("com.github.tc4auto.AutoResearchService");
        private static final Object SCHEDULER=field("SCHEDULER"),EXECUTOR=field("EXECUTOR");
        private static Class<?> type(String name){try{return Class.forName(name);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
        private static Object field(String name){try{var field=SERVICE.getDeclaredField(name);field.setAccessible(true);return field.get(null);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
        private static Object call(Object target,Class<?> type,String method,Class<?> argument,Object value){try{return type.getMethod(method,argument).invoke(target,value);}catch(ReflectiveOperationException e){throw new IllegalStateException("TC4AutoResearch adapter: "+method,e);}}
        static boolean start(ServerPlayer player){return Boolean.TRUE.equals(call(null,SERVICE,"start",ServerPlayer.class,player));}
        static void evict(UUID id){call(null,SERVICE,"evictPlayer",UUID.class,id);}
        static boolean busy(UUID id){return Boolean.TRUE.equals(call(SCHEDULER,SCHEDULER.getClass(),"isBusy",UUID.class,id))||Boolean.TRUE.equals(call(EXECUTOR,EXECUTOR.getClass(),"isRunning",UUID.class,id));}
    }
    private AutoResearchCompat() {}
}
