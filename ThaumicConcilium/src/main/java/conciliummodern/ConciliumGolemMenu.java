package conciliummodern;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.*;
import tc4revival.content.*;

public final class ConciliumGolemMenu extends GolemMenu {
    private final ResourceLocation task;
    public ConciliumGolemMenu(int id,Inventory inventory,GolemEntity golem){super(ConciliumGolems.MENU.get(),id,inventory,golem);task=golem.configuration().customTask();}
    public ConciliumGolemMenu(int id,Inventory inventory,FriendlyByteBuf buffer){this(id,inventory,opening(inventory,buffer));}
    private static GolemEntity opening(Inventory inventory,FriendlyByteBuf buffer){
        var tracked=inventory.player.level().getEntity(buffer.readInt());var snapshot=buffer.readNbt();
        var golem=tracked instanceof GolemEntity existing?existing:new GolemEntity(TCEntities.GOLEM.get(),inventory.player.level());
        if(snapshot!=null)golem.state().applyVisualData(snapshot);if(!(tracked instanceof GolemEntity))golem.discard();return golem;
    }
    public boolean valet(){return ConciliumGolems.VALET.equals(task);}
    @Override public boolean stillValid(Player player){return super.stillValid(player)&&ConciliumGolems.owned(golem(),player)&&ConciliumGolems.ours(golem())&&java.util.Objects.equals(task,golem().configuration().customTask());}
    @Override public boolean clickMenuButton(Player player,int button){
        if(button==66||button==67)return super.clickMenuButton(player,button);
        if(!valet())return false;
        if(button>=50&&button<=52||button>=0&&button<sampleCount()*2&&golem().configuration().upgrades(4)>0)return super.clickMenuButton(player,button);
        return false;
    }
}
