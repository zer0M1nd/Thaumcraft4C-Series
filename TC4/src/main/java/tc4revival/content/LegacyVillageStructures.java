package tc4revival.content;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
public  class LegacyVillageStructures {
    private static final DeferredRegister<StructureType<?>> TYPES=DeferredRegister.create(Registries.STRUCTURE_TYPE,"thaumcraft");
    private static final DeferredRegister<StructurePieceType> PIECES=DeferredRegister.create(Registries.STRUCTURE_PIECE,"thaumcraft");
    public static final RegistryObject<StructureType<LegacyVillageStructure>> TYPE=TYPES.register("legacy_village",()->()->LegacyVillageStructure.CODEC);
    public static final RegistryObject<StructurePieceType> PIECE=PIECES.register("legacy_village_house",()->LegacyVillagePiece::new);
    public static void register(IEventBus bus) { TYPES.register(bus);PIECES.register(bus); }
}
