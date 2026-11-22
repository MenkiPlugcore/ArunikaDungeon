package dev.arunika.dungeonwaystone;
import java.text.Normalizer;
import java.util.*;
/** Labels are metadata; UUIDs remain the durable identity and arena bindings do not use labels. */
public final class NodeNames {
 private NodeNames(){}
 public static String clean(String raw){
  if(raw==null||raw.codePoints().anyMatch(c->Character.isISOControl(c)||Character.getType(c)==Character.FORMAT))throw new IllegalArgumentException("Nama tidak boleh mengandung kontrol/format warna");
  String s=Normalizer.normalize(raw,Normalizer.Form.NFKC).strip().replaceAll(" +"," ");
  if(s.isEmpty()||s.codePointCount(0,s.length())>40||!s.matches("[\\p{L}\\p{N} _.\\-]+"))throw new IllegalArgumentException("Nama 1–40 huruf/angka/spasi, _ . atau -");
  return s;
 }
 public static String key(String raw){return clean(raw).toLowerCase(Locale.ROOT);}
 public static void requireUnique(Map<UUID,String> names,UUID own,String proposed){String k=key(proposed);if(names.entrySet().stream().anyMatch(e->!e.getKey().equals(own)&&key(e.getValue()).equals(k)))throw new IllegalArgumentException("Nama Waystone sudah dipakai");}
 public static String fallback(Node n){return n.role()+"-"+n.id().toString().substring(0,8);}
}
