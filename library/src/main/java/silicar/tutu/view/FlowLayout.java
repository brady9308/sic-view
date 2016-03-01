package silicar.tutu.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * 流式布局
 * @version  1.0
 * @since 2015/7/24
 * @author Work
 */

public class FlowLayout extends ViewGroup {

	private static final int DEFAULT_HORIZONTAL_SPACING = 5;
	private static final int DEFAULT_VERTICAL_SPACING = 5;

	private int mVerticalSpacing;
	private int mHorizontalSpacing;

	public FlowLayout(Context context) {
		super(context);
	}

	public FlowLayout(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout);
		try {
			mHorizontalSpacing = a.getDimensionPixelSize(
					R.styleable.FlowLayout_horizontalSpacing, DEFAULT_HORIZONTAL_SPACING);
			mVerticalSpacing = a.getDimensionPixelSize(
					R.styleable.FlowLayout_verticalSpacing, DEFAULT_VERTICAL_SPACING);
		} finally {
			a.recycle();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//int myWidth = resolveSize(0, widthMeasureSpec);
		int myWidth = MeasureSpec.getSize( widthMeasureSpec);
		int paddingLeft = getPaddingLeft();
		int paddingTop = getPaddingTop();
		int paddingRight = getPaddingRight();
		int paddingBottom = getPaddingBottom();

		int childLeft = paddingLeft;
		int childTop = paddingTop;

		int lineHeight = 0;

		for (int i = 0, childCount = getChildCount(); i < childCount; ++i) {
			View childView = getChildAt(i);
			//MarginLayoutParams params = new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			MarginLayoutParams params = (MarginLayoutParams) childView.getLayoutParams();

			if (childView.getVisibility() == View.GONE) {
				continue;
			}
			measureChild(childView, widthMeasureSpec, heightMeasureSpec);

			int childWidth = childView.getMeasuredWidth();
			int childHeight = childView.getMeasuredHeight();

			lineHeight = Math.max(childHeight, lineHeight);

			if (childLeft + childWidth + paddingRight > myWidth) {
				childLeft = paddingLeft;
				childTop += mVerticalSpacing + lineHeight;
				lineHeight = childHeight;
			}

			params.width = childWidth;
			params.height = childHeight;
			params.leftMargin = childLeft;
			params.topMargin = childTop;
			//childView.setLayoutParams(params);
			childLeft += childWidth + mHorizontalSpacing;
		}

		int wantedHeight = childTop + lineHeight + paddingBottom;

		setMeasuredDimension(myWidth, resolveSize(wantedHeight, heightMeasureSpec));
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {

		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				MarginLayoutParams params =
						(MarginLayoutParams) child.getLayoutParams();
				child.layout(params.leftMargin, params.topMargin, params.leftMargin + params.width, params.topMargin + params.height);
			}
		}
	}

	public void setHorizontalSpacing(int pixelSize) {
		mHorizontalSpacing = pixelSize;
	}

	public void setVerticalSpacing(int pixelSize) {
		mVerticalSpacing = pixelSize;
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new MarginLayoutParams(getContext(), attrs);
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	}

	@Override
	protected boolean checkLayoutParams(LayoutParams p) {
		return p instanceof MarginLayoutParams;
	}
}

