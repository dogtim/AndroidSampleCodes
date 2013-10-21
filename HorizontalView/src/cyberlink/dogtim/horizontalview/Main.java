package cyberlink.dogtim.horizontalview;

import java.util.ArrayList;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class Main extends Activity {
    private static final String TAG = Main.class.getSimpleName();
    
    private LeakTest leak = null;
    class LeakTest {
        void trackLeak(){
            Log.d(TAG,"Track Leak ! ! !");
        }
    }
    LinearLayout imageLayout;
    LinearLayout timelineLayout;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageLayout = (LinearLayout) findViewById(R.id.imageLayout);
        timelineLayout = (LinearLayout) findViewById(R.id.timelineHorizontalScrollView);
        if(leak == null){
            leak = new LeakTest();
        }
        
        MediaStoreHelper mediaStoreHelper = new MediaStoreHelper(this.getApplicationContext()); 
        ArrayList<String> Files_string = mediaStoreHelper.getImages();
        for (String file : Files_string) {
            imageLayout.addView(insertPhoto(file));
            timelineLayout.addView(insertPhoto(file));
        }
    }

    View insertPhoto(String path) {
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