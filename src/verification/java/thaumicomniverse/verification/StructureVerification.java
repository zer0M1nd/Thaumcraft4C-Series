package thaumicomniverse.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import tc4revival.content.EldritchRingStructure;
import thaumicomniverse.content.ObeliskClaims;
import thaumicomniverse.content.ObeliskSearch;
import static thaumicomniverse.verification.ServerVerification.*;

final class StructureVerification {
    static void run(MinecraftServer server) {
        if (!Boolean.getBoolean("omniverse.verify.structures")) return;
        check("natural obelisk search, boss filter and conquered exclusion", () -> {
            var level = server.overworld();
            var origin = new BlockPos(0, 80, 0);
            var any = new ObeliskSearch(level, origin, -1);
            for (int i = 0; !any.complete() && i < 256; i++) any.step();
            require(any.complete() && any.result() != null, "No natural obelisk in 256 search regions");
            var first = any.result();
            int boss = -1;
            for (var start : level.getChunk(first.getX() >> 4, first.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS).getAllStarts().values())
                for (var piece : start.getPieces()) if (piece instanceof EldritchRingStructure.Piece)
                    boss = piece.createTag(StructurePieceSerializationContext.fromLevel(level)).getInt("Boss");
            require(boss >= 0, "Target has no native structure piece");
            var filtered = new ObeliskSearch(level, origin, boss);
            for (int i = 0; !filtered.complete() && i < 256; i++) filtered.step();
            require(filtered.complete() && first.equals(filtered.result()), "Boss filter loses nearest matching structure");
            ObeliskClaims.get(server).conquer(first.getX() >> 4, first.getZ() >> 4);
            var next = new ObeliskSearch(level, origin, -1);
            for (int i = 0; !next.complete() && i < 256; i++) next.step();
            require(next.complete() && next.result() != null && !first.equals(next.result()), "Conquered obelisk still selected");
            System.out.println("OMNIVERSE_STRUCTURE first=" + first + " boss=" + boss + " next=" + next.result());
        });
    }
}
