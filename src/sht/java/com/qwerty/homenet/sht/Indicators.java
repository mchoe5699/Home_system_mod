package com.qwerty.homenet.sht;

/** The same physical lamps and state bits are used by the base renderer and its controls. */
public final class Indicators {
 public static final int POWER=1,CHARGE=2,ARMED=4,AWAY=8,EMERGENCY=16,DOOR=32,GUARD=64,LINE=128,FIRE=256,GAS=512,SECURITY=1024;
 public record Lamp(int bit,int x,int y,int w,int h,int rgb,boolean round){}
 public static final java.util.List<Lamp> LAMPS=java.util.List.of(
  new Lamp(POWER,460,249,12,4,0xff6553,false),new Lamp(CHARGE,460,229,12,4,0x9fe16c,false),
  new Lamp(ARMED,460,268,12,4,0xa7df71,false),new Lamp(AWAY,460,305,12,4,0xa7df71,false),
  new Lamp(DOOR,460,533,12,4,0xa7df71,false),new Lamp(GUARD,460,496,12,4,0xa7df71,false),new Lamp(LINE,460,573,12,4,0xa7df71,false),
  new Lamp(FIRE,318,182,20,20,0xff6653,true),new Lamp(GAS,366,182,20,20,0xf5df58,true),new Lamp(SECURITY,410,182,20,20,0x87e77c,true));
 private Indicators(){}
}
