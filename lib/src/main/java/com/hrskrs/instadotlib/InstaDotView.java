package com.hrskrs.instadotlib;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hrskrs on 10/16/17.
 */

public class InstaDotView extends View {

    private static final int MIN_VISIBLE_DOT_COUNT = 6;
    private static final int DEFAULT_VISIBLE_DOTS_COUNT = MIN_VISIBLE_DOT_COUNT;

    private int activeDotSize;
    private int inactiveDotSize;
    private int mediumDotSize;
    private int smallDotSize;
    private int dotMargin;

    private Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint inactivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int startPosX;
    private int posY = 0;
    private int previousPage = 0;
    private int currentPage = 0;
    private int indexWindowStart = 0;
    private int indexWindowEnd = 0;

    private ValueAnimator translationAnim;

    private List<Dot> dotsList = new ArrayList<>();

    private int noOfPages = 0;
    private int visibleDotCounts = DEFAULT_VISIBLE_DOTS_COUNT;

    public InstaDotView(Context context) {
        super(context);
        setup(context, null);
    }

    public InstaDotView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup(context, attrs);
    }

    public InstaDotView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(context, attrs);
    }

    public InstaDotView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup(context, attrs);
    }

    private void setup(Context context, AttributeSet attributeSet) {
        Resources resources = getResources();

        if (attributeSet != null) {
            TypedArray ta = context.obtainStyledAttributes(attributeSet, R.styleable.InstaDotView);
            activePaint.setStyle(Paint.Style.FILL);
            activePaint.setColor(ta.getColor(R.styleable.InstaDotView_dot_activeColor, resources.getColor(R.color.active)));
            inactivePaint.setStyle(Paint.Style.FILL);
            inactivePaint.setColor(ta.getColor(R.styleable.InstaDotView_dot_inactiveColor, resources.getColor(R.color.inactive)));
            activeDotSize = ta.getDimensionPixelSize(R.styleable.InstaDotView_dot_activeSize, resources.getDimensionPixelSize(R.dimen.dot_active_size));
            inactiveDotSize = ta.getDimensionPixelSize(R.styleable.InstaDotView_dot_inactiveSize, resources.getDimensionPixelSize(R.dimen.dot_inactive_size));
            mediumDotSize = ta.getDimensionPixelSize(R.styleable.InstaDotView_dot_mediumSize, resources.getDimensionPixelSize(R.dimen.dot_medium_size));
            smallDotSize = ta.getDimensionPixelSize(R.styleable.InstaDotView_dot_smallSize, resources.getDimensionPixelSize(R.dimen.dot_small_size));
            dotMargin = ta.getDimensionPixelSize(R.styleable.InstaDotView_dot_margin, resources.getDimensionPixelSize(R.dimen.dot_margin));
            setVisibleDotCounts(ta.getInteger(R.styleable.InstaDotView_dots_visible, DEFAULT_VISIBLE_DOTS_COUNT));

            ta.recycle();
        }

        posY = activeDotSize / 2;

        initCircles();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = (activeDotSize + dotMargin) * (dotsList.size() + 1);
        int desiredHeight = activeDotSize;

        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        if (widthMode == View.MeasureSpec.EXACTLY) width = widthSize;
        else if (widthMode == View.MeasureSpec.AT_MOST) width = Math.min(desiredWidth, widthSize);
        else width = desiredWidth;

        if (heightMode == View.MeasureSpec.EXACTLY) height = heightSize;
        else if (heightMode == View.MeasureSpec.AT_MOST)
            height = Math.min(desiredHeight, heightSize);
        else height = desiredHeight;

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawCircles(canvas);
    }

    private void initCircles() {
        int viewCount = Math.min(getNoOfPages(), getVisibleDotCounts());
        if (viewCount < 1) return;

        setStartPosX(noOfPages > visibleDotCounts ? getSmallDotStartX() : 0);

        dotsList = new ArrayList<>(viewCount);
        for (int i = 0; i < viewCount; i++) {
            Dot dot = new Dot();
            Dot.State state;

            if (noOfPages > visibleDotCounts) {
                indexWindowStart = 0;
                indexWindowEnd = visibleDotCounts - 1;
                if (i == getVisibleDotCounts() - 1) state = Dot.State.SMALL;
                else if (i == getVisibleDotCounts() - 2) state = Dot.State.MEDIUM;
                else state = i == 0 ? Dot.State.ACTIVE : Dot.State.INACTIVE;
            } else {
                state = i == 0 ? Dot.State.ACTIVE : Dot.State.INACTIVE;
            }

            dot.setState(state);
            dotsList.add(dot);
        }

        invalidate();
    }

    private void drawCircles(Canvas canvas) {
        int posX = getStartPosX();

        for (int i = 0; i < dotsList.size(); i++) {

            Dot d = dotsList.get(i);
            Paint paint = inactivePaint;
            int radius;

            switch (d.getState()) {
                case ACTIVE:
                    paint = activePaint;
                    radius = getActiveDotRadius();
                    posX += getActiveDotStartX();
                    break;
                case INACTIVE:
                    radius = getInactiveDotRadius();
                    posX += getInactiveDotStartX();
                    break;
                case MEDIUM:
                    radius = getMediumDotRadius();
                    posX += getMediumDotStartX();
                    break;
                case SMALL:
                    radius = getSmallDotRadius();
                    posX += getSmallDotStartX();
                    break;
                default:
                    radius = 0;
                    posX = 0;
                    break;
            }


            canvas.drawCircle(posX, posY, radius, paint);
        }
    }


    private ValueAnimator getTranslationAnimation(int from, int to, final AnimationListener listener) {
        if (translationAnim != null) translationAnim.cancel();
        translationAnim = ValueAnimator.ofInt(from, to);
        translationAnim.setDuration(120);
        translationAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        translationAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                if (getStartPosX() != val) {
                    setStartPosX(val);
                    invalidate();
                }
            }
        });
        translationAnim.addListener(new AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (listener != null) listener.onAnimationEnd();
            }
        });
        return translationAnim;
    }

    public void setNoOfPages(int noOfPages) {
        //Hide if noOfPages is 0 or 1
        setVisibility(noOfPages <= 1 ? GONE : VISIBLE);
        this.noOfPages = noOfPages;
        recreate();
    }

    public int getNoOfPages() {
        return noOfPages;
    }

    public void setVisibleDotCounts(int visibleDotCounts) {
        if (visibleDotCounts < MIN_VISIBLE_DOT_COUNT)
            throw new RuntimeException("Visible Dot count cannot be smaller than " + MIN_VISIBLE_DOT_COUNT);
        this.visibleDotCounts = visibleDotCounts;
        recreate();
    }

    private void recreate() {
        currentPage = 0;
        previousPage = 0;
        indexWindowStart = 0;
        indexWindowEnd = 0;
        initCircles();
        requestLayout();
        invalidate();
    }

    public int getVisibleDotCounts() {
        return visibleDotCounts;
    }

    public void setStartPosX(int startPosX) {
        this.startPosX = startPosX;
    }

    public int getStartPosX() {
        return startPosX;
    }

    public int getActiveDotStartX() {
        return activeDotSize + dotMargin;
    }

    private int getInactiveDotStartX() {
        return inactiveDotSize + dotMargin;
    }

    private int getMediumDotStartX() {
        return mediumDotSize + dotMargin;
    }

    private int getSmallDotStartX() {
        return smallDotSize + dotMargin;
    }

    private int getActiveDotRadius() {
        return activeDotSize / 2;
    }

    private int getInactiveDotRadius() {
        return inactiveDotSize / 2;
    }

    private int getMediumDotRadius() {
        return mediumDotSize / 2;
    }

    private int getSmallDotRadius() {
        return smallDotSize / 2;
    }

    public void onPageChange(int page) {
        this.currentPage = page;
        if (page != previousPage && page >= 0 && page <= getNoOfPages() - 1) {
            updateDots();
            previousPage = currentPage;
        }
    }

    private void updateDots() {

        //If pages does not exceed DOT COUNT limit
        if (noOfPages <= visibleDotCounts) {
            setupNormalDots();
            return;
        }

        int lastActiveIndex = 0;
        for (int i = 0; i < dotsList.size(); i++) {
            Dot currentDot = dotsList.get(i);
            if (currentDot.getState().equals(Dot.State.ACTIVE)) {
                lastActiveIndex = i;
                currentDot.setState(Dot.State.INACTIVE);
            }
        }

        calcIndexWindow(lastActiveIndex, previousPage);

        int rangeStart = 0;
        boolean animateAddLeft = false;
        if (currentPage >= 2) {
            rangeStart = 2;
            animateAddLeft = true;
        } else if (currentPage == 1) {
            rangeStart = 1;
        }

        int rangeEnd = getVisibleDotCounts();
        boolean animateAddRight = false;
        if (getNoOfPages() - currentPage >= 3) {
            rangeEnd = getVisibleDotCounts() - 2;
            animateAddRight = true;
        } else if (getNoOfPages() - currentPage == 2) {
            rangeEnd = getVisibleDotCounts() - 1;
        }

        int offset = currentPage - previousPage;
        int newActiveIndex;
        if (offset > 0) {
            newActiveIndex = Math.min(lastActiveIndex + offset, rangeEnd - 1);
            updateEdges(newActiveIndex);
            if (animateAddRight && newActiveIndex == lastActiveIndex) {
                removeAddRight(newActiveIndex);
            } else {
                dotsList.get(newActiveIndex).setState(Dot.State.ACTIVE);
            }
        } else {
            newActiveIndex = Math.max(lastActiveIndex + offset, rangeStart);
            updateEdges(newActiveIndex);
            if (animateAddLeft && newActiveIndex == lastActiveIndex) {
                removeAddLeft(newActiveIndex);
            } else {
                dotsList.get(newActiveIndex).setState(Dot.State.ACTIVE);
            }
        }
        invalidate();

    }

    private void updateEdges(int newActiveIndex) {
        calcIndexWindow(newActiveIndex, currentPage);

        updateLeftEdge();
        updateRightEdge();
    }

    private void updateRightEdge() {
        if (getNoOfPages() - indexWindowEnd > 2) {
            dotsList.get(dotsList.size() - 1).setState(Dot.State.SMALL);
            dotsList.get(dotsList.size() - 2).setState(Dot.State.MEDIUM);
        } else if (getNoOfPages() - indexWindowEnd == 2) {
            dotsList.get(dotsList.size() - 1).setState(Dot.State.MEDIUM);
            dotsList.get(dotsList.size() - 2).setState(Dot.State.INACTIVE);
        } else {
            dotsList.get(dotsList.size() - 1).setState(Dot.State.INACTIVE);
            dotsList.get(dotsList.size() - 2).setState(Dot.State.INACTIVE);
        }
    }

    private void updateLeftEdge() {
        if (indexWindowStart >= 2) {
            dotsList.get(0).setState(Dot.State.SMALL);
            dotsList.get(1).setState(Dot.State.MEDIUM);
        } else if (indexWindowStart == 1) {
            dotsList.get(0).setState(Dot.State.MEDIUM);
            dotsList.get(1).setState(Dot.State.INACTIVE);
        } else {
            dotsList.get(0).setState(Dot.State.INACTIVE);
            dotsList.get(1).setState(Dot.State.INACTIVE);
        }
    }

    private void calcIndexWindow(int dotIndex, int pageIndex) {
        indexWindowStart = pageIndex - dotIndex;
        indexWindowEnd = getVisibleDotCounts() + indexWindowStart - 1;
    }

    private void setupNormalDots() {
        dotsList.get(currentPage).setState(Dot.State.ACTIVE);
        dotsList.get(previousPage).setState(Dot.State.INACTIVE);

        invalidate();
    }

    private void removeAddRight(final int position) {
        dotsList.remove(0);
        setStartPosX(getStartPosX() + getSmallDotStartX());

        getTranslationAnimation(getStartPosX(), getSmallDotStartX(), new AnimationListener() {
            @Override
            public void onAnimationEnd() {
                updateLeftEdge();

                Dot newDot = new Dot();
                newDot.setState(Dot.State.ACTIVE);
                dotsList.add(position, newDot);
                invalidate();
            }
        }).start();
    }

    private void removeAddLeft(final int position) {
        dotsList.remove(dotsList.size() - 1);
        setStartPosX(0);

        getTranslationAnimation(getStartPosX(), getSmallDotStartX(), new AnimationListener() {
            @Override
            public void onAnimationEnd() {
                updateRightEdge();

                Dot newDot = new Dot();
                newDot.setState(Dot.State.ACTIVE);
                dotsList.add(position, newDot);
                invalidate();
            }
        }).start();
    }

}
