package com.releasebird.releasebird_sdk;

import android.content.Context;
import android.util.DisplayMetrics;

public class ReleasebirdUtils {
    public static int convertPixelsToDp(float px, Context context){
        return Math.round(px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static int convertDpToPixel(float dp, Context context){
        return Math.round(dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

}
