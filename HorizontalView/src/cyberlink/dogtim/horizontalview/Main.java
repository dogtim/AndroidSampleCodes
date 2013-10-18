package cyberlink.dogtim.horizontalview;

import java.util.ArrayList;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class Main extends Activity {

    LinearLayout imageLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageLayout = (LinearLayout) findViewById(R.id.imageLayout);
        MediaStoreHelper mediaStoreHelper = new MediaStoreHelper(this.getApplicationContext()); 
        ArrayList<String> Files_string = mediaStoreHelper.getImages();

        for (String file : Files_string) {
            imageLayout.addView(insertPhoto(file));
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