package tc4revival.content;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import thaumcraft.api.golems.GolemLocations;

/** Projects persistent frames for navigation; container access always keeps the original identity. */
public class GolemMobileBindings {
    public static boolean refresh(GolemEntity golem){
        if(!(golem.level() instanceof ServerLevel level)||!GolemLocations.enabled())return false;
        var state=golem.state();
        if(state.homeAnchor()==null)state.homeAnchor(GolemLocations.bind(level,golem.homeInventory(),golem.homeFacing()));
        boolean moving=GolemLocations.moving(level,state.homeAnchor());
        var home=GolemLocations.resolve(level,state.homeAnchor());
        if(home!=null){state.projectHome(BlockPos.containing(home.center()).relative(home.face()),home.face());golem.restrictTo(golem.home(),32);}
        var markers=new ArrayList<GolemMarker>();boolean changed=false;
        for(var marker:golem.markers().inDimension(level.dimension().location())){
            var anchor=marker.anchor();
            if(anchor==null){anchor=GolemLocations.bind(level,marker.pos(),marker.face());changed=true;}
            moving|=GolemLocations.moving(level,anchor);
            var target=GolemLocations.resolve(level,anchor);
            var next=new GolemMarker(target==null?marker.pos():BlockPos.containing(target.center()),marker.dimension(),target==null?marker.face():target.face(),marker.color(),anchor);
            changed|=!next.equals(marker);markers.add(next);
        }
        if(changed)golem.markers().replace(markers);
        return moving;
    }
    private GolemMobileBindings(){}
}
