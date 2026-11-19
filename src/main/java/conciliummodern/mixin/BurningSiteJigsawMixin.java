package conciliummodern.mixin;

import java.util.List;
import conciliummodern.ConciliumWorldgen;
import net.minecraft.core.Registry;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Only augments vanilla house candidates; the accepted structure's pieces enforce the legacy maximum of one. */
@Mixin(targets="net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement$Placer",remap=false)
public abstract class BurningSiteJigsawMixin {
    @Shadow(aliases="f_210314_",remap=false) @Final private Registry<StructureTemplatePool> pools;
    @Shadow(aliases="f_210319_",remap=false) @Final private List<? super PoolElementStructurePiece> pieces;

    @Group(name="concilium$burningSiteCandidates",min=1)
    @Redirect(method={"tryPlacingChildren","m_227264_"},at=@At(value="INVOKE",
            target="Lnet/minecraft/world/level/levelgen/structure/pools/StructureTemplatePool;getShuffledTemplates(Lnet/minecraft/util/RandomSource;)Ljava/util/List;"),require=0,remap=false)
    private List<StructurePoolElement> concilium$mojmapCandidates(StructureTemplatePool pool,RandomSource random) {
        return ConciliumWorldgen.villageCandidates(pools,pieces,pool,random);
    }
    @Group(name="concilium$burningSiteCandidates",min=1)
    @Redirect(method={"tryPlacingChildren","m_227264_"},at=@At(value="INVOKE",
            target="Lnet/minecraft/world/level/levelgen/structure/pools/StructureTemplatePool;m_227362_(Lnet/minecraft/util/RandomSource;)Ljava/util/List;"),require=0,remap=false)
    private List<StructurePoolElement> concilium$srgCandidates(StructureTemplatePool pool,RandomSource random) {
        return ConciliumWorldgen.villageCandidates(pools,pieces,pool,random);
    }
}
