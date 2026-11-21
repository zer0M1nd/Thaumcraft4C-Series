package thaumcraft.api.research;

import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.Event;
/** Posted on the Forge bus on the client before the research tooltip is laid out. */
public class ResearchTooltipEvent extends Event {
    private final String research;
    private final boolean locked;
    private Component description;
    public ResearchTooltipEvent(String research, boolean locked, Component description) {
        this.research=research; this.locked=locked; this.description=description;
    }
    public String research() { return research; }
    public boolean locked() { return locked; }
    public Component description() { return description; }
    public void setDescription(Component value) { description=java.util.Objects.requireNonNull(value); }
}
