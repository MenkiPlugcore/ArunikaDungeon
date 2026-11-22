package site.cadera.arunika.dungeon.paper;
public final class GatewayAimTest {
  static int n;
  static void yes(boolean cond,String name){if(!cond)throw new AssertionError(name);n++;System.out.println("PASS "+name);}
  public static void main(String[]args){
    var anchor=new GatewayAim.Vec(0,64,0);
    var eye=new GatewayAim.Vec(.5,65.3,-3);
    var toward=new GatewayAim.Vec(0,0,1);
    yes(GatewayAim.match(eye,toward,anchor,6,.75)!=null,"hit the model");
    yes(GatewayAim.match(eye,new GatewayAim.Vec(0,0,-1),anchor,6,.75)==null,"behind player denied");
    yes(GatewayAim.match(eye,toward,anchor,2,.75)==null,"beyond range denied");
    yes(GatewayAim.match(new GatewayAim.Vec(2,65.3,-3),toward,anchor,6,.75)==null,"side miss denied");
    yes(GatewayAim.match(eye,new GatewayAim.Vec(0,0,2),anchor,6,.75)==null,"nonunit direction denied");
    yes(GatewayAim.match(eye,new GatewayAim.Vec(Double.NaN,0,0),anchor,6,.75)==null,"NaN direction denied");
    yes(GatewayAim.match(eye,toward,anchor,6,Double.NaN)==null,"NaN tolerance denied");
    yes(GatewayAim.match(eye,toward,anchor,-1,.75)==null,"negative range denied");
    yes(GatewayAim.match(eye,toward,anchor,6,.1)!=null,"precise aim accepted");
    yes(GatewayAim.match(new GatewayAim.Vec(.5,66.3,-3),toward,anchor,6,.75)!=null,"upper model accepted");
    yes(GatewayAim.match(new GatewayAim.Vec(.5,64.6,-3),toward,anchor,6,.75)!=null,"lower model accepted");
    System.out.println("GatewayAim tests passed: "+n);
  }
}
