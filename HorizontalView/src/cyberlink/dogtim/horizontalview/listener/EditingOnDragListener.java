package cyberlink.dogtim.horizontalview.listener;

import cyberlink.dogtim.horizontalview.Item;
import cyberlink.dogtim.horizontalview.ItemType;
import cyberlink.dogtim.horizontalview.Main;
import cyberlink.dogtim.horizontalview.Project;
import cyberlink.dogtim.horizontalview.R;
import cyberlink.dogtim.horizontalview.widgets.TimelineHorizontalScrollView;
import cyberlink.dogtim.horizontalview.widgets.TimelineRelativeLayout;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class EditingOnDragListener implements View.OnDragListener {
    private static final String TAG = EditingOnDragListener.class.getSimpleName();
    
    /**
     * Faking Mode happen when user want to insert a item or reorder,<p>
     * It shows temporarily view let user know the expected result.<p>
     * TODO: Maybe concludes corresponding codes to a structure,<p>
     * mIsFakingMode, mFakeView, mFakeX etc..
     */
    private boolean mIsFakingMode = false;
    private View mFakeView = null;
    private int mFakeX = -1;
    private int mFakeY = -1;
    
    private Main mMainActivity;
    private TimelineRelativeLayout mTimelineLayout;
    private LinearLayout mPhotoTrackLayout;
    private FrameLayout mDecorateTrackLayout;
    private TimelineHorizontalScrollView mTHSView;
    private Project mProject;
    
    public EditingOnDragListener(Main mainAcitivty, LinearLayout editingTrackLayout, 
            TimelineRelativeLayout timelineLayout, FrameLayout decorateTrackLayout,
            TimelineHorizontalScrollView THSView){
        mMainActivity = mainAcitivty;
        mPhotoTrackLayout = editingTrackLayout;
        mProject = Project.get();
        mTimelineLayout = timelineLayout;
        mDecorateTrackLayout = decorateTrackLayout;
        mTHSView = THSView;
    }
    
    private boolean onInsertPhoto(View v, DragEvent event){
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
                if(targetItem.type == ItemType.EditingItem && !mIsFakingMode){
                    int[] location = new int[2];
                    v.getLocationInWindow(location);
                    mFakeX = location[0];
                    mFakeY = location[1];
                    int id = mPhotoTrackLayout.indexOfChild(v);
                    mFakeView = mMainActivity.createPhotoEditingView(draggedItem.path);
                    mFakeView.setAlpha((float)0.3);
                    mFakeView.setTag(new Item("fakePath", ItemType.FakeItem));
                    mPhotoTrackLayout.addView(mFakeView,id);
                    mProject.addEditingItem(id, draggedItem);
                    mMainActivity.measureTimeLineWidth();
                    mTimelineLayout.invalidate();
                    mIsFakingMode = true;
                }
                Log.d(TAG,"onDrag ACTION_DRAG_ENTERED");
                break;
            case DragEvent.ACTION_DRAG_LOCATION:
                if(mIsFakingMode && targetItem.type == ItemType.WindowItem){
                    int eventX = (int )event.getX();
                    int eventY = (int )event.getY();
                    if(mFakeX < eventX && (mFakeX+mFakeView.getWidth()) > eventX){
                        if(mFakeY < eventY && (mFakeY+mFakeView.getHeight()) > eventY){
                            break;
                        }
                    }
                    mProject.removeEditingItem(mPhotoTrackLayout.indexOfChild(mFakeView));
                    mPhotoTrackLayout.removeView(mFakeView);
                    mMainActivity.measureTimeLineWidth();
                    mTimelineLayout.invalidate();
                    resetFake();
                }
                Log.d(TAG,"onDrag ACTION_DRAG_LOCATION");
                break;
            case DragEvent.ACTION_DROP:
                if(targetItem.type == ItemType.WindowItem && mIsFakingMode){
                    mFakeView.setAlpha((float)1);
                    Item itemTag = (Item) mFakeView.getTag();
                    itemTag.type = ItemType.EditingItem;
                    mMainActivity.measureTimeLineWidth();
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
        //always return true
        return true;
    }
    
    private boolean onInsertTransition(View v, DragEvent event){
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
                if(targetItem.type == ItemType.EditingItem && !mIsFakingMode){
                    int[] location = new int[2];
                    v.getLocationInWindow(location);
                    mFakeX = location[0];
                    mFakeY = location[1];
                    mFakeView = mMainActivity.createTransitionEditingView(draggedItem.path);
                    mFakeView.setAlpha((float)0.3);
                    mFakeView.setTag(new Item("fakePath", ItemType.FakeItem));
                    mFakeView.setX((float)(v.getX()+(float)v.getWidth()) - (float)(draggedView.getWidth()/2));
                    mFakeView.setY((float)(v.getY()+(float)(v.getHeight()/2)) - (float)(draggedView.getHeight()/2));
                    mFakeView.setLayoutParams(new FrameLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    mDecorateTrackLayout.addView(mFakeView);
                    mMainActivity.measureTimeLineWidth();
                    mTimelineLayout.invalidate();
                    mIsFakingMode = true;
                }
                Log.d(TAG,"onDrag ACTION_DRAG_ENTERED");
                break;
            case DragEvent.ACTION_DRAG_LOCATION:
                if(mIsFakingMode && targetItem.type != ItemType.EditingItem){
                    mDecorateTrackLayout.removeView(mFakeView);
                    resetFake();
                }
                Log.d(TAG,"onDrag ACTION_DRAG_LOCATION");
                break;
            case DragEvent.ACTION_DROP:
                if(mIsFakingMode){
                    mFakeView.setAlpha((float)1);
                    targetItem.setTransitionView(mFakeView);
                    mMainActivity.measureTimeLineWidth();
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

    private boolean onReorder(View v, DragEvent event){
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
                if(targetItem.type == ItemType.EditingItem && !mIsFakingMode && v != draggedView){
                    mFakeView = mMainActivity.getLayoutInflater().inflate(R.layout.sperate_line, null);
                    mFakeView.setX((float)(v.getX()+(float)v.getWidth()));
                    mFakeView.setY((float)v.getY()-25);
                    mFakeView.setLayoutParams(new FrameLayout.LayoutParams(
                            3,
                            v.getWidth()
                    ));
                    mDecorateTrackLayout.addView(mFakeView);
                    mIsFakingMode = true;
                }
                Log.d(TAG,"onDrag ACTION_DRAG_ENTERED");
                break;
            case DragEvent.ACTION_DRAG_LOCATION:
                if(mIsFakingMode && targetItem.type != ItemType.EditingItem){
                    mDecorateTrackLayout.removeView(mFakeView);
                    resetFake();
                }
                Log.d(TAG,"onDrag ACTION_DRAG_LOCATION");
                break;
            case DragEvent.ACTION_DROP:
                if(mIsFakingMode){
                    mDecorateTrackLayout.removeView(mFakeView);
                    resetFake();
                    mPhotoTrackLayout.removeView(draggedView);
                    mPhotoTrackLayout.addView(draggedView, mPhotoTrackLayout.indexOfChild(v)+1);
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

    private boolean onScroll(View v, DragEvent event){
        final int action = event.getAction();
        final int OFFSET_X = 100;
        switch (action) {
            case DragEvent.ACTION_DRAG_STARTED:
                Log.d(TAG,"onDrag ACTION_DRAG_STARTED");
                break;
            
            case DragEvent.ACTION_DRAG_ENTERED:
                Log.d(TAG,"onDrag ACTION_DRAG_ENTERED");
                break;
            case DragEvent.ACTION_DRAG_LOCATION:
                final int position = mTHSView.getScrollX();
                if(v.getId() == R.id.right_scroll_controller){
                    mTHSView.scrollTo(position + OFFSET_X, mTHSView.getTop());
                }else if(v.getId() == R.id.left_scroll_controller){
                    mTHSView.scrollTo(position - OFFSET_X, mTHSView.getTop());
                }
                Log.d(TAG,"onDrag ACTION_DRAG_LOCATION");
                break;
            case DragEvent.ACTION_DROP:
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
    
    private void resetFake(){
        mIsFakingMode = false;
        mFakeView=null;
        mFakeX = 0;
        mFakeY = 0;
    }
    
    @Override
    public boolean onDrag(View v, DragEvent event) {
        View draggedView = (View) event.getLocalState();
        Item draggedItem = (Item) draggedView.getTag();
        if(v.getId() == R.id.right_scroll_controller || v.getId() == R.id.left_scroll_controller){
            return onScroll(v, event);
        }
        
        if(draggedItem.type == ItemType.PhotoItem){
            return onInsertPhoto(v, event);
        }else if(draggedItem.type == ItemType.TransitionItem){
            return onInsertTransition(v, event);
        }else if(draggedItem.type == ItemType.EditingItem){
            return onReorder(v, event);
        }else{
            Log.e(TAG, "You should define what the type of item you dragged");
        }
        
        return true;
    }
}
