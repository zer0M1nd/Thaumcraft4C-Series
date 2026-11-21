package temodern.machine;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import temodern.TE;

public final class Machines {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TE.ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TE.ID);
    public static final Map<String, RegistryObject<Block>> ALL = new LinkedHashMap<>();
    static {
        for (String name : new String[]{"essentia_provider", "infusion_provider",
                "essentia_vibration_chamber", "vis_interface", "creative_vis_interface", "knowledge_inscriber", "iron_gearbox", "thaumium_gearbox",
                "arcane_terminal", "arcane_assembler"}) {
            RegistryObject<Block> block = BLOCKS.register(name, () -> new MachineBlock(name)); ALL.put(name, block);
            if(name.equals("arcane_terminal"))TE.ITEMS.register(name,()->new appeng.items.parts.PartItem<>(new Item.Properties(),temodern.crafting.ArcaneTerminalPart.class,temodern.crafting.ArcaneTerminalPart::new));
            else if(name.endsWith("vis_interface"))TE.ITEMS.register(name,()->new appeng.items.parts.PartItem<>(new Item.Properties(),VisInterfacePart.class,item->new VisInterfacePart(item,name.startsWith("creative"))));
            else TE.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        }
    }
    public static final RegistryObject<BlockEntityType<NetworkMachine>> TYPE = ENTITIES.register("machine", () ->
            BlockEntityType.Builder.of(NetworkMachine::new, ALL.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
    public static void register(IEventBus bus) { BLOCKS.register(bus); ENTITIES.register(bus); }
}
