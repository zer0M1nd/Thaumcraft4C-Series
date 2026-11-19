package automagy.logic;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ScribeAnimationTest {
    @Test void poweredQuillTravelsToBookAndReturnsToInkwell(){
        var a=new ScribeAnimation();
        for(int i=0;i<300;i++)a.tick(true,bound->bound-1);
        assertEquals(-.5,a.quillHoriz,1e-8);assertTrue(a.quillY>=.15&&a.quillY<=.2);
        for(int i=0;i<1000;i++)a.tick(false,bound->bound-1);
        assertEquals(0,a.quillHoriz);assertEquals(0,a.quillR);assertEquals(0,a.quillY);
        assertTrue(a.bookY>=0&&a.bookY<=.01);
    }
    @Test void pagesFinishAfterPowerIsLost(){
        var a=new ScribeAnimation();assertTrue(a.tick(true,bound->0));assertEquals(.1f,a.page);
        for(int i=0;i<12;i++)assertFalse(a.tick(false,bound->0));
        assertEquals(0,a.page);
    }
}
