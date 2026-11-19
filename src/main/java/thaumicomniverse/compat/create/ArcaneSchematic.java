package thaumicomniverse.compat.create;

import java.util.*;
import com.simibubi.create.api.schematic.nbt.SafeNbtWriterRegistry;
import com.simibubi.create.api.schematic.requirement.SchematicRequirementRegistries;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.*;

/** Schematics copy construction and configuration, never inventories, vis, identities or rituals. */
public class ArcaneSchematic {
    public static void setup(){
        for(var block:ForgeRegistries.BLOCKS)if(ArcaneContraptions.thaumic(block.defaultBlockState())){
            if(hasStateFilters()) ModernStateFilters.register(block);
            SchematicRequirementRegistries.BLOCKS.register(block,(state,be)->{
                var actual=state(be,state);
                if(actual.isAir())return ItemRequirement.NONE;
                if(actual.getBlock().asItem()==Items.AIR)return ItemRequirement.INVALID;
                int count=actual.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE)
                    &&actual.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE)==net.minecraft.world.level.block.state.properties.SlabType.DOUBLE?2:1;
                return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME,new ItemStack(actual.getBlock(),count));
            });
        }
        for(var type:ForgeRegistries.BLOCK_ENTITY_TYPES)if(ArcaneContraptions.owned(ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(type)))
            SafeNbtWriterRegistry.REGISTRY.register(type,ArcaneSchematic::safe);
    }
    public static boolean hasStateFilters() {
        return ArcaneSchematic.class.getClassLoader().getResource("com/simibubi/create/api/schematic/state/SchematicStateFilterRegistry.class")!=null;
    }
    private static final class ModernStateFilters {
        static void register(Block block) {
            com.simibubi.create.api.schematic.state.SchematicStateFilterRegistry.REGISTRY.register(block,ArcaneSchematic::state);
        }
    }
    public static BlockState state(BlockEntity be,BlockState state){
        if(be instanceof ScannableNode||state.getBlock() instanceof PortableHoleBlock)return Blocks.AIR.defaultBlockState();
        if(state.getBlock() instanceof AdvancedAlchemyBlock)return AdvancedAlchemyStructure.restored(state.getValue(AdvancedAlchemyBlock.PART));
        if(state.getBlock() instanceof InfernalFurnaceBlock)return InfernalFurnaceStructure.restoredMaterial(state.getValue(InfernalFurnaceBlock.PART)).defaultBlockState();
        var id=ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if(id!=null&&id.toString().equals("witchinggadgets:infernal_blast_furnace")){
            var part=(net.minecraft.world.level.block.state.properties.IntegerProperty)state.getBlock().getStateDefinition().getProperty("part");int n=state.getValue(part);
            if(n==22)return Blocks.AIR.defaultBlockState();
            if(n==4||Set.of(9,11,15,17).contains(n))return Blocks.OBSIDIAN.defaultBlockState();
            if(n>=9&&n<18)return Blocks.SOUL_SAND.defaultBlockState();
            if(n<18)return Blocks.NETHER_BRICKS.defaultBlockState();
            int x=n%3-1,z=(n%9)/3-1;
            return Blocks.NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,x==-1?net.minecraft.core.Direction.EAST:x==1?net.minecraft.core.Direction.WEST:z==-1?net.minecraft.core.Direction.SOUTH:net.minecraft.core.Direction.NORTH);
        }
        return state;
    }
    private static void safe(BlockEntity be,CompoundTag output){
        var source=be.saveWithoutMetadata();
        // Every copied field is a setting. Excluding general inventory compounds also excludes
        // recipe results, stored energy, paired mirror endpoints and player-bound soul cores.
        for(var key:List.of("facing","face","orientation","side","open","choke","GadSupport","AspectFilter","Directions","EnabledSides","AbsorptionSide","Direction"))
            if(source.contains(key))output.put(key,source.get(key).copy());
        var id=ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(be.getType());
        if(id!=null&&id.toString().equals("automagy:vis_reader")&&source.contains("Channels"))output.put("Channels",source.get("Channels").copy());
    }
    /** Formed furnaces use fixed world-space shapes. Their component blocks remain movable;
     * an active furnace must first be dismantled, and an infusion in progress must finish. */
    public static boolean fixedStructure(BlockState state,BlockEntity be){
        var id=ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return state.getBlock() instanceof AdvancedAlchemyBlock||state.getBlock() instanceof InfernalFurnaceBlock
            ||id!=null&&Set.of("witchinggadgets:infernal_blast_furnace","thaumichorizons:vat","thaumichorizons:vat_slave").contains(id.toString())
            ||be instanceof InfusionMatrixBlockEntity matrix&&matrix.crafting();
    }
}
