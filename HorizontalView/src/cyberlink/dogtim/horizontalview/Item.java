package cyberlink.dogtim.horizontalview;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class Item {
    public ItemType type;
    public String path;
    private View transitionView = null;
    private TwinSelectionViews selectionView = null;

    public Item (String str, ItemType t){
        path = str;
        type = t;
    }
    
    public void setTransitionView (View v){
        transitionView = v;
    }

    public View getTransitionView (){
        return transitionView;
    }
    
    public TwinSelectionViews getTwinSelectionView (){
        return selectionView;
    }

    public void setTwinSelectionView(View v, FrameLayout layout, Context context) {
        selectionView = new TwinSelectionViews(v,layout, context);
    }
}
