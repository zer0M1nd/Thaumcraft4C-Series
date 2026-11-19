package wtmodern;

import java.util.Collection;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BloodVisualStateTest {
    @Test void repeatedBloodPacketsAccumulateUntilTheServerClearsTheEffect() throws Exception {
        var field = WarpClient.class.getDeclaredField("BLOODY");
        field.setAccessible(true);
        var sources = (Collection<?>) field.get(null);
        var position = new BlockPos(0, 60, 0);
        try {
            WarpClient.accept(new WarpNetwork.Visual("clear", BlockPos.ZERO, 0));
            for (int i = 0; i < 6; i++)
                WarpClient.accept(new WarpNetwork.Visual("blood", position, 0));
            WarpClient.accept(new WarpNetwork.Visual("blood", position.east(), 0));
            assertEquals(7, sources.size(), "Repeated hits on one ceiling must increase the drip density");
            assertEquals(6, sources.stream().filter(position::equals).count());
            WarpClient.accept(new WarpNetwork.Visual("clear_blood", BlockPos.ZERO, 0));
            assertTrue(sources.isEmpty());
            WarpClient.accept(new WarpNetwork.Visual("blood", position, 0));
            WarpClient.accept(new WarpNetwork.Visual("clear", BlockPos.ZERO, 0));
            assertTrue(sources.isEmpty());
        } finally {
            WarpClient.accept(new WarpNetwork.Visual("clear", BlockPos.ZERO, 0));
        }
    }
}
