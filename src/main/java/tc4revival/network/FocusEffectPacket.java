package tc4revival.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/** Server-selected visual effects only; this packet never authorizes damage or block changes. */
public record FocusEffectPacket(ResourceLocation dimension, Kind kind, Vec3 start, Vec3 end, float scale, long seed) {
    public enum Kind { LIGHTNING, BURST, BLOCK_SPARKLE, BLOCK_ARC, SONIC, MIST_SHORT, MIST_LONG, WARP_EVENT, WARP_RELIEF, ELEMENTAL_BLOCK_BUBBLE, NODE_ZAP }
    public void encode(FriendlyByteBuf out) {
        out.writeResourceLocation(dimension); out.writeEnum(kind);
        out.writeDouble(start.x); out.writeDouble(start.y); out.writeDouble(start.z);
        out.writeDouble(end.x); out.writeDouble(end.y); out.writeDouble(end.z);
        out.writeFloat(scale); out.writeLong(seed);
    }
    public static FocusEffectPacket decode(FriendlyByteBuf in) {
        return new FocusEffectPacket(in.readResourceLocation(), in.readEnum(Kind.class),
                new Vec3(in.readDouble(), in.readDouble(), in.readDouble()), new Vec3(in.readDouble(), in.readDouble(), in.readDouble()), in.readFloat(), in.readLong());
    }
    public static void lightning(Level level, Entity from, Entity to) {
        send(level, new FocusEffectPacket(level.dimension().location(), Kind.LIGHTNING,
                from.getBoundingBox().getCenter(), to.getBoundingBox().getCenter(), .125f, level.random.nextLong()));
    }
    public static void sonic(Entity source) {
        Level level=source.level(); if (level.isClientSide) return;
        var packet=new FocusEffectPacket(level.dimension().location(),Kind.SONIC,source.position(),source.position(),1,source.getId());
        TCNetwork.CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(source.getX(),source.getY(),source.getZ(),32,level.dimension())),packet);
    }
    public static void lightning(Level level,Vec3 from,Vec3 to) {
        send(level,new FocusEffectPacket(level.dimension().location(),Kind.LIGHTNING,from,to,.125f,level.random.nextLong()));
    }
    public static void nodeZap(Level level,Vec3 from,Vec3 to) {
        if (!level.isClientSide) TCNetwork.CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(from.x,from.y,from.z,32,level.dimension())),
                new FocusEffectPacket(level.dimension().location(),Kind.NODE_ZAP,from,to,1,0));
    }
    public static void burst(Level level, Vec3 position, float scale) {
        send(level, new FocusEffectPacket(level.dimension().location(), Kind.BURST, position, position, scale, level.random.nextLong()));
    }
    public static void blockArc(Level level,Entity source,net.minecraft.core.BlockPos destination,boolean crimson) {
        send(level,new FocusEffectPacket(level.dimension().location(),Kind.BLOCK_ARC,
                source.position().add(0,source.getBbHeight()/2,0),Vec3.atLowerCornerOf(destination).add(.5,1,.5),.5f,crimson ? 1 : 0));
    }
    public static void blockSparkle(Level level, net.minecraft.core.BlockPos pos, int color) {
        blockSparkle(level,pos,color,1);
    }
    public static void elementalBlockBubble(Level level,net.minecraft.core.BlockPos pos) {
        var at=Vec3.atLowerCornerOf(pos);
        if (!level.isClientSide) TCNetwork.CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(at.x,at.y,at.z,32,level.dimension())),
                new FocusEffectPacket(level.dimension().location(),Kind.ELEMENTAL_BLOCK_BUBBLE,at,at,1,0));
    }
    public static void blockSparkle(Level level, net.minecraft.core.BlockPos pos, int color,int count) {
        var at = Vec3.atLowerCornerOf(pos);
        send(level, new FocusEffectPacket(level.dimension().location(), Kind.BLOCK_SPARKLE, at, at, count, color));
    }
    private static void send(Level level, FocusEffectPacket packet) {
        if (!level.isClientSide) TCNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                packet.start.x, packet.start.y, packet.start.z, 64, level.dimension())), packet);
    }
}
