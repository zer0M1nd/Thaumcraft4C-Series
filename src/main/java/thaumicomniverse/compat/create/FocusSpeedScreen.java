package thaumicomniverse.compat.create;

import com.simibubi.create.content.kinetics.motor.KineticScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import tc4revival.client.WandKeys;

/** The actual Create speed board; only its activation key and save destination differ. */
public final class FocusSpeedScreen extends ValueSettingsScreen implements ThaumicCreateScreen {
    private final InteractionHand hand;
    public FocusSpeedScreen(InteractionHand hand,int rpm) {
        super(BlockPos.ZERO,new KineticScrollValueBehaviour(
                Component.translatable("create.kinetics.speed_controller.rotation_speed"),null,null)
                .createBoard(Minecraft.getInstance().player,null),
                new ValueSettings(rpm<0?0:1,Math.abs(rpm)),v->{},0);
        this.hand=hand;
    }
    @Override protected void saveAndClose(double x,double y) {
        var selected=getClosestCoordinate((int)x,(int)y);
        CreateFocusNetwork.send(hand,CreateFocusNetwork.SET_SPEED,
                Math.max(1,selected.value())*(selected.row()==0?-1:1));
        onClose();
    }
    @Override public boolean keyReleased(int key,int scan,int modifiers) {
        if(WandKeys.CHANGE_AREA.matches(key,scan)){confirm();return true;}
        return false;
    }
    @Override public boolean mouseReleased(double x,double y,int button) {
        if(WandKeys.CHANGE_AREA.matchesMouse(button)){saveAndClose(x,y);return true;}
        return false;
    }
    private void confirm(){
        var window=minecraft.getWindow();
        saveAndClose(minecraft.mouseHandler.xpos()/window.getGuiScale(),minecraft.mouseHandler.ypos()/window.getGuiScale());
    }
    @Override public boolean isPauseScreen(){return false;}
}
