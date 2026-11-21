package site.cadera.arunika.dungeon.core;
/** Optional progression; disabled unless an explicitly registered compatible provider is available. */
public record RpgPolicy(boolean enabled,int classExpPerBoss,boolean awakeningProgress,boolean nativeClassXp){
 public RpgPolicy(boolean enabled,int classExpPerBoss,boolean awakeningProgress){this(enabled,classExpPerBoss,awakeningProgress,false);}
 public RpgPolicy{if(classExpPerBoss<0||classExpPerBoss>100000)throw new IllegalArgumentException("Class EXP range");}
 public boolean active(){return enabled&&(classExpPerBoss>0||awakeningProgress);}
}
