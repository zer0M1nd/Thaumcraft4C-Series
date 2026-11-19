package tc4revival.content;

import java.util.Arrays;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectNbt;
import tc4revival.aspect.TCAspects;

/** TC4's local suction propagation and one-unit transfers, shared by the six tube variants. */
public  class EssentiaTubeFlow implements EssentiaTransport {
    public enum Kind { NORMAL, VALVE, FILTER, RESTRICTED, ONEWAY, BUFFER }
    public interface Host {
        /** Returns only a loaded neighbor whose opposite connection is open. */
        EssentiaTransport neighbor(Direction side);
        RandomSource random();
        boolean powered();
        int bellows();
        void changed();
        void valveSound();
        void creak();
        void vent(Aspect type);
    }
    private final Kind kind;
    private final Host host;
    private final boolean[] open=new boolean[6];
    private final byte[] choke=new byte[6];
    private Direction facing=Direction.NORTH;
    private Aspect storedType,suctionType,filter;
    private int amount,suction,venting,count,bellows=-1;
    private boolean allowFlow=true,hadPower;
    private AspectAmounts buffer=new AspectAmounts();
    public EssentiaTubeFlow(Kind kind,Host host) { this.kind=kind; this.host=host; Arrays.fill(open,true); }
    public Kind kind() { return kind; }
    public Direction facing() { return facing; }
    public void face(Direction side) { if (side!=null) { facing=side; host.changed(); } }
    public boolean open(Direction side) { return side!=null && open[side.get3DDataValue()]; }
    public void setOpen(Direction side,boolean value) { open[side.get3DDataValue()]=value; host.changed(); }
    public int choke(Direction side) { return side==null ? 0 : choke[side.get3DDataValue()]; }
    public void cycleChoke(Direction side) { if (kind==Kind.BUFFER) { int index=side.get3DDataValue(); choke[index]=(byte)((choke[index]+1)%3); host.changed(); } }
    public Aspect filter() { return filter; }
    public void filter(Aspect value) { if (kind==Kind.FILTER) { filter=value; host.changed(); } }
    public boolean allowFlow() { return allowFlow; }
    public void toggleValve() { if (kind==Kind.VALVE) { allowFlow=!allowFlow; host.changed(); host.valveSound(); } }
    public int venting() { return venting; }
    public AspectAmounts contents() { return kind==Kind.BUFFER ? buffer.copy() : storedType==null ? new AspectAmounts() : new AspectAmounts().add(storedType,amount); }
    public void tick() {
        if (kind==Kind.BUFFER) {
            count++; if (bellows<0 || count%20==0) bellows=host.bellows();
            if (count%5==0 && buffer.total()<8) fillBuffer(); return;
        }
        // Valve power edges are sampled before the base tube advances its counter, including while venting.
        if (kind==Kind.VALVE && count%5==0) {
            boolean powered=host.powered();
            if (hadPower && !powered && !allowFlow || !hadPower && powered && allowFlow) toggleValve();
            hadPower=powered;
        }
        if (venting>0) venting--;
        if (count==0) count=host.random().nextInt(10);
        if (venting>0) return;
        if (++count%2==0) {
            calculateSuction(); checkVenting();
            if (amount==0) storedType=null;
        }
        if (venting==0 && count%5==0 && suction>0) equalize();
    }
    void calculateSuction() {
        int previous=suction; Aspect previousType=suctionType; suction=0; suctionType=null;
        Aspect selectedFilter=kind==Kind.FILTER ? filter : null;
        for (var side:Direction.values()) {
            if (kind==Kind.ONEWAY && facing!=side.getOpposite() || !connectable(side)) continue;
            var other=host.neighbor(side); if (other==null) continue;
            var incoming=other.suctionType(side.getOpposite());
            if (selectedFilter!=null && incoming!=null && !selectedFilter.equals(incoming)) continue;
            if (amount>0 && incoming!=null && (selectedFilter==null || storedType!=null) && !Objects.equals(storedType,incoming)) continue;
            int strength=other.suction(side.getOpposite());
            if (strength<=0 || (long)strength<=suction+1L) continue;
            if (kind!=Kind.VALVE || allowFlow) {
                suctionType=incoming==null ? selectedFilter : incoming;
                suction=kind==Kind.RESTRICTED ? strength/2 : strength-1;
            }
        }
        if (previous!=suction || !Objects.equals(previousType,suctionType)) host.changed();
    }
    private void checkVenting() {
        if (suction<=0) return;
        for (var side:Direction.values()) {
            if (!connectable(side)) continue;
            var other=host.neighbor(side); if (other==null) continue;
            int strength=other.suction(side.getOpposite());
            if ((strength==suction || strength==suction-1) && !Objects.equals(suctionType,other.suctionType(side.getOpposite()))) {
                venting=40; host.vent(suctionType);
            }
        }
    }
    private void equalize() {
        if (amount>0) return;
        for (var side:Direction.values()) {
            if (kind==Kind.ONEWAY && facing==side.getOpposite() || !connectable(side)) continue;
            var other=host.neighbor(side); var opposite=side.getOpposite();
            if (other==null || !other.canOutput(opposite)) continue;
            if (suctionType!=null && other.essentiaType(opposite)!=null && !suctionType.equals(other.essentiaType(opposite))) continue;
            if (suction<=other.suction(opposite) || suction<other.minimumSuction()) continue;
            Aspect selected=suctionType;
            if (selected==null) { selected=other.essentiaType(opposite); if (selected==null) selected=other.essentiaType(null); }
            if (selected==null) continue;
            int accepted=addEssentia(selected,other.takeEssentia(selected,1,opposite),side);
            if (accepted>0) { if (host.random().nextInt(100)==0) host.creak(); return; }
        }
    }
    private void fillBuffer() {
        for (var side:Direction.values()) {
            if (!canInput(side)) continue;
            var other=host.neighbor(side); var opposite=side.getOpposite();
            if (other==null || !other.canOutput(opposite) || other.essentiaAmount(opposite)<=0 || other.suction(opposite)>=suction(side) || suction(side)<other.minimumSuction()) continue;
            var selected=other.essentiaType(opposite);
            if (selected!=null && other.takeEssentia(selected,1,opposite)==1) { addToBuffer(selected,1); return; }
        }
    }
    private int addToBuffer(Aspect selected,int quantity) {
        if (quantity!=1 || selected==null || buffer.total()>=8) return quantity;
        buffer.add(selected,1); host.changed(); return 0;
    }
    @Override public boolean connectable(Direction side) { return side!=null && (kind!=Kind.VALVE || side!=facing) && open(side); }
    @Override public boolean canInput(Direction side) { return connectable(side) && (kind!=Kind.VALVE || allowFlow) && (kind!=Kind.ONEWAY || side!=facing.getOpposite()); }
    @Override public boolean canOutput(Direction side) { return connectable(side) && (kind!=Kind.VALVE || allowFlow) && (kind!=Kind.ONEWAY || side==facing.getOpposite()); }
    @Override public Aspect suctionType(Direction side) { return kind==Kind.BUFFER ? null : suctionType; }
    @Override public int suction(Direction side) { return kind!=Kind.BUFFER ? suction : choke(side)==2 ? 0 : bellows<=0 || choke(side)==1 ? 1 : bellows*32; }
    @Override public int minimumSuction() { return 0; }
    @Override public Aspect essentiaType(Direction side) {
        if (kind!=Kind.BUFFER) return storedType;
        var types=buffer.aspects(); return types.isEmpty() ? null : types.get(host.random().nextInt(types.size()));
    }
    @Override public int essentiaAmount(Direction side) { return kind==Kind.BUFFER ? (int)buffer.total() : amount; }
    @Override public int addEssentia(Aspect selected,int quantity,Direction side) {
        if (!canInput(side) || selected==null || kind==Kind.FILTER && filter!=null && !filter.equals(selected)) return 0;
        if (kind==Kind.BUFFER) return quantity-addToBuffer(selected,quantity);
        if (amount>0 || quantity<=0) return 0;
        storedType=selected; amount=1; host.changed(); return 1;
    }
    @Override public int takeEssentia(Aspect selected,int quantity,Direction side) {
        if (!canOutput(side) || quantity<=0) return 0;
        if (kind!=Kind.BUFFER) {
            if (amount<=0 || !Objects.equals(storedType,selected)) return 0;
            amount--; if (amount==0) storedType=null; host.changed(); return 1;
        }
        if (selected==null) return 0;
        var requester=host.neighbor(side); int requested=requester==null ? 0 : requester.suction(side.getOpposite());
        for (var alternate:Direction.values()) {
            if (alternate==side || !canOutput(alternate)) continue;
            var other=host.neighbor(alternate); if (other==null) continue;
            int strength=other.suction(alternate.getOpposite()); var type=other.suctionType(alternate.getOpposite());
            if ((type==null || type.equals(selected)) && requested<strength && suction(alternate)<strength) return 0;
        }
        int taken=Math.min(quantity,buffer.amount(selected));
        if (taken>0) { buffer.reduce(selected,taken); if (buffer.amount(selected)==0) buffer.remove(selected); host.changed(); }
        return taken;
    }
    public CompoundTag save(boolean full) {
        var tag=new CompoundTag(); var sides=new byte[6]; for (int index=0;index<6;index++) sides[index]=(byte)(open[index] ? 1 : 0); tag.putByteArray("open",sides);
        if (kind==Kind.BUFFER) { AspectNbt.write(tag,buffer); tag.putByteArray("choke",choke.clone()); }
        else {
            if (storedType!=null) tag.putString("type",storedType.tag()); tag.putInt("amount",amount); tag.putInt("side",facing.get3DDataValue());
            if (full) { if (suctionType!=null) tag.putString("stype",suctionType.tag()); tag.putInt("samount",suction); }
        }
        if (kind==Kind.FILTER && filter!=null) tag.putString("AspectFilter",filter.tag());
        if (kind==Kind.VALVE) { tag.putBoolean("flow",allowFlow); tag.putBoolean("hadpower",hadPower); }
        if (full) { tag.putInt("TickPhase",Math.floorMod(count,20)); tag.putInt("Venting",venting); }
        return tag;
    }
    public void load(CompoundTag tag,boolean full) {
        var sides=tag.getByteArray("open"); if (sides.length==6) for (int index=0;index<6;index++) open[index]=sides[index]==1;
        if (kind==Kind.BUFFER) {
            buffer=AspectNbt.read(tag,TCAspects.catalog()).amounts();
            int space=8; var bounded=new AspectAmounts();
            for (var aspect:buffer.aspects()) {
                int kept=Math.min(space,Math.max(0,buffer.amount(aspect)));
                if (kept>0) { bounded.add(aspect,kept); space-=kept; }
            }
            buffer=bounded;
            var savedChokes=tag.getByteArray("choke"); for (int index=0;index<6;index++) choke[index]=savedChokes.length>=6 ? (byte)Math.max(0,Math.min(2,savedChokes[index])) : 0;
        } else {
            storedType=TCAspects.catalog().find(tag.getString("type")).orElse(null); amount=Math.max(0,Math.min(1,tag.getInt("amount")));
            facing=Direction.from3DDataValue(tag.getInt("side"));
            if (full) { suctionType=TCAspects.catalog().find(tag.getString("stype")).orElse(null); suction=Math.max(0,tag.getInt("samount")); }
        }
        filter=TCAspects.catalog().find(tag.getString("AspectFilter")).orElse(null);
        if (kind==Kind.VALVE) { allowFlow=tag.getBoolean("flow"); hadPower=tag.getBoolean("hadpower"); }
        if (full) { count=Math.floorMod(tag.getInt("TickPhase"),20); venting=Math.max(0,Math.min(40,tag.getInt("Venting"))); }
        bellows=-1;
    }
}
