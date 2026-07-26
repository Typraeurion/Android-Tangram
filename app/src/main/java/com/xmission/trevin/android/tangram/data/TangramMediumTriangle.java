package com.xmission.trevin.android.tangram.data;

import com.xmission.trevin.android.tangram.R;

/**
 * A right isosceles triangle which is the size of two
 * {@link TangramSmallTriangle}s placed back-to-back.
 * This has a length of 6 on the orthogonal sides and
 * 6&#8730;2&#773; on the hypotenuse.  It&rsquo;s default
 * orientation is with the hypotenuse on the bottom.
 */
public class TangramMediumTriangle extends TangramPiece {

    public static final TPoint[] VERTICES = new TPoint[] {
            new TPoint(0,  0, 0, -2),
            new TPoint(0,  3, 0,  1),
            new TPoint(0, -3, 0,  1)
    };

    public boolean canFlip() {
        return false;
    }

    @Override
    protected TPoint[] getShapeVertices() {
        return VERTICES;
    }

    public float getMaxRadius() {
        return 4.35889894f;
    }

    @Override
    public int getDrawableId() {
        return R.drawable.tangram_medium_triangle;
    }

    @Override
    public int getColorAttr() {
        return R.attr.tangramMediumTriangleColor;
    }

}
