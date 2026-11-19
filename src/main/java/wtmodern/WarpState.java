package wtmodern;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.*;
import tc4revival.research.*;
import tc4revival.content.WarpingGear;
import java.util.*;

public class WarpState {
    public static CompoundTag tag(ServerPlayer p){var data=p.getPersistentData();if(!data.contains("WarpTheory",Tag.TAG_COMPOUND))data.put("WarpTheory",new CompoundTag());return data.getCompound("WarpTheory");}
    public static PlayerKnowledge knowledge(ServerPlayer p){return KnowledgeSavedData.get(p.server).snapshot(p.getUUID()).orElseGet(()->PlayerKnowledge.firstLogin(tc4revival.aspect.TCAspects.catalog(),p.getRandom()::nextInt));}
    public static int total(ServerPlayer p){var k=knowledge(p);long amount=(long)k.warp(PlayerKnowledge.WarpKind.PERMANENT)*WarpConfig.MULTIPLIER.get()+k.warp(PlayerKnowledge.WarpKind.STICKY)+k.warp(PlayerKnowledge.WarpKind.TEMPORARY)+WarpingGear.equipped(p);return (int)Math.max(0,Math.min(Integer.MAX_VALUE,amount));}
    public static void remove(ServerPlayer p,int amount){KnowledgeSavedData.get(p.server).update(p.getUUID(),p.getRandom()::nextInt,k->{var next=WarpRules.remove(k.warp(PlayerKnowledge.WarpKind.PERMANENT),k.warp(PlayerKnowledge.WarpKind.STICKY),k.warp(PlayerKnowledge.WarpKind.TEMPORARY),amount,WarpConfig.MULTIPLIER.get(),WarpConfig.PERMANENT.get());k.setWarp(PlayerKnowledge.WarpKind.PERMANENT,next[0]);k.setWarp(PlayerKnowledge.WarpKind.STICKY,next[1]);k.setWarp(PlayerKnowledge.WarpKind.TEMPORARY,next[2]);k.setWarpCounter(k.totalWarp());return null;});}
    public static WarpRules.Event queueOne(ServerPlayer p,int severity){var e=WarpRules.choose(severity,WarpConfig.enabled(),p.getRandom()::nextInt);if(e!=null)queue(p,e.id());return e;}
    public static void queue(ServerPlayer p,String id){var t=tag(p);var q=t.getList("queue",Tag.TAG_STRING);q.add(StringTag.valueOf(id));t.put("queue",q);}
    public static String dequeue(ServerPlayer p){var q=tag(p).getList("queue",Tag.TAG_STRING);return q.isEmpty()?null:q.remove(p.getRandom().nextInt(q.size())).getAsString();}
    public static void purge(ServerPlayer p){int amount=total(p);var events=WarpRules.purgeQueue(amount,WarpConfig.enabled(),p.getRandom()::nextInt);if(!WarpConfig.REBOUND.get())events.forEach(id->queue(p,id));remove(p,amount);}
    public static void add(ServerPlayer p,String key,int amount){var t=tag(p);t.putInt(key,(int)Math.min(Integer.MAX_VALUE,(long)t.getInt(key)+amount));}
    public static void decrement(ServerPlayer p,String key,int amount){var t=tag(p);int n=t.getInt(key)-amount;if(n>0)t.putInt(key,n);else t.remove(key);}
}
