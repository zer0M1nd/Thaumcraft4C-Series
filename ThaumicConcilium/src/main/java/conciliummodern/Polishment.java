package conciliummodern;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import thaumcraft.api.crafting.InfusionCommit;
import thaumcraft.api.crafting.InfusionLifecycle;

public final class Polishment {
    private Polishment() {}
    public static void setup(){
        InfusionLifecycle.registerCommitParticipant("thaumicconcilium:polishment",100,context->{
            if(!(context.matrix().getLevel() instanceof ServerLevel level))return Optional.empty();
            var requirement=PolishmentRecipe.find(level,context.result());if(requirement.isEmpty())return Optional.empty();
            var player=level.getServer().getPlayerList().getPlayerByName(context.playerName());
            var operation=player==null?null:ConciliumItems.VIS_CONDUCTOR.get().preparePolishment(player,context.matrix().getBlockPos(),requirement.get());
            return Optional.of(new InfusionCommit.Operation(){
                public boolean validate(){return operation!=null&&operation.validate();}
                public boolean commit(){return operation!=null&&operation.commit();}
                public void abort(){
                    try{if(operation!=null)operation.abort();}finally{
                        if(player!=null){ConciliumItems.VIS_CONDUCTOR.get().cancelPolishment(player);
                            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.thaumicconcilium.polishment_failed"),true);}
                    }
                }
                public void completed(){operation.completed();}
            });
        });
    }
}
