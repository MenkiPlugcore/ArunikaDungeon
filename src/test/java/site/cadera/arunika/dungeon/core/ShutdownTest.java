package site.cadera.arunika.dungeon.core;
import java.util.*;
public final class ShutdownTest {
    public static void main(String[] args) {
        var actions=new ArrayList<String>(); var failed=new ArrayList<String>();
        var cleanup=new ShutdownSteps((name,error)->failed.add(name+":"+error.getClass().getSimpleName()));
        cleanup.run("first",()->actions.add("first"));
        cleanup.run("gui",()->{throw new IllegalStateException("GUI failed");});
        cleanup.run("dependency",()->{throw new NoClassDefFoundError("missing provider");});
        cleanup.run("database",()->actions.add("database"));
        if(!actions.equals(List.of("first","database")))throw new AssertionError(actions);
        if(!failed.equals(List.of("gui:IllegalStateException","dependency:NoClassDefFoundError")))throw new AssertionError(failed);
        try {cleanup.run("fatal",()->{throw new AssertionError("fatal");});throw new IllegalStateException("Fatal swallowed");}
        catch(AssertionError expected){if(!"fatal".equals(expected.getMessage()))throw expected;}
        System.out.println("PASS shutdown: 3 checks (continue, report, do not swallow fatal Error)");
    }
}
