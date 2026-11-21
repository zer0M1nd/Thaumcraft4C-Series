package automagy.logic;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RequisitionAnimationTest {
    @Test void poweredBookLiftsAndOpensWithoutConstantPageFlutter(){
        var a=new RequisitionAnimation();
        for(int i=0;i<60;i++)a.tick(true,bound->bound-1);
        assertEquals(1,a.open);assertTrue(a.bookY>0&&a.bookY<=.1);assertEquals(0,a.page);
        for(int i=0;i<3000;i++){a.tick(true,bound->bound-1);assertTrue(a.bookY>=0&&a.bookY<=.1);}
        for(int i=0;i<60;i++)a.tick(false,bound->bound-1);
        assertEquals(0,a.open);assertEquals(0,a.page);
    }
    @Test void pageSequenceEndsAfterPowerLoss(){
        var a=new RequisitionAnimation();for(int i=0;i<60;i++)a.tick(true,bound->bound-1);
        assertTrue(a.tick(true,bound->0));assertEquals(.1f,a.page);
        for(int i=0;i<20;i++)assertFalse(a.tick(false,bound->0));
        assertEquals(0,a.page);
    }
}
