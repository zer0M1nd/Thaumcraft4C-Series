package tbmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public final class DecorativeBlocks {
    public static final class EnderPlanks extends Block {
        public EnderPlanks() {super(Properties.of().strength(2,45).sound(SoundType.WOOD));}
        @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos pos,Entity entity) {
            return !(entity instanceof EnderDragon) && super.canEntityDestroy(state,level,pos,entity);
        }
    }
    public static final class VoidBlock extends Block {
        public VoidBlock() {super(Properties.of().strength(5,Float.MAX_VALUE).requiresCorrectToolForDrops());}
        @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
            thaumcraft.api.fx.LegacyEffects.emit(level,"dark_sparkle",new net.minecraft.world.phys.Vec3(pos.getX()+random.nextFloat(),pos.getY()+random.nextFloat(),pos.getZ()+random.nextFloat()),net.minecraft.world.phys.Vec3.ZERO,0x101010,2,18,1,0,-.1f);
        }
    }
    private DecorativeBlocks() {}
}
