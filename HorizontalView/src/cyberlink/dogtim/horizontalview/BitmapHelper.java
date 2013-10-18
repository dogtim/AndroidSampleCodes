package cyberlink.dogtim.horizontalview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BitmapHelper {
    
    public static Bitmap decodeSampledBitmapFromUri(String path, int reqWidth,
            int reqHeight) {
        Bitmap bm = null;

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth,reqHeight);

        options.inJustDecodeBounds = false;
        bm = BitmapFactory.decodeFile(path, options);

        return bm;
    }

     private static int calculateInSampleSize(

    BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }

        return inSampleSize;
    }
}
