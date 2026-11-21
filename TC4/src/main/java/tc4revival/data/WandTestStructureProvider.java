package tc4revival.data;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;
import com.google.common.hash.Hashing;
import net.minecraft.SharedConstants;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

/** An isolated empty fixture for later dedicated-server regressions. */
public  class WandTestStructureProvider implements DataProvider {
    private final PackOutput output;
    public WandTestStructureProvider(PackOutput output) { this.output = output; }
    private static ListTag xyz(int x, int y, int z) {
        var values = new ListTag(); values.add(IntTag.valueOf(x)); values.add(IntTag.valueOf(y)); values.add(IntTag.valueOf(z)); return values;
    }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        try {
            write(cache,"wand_test_empty",7,5,7);
            write(cache,"tree_test_empty",35,52,35);
            return CompletableFuture.completedFuture(null);
        } catch (java.io.IOException failure) { return CompletableFuture.failedFuture(failure); }
    }
    private void write(CachedOutput cache,String name,int width,int height,int depth) throws java.io.IOException {
            var root = new CompoundTag(); root.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
            root.put("size", xyz(width,height,depth)); root.put("entities", new ListTag());
            var palette = new ListTag(); var air = new CompoundTag(); air.putString("Name", "minecraft:air"); palette.add(air); root.put("palette", palette);
            var blocks = new ListTag();
            for (int x = 0; x < width; x++) for (int y = 0; y < height; y++) for (int z = 0; z < depth; z++) {
                var block = new CompoundTag(); block.putInt("state", 0); block.put("pos", xyz(x, y, z)); blocks.add(block);
            }
            root.put("blocks", blocks); var stream = new ByteArrayOutputStream(); NbtIo.writeCompressed(root, stream); byte[] bytes = stream.toByteArray();
            cache.writeIfNeeded(output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/structures/"+name+".nbt"), bytes, Hashing.sha1().hashBytes(bytes));
    }
    @Override public String getName() { return "TC4 wand server test structure"; }
}
