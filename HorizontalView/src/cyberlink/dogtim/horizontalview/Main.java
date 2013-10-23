package cyberlink.dogtim.horizontalview;

import java.util.ArrayList;

import cyberlink.dogtim.horizontalview.widgets.TimelineRelativeLayout;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;


public class Main extends Activity {
    private static final String TAG = Main.class.getSimpleName();

    private LinearLayout imageLayout;
    private LinearLayout timelineLayout;
    private TimelineRelativeLayout mTimelineLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageLayout = (LinearLayout) findViewById(R.id.imageLayout);
        timelineLayout = (LinearLayout) findViewById(R.id.timelineHorizontalScrollView);
        mTimelineLayout = (TimelineRelativeLayout)findViewById(R.id.timeline);

        MediaStoreHelper mediaStoreHelper = new MediaStoreHelper(this.getApplicationContext()); 
        ArrayList<String> Files_string = mediaStoreHelper.getImages();
        for (String file : Files_string) {
            imageLayout.addView(insertPhoto(file));
            timelineLayout.addView(insertPhoto(file));
        }

         
        measureTimeLineWidth();
    }

    /**
     *   This is a workaround to get measure spec because <p>
     *   we can not get correct value in onCreate function. <p>
     *   @see <a href="http://www.eoeandroid.com/thread-240677-1-1.html">getWidth() == 0</a>
     */
    private void measureTimeLineWidth(){
        
        int w = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);  
        int h = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);  
        timelineLayout.measure(w, h); 
        final int childrenCount = mTimelineLayout.getChildCount();
        for (int i = 0; i < childrenCount; i++) {
            final View child = mTimelineLayout.getChildAt(i);
            final ViewGroup.LayoutParams lp = child.getLayoutParams();

            lp.width = timelineLayout.getMeasuredWidth();
            child.setLayoutParams(lp);
        }
    }
    private View insertPhoto(String path) {
        Bitmap bm = BitmapHelper.decodeSampledBitmapFromUri(path, 40, 40);

        LinearLayout layout = new LinearLayout(getApplicationContext());
        layout.setLayoutParams(new LayoutParams(250, 250));
        layout.setGravity(Gravity.CENTER);

        ImageView imageView = new ImageView(getApplicationContext());
        imageView.setLayoutParams(new LayoutParams(220, 220));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageBitmap(bm);

        layout.addView(imageView);
        return layout;
    }

}