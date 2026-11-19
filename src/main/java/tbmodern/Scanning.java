package tbmodern;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;
import tc4revival.research.PlayerKnowledge.ScanKind;
import tc4revival.research.ScanTarget;
import thaumcraft.api.research.ScanRegistry;

/** A non-item fluid keeps its legacy object aspects without inventing an inventory item. */
public final class Scanning {
    public static void initialize() {
        ScanRegistry.register(new ResourceLocation(ThaumicBases.ID,"pyrofluid"),new ScanRegistry.Handler() {
            @Override public Optional<ScanTarget> block(ServerLevel level,BlockPos pos) {
                if(!level.getBlockState(pos).is(Content.block("pyrofluid")))return Optional.empty();
                var catalog=TCAspects.catalog();var aspects=new AspectAmounts();
                aspects.add(catalog.require("ignis"),16);
                aspects.add(catalog.require("praecantatio"),8);
                aspects.add(catalog.require("lux"),6);
                String identity="thaumicbases:pyrofluid";
                return Optional.of(new ScanTarget(new ScanTarget.Block(identity,0),ScanKind.OBJECT,
                    Optional.of(Integer.toString((identity+":-1").hashCode())),aspects));
            }
        });
    }
    private Scanning() {}
}
