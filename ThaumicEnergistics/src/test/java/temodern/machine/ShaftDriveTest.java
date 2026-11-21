package temodern.machine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShaftDriveTest {
    @Test void oneShaftRequiresThreeClicks(){var drive=new ShaftDrive();var active=new boolean[]{true,false,false,false,false,false};assertFalse(drive.crank(active)[0]);assertFalse(drive.crank(active)[0]);assertTrue(drive.crank(active)[0]);}
    @Test void sixShaftsSharePower(){var drive=new ShaftDrive();var active=new boolean[]{true,true,true,true,true,true};for(int i=0;i<17;i++)assertArrayEquals(new boolean[6],drive.crank(active));assertArrayEquals(active,drive.crank(active));}
    @Test void disconnectedShaftLosesPartialPower(){var drive=new ShaftDrive();var active=new boolean[]{true,false,false,false,false,false};drive.crank(active);drive.crank(active);drive.crank(new boolean[6]);assertFalse(drive.crank(active)[0]);}
}
