package site.cadera.arunika.dungeon.exchange;
import java.util.*;
import java.io.IOException;
/** Validated immutable view; preserve legacy YAML independently of this view. */
public final class ExchangeCatalog {
 public static final UUID TICKET=UUID.fromString("e79d39a9-1c13-44be-8ab1-f93077ec0700");
 public enum Type { ITEM, PERMISSION, COMMAND, BUNDLE, TICKET }
 public record Grant(String node,long seconds,String duplicate){}
 public record Reward(String key,UUID id,Type type,String item,int cost,int stock,int limit,boolean enabled,int revision,String category,boolean bundle,List<Grant> grants){public Reward{grants=List.copyOf(grants);}}
 public record Catalog(String token,List<Reward> rewards,boolean permissionEnabled,Set<String> allowed){public Catalog{rewards=List.copyOf(rewards);allowed=Set.copyOf(allowed);}}
 @SuppressWarnings("unchecked") public static Map<String,Object> map(Object o)throws IOException{if(!(o instanceof Map<?,?> m))throw new IOException("Expected mapping");for(Object k:m.keySet())if(!(k instanceof String))throw new IOException("Non-string key");return new LinkedHashMap<>((Map<String,Object>)m);}
 static int num(Map<String,Object> m,String k,int lo,int hi)throws IOException{Object v=m.get(k);if(!(v instanceof Number n)||n.doubleValue()!=n.longValue()||n.longValue()<lo||n.longValue()>hi)throw new IOException("Invalid "+k);return n.intValue();}
 static boolean bool(Map<String,Object> m,String k)throws IOException{if(!(m.get(k) instanceof Boolean b))throw new IOException("Invalid "+k);return b;}
 static String str(Map<String,Object> m,String k)throws IOException{if(!(m.get(k) instanceof String s))throw new IOException("Invalid "+k);return s;}
 static String blob(String x)throws IOException{try{byte[] b=Base64.getDecoder().decode(x);if(b.length<5||b.length>65536)throw new IllegalArgumentException();return x;}catch(IllegalArgumentException e){throw new IOException("Invalid serialized item",e);}}
 public static Catalog parse(Map<String,Object> config,Map<String,Object> data,Map<String,Object> settings)throws IOException{
  var tokenConfig=map(config.get("token"));String rawToken=str(tokenConfig,"template-base64");String token=rawToken.isEmpty()&&Boolean.TRUE.equals(tokenConfig.get("fresh-token-unset"))?"":blob(rawToken);num(data,"format",2,2);var raw=map(data.get("rewards"));if(raw.size()>500)throw new IOException("Catalog too large");
  var result=new ArrayList<Reward>();var ids=new HashSet<UUID>();
  for(var e:raw.entrySet()){var m=map(e.getValue());try{UUID id=UUID.fromString(str(m,"id"));if(id.equals(TICKET)||!ids.add(id))throw new IOException("Duplicate/reserved reward ID");Type t=Type.valueOf(str(m,"type"));if(t==Type.TICKET)throw new IOException("Reserved ticket type");String cat=(String)m.getOrDefault("category",t==Type.PERMISSION?"Mounts & Perks":t==Type.COMMAND?"Commands":"Items");if(cat.isBlank()||cat.length()>60)throw new IOException("Invalid category");boolean bundle=(Boolean)m.getOrDefault("bundle-item",true);String item=blob(str(m,"item-base64"));List<Grant> grants=new ArrayList<>();if(m.containsKey("permissions"))for(Object v:map(m.get("permissions")).values()){var g=map(v);String node=str(g,"node");if(!node.matches("[a-zA-Z0-9_.-]{1,150}"))throw new IOException("Invalid permission node");grants.add(new Grant(node,num(g,"duration-seconds",0,Integer.MAX_VALUE),str(g,"duplicate")));}result.add(new Reward(e.getKey(),id,t,item,num(m,"cost",1,1000000),num(m,"stock",-1,Integer.MAX_VALUE),num(m,"limit",0,Integer.MAX_VALUE),bool(m,"enabled"),num(m,"revision",1,Integer.MAX_VALUE),cat,bundle,grants));}catch(RuntimeException x){throw new IOException("Reward "+e.getKey()+" invalid",x);}}
  var ticket=map(settings.get("ticket-exchange"));result.add(new Reward("universal-ticket",TICKET,Type.TICKET,"",num(ticket,"cost",1,1000000),-1,0,bool(ticket,"enabled"),1,"Tickets",false,List.of()));
  var allow=new HashSet<String>();Object nodes=settings.get("allowed-permission-nodes");if(!(nodes instanceof List<?> list))throw new IOException("Invalid permission allowlist");for(Object x:list){if(!(x instanceof String s)||!s.matches("[a-zA-Z0-9_.-]{1,150}"))throw new IOException("Invalid allowed node");allow.add(s);}
  result.sort(Comparator.comparing(Reward::category).thenComparingInt(r->{try{return Integer.parseInt(r.key());}catch(Exception e){return Integer.MAX_VALUE;}}));return new Catalog(token,result,bool(settings,"permission-rewards-enabled"),allow);
 }
 public static boolean allowed(Catalog c,Reward r){if(!r.enabled())return false;if(r.type()==Type.ITEM||r.type()==Type.TICKET)return true;if(r.type()!=Type.PERMISSION||!c.permissionEnabled()||r.grants().size()!=1)return false;Grant g=r.grants().getFirst();return g.duplicate().equals("REJECT")&&g.seconds()>0&&c.allowed().stream().anyMatch(n->g.node().equals(n)||(n.endsWith(".")&&g.node().startsWith(n)));}
}
