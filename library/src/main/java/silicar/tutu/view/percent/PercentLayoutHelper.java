/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package silicar.tutu.view.percent;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import silicar.tutu.view.R;


/**
 * Helper for layouts that want to support percentage based dimensions.
 * <p/>
 * <p>This class collects utility methods that are involved in extracting percentage based dimension
 * attributes and applying them to ViewGroup's children. If you would like to implement a layout
 * that supports percentage based dimensions, you need to take several steps:
 * <p/>
 * <ol>
 * <li> You need a {@link ViewGroup.LayoutParams} subclass in your ViewGroup that implements
 * {@link silicar.tutu.view.percent.PercentLayoutHelper.PercentLayoutParams}.
 * <li> In your {@code LayoutParams(Context c, AttributeSet attrs)} constructor create an instance
 * of {@link PercentLayoutHelper.PercentLayoutInfo} by calling
 * {@link PercentLayoutHelper#getPercentLayoutInfo(Context, AttributeSet)}. Return this
 * object from {@code public PercentLayoutHelper.PercentLayoutInfo getPercentLayoutInfo()}
 * method that you implemented for {@link silicar.tutu.view.percent.PercentLayoutHelper.PercentLayoutParams} interface.
 * <li> Override
 * {@link ViewGroup.LayoutParams#setBaseAttributes(TypedArray, int, int)}
 * with a single line implementation {@code PercentLayoutHelper.fetchWidthAndHeight(this, a,
 * widthAttr, heightAttr);}
 * <li> In your ViewGroup override {@link ViewGroup#generateLayoutParams(AttributeSet)} to return
 * your LayoutParams.
 * <li> In your {@link ViewGroup#onMeasure(int, int)} override, you need to implement following
 * pattern:
 * <pre class="prettyprint">
 * protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
 * mHelper.adjustChildren(widthMeasureSpec, heightMeasureSpec);
 * super.onMeasure(widthMeasureSpec, heightMeasureSpec);
 * if (mHelper.handleMeasuredStateTooSmall()) {
 * super.onMeasure(widthMeasureSpec, heightMeasureSpec);
 * }
 * }
 * </pre>
 * <li>In your {@link ViewGroup#onLayout(boolean, int, int, int, int)} override, you need to
 * implement following pattern:
 * <pre class="prettyprint">
 * protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
 * super.onLayout(changed, left, top, right, bottom);
 * mHelper.restoreOriginalParams();
 * }
 * </pre>
 * </ol>
 */
public class PercentLayoutHelper
{
    private static final String TAG = "PercentLayout";

    private final ViewGroup mHost;

    private static int mWidthScreen;
    private static int mHeightScreen;

    public PercentLayoutHelper(ViewGroup host)
    {
        mHost = host;
        getDefaultSize();
    }

    private void getDefaultSize()
    {
//        WindowManager wm = (WindowManager) mHost.getContext().getSystemService(Context.WINDOW_SERVICE);
//        DisplayMetrics outMetrics = new DisplayMetrics();
//        wm.getDefaultDisplay().getMetrics(outMetrics);
//        mWidthScreen = outMetrics.widthPixels;
//        mHeightScreen = outMetrics.heightPixels;
        mHeightScreen = mHost.getResources().getDisplayMetrics().heightPixels;
        mWidthScreen = mHost.getResources().getDisplayMetrics().widthPixels;
    }

    public int getWidthScreen() {
        return mWidthScreen;
    }

    public int getHeightScreen() {
        return mHeightScreen;
    }

    /**
     * Helper method to be called from {@link ViewGroup.LayoutParams#setBaseAttributes} override
     * that reads layout_width and layout_height attribute values without throwing an exception if
     * they aren't present.
     */
    public static void fetchWidthAndHeight(ViewGroup.LayoutParams params, TypedArray array,
                                           int widthAttr, int heightAttr)
    {
        params.width = array.getLayoutDimension(widthAttr, 0);
        params.height = array.getLayoutDimension(heightAttr, 0);
    }

