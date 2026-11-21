package tc4revival.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import tc4revival.research.ScanRules;

/** Transient presentation of one server-completed attempt; never changes the client knowledge cache. */
public record ScanFeedbackPacket(ScanRules.Status status, String missingParent, List<Reward> rewards) {
    public record Reward(String aspect, int gainedAmount, boolean discovery) {
        public Reward(String aspect,short gained,boolean discovery) {this(aspect,(int)gained,discovery);}
        /** Saturated compatibility view; new consumers should use gainedAmount(). */
        public short gained() { return (short)Math.max(0,Math.min(Short.MAX_VALUE,gainedAmount)); }
    }
    public ScanFeedbackPacket {
        java.util.Objects.requireNonNull(status);
        java.util.Objects.requireNonNull(missingParent);
        rewards = List.copyOf(rewards);
        if (rewards.size() > 1024 || missingParent.length() > 64
                || rewards.stream().anyMatch(reward -> reward.gainedAmount()<0 || !reward.aspect().matches("[a-z0-9_:./-]{1,64}"))) {
            throw new IllegalArgumentException("Invalid scan feedback");
        }
        if (status != ScanRules.Status.SUCCESS && !rewards.isEmpty()) throw new IllegalArgumentException("Rejected scan cannot reward");
        if ((status == ScanRules.Status.MISSING_PARENT) != !missingParent.isEmpty()) throw new IllegalArgumentException("Invalid missing parent");
    }
    public static ScanFeedbackPacket from(ScanRules.Result result) {
        return new ScanFeedbackPacket(result.status(), result.missingParent().map(value -> value.tag()).orElse(""),
                result.rewards().stream().map(value -> new Reward(value.aspect().tag(), value.gained(), value.discovery())).toList());
    }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(status).writeUtf(missingParent, 64).writeVarInt(rewards.size());
        for (var reward : rewards) buffer.writeUtf(reward.aspect(), 64).writeVarInt(reward.gainedAmount()).writeBoolean(reward.discovery());
    }
    public static ScanFeedbackPacket decode(FriendlyByteBuf buffer) {
        try {
        var status = buffer.readEnum(ScanRules.Status.class);
        var missing = buffer.readUtf(64);
        int count = buffer.readVarInt();
        if (count < 0 || count > 1024) throw new IllegalArgumentException("Invalid reward count");
        var rewards = new ArrayList<Reward>(count);
        for (int i = 0; i < count; i++) rewards.add(new Reward(buffer.readUtf(64), buffer.readVarInt(), buffer.readBoolean()));
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing scan feedback");
        return new ScanFeedbackPacket(status, missing, rewards);
        } catch(IndexOutOfBoundsException truncated) { throw new IllegalArgumentException("Truncated scan feedback",truncated); }
    }
}
