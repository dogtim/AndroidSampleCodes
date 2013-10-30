package cyberlink.dogtim.horizontalview.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class UIUtil {
    public static int getScreenWidth(Context c){
        final Display display = ((WindowManager) c 
                .getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        final DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        return metrics.widthPixels;
    } 
}