    /**
     * Iterates over children and changes their width and height to one calculated from percentage
     * values.
     *
     * @param widthMeasureSpec  Width MeasureSpec of the parent ViewGroup.
     * @param heightMeasureSpec Height MeasureSpec of the parent ViewGroup.
     */
    public void adjustChildren(int widthMeasureSpec, int heightMeasureSpec)
    {
        if (Log.isLoggable(TAG, Log.DEBUG))
        {
            Log.d(TAG, "adjustChildren: " + mHost + " widthMeasureSpec: "
                    + View.MeasureSpec.toString(widthMeasureSpec) + " heightMeasureSpec: "
                    + View.MeasureSpec.toString(heightMeasureSpec));
        }
        int widthHint = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightHint = View.MeasureSpec.getSize(heightMeasureSpec);

        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "widthHint = " + widthHint + " , heightHint = " + heightHint);

        for (int i = 0, N = mHost.getChildCount(); i < N; i++)
        {
            View view = mHost.getChildAt(i);
            ViewGroup.LayoutParams params = view.getLayoutParams();

            if (Log.isLoggable(TAG, Log.DEBUG))
            {
                Log.d(TAG, "should adjust " + view + " " + params);
            }

            if (params instanceof PercentLayoutParams)
            {
                PercentLayoutInfo info =
                        ((PercentLayoutParams) params).getPercentLayoutInfo();
                if (Log.isLoggable(TAG, Log.DEBUG))
                {
                    Log.d(TAG, "using " + info);
                }
                if (info != null)
                {
                    fillTextSize(widthHint, heightHint, view, info);
                    fillPadding(widthHint, heightHint, view, info);
                    fillMinOrMaxDimension(widthHint, heightHint, view, info);

                    if (params instanceof ViewGroup.MarginLayoutParams)
                    {
                        fillMarginLayoutParams((ViewGroup.MarginLayoutParams) params,
                                widthHint, heightHint, info);
                    } else
                    {
                        fillLayoutParams(params, widthHint, heightHint, info);
                    }
                }
            }
        }


    }

    /**
     * Iterates over children and restores their original dimensions that were changed for
     * percentage values. Calling this method only makes sense if you previously called
     * {@link PercentLayoutHelper#adjustChildren(int, int)}.
     */
    public void restoreOriginalParams()
    {
        for (int i = 0, N = mHost.getChildCount(); i < N; i++)
        {
            View view = mHost.getChildAt(i);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (Log.isLoggable(TAG, Log.DEBUG))
            {
                Log.d(TAG, "should restore " + view + " " + params);
            }
            if (params instanceof PercentLayoutParams)
            {
                PercentLayoutInfo info =
                        ((PercentLayoutParams) params).getPercentLayoutInfo();
                if (Log.isLoggable(TAG, Log.DEBUG))
                {
                    Log.d(TAG, "using " + info);
                }
                if (info != null)
                {
                    if (params instanceof ViewGroup.MarginLayoutParams)
                    {
                        restoreMarginLayoutParams((ViewGroup.MarginLayoutParams) params, info);
                    } else
                    {
                        restoreLayoutParams(params, info);
                    }
                }
            }
        }
    }


    ///////////// 计算过程 /////////////

    public void fillTextSize(int widthHint, int heightHint, View view, PercentLayoutInfo info)
    {
        //textsize percent support

        PercentLayoutInfo.PercentVal textSizePercent = info.textSizePercent;
        if (textSizePercent == null) return;

        float base = getBaseByModeAndVal(widthHint, heightHint, info, textSizePercent.basemode);
        float textSize = (int) (base * textSizePercent.percent);

        //Button 和 EditText 是TextView的子类
        if (view instanceof TextView)
        {
            ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        }
    }

    public void fillPadding(int widthHint, int heightHint, View view, PercentLayoutInfo info)
    {
        int left = view.getPaddingLeft(), right = view.getPaddingRight(), top = view.getPaddingTop(), bottom = view.getPaddingBottom();
        PercentLayoutInfo.PercentVal percentVal = info.paddingLeftPercent;
        if (percentVal != null)
        {
            float base = getBaseByModeAndVal(widthHint, heightHint, info, percentVal.basemode);
            left = (int) (base * percentVal.percent);
        }
        percentVal = info.paddingRightPercent;
        if (percentVal != null)
        {
            float base = getBaseByModeAndVal(widthHint, heightHint, info, percentVal.basemode);
            right = (int) (base * percentVal.percent);
        }

        percentVal = info.paddingTopPercent;
        if (percentVal != null)
        {
            float base = getBaseByModeAndVal(widthHint, heightHint, info, percentVal.basemode);
            top = (int) (base * percentVal.percent);
        }

        percentVal = info.paddingBottomPercent;
        if (percentVal != null)
        {
            float base = getBaseByModeAndVal(widthHint, heightHint, info, percentVal.basemode);
            bottom = (int) (base * percentVal.percent);
        }
        view.setPadding(left, top, right, bottom);


    }

    public void fillMinOrMaxDimension(int widthHint, int heightHint, View view, PercentLayoutInfo info)
    {
        Class clazz = view.getClass();
        try
        {
            invokeMethod("setMaxWidth", widthHint, heightHint, view, clazz, info, info.maxWidthPercent);
            invokeMethod("setMaxHeight", widthHint, heightHint, view, clazz, info, info.maxHeightPercent);
            invokeMethod("setMinWidth", widthHint, heightHint, view, clazz, info, info.minWidthPercent);
            invokeMethod("setMinHeight", widthHint, heightHint, view, clazz, info, info.minHeightPercent);
        } catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    private void invokeMethod(String methodName, int widthHint, int heightHint, View view, Class clazz, PercentLayoutInfo info, PercentLayoutInfo.PercentVal percentVal) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        //if (Log.isLoggable(TAG, Log.DEBUG))
        //    Log.d(TAG, methodName + " ==> " + percentVal);
        if (percentVal != null)
        {
            Method setMaxWidthMethod = clazz.getMethod(methodName, int.class);
            setMaxWidthMethod.setAccessible(true);
            float base = getBaseByModeAndVal(widthHint, heightHint, info, percentVal.basemode);
            setMaxWidthMethod.invoke(view, (int) (base * percentVal.percent));
        }
    }

    /**
     * Fills {@code ViewGroup.MarginLayoutParams} dimensions and margins based on percentage
     * values.
     */
    public void fillMarginLayoutParams(ViewGroup.MarginLayoutParams params, int widthHint,
                                       int heightHint, PercentLayoutInfo info)
    {
        fillLayoutParams(params, widthHint, heightHint, info);

        // Preserver the original margins, so we can restore them after the measure step.
        info.mPreservedParams.leftMargin = params.leftMargin;
        info.mPreservedParams.topMargin = params.topMargin;
        info.mPreservedParams.rightMargin = params.rightMargin;
        info.mPreservedParams.bottomMargin = params.bottomMargin;
        MarginLayoutParamsCompat.setMarginStart(info.mPreservedParams,
                MarginLayoutParamsCompat.getMarginStart(params));
        MarginLayoutParamsCompat.setMarginEnd(info.mPreservedParams,
                MarginLayoutParamsCompat.getMarginEnd(params));

        if (info.leftMarginPercent != null)
        {
            float base = getBaseByModeAndVal(widthHint, heightHint, info, info.leftMarginPercent.basemode);
            params.leftMargin = (int) (base * info.leftMarginPercent.percent);
        }
        if (info.topMarginPercent != null)
        {
            float base = getBaseByModeAndVal(widthHint, heightHint, info, info.topMarginPercent.basemode);
            params.topMargin = (int) (base * info.topMarginPercent.percent);
        }
        if (info.rightMarginPercent != null)
        {
            float base = getBaseByModeAndVal(widthHint, heightHint, info, info.rightMarginPercent.basemode);
            params.rightMargin = (int) (base * info.rightMarginPercent.percent);
        }
        if (info.bottomMarginPercent != null)
        {
            float base = getBaseByModeAndVal(widthHint, heightHint, info, info.bottomMarginPercent.basemode);
            params.bottomMargin = (int) (base * info.bottomMarginPercent.percent);
        }
        if (info.startMarginPercent != null)
        {
            float base = getBaseByModeAndVal(widthHint, heightHint, info, info.startMarginPercent.basemode);
            MarginLayoutParamsCompat.setMarginStart(params,
                    (int) (base * info.startMarginPercent.percent));
        }
        if (info.endMarginPercent != null)
        {
            float base = getBaseByModeAndVal(widthHint, heightHint, info, info.endMarginPercent.basemode);
            MarginLayoutParamsCompat.setMarginEnd(params,
                    (int) (base * info.endMarginPercent.percent));
        }
        if (Log.isLoggable(TAG, Log.DEBUG))
        {
            Log.d(TAG, "after fillMarginLayoutParams: (" + params.width + ", " + params.height
                    + ")");
        }
    }

    /**
     * Fills {@code ViewGroup.LayoutParams} dimensions based on percentage values.
     */
    public void fillLayoutParams(ViewGroup.LayoutParams params, int widthHint,
                                 int heightHint, PercentLayoutInfo info)
    {
        // Preserve the original layout params, so we can restore them after the measure step.
        info.mPreservedParams.width = params.width;
        info.mPreservedParams.height = params.height;

        if (info.widthPercent != null)
        {
            float base = getBaseByModeAndVal(widthHint, heightHint, info, info.widthPercent.basemode);
            params.width = (int) (base * info.widthPercent.percent);
        }
        if (info.heightPercent != null)
        {
            float base = getBaseByModeAndVal(widthHint, heightHint, info, info.heightPercent.basemode);
            params.height = (int) (base * info.heightPercent.percent);
        }

        if (Log.isLoggable(TAG, Log.DEBUG))
        {
            Log.d(TAG, "after fillLayoutParams: (" + params.width + ", " + params.height + ")");
        }
    }


    ///////////// 重置过程 /////////////

    /**
     * Restores original dimensions and margins after they were changed for percentage based
     * values. Calling this method only makes sense if you previously called
     * {@link PercentLayoutHelper.PercentLayoutInfo#fillMarginLayoutParams}.
     */
    public void restoreMarginLayoutParams(ViewGroup.MarginLayoutParams params, PercentLayoutInfo info)
    {
        restoreLayoutParams(params, info);
        params.leftMargin = info.mPreservedParams.leftMargin;
        params.topMargin = info.mPreservedParams.topMargin;
        params.rightMargin = info.mPreservedParams.rightMargin;
        params.bottomMargin = info.mPreservedParams.bottomMargin;
        MarginLayoutParamsCompat.setMarginStart(params,
                MarginLayoutParamsCompat.getMarginStart(info.mPreservedParams));
        MarginLayoutParamsCompat.setMarginEnd(params,
                MarginLayoutParamsCompat.getMarginEnd(info.mPreservedParams));
    }

    /**
     * Restores original dimensions after they were changed for percentage based values. Calling
     * this method only makes sense if you previously called
     * {@link PercentLayoutHelper.PercentLayoutInfo#fillLayoutParams}.
     */
    public void restoreLayoutParams(ViewGroup.LayoutParams params, PercentLayoutInfo info)
    {
        params.width = info.mPreservedParams.width;
        params.height = info.mPreservedParams.height;
    }

    ///////////// 赋值过程 /////////////
    
    /**
     * Constructs a PercentLayoutInfo from attributes associated with a View. Call this method from
     * {@code LayoutParams(Context c, AttributeSet attrs)} constructor.
     */
    public static PercentLayoutInfo getPercentLayoutInfo(Context context, AttributeSet attrs){
        return getPercentLayoutInfo(context, attrs, 0);
    }

    public static PercentLayoutInfo getPercentLayoutInfo(Context context, AttributeSet attrs, int style){
        PercentLayoutInfo info = null;
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.PercentLayoutInfo, 0, style);

        info = setDesignSizeVal(array, info);

        info = setWidthAndHeightVal(array, info);

        info = setMarginRelatedVal(array, info);

        info = setTextSizeSupportVal(array, info);

        info = setMinMaxWidthHeightRelatedVal(array, info);

        info = setPaddingRelatedVal(array, info);


        array.recycle();

        if (Log.isLoggable(TAG, Log.DEBUG))
        {
            Log.d(TAG, "constructed: " + info);
        }
        return info;
    }

    private static PercentLayoutInfo setDesignSizeVal(TypedArray array, PercentLayoutInfo info) {
        info = checkForInfoExists(info);
        info.widthDesign = array.getDimension(R.styleable.PercentLayoutInfo_layout_widthDesign, mWidthScreen);
        info.heightDesign = array.getDimension(R.styleable.PercentLayoutInfo_layout_heightDesign, mWidthScreen);
        return info;
    }

    private static PercentLayoutInfo setWidthAndHeightVal(TypedArray array, PercentLayoutInfo info)
    {
        PercentLayoutInfo.PercentVal percentVal = getPercentVal(array, R.styleable.PercentLayoutInfo_layout_widthPercent, true);
        if (percentVal != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent width: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.widthPercent = percentVal;
        }
        percentVal = getPercentVal(array, R.styleable.PercentLayoutInfo_layout_heightPercent, false);

        if (percentVal != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent height: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.heightPercent = percentVal;
        }

        return info;
    }

    private static PercentLayoutInfo setTextSizeSupportVal(TypedArray array, PercentLayoutInfo info)
    {
        //textSizePercent 默认以高度作为基准
        PercentLayoutInfo.PercentVal percentVal = getPercentVal(array, R.styleable.PercentLayoutInfo_layout_textSizePercent, false);
        if (percentVal != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent text size: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.textSizePercent = percentVal;
        }

        return info;
    }

    private static PercentLayoutInfo setMinMaxWidthHeightRelatedVal(TypedArray array, PercentLayoutInfo info)
    {
        //maxWidth
        PercentLayoutInfo.PercentVal percentVal = getPercentVal(array,
                R.styleable.PercentLayoutInfo_layout_maxWidthPercent,
                true);
        if (percentVal != null)
        {
            info = checkForInfoExists(info);
            info.maxWidthPercent = percentVal;
        }
        //maxHeight
        percentVal = getPercentVal(array,
                R.styleable.PercentLayoutInfo_layout_maxHeightPercent,
                false);
        if (percentVal != null)
        {
            info = checkForInfoExists(info);
            info.maxHeightPercent = percentVal;
        }
        //minWidth
        percentVal = getPercentVal(array,
                R.styleable.PercentLayoutInfo_layout_minWidthPercent,
                true);
        if (percentVal != null)
        {
            info = checkForInfoExists(info);
            info.minWidthPercent = percentVal;
        }
        //minHeight
        percentVal = getPercentVal(array,
                R.styleable.PercentLayoutInfo_layout_minHeightPercent,
                false);
        if (percentVal != null)
        {
            info = checkForInfoExists(info);
            info.minHeightPercent = percentVal;
        }

        return info;
    }

    private static PercentLayoutInfo setMarginRelatedVal(TypedArray array, PercentLayoutInfo info)
    {
        //默认margin参考宽度
        PercentLayoutInfo.PercentVal percentVal =
                getPercentVal(array,
                        R.styleable.PercentLayoutInfo_layout_marginPercent,
                        true);

        if (percentVal != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.leftMarginPercent = percentVal;
            info.topMarginPercent = percentVal;
            info.rightMarginPercent = percentVal;
            info.bottomMarginPercent = percentVal;
        }

        percentVal = getPercentVal(array, R.styleable.PercentLayoutInfo_layout_marginLeftPercent, true);
        if (percentVal != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent left margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.leftMarginPercent = percentVal;
        }

        percentVal = getPercentVal(array, R.styleable.PercentLayoutInfo_layout_marginTopPercent, false);
        if (percentVal != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent top margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.topMarginPercent = percentVal;
        }

        percentVal = getPercentVal(array, R.styleable.PercentLayoutInfo_layout_marginRightPercent, true);
        if (percentVal != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent right margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.rightMarginPercent = percentVal;
        }

        percentVal = getPercentVal(array, R.styleable.PercentLayoutInfo_layout_marginBottomPercent, false);
        if (percentVal != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent bottom margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.bottomMarginPercent = percentVal;
        }
        percentVal = getPercentVal(array, R.styleable.PercentLayoutInfo_layout_marginStartPercent, true);
        if (percentVal != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent start margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.startMarginPercent = percentVal;
        }

        percentVal = getPercentVal(array, R.styleable.PercentLayoutInfo_layout_marginEndPercent, true);
        if (percentVal != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent end margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.endMarginPercent = percentVal;
        }

        return info;
    }

    /**
     * 设置paddingPercent相关属性
     *
     * @param array
     * @param info
     */
    private static PercentLayoutInfo setPaddingRelatedVal(TypedArray array, PercentLayoutInfo info)
    {
        //默认padding以宽度为标准
        PercentLayoutInfo.PercentVal percentVal = getPercentVal(array,
                R.styleable.PercentLayoutInfo_layout_paddingPercent,
                true);
        if (percentVal != null)
        {
            info = checkForInfoExists(info);
            info.paddingLeftPercent = percentVal;
            info.paddingRightPercent = percentVal;
            info.paddingBottomPercent = percentVal;
            info.paddingTopPercent = percentVal;
        }


        percentVal = getPercentVal(array,
                R.styleable.PercentLayoutInfo_layout_paddingLeftPercent,
                true);
        if (percentVal != null)
        {
            info = checkForInfoExists(info);
            info.paddingLeftPercent = percentVal;
        }

        percentVal = getPercentVal(array,
                R.styleable.PercentLayoutInfo_layout_paddingRightPercent,
                true);
        if (percentVal != null)
        {
            info = checkForInfoExists(info);
            info.paddingRightPercent = percentVal;
        }

        percentVal = getPercentVal(array,
                R.styleable.PercentLayoutInfo_layout_paddingTopPercent,
                true);
        if (percentVal != null)
        {
            info = checkForInfoExists(info);
            info.paddingTopPercent = percentVal;
        }

        percentVal = getPercentVal(array,
                R.styleable.PercentLayoutInfo_layout_paddingBottomPercent,
                true);
        if (percentVal != null)
        {
            info = checkForInfoExists(info);
            info.paddingBottomPercent = percentVal;
        }

        return info;
    }


    @NonNull
    private static PercentLayoutInfo checkForInfoExists(PercentLayoutInfo info)
    {
        info = (info != null) ? info : new PercentLayoutInfo();
        return info;
    }

    /**
     * 获取参数
     * @param array
     * @param index
     * @param baseWidth
     * @return
     */
    private static PercentLayoutInfo.PercentVal getPercentVal(TypedArray array, int index, boolean baseWidth)
    {
        String sizeStr = array.getString(index);
        PercentLayoutInfo.PercentVal percentVal = getPercentVal(sizeStr, baseWidth);
        return percentVal;
    }

    private static final String REGEX_PERCENT = "^(([0-9]+)([.]([0-9]+))?|([.]([0-9]+))?)(%|a)([s]?[wh]?)$";

    /**
     * 解析参数
     * widthStr to PercentVal
     * <br/>
     * eg: 35%w => new PercentVal("35%", true)
     *
     * @param percentStr
     * @param isOnWidth
     * @return
     */
    private static PercentLayoutInfo.PercentVal getPercentVal(String percentStr, boolean isOnWidth)
    {
        //valid param
        if (percentStr == null)
        {
            return null;
        }
        Pattern p = Pattern.compile(REGEX_PERCENT);
        Matcher matcher = p.matcher(percentStr);
        if (!matcher.matches())
        {
            throw new RuntimeException("the value of layout_xxxPercent invalid! ==>" + percentStr);
        }
        int len = percentStr.length();
        //extract the float value
        String floatVal = matcher.group(1);

        float percent = Float.parseFloat(floatVal) / 100f;

        PercentLayoutInfo.PercentVal percentVal = new PercentLayoutInfo.PercentVal();
        percentVal.percent = percent;
        if (percentStr.endsWith(PercentLayoutInfo.BaseMode.SW))
        {
            percentVal.basemode = PercentLayoutInfo.BaseMode.BASE_SCREEN_WIDTH;
        } else if (percentStr.endsWith(PercentLayoutInfo.BaseMode.SH))
        {
            percentVal.basemode = PercentLayoutInfo.BaseMode.BASE_SCREEN_HEIGHT;
        } else if (percentStr.endsWith(PercentLayoutInfo.BaseMode.AW))
        {
            percentVal.percent = percentVal.percent * 100f;
            percentVal.basemode = PercentLayoutInfo.BaseMode.BASE_AUTO_WIDTH;
        } else if (percentStr.endsWith(PercentLayoutInfo.BaseMode.AH))
        {
            percentVal.percent = percentVal.percent * 100f;
            percentVal.basemode = PercentLayoutInfo.BaseMode.BASE_AUTO_HEIGHT;
        } else if (percentStr.endsWith(PercentLayoutInfo.BaseMode.PERCENT))
        {
            if (isOnWidth)
            {
                percentVal.basemode = PercentLayoutInfo.BaseMode.BASE_WIDTH;
            } else
            {
                percentVal.basemode = PercentLayoutInfo.BaseMode.BASE_HEIGHT;
            }
        } else if (percentStr.endsWith(PercentLayoutInfo.BaseMode.AUTO))
        {
            percentVal.percent = percentVal.percent * 100f;
            if (isOnWidth)
            {
                percentVal.basemode = PercentLayoutInfo.BaseMode.BASE_AUTO_WIDTH;
            } else
            {
                percentVal.basemode = PercentLayoutInfo.BaseMode.BASE_AUTO_HEIGHT;
            }
        }else if (percentStr.endsWith(PercentLayoutInfo.BaseMode.W))
        {
            percentVal.basemode = PercentLayoutInfo.BaseMode.BASE_WIDTH;
        } else if (percentStr.endsWith(PercentLayoutInfo.BaseMode.H))
        {
            percentVal.basemode = PercentLayoutInfo.BaseMode.BASE_HEIGHT;
        } else
        {
            throw new IllegalArgumentException("the " + percentStr + " must be endWith [%|a|w|h|sw|sh|aw|ah]");
        }

        return percentVal;
    }

    /**
     * 获取不同模式基础值
     * @param widthHint
     * @param heightHint
     * @param info
     * @param baseMode
     * @return
     */
    private static float getBaseByModeAndVal(int widthHint, int heightHint, PercentLayoutInfo info, PercentLayoutInfo.BaseMode baseMode)
    {
        switch (baseMode)
        {
            case BASE_HEIGHT:
                return heightHint;
            case BASE_WIDTH:
                return widthHint;
            case BASE_SCREEN_WIDTH:
                return mWidthScreen;
            case BASE_SCREEN_HEIGHT:
                return mHeightScreen;
            case BASE_AUTO_HEIGHT:
                return mHeightScreen / info.heightDesign;
            case BASE_AUTO_WIDTH:
                return mWidthScreen / info.widthDesign;
        }
        return 0;
    }

    ///////////// 比较过程 /////////////

    /**
     * Iterates over children and checks if any of them would like to get more space than it
     * received through the percentage dimension.
     * <p/>
     * If you are building a layout that supports percentage dimensions you are encouraged to take
     * advantage of this method. The developer should be able to specify that a child should be
     * remeasured by adding normal dimension attribute with {@code wrap_content} value. For example
     * he might specify child's attributes as {@code app:layout_widthPercent="60%p"} and
     * {@code android:layout_width="wrap_content"}. In this case if the child receives too little
     * space, it will be remeasured with width set to {@code WRAP_CONTENT}.
     *
     * @return True if the measure phase needs to be rerun because one of the children would like
     * to receive more space.
     */
    public boolean handleMeasuredStateTooSmall()
    {
        boolean needsSecondMeasure = false;
        for (int i = 0, N = mHost.getChildCount(); i < N; i++)
        {
            View view = mHost.getChildAt(i);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (Log.isLoggable(TAG, Log.DEBUG))
            {
                Log.d(TAG, "should handle measured state too small " + view + " " + params);
            }
            if (params instanceof PercentLayoutParams)
            {
                PercentLayoutInfo info =
                        ((PercentLayoutParams) params).getPercentLayoutInfo();
                if (info != null)
                {
                    if (shouldHandleMeasuredWidthTooSmall(view, info))
                    {
                        needsSecondMeasure = true;
                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                    if (shouldHandleMeasuredHeightTooSmall(view, info))
                    {
                        needsSecondMeasure = true;
                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                }
            }
        }
        if (Log.isLoggable(TAG, Log.DEBUG))
        {
            Log.d(TAG, "should trigger second measure pass: " + needsSecondMeasure);
        }
        return needsSecondMeasure;
    }

    private static boolean shouldHandleMeasuredWidthTooSmall(View view, PercentLayoutInfo info)
    {
        int state = ViewCompat.getMeasuredWidthAndState(view) & ViewCompat.MEASURED_STATE_MASK;
        if (info == null || info.widthPercent == null)
        {
            return false;
        }
        return state == ViewCompat.MEASURED_STATE_TOO_SMALL && info.widthPercent.percent >= 0 &&
                info.mPreservedParams.width == ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    private static boolean shouldHandleMeasuredHeightTooSmall(View view, PercentLayoutInfo info)
    {
        int state = ViewCompat.getMeasuredHeightAndState(view) & ViewCompat.MEASURED_STATE_MASK;
        if (info == null || info.heightPercent == null)
        {
            return false;
        }
        return state == ViewCompat.MEASURED_STATE_TOO_SMALL && info.heightPercent.percent >= 0 &&
                info.mPreservedParams.height == ViewGroup.LayoutParams.WRAP_CONTENT;
    }


    /**
     * Container for information about percentage dimensions and margins. It acts as an extension
     * for {@code LayoutParams}.
     */
    public static class PercentLayoutInfo
    {

        public enum BaseMode
        {

            BASE_WIDTH, BASE_HEIGHT, BASE_SCREEN_WIDTH, BASE_SCREEN_HEIGHT, BASE_AUTO_WIDTH, BASE_AUTO_HEIGHT;

            /**
             * layout_auto
             */
            public static final String AUTO = "a";
            /**
             * layout_parent
             */
            public static final String PERCENT = "%";
            /**
             * width_parent
             */
            public static final String W = "w";
            /**
             * height_parent
             */
            public static final String H = "h";
            /**
             * width_auto
             */
            public static final String AW = "aw";
            /**
             * height_auto
             */
            public static final String AH = "ah";
            /**
             * width_screen
             */
            public static final String SW = "sw";
            /**
             * height_screen
             */
            public static final String SH = "sh";
        }

        public static class PercentVal
        {

            public float percent = -1;
            public BaseMode basemode;

            public PercentVal()
            {
            }

            public PercentVal(float percent, BaseMode baseMode)
            {
                this.percent = percent;
                this.basemode = baseMode;
            }

            @Override
            public String toString()
            {
                return "PercentVal{" +
                        "percent=" + percent +
                        ", basemode=" + basemode.name() +
                        '}';
            }
        }

        public PercentVal widthPercent;
        public PercentVal heightPercent;
        public float widthDesign;
        public float heightDesign;

        public PercentVal leftMarginPercent;
        public PercentVal topMarginPercent;
        public PercentVal rightMarginPercent;
        public PercentVal bottomMarginPercent;
        public PercentVal startMarginPercent;
        public PercentVal endMarginPercent;

        public PercentVal textSizePercent;

        //1.0.4 those attr for some views' setMax/min Height/Width method
        public PercentVal maxWidthPercent;
        public PercentVal maxHeightPercent;
        public PercentVal minWidthPercent;
        public PercentVal minHeightPercent;

        //1.0.6 add padding supprot
        public PercentVal paddingLeftPercent;
        public PercentVal paddingRightPercent;
        public PercentVal paddingTopPercent;
        public PercentVal paddingBottomPercent;


        /* package */
        ViewGroup.MarginLayoutParams mPreservedParams;


        public PercentLayoutInfo()
        {
            mPreservedParams = new ViewGroup.MarginLayoutParams(0, 0);
        }

        public int getWidthScreen() {
            return mWidthScreen;
        }

        public int getHeightScreen() {
            return mHeightScreen;
        }

        public int getPercentSize(int parentWidth, int parentHeight, PercentLayoutInfo info, PercentVal val){
            float base = getBaseByModeAndVal(parentWidth, parentHeight, info, val.basemode);
            return (int) (base * val.percent);
        }

        @Override
        public String toString()
        {
            return "PercentLayoutInfo{" +
                    "widthPercent=" + widthPercent +
                    ", heightPercent=" + heightPercent +
                    ", widthDesign=" + widthDesign +
                    ", heightDesign=" + heightDesign +
                    ", leftMarginPercent=" + leftMarginPercent +
                    ", topMarginPercent=" + topMarginPercent +
                    ", rightMarginPercent=" + rightMarginPercent +
                    ", bottomMarginPercent=" + bottomMarginPercent +
                    ", startMarginPercent=" + startMarginPercent +
                    ", endMarginPercent=" + endMarginPercent +
                    ", textSizePercent=" + textSizePercent +
                    ", maxWidthPercent=" + maxWidthPercent +
                    ", maxHeightPercent=" + maxHeightPercent +
                    ", minWidthPercent=" + minWidthPercent +
                    ", minHeightPercent=" + minHeightPercent +
                    ", paddingLeftPercent=" + paddingLeftPercent +
                    ", paddingRightPercent=" + paddingRightPercent +
                    ", paddingTopPercent=" + paddingTopPercent +
                    ", paddingBottomPercent=" + paddingBottomPercent +
                    ", mPreservedParams=" + mPreservedParams +
                    '}';
        }
    }

    /**
     * If a layout wants to support percentage based dimensions and use this helper class, its
     * {@code LayoutParams} subclass must implement this interface.
     * <p/>
     * Your {@code LayoutParams} subclass should contain an instance of {@code PercentLayoutInfo}
     * and the implementation of this interface should be a simple accessor.
     */
    public interface PercentLayoutParams
    {
        PercentLayoutInfo getPercentLayoutInfo();
    }
}
