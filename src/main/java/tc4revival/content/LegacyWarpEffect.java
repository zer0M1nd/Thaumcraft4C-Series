package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** The old potion callbacks; gaze, ward and blurred vision also have external consumers. */
public  class LegacyWarpEffect extends MobEffect {
    public enum Kind {
        HUNGER(0x446633,7,1), WARD(14742263,3,2), GAZE(0x664433,4,2),
        BLURRED(0x808080,5,2), SUN(16308330,6,2), THAUMARHIA(0x664477,7,2);
        final int color,x,y;
        Kind(int color,int x,int y) { this.color=color; this.x=x; this.y=y; }
    }
    private final Kind kind;
    public LegacyWarpEffect(Kind kind) { super(kind==Kind.WARD ? MobEffectCategory.BENEFICIAL : MobEffectCategory.HARMFUL,kind.color); this.kind=kind; }
    @Override public boolean isDurationEffectTick(int duration,int amplifier) {
        return switch(kind) { case HUNGER -> true; case SUN -> duration%40==0; case THAUMARHIA -> duration%20==0; default -> false; };
    }
    @Override public void applyEffectTick(LivingEntity target,int amplifier) {
        var world=target.level(); if (world.isClientSide) return;
        switch(kind) {
            case HUNGER -> { if (target instanceof Player player) player.causeFoodExhaustion(.025f*(amplifier+1)); }
            case SUN -> {
                float brightness=target.getLightLevelDependentMagicValue();
                if (brightness>.5f && world.random.nextFloat()*30<(brightness-.4f)*2 && world.canSeeSky(target.blockPosition())) target.setSecondsOnFire(4);
                else if (brightness<.25f && world.random.nextFloat()>brightness*2) target.heal(1);
            }
            case THAUMARHIA -> {
                if (world.random.nextInt(15)!=0) return;
                var pos=BlockPos.containing(target.position());
                if (!world.isOutsideBuildHeight(pos) && world.hasChunkAt(pos) && world.isEmptyBlock(pos))
                    world.setBlock(pos,TCArcane.FLUX_GOO.get().defaultBlockState(),3);
            }
            default -> { }
        }
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientMobEffectExtensions> consumer) {
        consumer.accept(new tc4revival.client.LegacyEffectIcon(()->kind.x,()->kind.y));
    }
}
