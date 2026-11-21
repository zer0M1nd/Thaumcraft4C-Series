package temodern.machine;

import java.util.UUID;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import tc4revival.aspect.Aspect;

/** Shared binding contract for legacy blocks and cable-mounted interfaces. */
public interface VisSupply {
    UUID visIdentity();
    boolean visInput();
    IManagedGridNode node();
    int takeVis(Aspect aspect,int amount);
    static VisSupply find(IGrid grid,UUID id,Object except){
        if(grid==null||id==null)return null;
        for(var host:grid.getActiveMachines(NetworkMachine.class))
            if(host!=except&&host.visInput()&&id.equals(host.visIdentity()))return host;
        for(var host:grid.getActiveMachines(VisInterfacePart.class))
            if(host!=except&&host.visInput()&&id.equals(host.visIdentity()))return host;
        return null;
    }
}
