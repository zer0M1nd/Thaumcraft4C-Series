package advancedthaumaturgy;
import java.util.*;

/** Original AT2-30r costs and eligibility, independent of the client menu. */
public enum NodeOperation {
    REMOVE_UNSTABLE("ordo",64,"",1), REMOVE_SINISTER("victus",64,"",2), REMOVE_TAINTED("ordo",64,"",4),
    ADD_BRIGHT("auram",64,"auram",1), REMOVE_HUNGRY("messis",32,"messis",1), REMOVE_PALE("auram",32,"",2),
    REMOVE_FADING("lux",32,"",2), ADD_ASPECT("auram",32,"",4), INCREASE_ASPECT("auram",64,"auram",4),
    ADD_HUNGRY("fames",64,"fames",2), ADD_TAINTED("vitium",64,"vitium",2), ADD_SINISTER("exanimis",32,"exanimis",2),
    ADD_PURE("auram",32,"auram",4);
    public final String essentia, essence;
    public final int amount, essences;
    NodeOperation(String essentia,int amount,String essence,int essences) { this.essentia=essentia;this.amount=amount;this.essence=essence;this.essences=essences; }
    public String key() { return "advancedthaumaturgy.node." + name().toLowerCase(Locale.ROOT); }
    public boolean available(String type, String modifier, int aspectCount) {
        return switch (this) {
            case REMOVE_UNSTABLE -> type.equals("UNSTABLE");
            case REMOVE_SINISTER -> type.equals("DARK");
            case REMOVE_TAINTED -> type.equals("TAINTED");
            case REMOVE_HUNGRY -> type.equals("HUNGRY");
            case REMOVE_PALE -> modifier.equals("PALE");
            case REMOVE_FADING -> modifier.equals("FADING");
            case ADD_BRIGHT -> modifier.isEmpty();
            case ADD_PURE -> modifier.isEmpty() && !type.equals("PURE");
            case ADD_HUNGRY, ADD_TAINTED, ADD_SINISTER -> type.equals("NORMAL");
            case ADD_ASPECT -> aspectCount < 6;
            case INCREASE_ASPECT -> aspectCount > 0;
        };
    }
    public static Optional<NodeOperation> byId(int id) { return id >= 0 && id < values().length ? Optional.of(values()[id]) : Optional.empty(); }
}
