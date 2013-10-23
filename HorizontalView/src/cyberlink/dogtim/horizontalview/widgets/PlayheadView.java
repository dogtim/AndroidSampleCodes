package cyberlink.dogtim.horizontalview.widgets;

import cyberlink.dogtim.horizontalview.R;
import cyberlink.dogtim.horizontalview.util.StringUtils;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

/**
 * The view which displays the scroll position
 */
public class PlayheadView extends View {
    private static final String TAG = PlayheadView.class.getSimpleName();
    // Instance variables
    private final Paint mLinePaint;
    private final Paint mTextPaint;
    private final int mTicksHeight;
    // Timeline text size.
    private final float mTimeTextSize;
    private final int mScreenWidth;
    private final ScrollViewListener mScrollListener;
    private int mScrollX;
    //private VideoEditorProject mProject;

    public PlayheadView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final Resources resources = context.getResources();

        // Prepare the Paint used to draw the tick marks
        mLinePaint = new Paint();
        mLinePaint.setColor(resources.getColor(R.color.playhead_tick_color));
        mLinePaint.setStrokeWidth(2);
        mLinePaint.setStyle(Paint.Style.STROKE);

        // Prepare the Paint used to draw the text
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(resources.getColor(R.color.playhead_tick_color));
        mTimeTextSize = resources.getDimension(R.dimen.playhead_layout_text_size);
        mTextPaint.setTextSize(mTimeTextSize);

        // The ticks height
        mTicksHeight = (int)resources.getDimension(R.dimen.playhead_tick_height);

        // Get the screen width
        final Display display = ((WindowManager)context.getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay();
        final DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        mScreenWidth = metrics.widthPixels;

        // Listen to scroll events and repaint this view as needed
        mScrollListener = new ScrollViewListener() {

            @Override
            public void onScrollBegin(View view, int scrollX, int scrollY, boolean appScroll) {
            }

            @Override
            public void onScrollProgress(View view, int scrollX, int scrollY, boolean appScroll) {
                mScrollX = scrollX;
                invalidate();
            }

            @Override
            public void onScrollEnd(View view, int scrollX, int scrollY, boolean appScroll) {
                mScrollX = scrollX;
                invalidate();
            }
        };
    }

    public PlayheadView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayheadView(Context context) {
        this(context, null, 0);
    }

    @Override
    protected void onAttachedToWindow() {
        final TimelineHorizontalScrollView scrollView =
            (TimelineHorizontalScrollView)((View)getParent()).getParent();
        mScrollX = scrollView.getScrollX();
        scrollView.addScrollListener(mScrollListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        final TimelineHorizontalScrollView scrollView =
            (TimelineHorizontalScrollView)((View)getParent()).getParent();
        scrollView.removeScrollListener(mScrollListener);
    }

    /**
     * @param project The project
     */
/*    public void setProject(VideoEditorProject project) {
        mProject = project;
    }*/

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        /* TODO
         * hard code to define durationMs
         * this variable could determine how many column we would split on duratio track
         * */
        final long durationMs = 100000;

        final int y = (int) -mTextPaint.getFontMetrics().top;

        final int width = getWidth() - mScreenWidth;

        final long tickMs = 10000;

        final float spacing = ((float) (width * tickMs) / (float) durationMs);
        final float startX = 50;
        float startMs = 0;
        
        final Context context = getContext();
        final float endX = mScrollX + mScreenWidth;

        for (float i = startX; i <= endX; i += spacing, startMs += tickMs) {
            final String timeText = StringUtils.getSimpleTimestampAsString(context, (long) startMs);
            final int x = (int) (i - mTextPaint.measureText(timeText) / 2);
            canvas.drawText(timeText, x, y, mTextPaint);
            canvas.drawLine(i, mTimeTextSize, i, mTimeTextSize + mTicksHeight, mLinePaint);
        }
    }
}
