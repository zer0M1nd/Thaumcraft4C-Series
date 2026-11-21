package tc4revival.content;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.LegacyAspect;
import tc4revival.aspect.TCAspects;

/** Each draw attempt, including reserve attempts within the same tick, advances the five-call throttle. */
public  class ArcaneLampFuel implements EssentiaTransport {
    public interface Host {
        Direction facing();
        EssentiaTransport neighbor(Direction face);
        void changed();
    }
    private final Host host;
    private final boolean fertility;
    private int charges,drawDelay;
    private boolean reserve;
    public ArcaneLampFuel(Host host,boolean fertility) { this.host=host; this.fertility=fertility; charges=fertility?0:-1; }
    public int charges() { return charges; }
    public boolean reserve() { return reserve; }
    public void consume(int amount) { charges-=amount; host.changed(); }
    public void tick() {
        if (fertility) {
            if (charges<4 && draw()) { charges++; host.changed(); }
        } else {
            if (charges<=0) {
                if (reserve) { charges=100; reserve=false; host.changed(); }
                else if (draw()) { charges=100; host.changed(); }
            }
            if (!reserve && draw()) { reserve=true; host.changed(); }
            if (charges==0) { charges=-1; host.changed(); }
        }
    }
    private boolean draw() {
        if (++drawDelay%5!=0) return false;
        var face=host.facing(); var source=host.neighbor(face);
        return source!=null && source.connectable(face.getOpposite()) && source.canOutput(face.getOpposite())
                && source.suction(face.getOpposite())<suction(face) && source.takeEssentia(suctionType(face),1,face.getOpposite())==1;
    }
    public void save(CompoundTag tag) { tag.putInt("charges",charges); if (!fertility) tag.putBoolean("reserve",reserve); }
    public void load(CompoundTag tag) { charges=tag.getInt("charges"); reserve=!fertility && tag.getBoolean("reserve"); }
    @Override public boolean connectable(Direction face) { return face==host.facing(); }
    @Override public boolean canInput(Direction face) { return connectable(face); }
    @Override public boolean canOutput(Direction face) { return false; }
    @Override public Aspect suctionType(Direction face) { return TCAspects.get(fertility?LegacyAspect.LIFE:LegacyAspect.PLANT); }
    @Override public int suction(Direction face) { return face!=host.facing()?0:fertility?128-charges*10:!reserve || charges<=0?128:0; }
    @Override public int minimumSuction() { return 0; }
    @Override public Aspect essentiaType(Direction face) { return null; }
    @Override public int essentiaAmount(Direction face) { return 0; }
    @Override public int takeEssentia(Aspect aspect,int amount,Direction face) { return 0; }
    @Override public int addEssentia(Aspect aspect,int amount,Direction face) { return 0; }
}
