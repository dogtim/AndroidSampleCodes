package cyberlink.dogtim.horizontalview;

import java.util.ArrayList;

import cyberlink.dogtim.horizontalview.listener.EditingOnDragListener;
import cyberlink.dogtim.horizontalview.util.MaterialShadowBuilder;
import cyberlink.dogtim.horizontalview.util.UIUtil;
import cyberlink.dogtim.horizontalview.widgets.PlayheadView;
import cyberlink.dogtim.horizontalview.widgets.TimelineHorizontalScrollView;
import cyberlink.dogtim.horizontalview.widgets.TimelineRelativeLayout;

import android.os.Bundle;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;


public class Main extends Activity {
    private static final String TAG = Main.class.getSimpleName();

    private LinearLayout mMaterialLayout;
    private LinearLayout mPhotoTrackLayout;
    private FrameLayout mDecorateTrackLayout;
    private TimelineRelativeLayout mTimelineLayout;
    private TimelineHorizontalScrollView mTHSView;
    private PlayheadView mPlayheadView;

    private Project mProject;
    private Activity mActivity = this;
    private EditingOnDragListener mDragListener;

    private View.OnClickListener mOnMediaItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Item item = (Item)v.getTag();
            v.setSelected(!v.isSelected());
            if(item.type == ItemType.EditingItem){
                if (v.isSelected()){
                    item.setTwinSelectionView(v, mDecorateTrackLayout, mActivity);
                }
                else {
                    TwinSelectionViews twinSelectionView = item.getTwinSelectionView();
                    if(twinSelectionView != null){
                        twinSelectionView.removeTwinSeleciton(mDecorateTrackLayout);
                    } 
                }
            }
        }
    }; 
    
    private View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {
        public boolean onLongClick(View v) {
            ClipData data = null;//ClipData.newPlainText("dot", "Dot : " + v.toString());
            View.DragShadowBuilder shadowView = new View.DragShadowBuilder (v);
            //MaterialShadowBuilder shadowView = new MaterialShadowBuilder(v);
            v.startDrag(data, shadowView,(Object)v, 0);
            return true;
        }
    };

    private View.OnLayoutChangeListener mOnLayoutChangeListener = new View.OnLayoutChangeListener(){

        @Override
        public void onLayoutChange(View v, int left, int top, int right,
                int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            Item item = (Item) v.getTag();
            View transitionView = item.getTransitionView();
            if(transitionView != null){
                transitionView.setX((float)(v.getX()+(float)v.getWidth()) - (float)(transitionView.getWidth()/2));
                transitionView.setY((float)(v.getY()+(float)(v.getHeight()/2)) - (float)(transitionView.getHeight()/2));
            }
            TwinSelectionViews twinSelectionViews = item.getTwinSelectionView();
            if(twinSelectionViews != null){
                twinSelectionViews.setPostion(v);
            }
        }
        
    };
    
    private View.OnClickListener mOnTabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int viewID = v.getId();
            switch (viewID) {
            case R.id.photo_material_icon:
                Log.e(TAG, "photo_material_icon");
                mMaterialLayout.removeAllViews();
                setPhotoMaterial();
                break;
            case R.id.transition_material_icon:
                Log.e(TAG, "transition_material_icon");
                mMaterialLayout.removeAllViews();
                setTransitionMaterial();
                break;
            default:
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMaterialLayout = (LinearLayout) findViewById(R.id.imageLayout);
        mPhotoTrackLayout = (LinearLayout) findViewById(R.id.photo_track);
        mDecorateTrackLayout = (FrameLayout) findViewById(R.id.decorate_vp_track);
        mTimelineLayout = (TimelineRelativeLayout)findViewById(R.id.timeline);
        mPlayheadView = (PlayheadView)findViewById(R.id.timeline_playhead );
        mTHSView = (TimelineHorizontalScrollView)findViewById(R.id.timeline_scroller);
        mDragListener = new EditingOnDragListener(this,mPhotoTrackLayout, mTimelineLayout, mDecorateTrackLayout, mTHSView);

        getWindow().getDecorView().getRootView().setOnDragListener(mDragListener);
        getWindow().getDecorView().getRootView().setTag(new Item("window", ItemType.WindowItem));
        
        setMaterialButton();
        mProject = Project.get();
        mPlayheadView.setProject(mProject);
        setScrollController();
        initAnimation();
        setTimeLineEditingItem();
        setPhotoMaterial();
        measureTimeLineWidth();
    }
    private void setScrollController(){
        ImageView leftScrollController = (ImageView)findViewById(R.id.left_scroll_controller);
        ImageView rightScrollController = (ImageView)findViewById(R.id.right_scroll_controller);
        
        rightScrollController.setOnDragListener(mDragListener);
        leftScrollController.setOnDragListener(mDragListener);
    }

    private void setMaterialButton(){
        ImageView photoBtn = (ImageView) findViewById(R.id.photo_material_icon);
        ImageView transitionBtn = (ImageView) findViewById(R.id.transition_material_icon);
        photoBtn.setOnClickListener(mOnTabClickListener);
        transitionBtn.setOnClickListener(mOnTabClickListener);
        
    }
    /**
     * Animation codes from Api Demos project <p>
     * com.example.android.apis.animation.LayoutAnimations.java
     */
    private void initAnimation(){
        final LayoutTransition transitioner = new LayoutTransition();
        mPhotoTrackLayout.setLayoutTransition(transitioner);
        Animator customAppearingAnim;
        // Adding animation
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
    
    private void setTimeLineEditingItem(){
        MediaStoreHelper mediaStoreHelper = new MediaStoreHelper(getApplicationContext());
        ArrayList<String> Files_string = mediaStoreHelper.getImages();
        mPhotoTrackLayout.addView(createPhotoEditingView(Files_string.get(0)));
    }
    
    private void setPhotoMaterial(){
        MediaStoreHelper mediaStoreHelper = new MediaStoreHelper(getApplicationContext());
        ArrayList<String> Files_string = mediaStoreHelper.getImages();
        for (String file : Files_string) {
            mMaterialLayout.addView(createPhotoMaterialView(file));
            //mPhotoTrackLayout.addView(insertPhotoToTimeLine(file));
        }
    }

    private void setTransitionMaterial(){
        int files[] = TransitionMaterial.items;

        for(int i=0; i<files.length ; i++){
            mMaterialLayout.addView(createTransitionMaterialView(files[i]));
        }
    }
    
    /**
     *   This is a workaround to get measure spec because <p>
     *   we can not get correct value in onCreate function. <p>
     *   @see <a href="http://www.eoeandroid.com/thread-240677-1-1.html">getWidth() == 0</a>
     */
    public void measureTimeLineWidth(){

        final int screenWidth = UIUtil.getScreenWidth(getApplicationContext());
        
        int w = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
        mPhotoTrackLayout.measure(w, h); 
        final int childrenCount = mTimelineLayout.getChildCount();
        for (int i = 0; i < childrenCount; i++) {
            final View child = mTimelineLayout.getChildAt(i);
            final ViewGroup.LayoutParams lp = child.getLayoutParams();
            child.measure(w, h);
            lp.width = child.getMeasuredWidth();
            if(screenWidth > lp.width)
                lp.width = screenWidth;
            if(mPhotoTrackLayout.getMeasuredWidth() > lp.width)
                lp.width = mPhotoTrackLayout.getMeasuredWidth();
            child.setLayoutParams(lp);
        }
    }

    private ImageView createImageView(String path, boolean isNeedDrag){
        Bitmap bm = BitmapHelper.decodeSampledBitmapFromUri(path, 80, 80);
        ImageView imageView = (ImageView) getLayoutInflater().inflate(R.layout.media_item, null);//new ImageView(getApplicationContext());
        imageView.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageBitmap(bm);
        
        setImageViewEvent(imageView, isNeedDrag);
        return imageView;
    }

    private void setImageViewEvent(ImageView imageView, boolean isNeedDrag){
        if(isNeedDrag == true)
            imageView.setOnDragListener(mDragListener);
        imageView.setOnClickListener(mOnMediaItemClickListener);
        imageView.setOnLongClickListener(mOnLongClickListener);
    }

    private View createPhotoMaterialView(String path) {
        LinearLayout layout = new LinearLayout(getApplicationContext());
        layout.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.setGravity(Gravity.CENTER);
        ImageView imageView = createImageView(path, false);
        imageView.setTag(new Item(path, ItemType.PhotoItem));
        layout.addView(imageView);
        return layout;
    }

    private View createTransitionMaterialView(int resourceId) {
        ImageView imageView = (ImageView) getLayoutInflater().inflate(R.layout.media_item, null);
        imageView.setImageResource(resourceId);
        imageView.setTag(new Item( Integer.toString(resourceId),ItemType.TransitionItem));
        setImageViewEvent(imageView, false);
        return imageView;
    }

    public View createPhotoEditingView(String path) {
        ImageView imageView = createImageView(path, true);
        //TODO for add transition using, it could bundle the transition view to attached view
        imageView.addOnLayoutChangeListener(mOnLayoutChangeListener);
        imageView.setTag(new Item(path,ItemType.EditingItem));
        return imageView;
    }

    public View createTransitionEditingView(String resourceId) {
        ImageView imageView = (ImageView) getLayoutInflater().inflate(R.layout.media_item, null);
        imageView.setImageResource(Integer.parseInt(resourceId));
        imageView.setTag(new Item(resourceId,ItemType.EditingItem));
        return imageView;
    }
}