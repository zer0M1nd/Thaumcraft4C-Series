package thaumicomniverse.compat.create.airship;

import java.util.*;
import net.minecraft.world.phys.Vec3;

/** Persistent node-local connector. Entry/exit distances are measured outwards from the node. */
public record FlightBend(UUID id,UUID node,UUID incoming,UUID outgoing,boolean passing,
                         double entry,double exit,double radius,boolean pivot,String name,int color,FlightCurve curve,boolean lockEndDirections) {
    public FlightBend(UUID id,UUID node,UUID incoming,UUID outgoing,boolean passing,double entry,double exit,double radius,boolean pivot,String name,int color,FlightCurve curve){this(id,node,incoming,outgoing,passing,entry,exit,radius,pivot,name,color,curve,true);}
    public FlightBend {
        Objects.requireNonNull(id);Objects.requireNonNull(node);Objects.requireNonNull(incoming);Objects.requireNonNull(outgoing);Objects.requireNonNull(curve);
        name=FlightGraph.checkedName(name);
        if(!Double.isFinite(entry)||!Double.isFinite(exit)||!Double.isFinite(radius)||entry<0||exit<0||radius<0||color<0||color>15)
            throw new IllegalArgumentException("airship.error.manoeuvre");
    }
    public boolean pair(UUID a,UUID b){return incoming.equals(a)&&outgoing.equals(b)||incoming.equals(b)&&outgoing.equals(a);}
    public boolean matches(UUID at,UUID a,UUID b,boolean pass){return node.equals(at)&&passing==pass&&(pass?pair(a,b):incoming.equals(a)&&outgoing.equals(b));}
    public FlightBend reversed(){return new FlightBend(id,node,outgoing,incoming,passing,exit,entry,radius,pivot,name,color,curve.reverse(),lockEndDirections);}
    public static boolean needed(FlightGraph.Node n,FlightGraph.Route a,FlightGraph.Route b){
        return !FlightSteering.sameDirection(a.leaving(n.id()).tangent(0).scale(-1),b.leaving(n.id()).tangent(0));
    }
    public static FlightBend generate(FlightGraph.Node n,FlightGraph.Route a,FlightGraph.Route b,boolean passing){
        double radius=FlightManeuver.defaultRadius();
        for(int attempt=0;attempt<24;attempt++){
            try{return generate(n,a,b,passing,radius);}catch(IllegalArgumentException failed){if(radius==0)throw failed;radius*=.8;}
        }
        throw new IllegalArgumentException("airship.error.manoeuvre");
    }
    private static FlightBend generate(FlightGraph.Node n,FlightGraph.Route a,FlightGraph.Route b,boolean passing,double radius){
        var in=a.leaving(n.id());var out=b.leaving(n.id());
        // Stable vanilla dye IDs, without bootstrapping registries in this geometry layer.
        if(radius==0)return new FlightBend(UUID.randomUUID(),n.id(),a.id(),b.id(),passing,0,0,0,true,"",passing?14:11,FlightCurve.straight(n.point(),n.point()));
        Vec3 arrival=in.tangent(0).scale(-1),departure=out.tangent(0);
        double angle=FlightSteering.angle(arrival,departure);
        double trim=Math.max(.5,radius*1.03*Math.tan(Math.min(angle,Math.PI*.7)/2));
        if(!passing)trim=Math.max(radius*2,trim);
        FlightCurve best=null;double bestEntry=0,bestExit=0;
        for(double scale:new double[]{.65,1,.85,1.25,1.6,2.2})for(double skew:new double[]{1,.6,1.6}){
            double entry=passing?Math.min(in.length()*.49,trim*scale*skew):0;
            double exit=Math.min(out.length()*.49,trim*scale/skew);
            var start=passing?in.position(entry):n.point();var end=out.position(exit);
            var adir=passing?in.tangent(entry).scale(-1):arrival;var bdir=out.tangent(exit);
            var candidate=FlightManeuver.compact(start,adir,end,bdir,radius,best==null?FlightManeuver.MAXIMUM_LENGTH:best.length());
            if(candidate!=null){best=candidate;bestEntry=entry;bestExit=exit;}
            if(best!=null&&best.length()<=FlightManeuver.PREFERRED_LENGTH)break;
        }
        if(best==null)throw new IllegalArgumentException("airship.error.manoeuvre");
        return new FlightBend(UUID.randomUUID(),n.id(),a.id(),b.id(),passing,bestEntry,bestExit,radius,false,"",passing?14:11,best);
    }
}
