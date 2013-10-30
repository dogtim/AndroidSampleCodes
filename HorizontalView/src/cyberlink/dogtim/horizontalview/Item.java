package cyberlink.dogtim.horizontalview;

import android.view.View;

public class Item {
    public ItemType type;
    public String path;
    private View transitionView = null;
    
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
}
