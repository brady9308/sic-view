package silicar.tutu.view.percent;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/**
 * Created by tutu on 2016/2/24.
 */
public class PercentLinearLayout extends LinearLayout {

    private final PercentLayoutHelper mPercentLayoutHelper = new PercentLayoutHelper(this);

    public PercentLinearLayout(Context context) {
        super(context);
    }

    public PercentLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public PercentLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PercentLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int tmpHeightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, heightMode);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int tmpWidthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize, widthMode);

        //fixed scrollview height problems
        if (heightMode == MeasureSpec.UNSPECIFIED && getParent() != null && (getParent() instanceof ScrollView))
        {
            int baseHeight = mPercentLayoutHelper.getHeightScreen();
            tmpHeightMeasureSpec = MeasureSpec.makeMeasureSpec(baseHeight, heightMode);
        }
        if (heightMode == MeasureSpec.UNSPECIFIED && getParent() != null && (getParent() instanceof HorizontalScrollView))
        {
            int baseWidth = mPercentLayoutHelper.getWidthScreen();
            tmpWidthMeasureSpec = MeasureSpec.makeMeasureSpec(baseWidth, widthMode);
        }

        mPercentLayoutHelper.adjustChildren(tmpWidthMeasureSpec, tmpHeightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mPercentLayoutHelper.handleMeasuredStateTooSmall())
        {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        super.onLayout(changed, l, t, r, b);
        mPercentLayoutHelper.restoreOriginalParams();
    }

    @Override
    protected LinearLayout.LayoutParams generateDefaultLayoutParams() {
        if (getOrientation() == HORIZONTAL) {
            return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        } else if (getOrientation() == VERTICAL) {
            return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        }
        return null;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs)
    {
        return new LayoutParams(getContext(), attrs);
    }


    public static class LayoutParams extends LinearLayout.LayoutParams
            implements PercentLayoutHelper.PercentLayoutParams
    {
        private PercentLayoutHelper.PercentLayoutInfo mPercentLayoutInfo;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            mPercentLayoutInfo = PercentLayoutHelper.getPercentLayoutInfo(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, float weight) {
            super(width, height, weight);
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(LinearLayout.LayoutParams source) {
            super((MarginLayoutParams)source);
            this.weight = source.weight;
            this.gravity = source.gravity;
        }

        public LayoutParams(PercentLinearLayout.LayoutParams source) {
            super((MarginLayoutParams)source);
            this.weight = source.weight;
            this.gravity = source.gravity;
            this.mPercentLayoutInfo = source.mPercentLayoutInfo;
       }


        @Override
        public PercentLayoutHelper.PercentLayoutInfo getPercentLayoutInfo()
        {
            if (mPercentLayoutInfo == null) {
                mPercentLayoutInfo = new PercentLayoutHelper.PercentLayoutInfo();
            }
            return mPercentLayoutInfo;
        }

        @Override
        protected void setBaseAttributes(TypedArray a, int widthAttr, int heightAttr)
        {
            PercentLayoutHelper.fetchWidthAndHeight(this, a, widthAttr, heightAttr);
        }

    }
}
