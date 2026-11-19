package tc4revival.content;

import java.util.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraftforge.event.level.NoteBlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Loaded listeners only; note playback can occur after block-entity ticking in modern Minecraft. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public  class ArcaneEarListening {
    private static final Map<Level,Set<ArcaneEarBlockEntity>> EARS=new WeakHashMap<>();
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> ROCK = materialTag("rock");
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> GLASS = materialTag("glass");
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> WOOD = materialTag("wood");
    private static net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> materialTag(String name) {
        return net.minecraft.tags.BlockTags.create(net.minecraft.resources.ResourceLocation.parse("thaumcraft:note_material/"+name));
    }
    private ArcaneEarListening() {}
    public static void register(ArcaneEarBlockEntity ear) { EARS.computeIfAbsent(ear.getLevel(),ignored->Collections.newSetFromMap(new WeakHashMap<>())).add(ear); }
    public static void remove(ArcaneEarBlockEntity ear) { var set=EARS.get(ear.getLevel()); if (set!=null) set.remove(ear); }
    public static int instrument(NoteBlockInstrument instrument) { return instrument.ordinal(); }
    public static int tone(BlockState below) { return instrument(below.instrument()); }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void note(NoteBlockEvent.Play event) {
        if (!(event.getLevel() instanceof Level level)) return;
        if (level.isClientSide) return;
        int instrument=instrument(event.getInstrument()); if (instrument<0) return;
        var set=EARS.get(level); if (set==null) return;
        for (var ear:List.copyOf(set)) if (!ear.isRemoved() && level.hasChunkAt(ear.getBlockPos())) ear.hear(instrument,event.getVanillaNoteId(),event.getPos());
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) { EARS.remove(event.getLevel()); }
}
