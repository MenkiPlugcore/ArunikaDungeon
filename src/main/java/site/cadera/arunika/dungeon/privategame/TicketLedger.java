package site.cadera.arunika.dungeon.privategame;

import java.io.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.util.*;
import static java.nio.file.StandardOpenOption.*;

/** One durable record per serial. Visible duplicate copies can never redeem twice.
 * Called on one server thread; one server process per data directory. */
public final class TicketLedger {
 public record Ticket(UUID serial,int generation){public Ticket{Objects.requireNonNull(serial);if(generation<0)throw new IllegalArgumentException("generation");}}
 public enum State { AVAILABLE, HELD, SPENT, EXPOSED, CONSUMED }
 public record Record(Ticket ticket,State state,UUID session,UUID payer,UUID claimant){}
 private final Path root;
 public TicketLedger(Path root)throws IOException{this.root=root.toAbsolutePath().normalize();Files.createDirectories(this.root);if(Files.isSymbolicLink(this.root))throw new IOException("Ticket directory symlink refused");}
 private Path path(UUID serial){return root.resolve(serial+".ticket");}
 public synchronized Record read(UUID serial)throws IOException{
  Path p=path(serial);if(!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)||Files.size(p)>512)throw new IOException("Unknown or invalid ticket serial");
  try{String[] v=Files.readString(p).strip().split(" ",-1);if(v.length!=5)throw new IllegalArgumentException();var r=new Record(new Ticket(serial,Integer.parseInt(v[0])),State.valueOf(v[1]),uuid(v[2]),uuid(v[3]),uuid(v[4]));if(r.state==State.AVAILABLE?(r.session!=null||r.payer!=null):(r.session==null||r.payer==null||r.claimant!=null))throw new IllegalArgumentException();return r;}catch(RuntimeException e){throw new IOException("Corrupt ticket "+serial,e);}
 }
 private static UUID uuid(String s){return s.equals("-")?null:UUID.fromString(s);}
 private static String str(UUID u){return u==null?"-":u.toString();}
 private void write(Record r)throws IOException{
  Path tmp=Files.createTempFile(root,"ticket-",".tmp");try{Files.writeString(tmp,r.ticket.generation+" "+r.state+" "+str(r.session)+" "+str(r.payer)+" "+str(r.claimant)+"\n",TRUNCATE_EXISTING);try(var channel=FileChannel.open(tmp,WRITE)){channel.force(true);}Files.move(tmp,path(r.ticket.serial),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);try(var dir=FileChannel.open(root,READ)){dir.force(true);}catch(IOException|UnsupportedOperationException ignored){}}finally{Files.deleteIfExists(tmp);}
 }
 public synchronized Ticket issue(UUID claimant)throws IOException{return issueRecord(Objects.requireNonNull(claimant));}
 /** Ground loot has no claim owner; identical copies still share the same serial. */
 /** Exchange serial is the durable transaction ID. Never recreate or regrant spent tickets. */
 public synchronized Ticket issueForExchange(UUID serial,UUID claimant)throws IOException{Objects.requireNonNull(claimant);if(Files.exists(path(serial))){Record r=read(serial);if(r.state==State.AVAILABLE&&r.ticket.generation==0&&claimant.equals(r.claimant))return r.ticket;throw new IllegalStateException("Exchange ticket already issued or claimed");}Ticket t=new Ticket(serial,0);write(new Record(t,State.AVAILABLE,null,null,claimant));return t;}
 public synchronized Ticket issuePhysical()throws IOException{return issueRecord(null);}
 private Ticket issueRecord(UUID claimant)throws IOException{Ticket t;do{t=new Ticket(UUID.randomUUID(),0);}while(Files.exists(path(t.serial)));write(new Record(t,State.AVAILABLE,null,null,claimant));return t;}
 public synchronized boolean available(Ticket t)throws IOException{Record r=read(t.serial);return r.ticket.equals(t)&&r.state==State.AVAILABLE;}
 public synchronized boolean availableFor(Ticket t,UUID payer)throws IOException{Objects.requireNonNull(payer);Record r=read(t.serial);return r.ticket.equals(t)&&r.state==State.AVAILABLE&&(r.claimant==null||r.claimant.equals(payer));}
 public synchronized void hold(Ticket t,UUID session,UUID payer)throws IOException{Objects.requireNonNull(session);Objects.requireNonNull(payer);if(!availableFor(t,payer))throw new IllegalStateException("Ticket invalid or already used");write(new Record(t,State.HELD,session,payer,null));}
 public synchronized void commit(Ticket t,UUID session)throws IOException{Record r=read(t.serial);require(r,t,session,State.HELD);write(new Record(t,State.SPENT,session,r.payer,null));}
 /** Final successful or player-failed encounter cannot be refunded by the normal path. */
 public synchronized void markExposed(Ticket t,UUID session)throws IOException{Record r=read(t.serial);if(r.state==State.EXPOSED){require(r,t,session,State.EXPOSED);return;}require(r,t,session,State.SPENT);write(new Record(t,State.EXPOSED,session,r.payer,null));}
 public synchronized void complete(Ticket t,UUID session)throws IOException{Record r=read(t.serial);if(r.state!=State.SPENT&&r.state!=State.EXPOSED)throw new IllegalStateException("Ticket not committed");require(r,t,session,r.state);write(new Record(t,State.CONSUMED,session,r.payer,null));}
 private static void require(Record r,Ticket t,UUID session,State state){if(!r.ticket.equals(t)||r.state!=state||!Objects.equals(r.session,session))throw new IllegalStateException("Ticket transaction mismatch");}
 /** Refund creates a new generation; old physical copies never become valid again. */
 public synchronized Ticket refund(Ticket t,UUID session)throws IOException{Record r=read(t.serial);if(r.state!=State.HELD&&r.state!=State.SPENT)throw new IllegalStateException("Already refunded or invalid");require(r,t,session,r.state);Ticket next=new Ticket(t.serial,Math.addExact(t.generation,1));write(new Record(next,State.AVAILABLE,null,null,r.payer));return next;}
 /** Claim is replayable after a crash/full inventory but all copies share one redemption. */
 public synchronized Ticket claim(UUID serial,UUID player)throws IOException{Record r=read(serial);if(r.state!=State.AVAILABLE||!player.equals(r.claimant))throw new IllegalStateException("No claim for player");return r.ticket;}
 public synchronized void acknowledge(Ticket t,UUID player)throws IOException{Record r=read(t.serial);if(!r.ticket.equals(t)||r.state!=State.AVAILABLE||!player.equals(r.claimant))throw new IllegalStateException("Claim mismatch");write(new Record(t,State.AVAILABLE,null,null,null));}
 /** Only on startup before any new session. HELD never reached battle commit. */
 public synchronized List<UUID> recoverHeld()throws IOException{List<UUID> recovered=new ArrayList<>();try(var files=Files.newDirectoryStream(root,"*.ticket")){for(Path p:files){UUID id=UUID.fromString(p.getFileName().toString().replaceFirst("\\.ticket$",""));Record r=read(id);if(r.state==State.HELD){refund(r.ticket,r.session);recovered.add(id);}}}return recovered;}
}
