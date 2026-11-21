package site.cadera.arunika.dungeon.core;
public final class PerformanceGuardTest {
 public static void main(String[] args){var g=new PerformanceGuard();check(g.sample(20,30,18,50));check(!g.sample(17,30,18,50));for(int i=0;i<4;i++)check(!g.sample(20,30,18,50));check(g.sample(20,30,18,50));check(!g.sample(20,51,18,50));check(!g.sample(18.1,30,18,50));check(!g.sample(Double.NaN,30,18,50));System.out.println("PerformanceGuard: 10 PASS");}
 static void check(boolean ok){if(!ok)throw new AssertionError("guard hysteresis");}
}
