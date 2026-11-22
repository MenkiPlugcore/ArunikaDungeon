package site.cadera.arunika.dungeon.paper;
/** Typed admin text input is data; it cannot inject a second command or formatting code. */
public final class AdminInputs {
 private AdminInputs(){}
 public static int price(String raw){try{if(raw==null||!raw.matches("[0-9]{1,7}"))throw new IllegalArgumentException();int n=Integer.parseInt(raw);if(n<1||n>1000000)throw new IllegalArgumentException();return n;}catch(RuntimeException e){throw new IllegalArgumentException("Harga harus 1–1000000 token");}}
 public static String category(String raw){
  if(raw==null||raw.codePoints().anyMatch(c->Character.isISOControl(c)||Character.getType(c)==Character.FORMAT))throw new IllegalArgumentException("Kategori tidak valid");
  String text=raw.strip();if(text.isEmpty()||text.length()>60||!text.matches("[\\p{L}\\p{N} _.,&()\\-]+"))throw new IllegalArgumentException("Kategori 1–60 huruf/angka atau tanda baca sederhana");return text;
 }
}
