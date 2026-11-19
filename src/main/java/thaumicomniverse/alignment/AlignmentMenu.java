package thaumicomniverse.alignment;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;


public class AlignmentMenu extends AbstractContainerMenu {
    public final AlignmentCoreEntity altar;
    private final Player owner;
    public CompoundTag view=new CompoundTag();
    private int tick;
    public AlignmentMenu(int id,Inventory inventory,FriendlyByteBuf buf) {this(id,inventory,(AlignmentCoreEntity)inventory.player.level().getBlockEntity(buf.readBlockPos()));}
    public AlignmentMenu(int id,Inventory inventory,AlignmentCoreEntity altar) {super(AlignmentContent.MENU.get(),id);this.altar=altar;owner=inventory.player;}
    @Override public boolean stillValid(Player player) {return player==owner&&altar!=null&&altar.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index) {return ItemStack.EMPTY;}
    @Override public boolean clickMenuButton(Player player,int button) {
        if(button!=0||!(player instanceof ServerPlayer server)||!stillValid(player))return false;
        var wand=AlignmentAccess.wandAllowed(player.getMainHandItem())?player.getMainHandItem():player.getOffhandItem();
        boolean result=altar.start(server,wand);sendView();return result;
    }
    @Override public void broadcastChanges() {super.broadcastChanges();if(tick++%10==0)sendView();}
    private static boolean recognized(ServerPlayer player,String aspect) {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("thaumicexpedition"))return true;
        try{return (boolean)Class.forName("thaumicexpedition.aspect.HiddenAspects").getMethod("recognized",tc4revival.research.PlayerKnowledge.class,String.class)
            .invoke(null,tc4revival.research.KnowledgeSavedData.get(player.server).snapshot(player.getUUID()).orElse(null),aspect);
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Hidden-aspect integration unavailable",e);}
    }
    public void sendView() {
        if(!(owner instanceof ServerPlayer player)||player.containerMenu!=this||altar==null)return;
        var tag=altar.progress();tag.putString("diagnostic",altar.diagnostic);
        if(!altar.running()) {
            try {
                var plan=altar.preview(player);
                if(plan!=null) {
                    tag.putString("recipe",plan.recipe().id());tag.putBoolean("exact",plan.exact());tag.putBoolean("ingredients",plan.ingredients());tag.putInt("yield",plan.yield());tag.putDouble("risk",plan.risk());tag.put("result",plan.recipe().result().copyWithCount(plan.yield()).save(new CompoundTag()));
                    var keys=new TreeSet<>(plan.target().summary().vector().keySet());keys.addAll(plan.actual().vector().keySet());var rows=new ListTag();boolean unknown=false;
                    for(var aspect:keys) {
                        if(!recognized(player,aspect)){unknown=true;continue;}
                        var row=new CompoundTag();row.putString("aspect",aspect);long target=plan.target().summary().vector().getOrDefault(aspect,0L),current=plan.actual().vector().getOrDefault(aspect,0L);
                        row.putLong("target",target);row.putLong("current",current);row.putLong("delta",target-current);rows.add(row);
                    }
                    tag.put("rows",rows);tag.putBoolean("unknown",unknown);tag.putLong("count",plan.actual().count());tag.putInt("kinds",plan.actual().kinds());
                }
            }catch(IllegalArgumentException|ArithmeticException failure){tag.putString("diagnostic",failure.getMessage());}
        }
        AlignmentNetwork.send(player,containerId,tag);
    }
}
