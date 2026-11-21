package thaumcraft.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.*;
import tc4revival.research.*;

/** Server-owned knowledge and vis operations. No player-name lookup or client authority. */
public  class ThaumcraftApiHelper {
    private ThaumcraftApiHelper() {}
    public static AspectList getObjectAspects(ItemStack stack) { return new AspectList(stack); }
    public static boolean isResearchComplete(ServerPlayer player,String key) {
        return KnowledgeSavedData.get(player.server).snapshot(player.getUUID()).map(k->ResearchCatalogs.rules().completed(k,key)).orElse(false);
    }
    public static boolean hasDiscoveredAspect(ServerPlayer player,Aspect aspect) { return KnowledgeSavedData.get(player.server).snapshot(player.getUUID()).map(k->k.knows(aspect.unwrap())).orElse(false); }
    public static AspectList getDiscoveredAspects(ServerPlayer player) {
        var result=new AspectList();KnowledgeSavedData.get(player.server).snapshot(player.getUUID()).ifPresent(k->k.knownAspects().forEach((aspect,amount)->result.add(Aspect.wrap(aspect),amount)));return result;
    }
    public static boolean completeResearch(ServerPlayer player,String key) {
        ResearchCatalogs.catalog().require(key);
        return KnowledgeSavedData.get(player.server).update(player.getUUID(),player.getRandom()::nextInt,k->ResearchCatalogs.progression().completeTrusted(k,key,tc4revival.config.TCConfig.SERVER.disableWarp.get()).isPresent());
    }
    public static void addResearchPoints(ServerPlayer player,Aspect aspect,short amount) {
        var reward=KnowledgeSavedData.get(player.server).update(player.getUUID(),player.getRandom()::nextInt,k->{
            var value=aspect.unwrap(); boolean discovered=!k.knows(value); int before=k.poolAmount(value);
            k.changePool(value,amount);
            return new tc4revival.network.ScanFeedbackPacket.Reward(value.tag(),Math.max(0,k.poolAmount(value)-before),discovered);
        });
        if (reward.gainedAmount()>0) tc4revival.network.TCNetwork.send(player,new tc4revival.network.ScanFeedbackPacket(
                tc4revival.research.ScanRules.Status.SUCCESS,"",java.util.List.of(reward)));
    }
    public static void addWarpToPlayer(ServerPlayer player,int amount,boolean temporary) { addWarp(player,amount,temporary?PlayerKnowledge.WarpKind.TEMPORARY:PlayerKnowledge.WarpKind.PERMANENT); }
    public static void addStickyWarpToPlayer(ServerPlayer player,int amount) { addWarp(player,amount,PlayerKnowledge.WarpKind.STICKY); }
    private static void addWarp(ServerPlayer player,int amount,PlayerKnowledge.WarpKind kind) {
        if(amount==0||tc4revival.config.TCConfig.SERVER.disableWarp.get())return;
        KnowledgeSavedData.get(player.server).update(player.getUUID(),player.getRandom()::nextInt,k->{k.changeWarp(kind,amount);k.setWarpCounter(k.totalWarp());return null;});
        tc4revival.network.TCNetwork.send(player,new tc4revival.network.WarpGainPacket(kind,amount));
    }
    /** Costs are whole vis points, matching the old helper. Simulation does not change the wand. */
    public static boolean consumeVisFromWand(ItemStack stack,ServerPlayer player,AspectList cost,boolean commit,boolean crafting) {
        return stack.getItem() instanceof tc4revival.content.CastingWandItem wand&&wand.consumeForPlayer(stack,player,cost.unwrap().view(),true,crafting,commit);
    }
    public static boolean consumeVisFromWandCrafting(ItemStack stack,ServerPlayer player,AspectList cost,boolean commit) { return consumeVisFromWand(stack,player,cost,commit,true); }
    public static boolean consumeVisFromInventory(ServerPlayer player,AspectList costs) { var centivis=new java.util.LinkedHashMap<tc4revival.aspect.Aspect,Integer>();costs.unwrap().view().forEach((aspect,amount)->centivis.put(aspect,Math.multiplyExact(amount,100)));return tc4revival.content.WandInventory.consume(player,centivis); }
    public static AspectList getAllAspects(int amount) { var result=new AspectList();tc4revival.aspect.TCAspects.catalog().all().forEach(a->result.add(Aspect.wrap(a),amount));return result; }
    public static AspectList getAllCompoundAspects(int amount) { var result=new AspectList();Aspect.getCompoundAspects().forEach(a->result.add(a,amount));return result; }
    public static tc4revival.content.EssentiaTransport getConnectableTile(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,net.minecraft.core.Direction face) {
        var adjacent=pos.relative(face);if(!level.hasChunkAt(adjacent))return null;
        return level.getBlockEntity(adjacent) instanceof tc4revival.content.EssentiaTransport transport&&transport.connectable(face.getOpposite())?transport:null;
    }
}
