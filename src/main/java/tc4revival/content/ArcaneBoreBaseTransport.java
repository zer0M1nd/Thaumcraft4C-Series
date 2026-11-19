package tc4revival.content;

import net.minecraft.core.Direction;
import tc4revival.aspect.*;

/** The output-facing port remains connectable, but has zero suction. */
public  class ArcaneBoreBaseTransport implements EssentiaTransport {
    public interface Host {
        Direction output();
        EssentiaTransport neighbor(Direction side);
    }
    private final Host host;
    public ArcaneBoreBaseTransport(Host host) { this.host=host; }
    public boolean draw() {
        for (var side:Direction.values()) {
            var source=host.neighbor(side);
            if (source==null || !source.connectable(side.getOpposite())) continue;
            if (!source.canOutput(side.getOpposite())) continue;
            if (source.suction(side.getOpposite())<suction(side)
                    && source.takeEssentia(suctionType(side),1,side.getOpposite())==1) return true;
        }
        return false;
    }
    @Override public boolean connectable(Direction side) { return true; }
    @Override public boolean canInput(Direction side) { return true; }
    @Override public boolean canOutput(Direction side) { return false; }
    @Override public Aspect suctionType(Direction side) { return TCAspects.get(LegacyAspect.ENTROPY); }
    @Override public int suction(Direction side) { return side==host.output()?0:128; }
    @Override public int minimumSuction() { return 0; }
    @Override public Aspect essentiaType(Direction side) { return null; }
    @Override public int essentiaAmount(Direction side) { return 0; }
    @Override public int takeEssentia(Aspect aspect,int amount,Direction side) { return 0; }
    @Override public int addEssentia(Aspect aspect,int amount,Direction side) { return 0; }
}
