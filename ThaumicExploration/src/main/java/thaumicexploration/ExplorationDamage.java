package thaumicexploration;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.level.Level;
public final class ExplorationDamage {
    public static final ResourceKey<DamageType> SOULS=ResourceKey.create(Registries.DAMAGE_TYPE,ExplorationMod.id("soul_crucible"));
    public static final ResourceKey<DamageType> WITHDRAWAL=ResourceKey.create(Registries.DAMAGE_TYPE,ExplorationMod.id("taint_withdrawal"));
    public static DamageSource source(Level level,ResourceKey<DamageType> type){return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type));}
    private ExplorationDamage(){}
}
