package org.beckenburg;
/** Independent arrival module, shared by all site surroundings. Local metres; +Z enters the van. */
public final class VanArrivalTile {
 private VanArrivalTile(){}
 public static final String ID="arrival-road-v1";
 public static final double SUPPORT_END=22.0;
 public static final double SIDEWALK_OUTER=VanArrivalStreet.ROAD_HALF_WIDTH+2.10;
 public static final double CLEAR_ROUTE_HALF_WIDTH=VanArrivalStreet.ACCESS_HALF_WIDTH;
 public static double supportLeft(double vanWorldX,double siteLeft){return Math.min(siteLeft,vanWorldX-SUPPORT_END);}
 /** Visible closed outer fence is beyond the road end; the street continues as backdrop past it. */
 public static double distantRoadEnd(){return -95;}
}
