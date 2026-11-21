package tc4revival.research;

import com.google.gson.JsonParser;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChineseBookTypographyTest {
    private String n(String s){return ChineseBookTypography.normalize(s,"zh_cn");}
    @Test void punctuationAndSpacingInBothLocales() {
        String source="测试AE2功能, 中文 文字. 按shift! (真的?)";
        String expected="测试 AE2 功能，中文文字。按 shift！（真的？）";
        assertEquals(expected,n(source));
        assertEquals("測試 AE2 功能，中文文字。按 shift！（真的？）",ChineseBookTypography.normalize(source.replace("测试","測試"),"zh_tw"));
    }
    @Test void unrelatedLanguagesAndEnglishFallbackAreIdentical() {
        for(String locale:List.of("en_us","ja_jp","zh_hk","de_de"))assertEquals("中文AE2, .",ChineseBookTypography.normalize("中文AE2, .",locale));
        assertEquals("English,  text... (x)!",n("English,  text... (x)!"));
    }
    @Test void englishFallbackWithSemanticRangesOnlyAddsAspectSpacing() {
        String source="Use\uE000Aer, then\uE000Terra... (2.0)!";
        int first=source.indexOf('\uE000'),second=source.indexOf('\uE000',first+1);
        var ranges=List.of(new ChineseBookTypography.Range(first,first+4,'A','A'),new ChineseBookTypography.Range(second,second+6,'A','A'));
        for(String locale:List.of("zh_cn","zh_tw")) {
            assertEquals("Use \uE000Aer, then \uE000Terra... (2.0)!",ChineseBookTypography.normalize(source,locale,List.of(),ranges).text());
            String heading="Heading Text, untouched.";
            assertEquals(heading,ChineseBookTypography.normalize(heading,locale,List.of(),List.of(new ChineseBookTypography.Range(7,8,(char)0,(char)0))).text());
        }
    }
    @Test void technicalTokensAndAmbiguousDecimalsStayIntact() {
        String text="数值0.25和.65、值为.12，80,000,000；1.20.1，3x3，-20、1-5，AE2与Direwolf20，%s和%1$s。";
        assertEquals("数值0.25和.65、值为.12，80,000,000；1.20.1，3x3，-20、1-5，AE2 与 Direwolf20，%s和%1$s。",n(text));
        assertEquals("从10转为5，3转为12。",n("从10转为5,3转为12."));
    }
    @Test void urlsIdsInitialsAndApostrophesStayIntact() {
        assertEquals("访问 https://a.org/x?a=1&b=2。物品 mod:item，使用 A.C.T 和 don't。",n("访问https://a.org/x?a=1&b=2.物品mod:item,使用A.C.T和don't."));
        assertEquals("按 [Esc]-[Options]。",n("按 [Esc]-[Options]."));
        assertEquals("详见【选项】-【控制】。",n(n("详见[选项]-[控制].")));
    }
    @Test void pairedQuotesAndEllipsesAreIdempotent() {
        String value=n("他说\"里面'文字'\"... -- 然后…继续.");
        assertEquals("他说“里面‘文字’”……——然后……继续。",value);
        assertEquals(value,n(value));
        assertEquals("未配对\"文字。",n("未配对\"文字."));
    }
    @Test void resourcePhrasesAndStructuralGapsArePreserved() {
        String source="支持CC: Tweaked的击中-扫描功能。标题 正文。";
        int gap=source.indexOf(" 正文");
        assertEquals("支持 CC: Tweaked 的击中-扫描功能。标题 正文。",ChineseBookTypography.normalize(source,"zh_cn",List.of("CC: Tweaked","击中-扫描"),List.of(new ChineseBookTypography.Range(gap,gap+1,(char)0,(char)0))).text());
    }
    @Test void aspectIconAndNameAreOneWordWithNeutralExternalSpaces() {
        String text="含\uE000Aer和\uE000Ignis2点";
        var result=ChineseBookTypography.normalize(text,"zh_tw",List.of(),List.of(new ChineseBookTypography.Range(1,5,'A','A'),new ChineseBookTypography.Range(6,12,'A','A')));
        assertEquals("含 \uE000Aer 和 \uE000Ignis 2点",result.text());
        for(int i=0;i<result.text().length();i++)if(result.text().charAt(i)==' ')assertEquals(-1,result.origins()[i]);
    }
    @Test void glyphOriginsRetainStylesAcrossDeletionAndExpansion() {
        var result=ChineseBookTypography.normalize("汉 字...AE2中文","zh_cn",List.of(),List.of());
        assertEquals("汉字……AE2 中文",result.text());
        for(int i=0;i<result.text().length();i++) {
            char c=result.text().charAt(i);int from=result.origins()[i];
            if(c=='…')assertEquals(3,from);
            else if(c==' ')assertEquals(-1,from);
            else assertEquals(c,"汉 字...AE2中文".charAt(from));
        }
    }
    @Test void newlinesDiagramsAndIndentationSurvive() {
        assertEquals("  中文\nR.L\n .N\n下文。",n("  中 文\nR.L\n .N\n下文."));
    }
    @Test void resourceDiffsAreGuardedAndLocaleScoped() {
        var rule=BookTextPatches.decode(JsonParser.parseString("""
                {"format":1,"languages":["zh_cn","zh_tw"],"patches":[{"keys":["page"],"find":"力量 低","replace":"力量：低","count":1}]}
                """).getAsJsonObject());
        var warnings=new ArrayList<String>();
        assertEquals("力量：低",rule.apply("zh_tw","page","力量 低",warnings::add));
        assertEquals("力量 低",rule.apply("en_us","page","力量 低",warnings::add));
        assertEquals("力量 低",rule.apply("zh_cn","other","力量 低",warnings::add));
        assertEquals("玩家改写",rule.apply("zh_cn","page","玩家改写",warnings::add));
        assertTrue(warnings.isEmpty());
        assertEquals("力量 低、力量 低",rule.apply("zh_cn","page","力量 低、力量 低",warnings::add));
        assertEquals(1,warnings.size());
        assertThrows(IllegalArgumentException.class,()->BookTextPatches.decode(JsonParser.parseString("{\"format\":1,\"languages\":[\"en_us\"]}").getAsJsonObject()));
    }
}
