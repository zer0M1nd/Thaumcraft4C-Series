package tc4revival.content;

import java.util.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EldritchMazeTest {
    @Test void generatedLayoutsHaveReachableKeyAndAllFourBossQuarters() {
        for (int size:new int[]{7,11,21,31}) for (long seed:new long[]{0,17,-129}) {
            var maze=EldritchMazeLayout.generate(size,size,seed);
            assertArrayEquals(maze.packedCells(),EldritchMazeLayout.generate(size,size,seed).packedCells());
            var reached=new HashSet<EldritchMazeLayout.Point>(); var queue=new ArrayDeque<EldritchMazeLayout.Point>();
            queue.add(maze.entrance()); reached.add(maze.entrance());
            while (!queue.isEmpty()) {
                var point=queue.removeFirst(); var cell=maze.cell(point.x(),point.z());
                for (int direction:new int[]{1,2,4,8}) {
                    var next=new EldritchMazeLayout.Point(point.x()+EldritchMazeLayout.dx(direction),point.z()+EldritchMazeLayout.dz(direction));
                    var other=maze.cell(next.x(),next.z());
                    boolean sharedHall=cell.feature()>=2 && cell.feature()<=5 && other.feature()>=2 && other.feature()<=5;
                    if (!cell.opens(direction) && !sharedHall) continue;
                    assertTrue(other.packed()!=0,"Passage must lead to a room");
                    if (!sharedHall) assertTrue(other.opens(EldritchMazeLayout.opposite(direction)),"Passages must be reciprocal");
                    if (reached.add(next)) queue.addLast(next);
                }
            }
            int[] features=new int[15];
            for (int z=0;z<size;z++) for (int x=0;x<size;x++) {
                var cell=maze.cell(x,z); if (cell.packed()==0) continue;
                assertTrue(reached.contains(new EldritchMazeLayout.Point(x,z)),"Unreachable room at "+x+","+z);
                features[cell.feature()]++;
            }
            for (int feature=1;feature<=6;feature++) assertEquals(1,features[feature]);
        }
    }
    @Test void reservationsSurviveReloadAndOnlyPublishRemovesThem() {
        var data=new EldritchMazeSavedData();
        var request=new EldritchMazeSavedData.Reservation(-200,300,11,11,921);
        assertTrue(data.reserve(request));
        data=EldritchMazeSavedData.load(data.save(new CompoundTag()));
        assertEquals(List.of(request),List.copyOf(data.reservations()));
        assertTrue(data.publish(-200,300,EldritchMazeLayout.generate(11,11,921)));
        assertTrue(data.reservations().isEmpty()); assertFalse(data.isGenerated(-200,300));
        data.markGenerated(-200,300);
        var snapshot=data.save(new CompoundTag()); var loaded=EldritchMazeSavedData.load(snapshot);
        assertTrue(loaded.isGenerated(-200,300)); assertEquals(snapshot,loaded.save(new CompoundTag()));
        assertFalse(loaded.publish(-200,300,EldritchMazeLayout.generate(11,11,2)));
        assertEquals(snapshot,loaded.save(new CompoundTag()),"Overlap rejection must not mutate saved data");
    }
    @Test void corruptPlansAndOrphanProgressArePreservedInsteadOfSilentlyCleared() {
        var data=new EldritchMazeSavedData(); data.publish(0,0,EldritchMazeLayout.generate(11,11,44));
        CompoundTag valid=data.save(new CompoundTag());
        for (int variant=0;variant<4;variant++) {
            CompoundTag broken=valid.copy();
            if (variant==0) { var list=new ListTag(); list.add(StringTag.valueOf("bad plan")); broken.put("Plans",list); }
            if (variant==1) broken.putLongArray("Generated",new long[]{Long.MAX_VALUE});
            if (variant==2) broken.getList("Plans",Tag.TAG_COMPOUND).getCompound(0).putString("Width","11");
            if (variant==3) broken.putString("Reservations","bad reservations");
            var loaded=EldritchMazeSavedData.load(broken);
            assertFalse(loaded.readable()); assertEquals(broken,loaded.save(new CompoundTag()));
            assertThrows(IllegalStateException.class,()->loaded.reserve(new EldritchMazeSavedData.Reservation(100,100,11,11,0)));
        }
        assertThrows(IllegalArgumentException.class,()->new EldritchMazeSavedData.Reservation(1874999,0,31,31,0));
        assertThrows(IllegalArgumentException.class,()->EldritchMazeLayout.restore(11,11,0,new int[5]));
    }
}
