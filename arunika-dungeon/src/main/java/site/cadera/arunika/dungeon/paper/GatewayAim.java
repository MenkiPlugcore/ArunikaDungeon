package site.cadera.arunika.dungeon.paper;

/** Pure geometry for deliberate Bedrock air taps at a registered Waystone.
 *  Has no Bukkit dependencies; deterministic and unit-testable.
 */
final class GatewayAim {
    record Vec(double x,double y,double z) {
        Vec add(Vec v){return new Vec(x+v.x,y+v.y,z+v.z);}
        Vec subtract(Vec v){return new Vec(x-v.x,y-v.y,z-v.z);}
        double dot(Vec v){return x*v.x+y*v.y+z*v.z;}
        double lengthSquared(){return dot(this);}
    }
    record Hit(double distance,double deviationSquared) {}
    private GatewayAim(){}

    /** 3 positions within the visible model, relative to the registered anchor BLOCK origin.
     *  Rejects NaN, out-of-range and non-unit directions. No block occlusion is checked here.
     */
    static Hit match(Vec eye,Vec direction,Vec anchor,double maxDistance,double tolerance){
        if(!finite(eye)||!finite(direction)||!finite(anchor)||!Double.isFinite(maxDistance)||
                !Double.isFinite(tolerance)||maxDistance<=0||tolerance<=0||
                Math.abs(direction.lengthSquared()-1)>0.03)return null;
        Hit best=null;
        for(double y:new double[]{0.6,1.3,2.0}){
            Vec target=anchor.add(new Vec(0.5,y,0.5));
            Vec diff=target.subtract(eye);
            double along=diff.dot(direction);
            if(along<0.25||along>maxDistance)continue;
            Vec projection=new Vec(direction.x*along,direction.y*along,direction.z*along);
            double off=diff.subtract(projection).lengthSquared();
            if(off<=tolerance*tolerance&&(best==null||off<best.deviationSquared))best=new Hit(along,off);
        }
        return best;
    }
    private static boolean finite(Vec p){return Double.isFinite(p.x)&&Double.isFinite(p.y)&&Double.isFinite(p.z);}
}
