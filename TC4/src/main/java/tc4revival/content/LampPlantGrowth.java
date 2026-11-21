package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Old plant updates mapped to the modern random-tick entry, not instant bonemeal growth. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public  class LampPlantGrowth {
    private static final Set<String> VANILLA=Set.of("wheat","carrots","potatoes","nether_wart","cocoa","sugar_cane","cactus",
            "oak_sapling","spruce_sapling","birch_sapling","jungle_sapling","acacia_sapling","dark_oak_sapling",
            "melon_stem","pumpkin_stem","attached_melon_stem","attached_pumpkin_stem","brown_mushroom","red_mushroom",
            "dandelion","poppy","blue_orchid","allium","azure_bluet","red_tulip","orange_tulip","white_tulip","pink_tulip","oxeye_daisy",
            "grass","fern","dead_bush","vine","lily_pad","sunflower","lilac","tall_grass","large_fern","rose_bush","peony");
    private record Scheduled(Block block,long due) {}
    static  class Pending extends SavedData {
        final Map<BlockPos,Scheduled> entries=new LinkedHashMap<>();
        static Pending load(CompoundTag tag) {
            var result=new Pending();
            for (var entry:tag.getList("updates",Tag.TAG_COMPOUND)) {
                var row=(CompoundTag)entry; var id=ResourceLocation.tryParse(row.getString("block"));
                var block=id==null?null:ForgeRegistries.BLOCKS.getValue(id);
                if (block!=null && block!=Blocks.AIR) result.entries.put(BlockPos.of(row.getLong("pos")),new Scheduled(block,row.getLong("due")));
            }
            return result;
        }
        @Override public CompoundTag save(CompoundTag tag) {
            var rows=new ListTag(); entries.forEach((pos,scheduled)->{
                var row=new CompoundTag(); row.putLong("pos",pos.asLong()); row.putLong("due",scheduled.due());
                row.putString("block",ForgeRegistries.BLOCKS.getKey(scheduled.block()).toString()); rows.add(row);
            }); tag.put("updates",rows); return tag;
        }
    }
    static Pending pending(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Pending::load,Pending::new,"thaumcraft_lamp_growth_updates");
    }
    private LampPlantGrowth() {}
    public static boolean candidate(ServerLevel level,BlockPos pos) {
        var state=level.getBlockState(pos); var block=state.getBlock(); var id=ForgeRegistries.BLOCKS.getKey(block);
        boolean own=block instanceof MagicalPlantBlock || block instanceof MagicalSaplingBlock || block instanceof ManaPodBlock;
        if (!own && (id==null || !id.getNamespace().equals("minecraft") || !VANILLA.contains(id.getPath()))) return false;
        if (block instanceof ManaPodBlock) return state.getValue(ManaPodBlock.AGE)<7;
        if (block instanceof CropBlock crop && crop.isMaxAge(state)) return false;
        if (block instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE)>=3) return false;
        if (block instanceof CocoaBlock && state.getValue(CocoaBlock.AGE)>=2) return false;
        if ((block instanceof SugarCaneBlock || block instanceof CactusBlock) && level.getBlockState(pos.below()).is(block)) return false;
        if (block instanceof BonemealableBlock plant && !(block instanceof StemBlock) && !(block instanceof AttachedStemBlock)
                && !plant.isValidBonemealTarget(level,pos,state,false)) return false;
        return true;
    }
    public static void schedule(ServerLevel level,BlockPos pos,BlockState state) {
        schedule(level,pos,state,1);
    }
    public static void schedule(ServerLevel level,BlockPos pos,BlockState state,int delay) {
        var pending=pending(level);
        if (pending.entries.putIfAbsent(pos.immutable(),new Scheduled(state.getBlock(),level.getGameTime()+delay))==null) pending.setDirty();
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase!=TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        var pending=pending(level); if (pending.entries.isEmpty()) return;
        var ready=new ArrayList<Map.Entry<BlockPos,Scheduled>>();
        var iterator=pending.entries.entrySet().iterator(); while (iterator.hasNext()) {
            var entry=iterator.next(); if (entry.getValue().due()>level.getGameTime() || !level.hasChunkAt(entry.getKey())) continue;
            ready.add(Map.entry(entry.getKey(),entry.getValue())); iterator.remove();
        }
        for (var entry:ready) {
            var pos=entry.getKey(); if (!level.hasChunkAt(pos)) continue;
            var state=level.getBlockState(pos); if (state.is(entry.getValue().block())) state.randomTick(level,pos,level.random);
        }
        if (!ready.isEmpty()) pending.setDirty();
    }
}
