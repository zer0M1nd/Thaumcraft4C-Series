package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraftforge.registries.*;

/** Vanilla banner geometry, placement, cloth animation, item rendering and loot preservation. */
public  class CrimsonBanner {
    private static final DeferredRegister<BannerPattern> PATTERNS=DeferredRegister.create(Registries.BANNER_PATTERN,"thaumcraft");
    public static final RegistryObject<BannerPattern> CRIMSON=PATTERNS.register("crimson",()->new BannerPattern("tc_crimson"));
    private CrimsonBanner() {}
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) { PATTERNS.register(bus); }
    public static ItemStack item() {
        var stack=new ItemStack(Items.RED_BANNER);var pattern=new CompoundTag();
        pattern.putString("Pattern",CRIMSON.get().getHashname());pattern.putInt("Color",DyeColor.YELLOW.getId());
        var list=new ListTag();list.add(pattern);var data=new CompoundTag();data.put("Patterns",list);BlockItem.setBlockEntityData(stack,net.minecraft.world.level.block.entity.BlockEntityType.BANNER,data);return stack;
    }
    public static void place(LevelAccessor level,BlockPos pos,int rotation) {
        level.setBlock(pos,Blocks.RED_BANNER.defaultBlockState().setValue(BannerBlock.ROTATION,Math.floorMod(rotation,16)),3);
        if(level.getBlockEntity(pos) instanceof BannerBlockEntity banner) {
            banner.fromItem(item(),DyeColor.RED);banner.setChanged();
            if(level instanceof net.minecraft.world.level.Level world)world.sendBlockUpdated(pos,banner.getBlockState(),banner.getBlockState(),3);
        }
    }
}
