package site.cadera.arunika.dungeon.core;
/** Main-thread state; one sample per second from the caller. Fail closed on invalid metrics. */
public final class PerformanceGuard {
 private boolean healthy=true;private int good;
 public boolean sample(double tps,double mspt,double minTps,double maxMspt){
  if(!Double.isFinite(tps)||!Double.isFinite(mspt)||tps<minTps||mspt<0||mspt>maxMspt){healthy=false;good=0;}
  else if(!healthy){if(tps>=Math.min(20,minTps+0.25)&&mspt<=maxMspt*0.95){if(++good>=5){healthy=true;good=0;}}else good=0;}
  return healthy;
 }
 public boolean healthy(){return healthy;}
}
