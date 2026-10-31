package org.beckenburg;
public final class VanTelemetryTest {
 static void ok(boolean x,String s){if(!x)throw new AssertionError(s);}
 public static void main(String[] args){
   VanTelemetry m=new VanTelemetry(45);int a=m.sample(0,100);
   ok(a<=3,"fully charged ghost must not begin with a high activity signal");
   ok(a==m.sample(1,0),"polling cannot reroll a sample");
   for(int t=12;t<=420;t+=12)m.sample(t,100);
   ok(m.sample(421,100)<=3,"full ghost energy trends toward low activity");
   for(int t=432;t<=852;t+=12)m.sample(t,0);
   ok(m.sample(853,0)>=7,"energy expenditure must raise the activity meter");
   for(int t=864;t<=1284;t+=12)m.sample(t,100);
   ok(m.sample(1285,100)<=3,"energy recovery must lower activity again");
   int[] san={100,82,47,0,-1,-1};
   var d=new VanTelemetry.Display(60,4,san);san[0]=0;
   ok(d.sanity()[0]==100&&VanTelemetry.Display.decode(d.frame()).sanity()[2]==47,"six sanitized seats");
   System.out.println("PASS van monitor: inverted noisy expenditure activity and true private sanity");
 }
}