package site.cadera.arunika.dungeon.paper;
import java.util.*;
public final class GatewayMenuStyleTest {
    private static int passed;
    private static void ok(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        passed++; System.out.println("PASS " + label);
    }
    public static void main(String[] ignored) {
        ok(GatewayMenuStyle.pages(0)==1, "empty state still one page");
        ok(GatewayMenuStyle.pages(36)==1, "exactly one page");
        ok(GatewayMenuStyle.pages(37)==2, "extra entry creates page two");
        ok(GatewayMenuStyle.pages(Integer.MAX_VALUE)>0, "page count does not overflow");
        ok(GatewayMenuStyle.page(-1,0)==0, "negative page clamped");
        ok(GatewayMenuStyle.page(999,37)==1, "oversized page clamped");
        ok(GatewayMenuStyle.page(1,0)==0, "shrinking list returns to page zero");
        ok(GatewayMenuStyle.pageLabel(9,10).equals("Page 1 / 1"), "safe page label");
        ok(GatewayMenuStyle.slot(0,35)==35, "last slot first page");
        ok(GatewayMenuStyle.slot(1,36)==0, "first slot second page");
        ok(GatewayMenuStyle.slot(0,36)==-1, "page boundary excluded");
        ok(GatewayMenuStyle.slot(Integer.MAX_VALUE,3)==-1, "slot overflow safe");
        ok(GatewayMenuStyle.compact("Dungeon",42).equals("Dungeon"), "normal title preserved");
        ok(!GatewayMenuStyle.compact("§cDungeon",42).contains("§"), "unsafe formatting removed");
        ok(GatewayMenuStyle.compact("😀".repeat(80),42).codePointCount(0,GatewayMenuStyle.compact("😀".repeat(80),42).length())<=42, "emoji truncated without splitting surrogate pairs");
        ok(GatewayMenuStyle.compact("X".repeat(100),42).endsWith("..."), "long name ellipsis");
        ok(GatewayMenuStyle.lore(null).isEmpty(), "null lore safe");
        ok(GatewayMenuStyle.lore(List.of("a")).size()==1, "normal lore preserved");
        ok(GatewayMenuStyle.lore(Collections.nCopies(12,"Long description")).size()==7, "large lore bounded");
        ok(GatewayMenuStyle.lore(Collections.nCopies(12,"Long description")).get(6).startsWith("..."), "truncation indicated");
        ok(GatewayMenuStyle.lore(List.of("Y".repeat(100))).get(0).length()<=60, "lore line width bounded");
        System.out.println("GatewayMenuStyle: "+passed+" PASS");
    }
}
