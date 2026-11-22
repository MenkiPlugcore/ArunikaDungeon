package site.cadera.arunika.dungeon.paper;

import java.util.*;

public final class GatewayPhase6Test {
    private static int n;
    private static void check(boolean condition,String name){if(!condition)throw new AssertionError(name);n++;System.out.println("PASS "+name);}
    private static void rejects(Runnable r,String name){boolean failed=false;try{r.run();}catch(IllegalArgumentException e){failed=true;}check(failed,name);}
    public static void main(String[] args){
        GatewayRateLimiter rate=new GatewayRateLimiter(350,3);
        UUID a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID(),d=UUID.randomUUID();
        check(rate.ready(a,1000),"fresh player accepted");
        rate.commit(a,1000);check(!rate.ready(a,1000),"same tick denied");
        check(!rate.ready(a,1349),"cooldown not elapsed");
        check(rate.ready(a,1350),"cooldown boundary accepted");
        check(rate.size()==1,"single map entry");
        rate.commit(b,1000);rate.commit(c,1000);check(rate.size()==3,"cap filled");
        rate.commit(d,1001);check(rate.size()==3,"cap enforced");
        check(rate.ready(a,1001),"eldest entry evicted");
        check(!rate.ready(d,1001),"newest entry retained");
        rate.commit(b,1400);check(!rate.ready(b,1401),"repeat updates cooldown");
        check(rate.size()==3,"repeat does not grow map");
        rate.remove(b);check(rate.ready(b,1401),"quit releases entry");
        rate.clear();check(rate.size()==0,"close releases all entries");
        rejects(()->new GatewayRateLimiter(0,1),"reject zero cooldown");
        rejects(()->new GatewayRateLimiter(350,0),"reject zero capacity");
        GatewayWorkBudget.Window empty=GatewayWorkBudget.next(0,0,16);
        check(empty.count()==0&&empty.next()==0,"empty menus do no work");
        GatewayWorkBudget.Window shortMenus=GatewayWorkBudget.next(8,0,16);
        check(shortMenus.count()==8&&shortMenus.next()==0,"small menu set refreshes fully");
        GatewayWorkBudget.Window large=GatewayWorkBudget.next(50,0,16);
        check(large.count()==16&&large.next()==16,"large set bounded to 16");
        boolean[] seen=new boolean[50];int cursor=0;
        for(int cycle=0;cycle<4;cycle++){
            var w=GatewayWorkBudget.next(50,cursor,16);
            for(int i=0;i<w.count();i++)seen[(w.start()+i)%50]=true;
            cursor=w.next();
        }
        check(java.util.Arrays.stream(toInt(seen)).sum()==50,"round robin reaches every menu within four refreshes");
        check(GatewayWorkBudget.next(50,48,16).next()==14,"wrap around is safe");
        check(GatewayWorkBudget.next(50,-2,16).start()==48,"negative cursor normalized");
        rejects(()->GatewayWorkBudget.next(10,0,0),"reject zero refresh budget");
        System.out.println("GatewayPhase6Test: "+n+" PASS");
    }
    private static int[] toInt(boolean[] values){int[] n=new int[values.length];for(int i=0;i<n.length;i++)n[i]=values[i]?1:0;return n;}
}
