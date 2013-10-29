package cyberlink.dogtim.horizontalview.util;

import java.lang.ref.WeakReference;

import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;
import android.view.View;

/**
 * @author dogtim_chen
 * A customized builder for drag shadow view <p>
 * @see <a href="https://github.com/android/platform_frameworks_base/blob/master/core/java/android/view/View.java">View.DragShadowBuilder</a>
 *
 */
public class MaterialShadowBuilder extends View.DragShadowBuilder{
    private static final String TAG = MaterialShadowBuilder.class.getSimpleName();
    private final WeakReference<View> mView;
    private final static int ADDITIONAL_METRIC = 0;

    public MaterialShadowBuilder(View v) {
        super(v);
        mView = new WeakReference<View>(v);
        final View view = mView.get();
        view.setBackgroundColor(Color.WHITE);
        view.getLayoutParams().height = view.getLayoutParams().height + ADDITIONAL_METRIC;
        view.getLayoutParams().width = view.getLayoutParams().width + ADDITIONAL_METRIC;
    }

    @Override
    public void onProvideShadowMetrics (Point shadowSize, Point shadowTouchPoint){
        final View view = mView.get();
        if (view != null) {
            
            shadowSize.set(view.getWidth(), view.getHeight());
            shadowTouchPoint.set(shadowSize.x / 2, shadowSize.y / 2);
        } else {
            Log.e(TAG, "Asked for drag thumb metrics but no view");
        }
    }

}
