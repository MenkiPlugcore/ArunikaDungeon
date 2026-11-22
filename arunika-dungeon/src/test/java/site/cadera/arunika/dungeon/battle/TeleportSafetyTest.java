package site.cadera.arunika.dungeon.battle;
public final class TeleportSafetyTest {
    private static int n;
    private static void check(boolean actual,boolean expected,String label){
        if(actual!=expected)throw new AssertionError(label+": expected "+expected+" got "+actual);
        n++;System.out.println("PASS "+label);
    }
    public static void main(String[] args){
        check(TeleportSafety.allowExternal(false,false,false,false,false),false,"outsider cannot enter protected arena");
        check(TeleportSafety.allowExternal(true,false,false,false,false),true,"survival teleport unaffected");
        check(TeleportSafety.allowExternal(false,true,true,true,false),false,"core reject wins");
        check(TeleportSafety.allowExternal(true,true,true,true,false),true,"inside-arena skill teleport remains possible");
        check(TeleportSafety.allowExternal(true,true,true,true,true),false,"vanilla command teleport blocked inside arena");
        check(TeleportSafety.allowExternal(true,true,false,true,false),false,"outside-to-arena reentry blocked");
        check(TeleportSafety.allowExternal(true,true,true,false,false),false,"living participant cannot escape arena");
        check(TeleportSafety.allowExternal(true,true,false,false,false),false,"living participant cannot warp outside");
        check(TeleportSafety.allowExternal(true,false,true,false,true),true,"nonparticipant voluntary exit remains possible");
        check(TeleportSafety.allowExternal(true,false,false,false,true),true,"survival command teleport unaffected");
        check(TeleportSafety.allowExternal(false,false,true,false,false),false,"configured arena access remains protected");
        check(TeleportSafety.allowExternal(true,true,false,true,true),false,"command reentry blocked");
        System.out.println("TeleportSafety: "+n+" PASS");
    }
}
