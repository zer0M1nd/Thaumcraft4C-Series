package technomancy;

import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class TechRegistry {
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,Technomancy.ID);
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,Technomancy.ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,Technomancy.ID);
    public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,Technomancy.ID);
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,Technomancy.ID);
    public static final EnumMap<MachineKind,RegistryObject<MachineBlock>> MACHINES=new EnumMap<>(MachineKind.class);
    public static final Map<String,RegistryObject<Item>> MATERIALS=new LinkedHashMap<>();
    public static final RegistryObject<Block> GLASS=BLOCKS.register("quantized_glass",()->new GlassBlock(BlockBehaviour.Properties.copy(Blocks.GLASS).strength(.3f)));
    public static final RegistryObject<Block> GLASS_PANE=BLOCKS.register("quantized_glass_pane",()->new IronBarsBlock(BlockBehaviour.Properties.copy(Blocks.GLASS_PANE)));
    public static final RegistryObject<Block> BASALT=BLOCKS.register("basalt",()->new Block(BlockBehaviour.Properties.copy(Blocks.BASALT)));
    public static final RegistryObject<Block> NODE_PART=BLOCKS.register("node_fabricator_part",NodePartBlock::new);
    public static final RegistryObject<BlockEntityType<MachineEntity>> MACHINE;
    public static final RegistryObject<MenuType<MachineMenu>> MENU=MENUS.register("machine",()->IForgeMenuType.create((id,inv,buf)->new MachineMenu(id,inv,buf.readBlockPos())));
    static {
        for(var kind:MachineKind.values()) {
            var block=BLOCKS.register(kind.id(),()->new MachineBlock(kind));
            MACHINES.put(kind,block);
            ITEMS.register(kind.id(),()->new MachineBlockItem(block.get(),new Item.Properties()));
        }
        ITEMS.register("quantized_glass",()->new BlockItem(GLASS.get(),new Item.Properties()));
        ITEMS.register("quantized_glass_pane",()->new BlockItem(GLASS_PANE.get(),new Item.Properties()));
        ITEMS.register("basalt",()->new BlockItem(BASALT.get(),new Item.Properties()));
        for(String id:List.of("neutronized_metal","enchanted_coil","neutronized_gear","pen_core","sacrificial_ingot","life_coil","mana_coil","manasteel_gear","energized_wand_core","technoturge_scepter_core"))
            MATERIALS.put(id,ITEMS.register(id,()->new Item(new Item.Properties())));
        for(String id:List.of("coil_coupler","potency_gem","ritual_tome","existence_gem","fire_gem","pure_existence_plate","magical_beast_wing"))
            MATERIALS.put(id,ITEMS.register(id,()->new TechItem(id)));
        MATERIALS.put("pen",ITEMS.register("pen",TechWands.Pen::new));
        MATERIALS.put("technoturge_scepter",ITEMS.register("technoturge_scepter",TechWands.Scepter::new));
        MATERIALS.put("fusion_focus",ITEMS.register("fusion_focus",FusionFocus::new));
        for(int stage=0;stage<6;stage++) {
            int value=stage;
            MATERIALS.put("purified_ore_"+(stage+1),ITEMS.register("purified_ore_"+(stage+1),()->new PurifiedOreItem(value)));
        }
        MACHINE=ENTITIES.register("machine",()->BlockEntityType.Builder.of(MachineEntity::create,MACHINES.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
        TABS.register("technomancy",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.technomancy"))
            .icon(()->new ItemStack(MATERIALS.get("neutronized_metal").get())).displayItems((params,out)-> {
                for(var entry:MACHINES.entrySet()) if(entry.getKey().enabled()) out.accept(entry.getValue().get());
                out.accept(GLASS.get());out.accept(GLASS_PANE.get());out.accept(BASALT.get());
                MATERIALS.forEach((id,item)->{if(!id.startsWith("purified_ore_"))out.accept(item.get().getDefaultInstance());});
                if(net.minecraftforge.fml.ModList.get().isLoaded("botania"))out.accept(TechFluids.BUCKET.get());
                out.accept(thaumcraft.api.ThaumcraftApi.createWand("technomancy:electric","thaumium",false,false));
            }).build());
    }
    public static Item item(String id){return MATERIALS.containsKey(id)?MATERIALS.get(id).get():ForgeRegistries.ITEMS.getValue(Technomancy.id(id));}
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);MENUS.register(bus);TABS.register(bus);}
    private TechRegistry() {}
}
