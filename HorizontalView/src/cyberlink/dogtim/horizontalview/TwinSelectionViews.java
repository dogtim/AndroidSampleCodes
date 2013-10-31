package cyberlink.dogtim.horizontalview;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Contains left and right view for UI control
 *
 */
public class TwinSelectionViews {

    public ImageView mLeftView;
    public ImageView mRightView;
    private Context mContext;
    private FrameLayout mLayout;
    TwinSelectionViews(View v, FrameLayout layout, Context context){
        mContext = context;
        mLayout = layout;
        addCircleView(v);
    }
    
    public void setPostion(View attachedView) {
        mLeftView.setX((float)(attachedView.getX()) - (float)(mLeftView.getMeasuredWidth()/2));
        mLeftView.setY((float)(attachedView.getY()+(float)(attachedView.getMeasuredHeight()/2)) - (float)(mLeftView.getMeasuredHeight()/2));
        
        mRightView.setX((float)(attachedView.getX()+(float)(attachedView.getMeasuredWidth())) - (float)(mRightView.getMeasuredWidth()/2));
        mRightView.setY((float)(attachedView.getY()+(float)(attachedView.getMeasuredHeight()/2)) - (float)(mRightView.getMeasuredHeight()/2));
    }

    public void removeTwinSeleciton(FrameLayout layout) {
        layout.removeView(mLeftView);
        layout.removeView(mRightView);
    }
    
    private void addCircleView(View v){
        mLeftView = new ImageView(mContext);
        mRightView = new ImageView(mContext);
        setCircleViewProperty(mLeftView);
        setCircleViewProperty(mRightView);

        setPostion(v);
        mLayout.addView(mLeftView);
        mLayout.addView(mRightView);
    }
    
    private void setCircleViewProperty(ImageView v){
        v.setImageResource(R.drawable.select_btn);
        
        v.setLayoutParams(new FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        int w = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
        v.measure(w, h); 
    }
}
