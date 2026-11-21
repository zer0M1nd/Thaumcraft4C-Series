package automagy.api;

import automagy.logic.NetherRuneCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Implement on a block to participate in Nethermind structures without editing Automagy registries.
 * Each persistent variant must have a stable namespaced definition key; never reuse a builtin key.
 * Definitions are observed on the server only, and must not mutate the world or load chunks.
 * Output is a registered block/entity identifier according to definition.creature().
 */
public interface NetherRune {
    NetherRuneCatalog.Definition netherRune(BlockState state);
    /** Output state, including variant properties. Null pauses/retries without charging again. */
    default BlockState conjuredBlock(BlockState rune,net.minecraft.server.level.ServerLevel level,BlockPos target){
        var id=net.minecraft.resources.ResourceLocation.tryParse(netherRune(rune).output());
        if(id==null||!net.minecraftforge.registries.ForgeRegistries.BLOCKS.containsKey(id))return null;
        return net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(id).defaultBlockState();
    }
    /** Return a fresh, unspawned mob belonging to this level, or null to retry. The caller runs
     * Forge finalizeSpawn, checks collision/population limits and adds it to the world. */
    default net.minecraft.world.entity.Mob createCreature(BlockState rune,net.minecraft.server.level.ServerLevel level){
        var id=net.minecraft.resources.ResourceLocation.tryParse(netherRune(rune).output());
        var type=id==null?null:net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(id);
        var entity=type==null?null:type.create(level);
        if(entity instanceof net.minecraft.world.entity.Mob mob)return mob;
        if(entity!=null)entity.discard();return null;
    }
    /** Number requested per paid cycle; bounded to 1..64 by the caller. */
    default int creatureCount(BlockState rune){return 1;}
    /** Runs once per successfully inserted creature. Must not spawn the entity a second time. */
    default void creatureSpawned(BlockState rune,net.minecraft.world.entity.Mob creature) {}
    /** Optional visual feedback. Must preserve the rune definition and block entity data. */
    default void setNetherRuneActive(Level level,BlockPos pos,BlockState state,boolean active) {}
}
