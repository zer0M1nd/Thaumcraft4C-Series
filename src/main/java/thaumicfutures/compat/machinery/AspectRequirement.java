package thaumicfutures.compat.machinery;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.reborn.modularmachinery.machine.HatchCollection;
import com.reborn.modularmachinery.recipe.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;

public final class AspectRequirement extends MachineRequirement {
    public final boolean vis;
    public final String aspectTag;
    public final int amount;
    public final float chance;
    public AspectRequirement(boolean vis, IOType io, String aspect, int amount, boolean perTick, float chance) {
        super(io, perTick);
        if (aspect == null || aspect.isBlank() || aspect.length() > 128 || amount <= 0
                || !Float.isFinite(chance) || chance < 0 || chance > 1) throw new IllegalArgumentException("Invalid aspect requirement");
        this.vis = vis;
        aspectTag = aspect;
        this.amount = amount;
        this.chance = chance;
    }
    public Aspect aspect() {
        var aspect = TCAspects.catalog().find(aspectTag).orElse(null);
        return aspect != null && (!vis || aspect.isPrimal()) ? aspect : null;
    }
    public AspectPort.Kind portKind() {
        return vis ? (ioType() == IOType.INPUT ? AspectPort.Kind.VIS_INPUT : AspectPort.Kind.VIS_OUTPUT)
                : (ioType() == IOType.INPUT ? AspectPort.Kind.ESSENTIA_INPUT : AspectPort.Kind.ESSENTIA_OUTPUT);
    }
    public long amountFor(int parallel) { return (long) amount * (isParallelizeUnaffected() ? 1 : Math.max(1, parallel)); }
    @Override public RecipeModifier.Target modifierTarget() { return RecipeModifier.Target.ALL; }
    @Override public boolean canSatisfy(HatchCollection ports) {
        return thaumicfutures.FutureFeatures.Group.MACHINERY.enabled() && aspect() != null && MachineAspectPorts.available(ports, portKind(), aspect()) >= amountFor(parallelism());
    }
    @Override public String startFailure(HatchCollection ports) { return "Missing " + describe(); }
    @Override public int parallelLimit(HatchCollection ports, RecipeModifiers modifiers, int ceiling) {
        if(!thaumicfutures.FutureFeatures.Group.MACHINERY.enabled())return 0;
        long available = MachineAspectPorts.available(ports, portKind(), aspect());
        return (int) Math.min(ceiling, available / amount);
    }
    @Override public boolean apply(HatchCollection ports, RandomSource random) {
        if (!canSatisfy(ports)) return false;
        return random.nextFloat() >= chance || MachineAspectPorts.transfer(ports, portKind(), aspect(), amountFor(parallelism()));
    }
    @Override public boolean tick(HatchCollection ports, RandomSource random) { return apply(ports, random); }
    @Override public String describe() {
        return amount + " " + aspectTag + (vis ? " centivis" : " essentia") + (perTick() ? "/tick" : "") + " " + ioType();
    }
    public static boolean customKind(String type) { return type.equals("thaumicfutures:essentia") || type.equals("thaumicfutures:vis"); }
    public static AspectRequirement parse(JsonObject json) {
        try {
            var requirement = new AspectRequirement(json.get("type").getAsString().endsWith(":vis"),
                    IOType.byName(json.get("io-type").getAsString()), json.get("aspect").getAsString(),
                    json.get("amount").getAsBigDecimal().intValueExact(),
                    json.has("per-tick") && json.get("per-tick").getAsBoolean(),
                    json.has("chance") ? json.get("chance").getAsFloat() : 1);
            requirement.setParallelizeUnaffected(json.has("parallelize-unaffected") && json.get("parallelize-unaffected").getAsBoolean());
            return requirement;
        } catch (RuntimeException exception) { throw new JsonParseException("Invalid thaumicomniverse aspect requirement: " + json, exception); }
    }
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(vis ? "thaumicfutures:vis" : "thaumicfutures:essentia");
        buffer.writeUtf(ioType().name());
        buffer.writeUtf(aspectTag, 128);
        buffer.writeVarInt(amount);
        buffer.writeBoolean(perTick());
        buffer.writeFloat(chance);
        buffer.writeBoolean(isParallelizeUnaffected());
    }
    public static AspectRequirement read(String type, FriendlyByteBuf buffer) {
        var result = new AspectRequirement(type.endsWith(":vis"), IOType.valueOf(buffer.readUtf()), buffer.readUtf(128),
                buffer.readVarInt(), buffer.readBoolean(), buffer.readFloat());
        result.setParallelizeUnaffected(buffer.readBoolean());
        return result;
    }
}
