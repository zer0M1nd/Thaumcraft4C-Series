package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.api.golems.*;

@Mod.EventBusSubscriber(modid="thaumcraft")
public class GolemAnchorViewEvents {
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||player.tickCount%5!=0||!GolemLocations.enabled())return;
        for(var hand:InteractionHand.values()){
            var stack=player.getItemInHand(hand);if(!(stack.getItem() instanceof GolemBellItem)&&!(stack.getItem() instanceof GolemPlacerItem)||!stack.hasTag())continue;
            var tag=stack.getTag();var views=new CompoundTag();
            var golem=stack.getItem() instanceof GolemBellItem?GolemBellItem.selected(stack,player.level()):null;
            if(golem!=null){
                GolemMobileBindings.refresh(golem);tag.put("markers",golem.markers().save());
                var anchor=golem.state().homeAnchor();
                if(anchor!=null){tag.put("golemhomeanchor",anchor.save());var view=GolemLocations.view(player.serverLevel(),anchor);if(!view.isEmpty())views.put(anchor.id().toString(),view);}
                tag.putInt("golemhomex",golem.home().getX());tag.putInt("golemhomey",golem.home().getY());tag.putInt("golemhomez",golem.home().getZ());tag.putInt("golemhomeface",golem.homeFacing().get3DDataValue());
            }
            var updated=new ListTag();
            for(var entry:tag.getList("markers",Tag.TAG_COMPOUND)){
                var marker=GolemMarker.load((CompoundTag)entry).orElse(null);if(marker==null)continue;
                if(marker.dimension().equals(player.level().dimension().location())){
                    var anchor=marker.anchor()==null?GolemLocations.bind(player.serverLevel(),marker.pos(),marker.face()):marker.anchor();
                    var target=GolemLocations.resolve(player.serverLevel(),anchor);
                    marker=new GolemMarker(target==null?marker.pos():BlockPos.containing(target.center()),marker.dimension(),target==null?marker.face():target.face(),marker.color(),anchor);
                    var view=GolemLocations.view(player.serverLevel(),anchor);if(!view.isEmpty())views.put(anchor.id().toString(),view);
                }updated.add(marker.save());
            }
            tag.put("markers",updated);tag.put("MovingMarkerFrames",views);
        }
    }
}
