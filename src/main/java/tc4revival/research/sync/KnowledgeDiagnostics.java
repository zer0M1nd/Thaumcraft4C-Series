package tc4revival.research.sync;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import tc4revival.research.KnowledgeNbt;
import tc4revival.research.PlayerKnowledge;
import tc4revival.research.PlayerKnowledge.ScanKind;

/** Comparable server/client diagnostics; does not request data from the opposite side. */
public  class KnowledgeDiagnostics {
    private KnowledgeDiagnostics() { }
    public static String describe(PlayerKnowledge knowledge) {
        String digest;
        try {
            digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(KnowledgeNbt.write(knowledge).toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        return "aspects=" + knowledge.knownAspects().size() + "; research=" + knowledge.researchMarkers().size()
                + "; scans=" + knowledge.scannedKeys(ScanKind.OBJECT).size() + "/" + knowledge.scannedKeys(ScanKind.ENTITY).size()
                + "/" + knowledge.scannedKeys(ScanKind.PHENOMENON).size() + "; warp=" + knowledge.totalWarp()
                + "; counter=" + knowledge.warpCounter() + "; SHA256=" + digest;
    }
}
