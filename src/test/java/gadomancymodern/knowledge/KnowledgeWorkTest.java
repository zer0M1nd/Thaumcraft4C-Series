package gadomancymodern.knowledge;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
public class KnowledgeWorkTest {
    @Test void eachResearchAspectRetainsTenMinimumFourfoldBudget(){var work=KnowledgeWork.initial(Map.of("aer",1,"cognitio",12));assertEquals(10,work.get("aer"));assertEquals(48,work.get("cognitio"));}
    @Test void libraryHasOriginalFloorAndCeiling(){assertEquals(300,KnowledgeWork.chance(0));assertEquals(298,KnowledgeWork.chance(2));assertEquals(100,KnowledgeWork.chance(200));assertEquals(100,KnowledgeWork.chance(500));}
    @Test void selectedAspectConsumesOneUnitWithoutTouchingOthers(){var work=KnowledgeWork.initial(Map.of("aer",1));for(int i=0;i<9;i++)KnowledgeWork.progress(work,0);assertEquals(1,work.get("aer"));KnowledgeWork.progress(work,0);assertTrue(work.isEmpty());KnowledgeWork.progress(work,0);}
}
