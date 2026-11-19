package tc4revival.research;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import tc4revival.data.BookDefinitionMigration;
import static org.junit.jupiter.api.Assertions.*;
class LegacyBookImportTest {
 @Test void olderAddonPagesReceiveStableUniqueIdsAndTranslationComponents(){
  var original=JsonParser.parseString("[{\"key\":\"OLD\",\"pages\":[{\"type\":\"TEXT\",\"text\":\"legacy.first\"},{\"type\":\"TEXT\",\"text\":\"legacy.second\"}]}]").getAsJsonArray();
  var first=original.deepCopy();BookDefinitionMigration.upgradeMissingIds(first,"addon");
  var pages=ResearchPages.decode(first).require("OLD");
  assertEquals(java.util.List.of("addon:OLD/page/1","addon:OLD/page/2"),pages.stream().map(ResearchPage::id).toList());
  assertEquals("legacy.first",pages.get(0).text().orElseThrow());assertTrue(pages.get(0).pageBreak());
  var second=original.deepCopy();BookDefinitionMigration.upgradeMissingIds(second,"addon");assertEquals(first,second);
  BookDefinitionMigration.upgradeMissingIds(first,"addon");assertEquals(second,first);
 }
 @Test void explicitModernComponentsRemainByteForByteEquivalentAndInvalidIdsStillFail(){
  var source=JsonParser.parseString("[{\"key\":\"addon:NEW\",\"pages\":[{\"id\":\"addon:literal\",\"type\":\"TEXT\",\"text\":\"literal text\",\"page_break\":false}]}]").getAsJsonArray();
  var copy=source.deepCopy();BookDefinitionMigration.upgradeMissingIds(copy,"addon");assertEquals(source,copy);
  copy.get(0).getAsJsonObject().getAsJsonArray("pages").get(0).getAsJsonObject().addProperty("id","");
  BookDefinitionMigration.upgradeMissingIds(copy,"addon");assertThrows(IllegalArgumentException.class,()->ResearchPages.decode(copy));
 }
}
