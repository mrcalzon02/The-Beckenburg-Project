package org.beckenburg;

import java.awt.FontMetrics;
import java.util.Objects;

/** Pixel-accurate clipping for compact in-game overlay labels. */
final class HudTextFit {
    private HudTextFit(){}

    static String ellipsize(FontMetrics metrics,String value,int maxWidth){
        Objects.requireNonNull(metrics);
        Objects.requireNonNull(value);
        if(maxWidth<=0)return "";
        if(metrics.stringWidth(value)<=maxWidth)return value;
        String ellipsis="...";
        if(metrics.stringWidth(ellipsis)>maxWidth)return "";
        int low=0,high=value.length();
        while(low<high){
            int mid=(low+high+1)>>>1;
            if(metrics.stringWidth(value.substring(0,mid)+ellipsis)<=maxWidth)low=mid;
            else high=mid-1;
        }
        // Preserve the entire Unicode code point at the cut boundary.
        if(low>0&&low<value.length()&&Character.isHighSurrogate(value.charAt(low-1))
                &&Character.isLowSurrogate(value.charAt(low)))low--;
        return value.substring(0,low)+ellipsis;
    }
}
