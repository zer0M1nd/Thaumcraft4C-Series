package thaumicfutures.compat;

import net.minecraftforge.eventbus.api.IEventBus;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.VolatileDataModifierHook;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;

public final class TinkersCompat {
    private static final net.minecraft.resources.ResourceLocation STORED_SLOTS=net.minecraft.resources.ResourceLocation.parse("thaumicfutures:thaumic_slots");
    private static final slimeknights.tconstruct.library.modifiers.ModifierId THAUMIC=new slimeknights.tconstruct.library.modifiers.ModifierId("thaumicfutures:thaumic");
    private TinkersCompat() {}
    public static void register(IEventBus bus) {
        var modifiers = ModifierDeferredRegister.create("thaumicfutures");
        modifiers.register("thaumic", ThaumicModifier::new);
        modifiers.register(bus);
    }
    public static int bonusSlots(int parts, int thaumiumParts) {
        int count = Math.min(Math.max(0, thaumiumParts), Math.max(0, parts));
        return parts == 2 ? count : (count + 1) / 2;
    }
    public static final class ThaumicModifier extends Modifier implements VolatileDataModifierHook {
        @Override protected void registerHooks(ModuleHookMap.Builder builder) {
            builder.addHook(this, ModifierHooks.VOLATILE_DATA);
        }
        @Override public void addVolatileData(IToolContext tool, ModifierEntry modifier, ToolDataNBT data) {
            int bonus=thaumicfutures.FutureFeatures.Group.TINKERS.enabled()?bonusSlots(tool.getMaterials().size(), modifier.getLevel()):0;
            data.putInt(STORED_SLOTS,bonus);
            data.addSlots(SlotType.UPGRADE,bonus);
        }
    }
    /** Correct a stored tool's slot view without discarding upgrades or rewriting the saved material data. */
    public static slimeknights.tconstruct.library.tools.nbt.IModDataView currentSlots(
            slimeknights.tconstruct.library.tools.nbt.ToolStack tool,slimeknights.tconstruct.library.tools.nbt.IModDataView data) {
        return thaumicfutures.api.tinkers.TinkersSlotViews.compose(tool,data,baseSlots(tool,data));
    }
    private static slimeknights.tconstruct.library.tools.nbt.IModDataView baseSlots(
            slimeknights.tconstruct.library.tools.nbt.ToolStack tool,slimeknights.tconstruct.library.tools.nbt.IModDataView data) {
        if(data==slimeknights.tconstruct.library.tools.nbt.IModDataView.EMPTY)return data;
        int level=tool.getModifiers().getLevel(THAUMIC);
        if(level<=0)return data;
        int full=bonusSlots(tool.getMaterials().size(),level);
        // Tools created before profiles always stored the complete bonus.
        int stored=data.contains(STORED_SLOTS)?data.getInt(STORED_SLOTS):full;
        int effective=thaumicfutures.FutureFeatures.Group.TINKERS.enabled()?full:0;
        if(stored==effective)return data;
        return new AdjustedSlots(data,effective-stored);
    }
    private record AdjustedSlots(slimeknights.tconstruct.library.tools.nbt.IModDataView delegate,int delta)
            implements slimeknights.tconstruct.library.tools.nbt.IModDataView {
        @Override public <T> T get(net.minecraft.resources.ResourceLocation key,java.util.function.BiFunction<net.minecraft.nbt.CompoundTag,String,T> reader){return delegate.get(key,reader);}
        @Override public boolean contains(net.minecraft.resources.ResourceLocation key){return delegate.contains(key);}
        @Override public boolean contains(net.minecraft.resources.ResourceLocation key,int type){return delegate.contains(key,type);}
        @Override public int getSlots(SlotType type){return delegate.getSlots(type)+(type==SlotType.UPGRADE?delta:0);}
    }
}
