package emtmodern.item;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

/** Explicit mod-bus registration keeps both dungeon entry serializers available before table loading. */
public class LegacyLootTypes {
    public static final DeferredRegister<LootPoolEntryType> TYPES=DeferredRegister.create(Registries.LOOT_POOL_ENTRY_TYPE,"emt");
    public static final RegistryObject<LootPoolEntryType> ONE_RING=TYPES.register("one_ring",()->new LootPoolEntryType(new OneRingLoot.Entry.Serializer()));
    public static final RegistryObject<LootPoolEntryType> HAMMER=TYPES.register("tainted_mjolnir",()->new LootPoolEntryType(new HammerLoot.Entry.Serializer()));
    public static void register(IEventBus bus){TYPES.register(bus);}
}
