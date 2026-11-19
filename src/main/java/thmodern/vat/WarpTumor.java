package thmodern.vat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeConfigSpec;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.PlayerKnowledge.WarpKind;

/** Stored warp is a liability: it must be returned before discarding a body. */
public final class WarpTumor {
    public static final ForgeConfigSpec CONFIG;
    public static final ForgeConfigSpec.IntValue CAPACITY;
    private static final WarpKind[] ORDER={WarpKind.PERMANENT,WarpKind.STICKY,WarpKind.TEMPORARY};
    static {var builder=new ForgeConfigSpec.Builder();CAPACITY=builder.comment("扭曲肿瘤容纳的扭曲值；初始 50，范围 0..100000；降低容量不丢失已存扭曲；重启世界。", "Warp held by a warped tumor; default 50, range 0..100000; reducing capacity never discards stored warp; restart the world.").defineInRange("warpedTumorValue",50,0,100000);thmodern.LegacyConfig.define(builder);CONFIG=builder.build();}
    private static CompoundTag stored(Player player){return player.getPersistentData().getCompound(SelfInfusions.DATA).getCompound("TumorWarp");}
    public static void absorb(Player player){
        if(!(player instanceof ServerPlayer server)||!SelfInfusions.has(player,"warped_tumor"))return;
        var held=stored(player);long occupied=0;for(var kind:ORDER)occupied+=Math.max(0,held.getInt(kind.name()));
        int capacity=(int)Math.max(0,CAPACITY.get()-occupied);if(capacity==0)return;
        var knowledge=KnowledgeSavedData.get(server.server);var snapshot=knowledge.snapshot(player.getUUID());
        if(snapshot.isEmpty()||java.util.Arrays.stream(ORDER).allMatch(kind->snapshot.get().warp(kind)==0))return;
        int[] taken=knowledge.update(player.getUUID(),player.getRandom()::nextInt,k->{
            int remaining=capacity;var amounts=new int[3];
            for(int i=0;i<ORDER.length;i++){int count=Math.min(remaining,k.warp(ORDER[i]));amounts[i]=count;k.changeWarp(ORDER[i],-count);remaining-=count;}return amounts;
        });
        for(int i=0;i<ORDER.length;i++)held.putInt(ORDER[i].name(),Math.max(0,held.getInt(ORDER[i].name()))+taken[i]);
        player.getPersistentData().getCompound(SelfInfusions.DATA).put("TumorWarp",held);
    }
    public static void release(Player player){
        if(!(player instanceof ServerPlayer server))return;var held=stored(player);
        if(java.util.Arrays.stream(ORDER).noneMatch(kind->held.getInt(kind.name())>0))return;
        KnowledgeSavedData.get(server.server).update(player.getUUID(),player.getRandom()::nextInt,k->{
            for(var kind:ORDER){long sum=(long)k.warp(kind)+Math.max(0,held.getInt(kind.name()));k.setWarp(kind,(int)Math.min(Integer.MAX_VALUE,sum));}return null;
        });
        player.getPersistentData().getCompound(SelfInfusions.DATA).remove("TumorWarp");
    }
}
