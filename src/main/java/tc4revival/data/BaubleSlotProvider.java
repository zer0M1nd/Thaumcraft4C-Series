package tc4revival.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import tc4revival.content.MundaneBaubleType;
import top.theillusivec4.curios.api.CuriosDataProvider;

/** Baubles' necklace, two rings and belt, exposed through modern Curios slot data. */
public  class BaubleSlotProvider extends CuriosDataProvider {
    public BaubleSlotProvider(PackOutput output, ExistingFileHelper helper, CompletableFuture<HolderLookup.Provider> lookup) {
        super("thaumcraft", output, helper, lookup);
    }
    @Override public void generate(HolderLookup.Provider lookup, ExistingFileHelper helper) {
        for (var type : MundaneBaubleType.values())
            createSlot(type.slot()).size(type.slots()).order(type.metadata() * 10).addCosmetic(false);
        createEntities("player").addPlayer().addSlots("necklace", "ring", "belt");
    }
}
