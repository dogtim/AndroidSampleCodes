package cyberlink.dogtim.horizontalview;

import java.util.ArrayList;

import cyberlink.dogtim.horizontalview.widgets.TimelineRelativeLayout;

import android.os.Bundle;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;


public class Main extends Activity {
    private static final String TAG = Main.class.getSimpleName();

    private LinearLayout mMaterialLayout;
    private LinearLayout mPhotoTrackLayout;
    private TimelineRelativeLayout mTimelineLayout;
    
    private static enum ItemType {
        OriginalItem("original item"),

        EditingItem("editing item");
        
        private String name;
        private ItemType(String source) {
            this.name = source;
        }
        public String getName() {
            return name;
        }
    };
    
    private class Item {
        ItemType type;
        String path;
    }
    
    private Item selectItem = null;
    
    private View.OnDragListener mDragListener = new View.OnDragListener() {
        public boolean onDrag(View v, DragEvent event) {
            final int action = event.getAction();
            Item item = selectItem;
            Log.d(TAG,"onDrag item: "+item.type.getName());
            Item target = (Item) v.getTag();
            if (target == null)
                return true;
            switch (action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    Log.d(TAG,"onDrag ACTION_DRAG_STARTED");
                    break;
                
                case DragEvent.ACTION_DRAG_ENTERED:
                    
                    if(target.type == ItemType.EditingItem){

                        /*Animation am = new TranslateAnimation( 0, 100, 0, 0);
                        am.setDuration(2500);
                        mTimelineLayout.setAnimation(am);
                        am.startNow();*/
                        /*v.setPadding(200, 0, 0, 0);
                        mTimelineLayout.invalidate();*/
                        //v.setVisibility(View.INVISIBLE);
                    }
                    Log.d(TAG,"onDrag ACTION_DRAG_ENTERED");
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                    Log.d(TAG,"onDrag ACTION_DRAG_LOCATION");
                    break;
                case DragEvent.ACTION_DROP:
                    if(target.type == ItemType.EditingItem){
                        int id = mPhotoTrackLayout.indexOfChild(v);
                        mPhotoTrackLayout.addView(insertPhotoToTimeLine(item.path),id);
                        measureTimeLineWidth();
                        mTimelineLayout.invalidate();
                    }
                    Log.d(TAG,"onDrag ACTION_DROP");
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    Log.d(TAG,"onDrag ACTION_DRAG_EXITED");
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    Log.d(TAG,"onDrag ACTION_DRAG_ENDED");
                    break;
            }
            return true;
        }
    };
    
    private View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
        public boolean onLongClick(View v) {
            ClipData data = null;//ClipData.newPlainText("dot", "Dot : " + v.toString());
            View.DragShadowBuilder shadowView = new View.DragShadowBuilder (v);
            selectItem = (Item) v.getTag();
            v.startDrag(data, shadowView,(Object)v, 0);
            return true;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMaterialLayout = (LinearLayout) findViewById(R.id.imageLayout);
        mPhotoTrackLayout = (LinearLayout) findViewById(R.id.photo_track);
        mTimelineLayout = (TimelineRelativeLayout)findViewById(R.id.timeline);
        //mPhotoTrackLayout.setClipChildren(false);
        //mTimelineLayout.setClipChildren(false);
        initAnimation();
        prepareMaterial();
        measureTimeLineWidth();
    }
    
    private void initAnimation(){
        final LayoutTransition transitioner = new LayoutTransition();
        mPhotoTrackLayout.setLayoutTransition(transitioner);
        Animator customAppearingAnim;
        // Adding
        customAppearingAnim = ObjectAnimator.ofFloat(null, "rotationY", 90f, 0f).
                setDuration(transitioner.getDuration(LayoutTransition.APPEARING));
        customAppearingAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator anim) {
                View view = (View) ((ObjectAnimator) anim).getTarget();
                view.setRotationY(0f);
            }
        });
        transitioner.setAnimator(LayoutTransition.APPEARING, customAppearingAnim);
    }
    
    private void prepareMaterial(){
        MediaStoreHelper mediaStoreHelper = new MediaStoreHelper(getApplicationContext()); 
        ArrayList<String> Files_string = mediaStoreHelper.getImages();
        for (String file : Files_string) {
            mMaterialLayout.addView(insertPhotoToMaterial(file));
            mPhotoTrackLayout.addView(insertPhotoToTimeLine(file));
        }
    }
    /**
     *   This is a workaround to get measure spec because <p>
     *   we can not get correct value in onCreate function. <p>
     *   @see <a href="http://www.eoeandroid.com/thread-240677-1-1.html">getWidth() == 0</a>
     */
    private void measureTimeLineWidth(){
        
        int w = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);  
        int h = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);  
        mPhotoTrackLayout.measure(w, h); 
        final int childrenCount = mTimelineLayout.getChildCount();
        for (int i = 0; i < childrenCount; i++) {
            final View child = mTimelineLayout.getChildAt(i);
            final ViewGroup.LayoutParams lp = child.getLayoutParams();

            lp.width = mPhotoTrackLayout.getMeasuredWidth();
            child.setLayoutParams(lp);
        }
    }
    
    private ImageView createImageView(String path){
        
        Bitmap bm = BitmapHelper.decodeSampledBitmapFromUri(path, 40, 40);
        ImageView imageView = new ImageView(getApplicationContext());
        imageView.setLayoutParams(new LayoutParams(220, 220));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageBitmap(bm);
        
        imageView.setOnDragListener(mDragListener);
        imageView.setLongClickable(true);
        imageView.setOnLongClickListener(mLongClickListener);
        return imageView;
    }
    
    private View insertPhotoToMaterial(String path) {
        LinearLayout layout = new LinearLayout(getApplicationContext());
        layout.setLayoutParams(new LayoutParams(250, 250));
        layout.setGravity(Gravity.CENTER);
        ImageView imageView = createImageView(path);
        /* TODO 
         * maybe we could store object in setTag to split different object
         * for variety of scenario using.
         * */
        Item item = new Item();
        item.type = ItemType.OriginalItem;
        item.path = path;
        imageView.setTag(item);
        layout.addView(imageView);
        return layout;
    }

    private View insertPhotoToTimeLine(String path) {
        ImageView imageView = createImageView(path);
        /* TODO 
         * maybe we could store object in setTag to split different object
         * for variety of scenario using.
         * */
        Item item = new Item();
        item.type = ItemType.EditingItem;
        item.path = path;
        imageView.setTag(item);
        return imageView;

    }
}