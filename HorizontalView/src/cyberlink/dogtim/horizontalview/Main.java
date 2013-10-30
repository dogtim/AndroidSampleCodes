package cyberlink.dogtim.horizontalview;

import java.util.ArrayList;

import cyberlink.dogtim.horizontalview.listener.EditingItemOnDragListener;
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
import android.view.DragEvent;
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
    
    private View mFakeView = null;
    private boolean mIsFakingMode = false;
    private int mFakeX = -1;
    private int mFakeY = -1;
    private Project mProject;
    
    private View.OnDragListener mDragListener = new View.OnDragListener() {
        public boolean onDrag(View v, DragEvent event) {
            final int action = event.getAction();
            View draggedView = (View) event.getLocalState();
            Item draggedItem = (Item) draggedView.getTag();
            Item targetItem = (Item) v.getTag();

            if (targetItem == null)
                return true;
            switch (action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    Log.d(TAG,"onDrag ACTION_DRAG_STARTED");
                    break;
                
                case DragEvent.ACTION_DRAG_ENTERED:
                    
                    if(draggedItem.type == ItemType.photoItem && targetItem.type == ItemType.EditingItem && !mIsFakingMode){
                        int[] location = new int[2];
                        v.getLocationInWindow(location);
                        mFakeX = location[0];
                        mFakeY = location[1];
                        int id = mPhotoTrackLayout.indexOfChild(v);
                        mFakeView = createPhotoEditingView(draggedItem.path);
                        mFakeView.setAlpha((float)0.3);
                        mFakeView.setTag(new Item("fakePath", ItemType.FakeItem));
                        mPhotoTrackLayout.addView(mFakeView,id);
                        mProject.addEditingItem(id, draggedItem);
                        measureTimeLineWidth();
                        mTimelineLayout.invalidate();
                        mIsFakingMode = true;
                        return false;
                    }else if(draggedItem.type == ItemType.TransitionItem && targetItem.type == ItemType.EditingItem && !mIsFakingMode){
                        int[] location = new int[2];
                        v.getLocationInWindow(location);
                        mFakeX = location[0];
                        mFakeY = location[1];
                        mFakeView = createTransitionEditingView(draggedItem.path);
                        mFakeView.setAlpha((float)0.3);
                        mFakeView.setTag(new Item("fakePath", ItemType.FakeItem));
                        mFakeView.setX((float)(v.getX()+(float)v.getWidth()) - (float)(draggedView.getWidth()/2));
                        mFakeView.setY((float)(v.getY()+(float)(v.getHeight()/2)) - (float)(draggedView.getHeight()/2));
                        mFakeView.setLayoutParams(new FrameLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        ));
                        targetItem.setTransitionView(mFakeView);
                        mDecorateTrackLayout.addView(mFakeView);
                        measureTimeLineWidth();
                        mTimelineLayout.invalidate();
                        mIsFakingMode = true;
                        return false;
                    }
                    Log.d(TAG,"onDrag ACTION_DRAG_ENTERED");
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                    if(mIsFakingMode && targetItem.type == ItemType.WindowItem && draggedItem.type == ItemType.photoItem){
                        int eventX = (int )event.getX();
                        int eventY = (int )event.getY();
                        if(mFakeX < eventX && (mFakeX+mFakeView.getWidth()) > eventX){
                            if(mFakeY < eventY && (mFakeY+mFakeView.getHeight()) > eventY){
                                return true;
                            }
                        }
                        mProject.removeEditingItem(mPhotoTrackLayout.indexOfChild(mFakeView));
                        mPhotoTrackLayout.removeView(mFakeView);
                        measureTimeLineWidth();
                        mTimelineLayout.invalidate();
                        resetFake();
                    }else if(mIsFakingMode && targetItem.type != ItemType.EditingItem && draggedItem.type == ItemType.TransitionItem){
                        mDecorateTrackLayout.removeView(mFakeView);
                        resetFake();
                    }
                    Log.d(TAG,"onDrag ACTION_DRAG_LOCATION");
                    break;
                case DragEvent.ACTION_DROP:
                    if(targetItem.type == ItemType.WindowItem && mIsFakingMode && draggedItem.type == ItemType.photoItem){
                        mFakeView.setAlpha((float)1);
                        Item itemTag = (Item) mFakeView.getTag();
                        itemTag.type = ItemType.EditingItem;
                        measureTimeLineWidth();
                        mTimelineLayout.invalidate();
                        resetFake();
                    }else if(mIsFakingMode && draggedItem.type == ItemType.TransitionItem){
                        mFakeView.setAlpha((float)1);
                        measureTimeLineWidth();
                        mTimelineLayout.invalidate();
                        resetFake();
                    }
                    Log.d(TAG,"onDrag ACTION_DROP");
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    Log.d(TAG,"onDrag ACTION_DRAG_EXITED");
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    Log.d(TAG,"onDrag ACTION_DRAG_ENDED");
                    break;
                default:
                    Log.d(TAG,"onDrag default");
                    break;
            }
            return true;
        }
    };
    
    private void resetFake(){
        mIsFakingMode = false;
        mFakeView=null;
        mFakeX = 0;
        mFakeY = 0;
    }
    
    private View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
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
        }
        
    };
    
    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
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
        getWindow().getDecorView().getRootView().setOnDragListener(mDragListener);
        getWindow().getDecorView().getRootView().setTag(new Item("window", ItemType.WindowItem));
        
        setMaterialButton();
        mProject = Project.get();
        mPlayheadView.setProject(mProject);
        initAnimation();
        setTimeLineEditingItem();
        setPhotoMaterial();
        measureTimeLineWidth();
    }

    private void setMaterialButton(){
        ImageView photoBtn = (ImageView) findViewById(R.id.photo_material_icon);
        ImageView transitionBtn = (ImageView) findViewById(R.id.transition_material_icon);
        photoBtn.setOnClickListener(mOnClickListener);
        transitionBtn.setOnClickListener(mOnClickListener);
        
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
    private void measureTimeLineWidth(){

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
        Bitmap bm = BitmapHelper.decodeSampledBitmapFromUri(path, 40, 40);
        ImageView imageView = new ImageView(getApplicationContext());
        imageView.setLayoutParams(new LayoutParams(220, 220));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageBitmap(bm);
        
        setImageViewEvent(imageView, isNeedDrag);
        return imageView;
    }

    private void setImageViewEvent(ImageView imageView, boolean isNeedDrag){
        if(isNeedDrag == true)
            imageView.setOnDragListener(mDragListener);
        imageView.setLongClickable(true);
        imageView.setOnLongClickListener(mLongClickListener);
    }
    
    private View createPhotoMaterialView(String path) {
        LinearLayout layout = new LinearLayout(getApplicationContext());
        layout.setLayoutParams(new LayoutParams(250, 250));
        layout.setGravity(Gravity.CENTER);
        ImageView imageView = createImageView(path, false);
        imageView.setTag(new Item(path, ItemType.photoItem));
        layout.addView(imageView);
        return layout;
    }

    private View createTransitionMaterialView(int resourceId) {
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(resourceId);
        imageView.setTag(new Item( Integer.toString(resourceId),ItemType.TransitionItem));
        setImageViewEvent(imageView, false);
        return imageView;
    }

    private View createPhotoEditingView(String path) {
        ImageView imageView = createImageView(path, true);
        //TODO for add transition using, it could bundle the transition view to attached view
        imageView.addOnLayoutChangeListener(mOnLayoutChangeListener);
        imageView.setTag(new Item(path,ItemType.EditingItem));
        return imageView;
    }

    private View createTransitionEditingView(String resourceId) {
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(Integer.parseInt(resourceId));
        imageView.setTag(new Item(resourceId,ItemType.EditingItem));
        return imageView;
    }
}