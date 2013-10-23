package cyberlink.dogtim.horizontalview;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

public class MediaStoreHelper {
    private Context mContext;
    private static final String TAG = MediaStoreHelper.class.getSimpleName();
    
    private int MAX_IMAGE_SIZE = 20;
    public MediaStoreHelper(Context context){
        mContext = context;
    }
    
    public ArrayList<String> getImages(){
        ArrayList<String> results = new ArrayList<String>();
        // request only the image ID to be returned
        String[] projection = { MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.MIME_TYPE};
        
        // Create the cursor pointing to the Download folder
        // In my case are: storage/sdcard0/pictures
        String selection = MediaStore.Images.ImageColumns.DATA + " like ? ";
        String selectionArgs[] = new String[] { "%/Pictures/%" };
        
        Cursor cursor = mContext.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                selection, selectionArgs, null);

        // Get the column index of the image ID
        // Query for all images on querying storage folder.
        String id;
        String photoFilePath;
        String mime;
        
        if (cursor != null && cursor.getCount() >= 1) {
            cursor.moveToFirst();
            do {
                id = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media._ID));
                photoFilePath = cursor.getString(cursor
                        .getColumnIndex(MediaStore.Images.Media.DATA));
                mime = cursor.getString(cursor
                        .getColumnIndex(MediaStore.Images.Media.MIME_TYPE));
                if(MAX_IMAGE_SIZE < cursor.getPosition()){
                    Log.v(TAG,"Over max size for image");
                    break;
                }
                results.add(photoFilePath);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        
        return results;
    }
}
