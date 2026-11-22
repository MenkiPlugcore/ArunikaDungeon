package site.cadera.arunika.dungeon.paper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AdminInputsTest {
 @Test void categoryCannotInjectCommandsOrFormat(){assertEquals("Senjata Legend",AdminInputs.category(" Senjata Legend "));for(String text:new String[]{"a\nstop","§aTest","a\u200bb","", "x".repeat(61),"/op me"})assertThrows(IllegalArgumentException.class,()->AdminInputs.category(text));}
 @Test void boundedPriceInput(){assertEquals(1,AdminInputs.price("1"));assertEquals(1000000,AdminInputs.price("1000000"));for(String text:new String[]{"0","-1","1000001","2147483648","1e3","10\nstop"," 10"})assertThrows(IllegalArgumentException.class,()->AdminInputs.price(text));}
}
