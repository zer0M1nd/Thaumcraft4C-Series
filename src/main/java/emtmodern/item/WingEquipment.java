package emtmodern.item;

/** Data-driven wing parameters; addon implementations can provide additional profiles. */
public interface WingEquipment {
    WingFlight.Profile wingProfile();
}
