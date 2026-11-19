package wtmodern;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
public class PassiveCreeper extends Creeper {
    public PassiveCreeper(EntityType<? extends Creeper> type,Level level){super(type,level);}
    @Override public int getSwellDir(){return -1;}
    @Override public void ignite(){}
    @Override public boolean isIgnited(){return false;}
    @Override public boolean canBeLeashed(Player p){return !isLeashed();}
}
