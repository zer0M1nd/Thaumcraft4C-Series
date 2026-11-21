package thaumicomniverse.compat.create.airship;

import com.electronwill.nightconfig.core.CommentedConfig;
import org.junit.jupiter.api.BeforeEach;
import thaumicomniverse.RevivalFeatures;

/** Geometry tests run outside Forge's world/config lifecycle. */
abstract class FlightTestConfig {
    @BeforeEach void loadServerDefaults() {
        var config = CommentedConfig.inMemory();
        RevivalFeatures.SPEC.correct(config);
        RevivalFeatures.SPEC.setConfig(config);
    }
}
