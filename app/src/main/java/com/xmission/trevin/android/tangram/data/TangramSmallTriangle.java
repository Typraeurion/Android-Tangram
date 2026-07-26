package com.xmission.trevin.android.tangram.data;

import com.xmission.trevin.android.tangram.R;

/**
 * The smallest piece of a Tangram: a right isosceles triangle.
 * In order to avoid non-binary division to get the centroid,
 * it&rsquo;s scaled to a length of 3&#8730;2&#773; on the
 * orthogonal sides and 6 on the hypotenuse.  It&rsquo;s
 * default orientation is with the hypotenuse on the bottom.
 */
public class TangramSmallTriangle extends TangramPiece {

    public static final TPoint[] VERTICES = new TPoint[] {
            new TPoint( 0, 0, -2, 0),
            new TPoint( 3, 0,  1, 0),
            new TPoint(-3, 0,  1, 0)
    };

    public boolean canFlip() {
        return false;
    }

    @Override
    protected TPoint[] getShapeVertices() {
        return VERTICES;
    }

    public float getMaxRadius() {
        return 3.16227766f;
    }

    @Override
    public int getDrawableId() {
        return R.drawable.tangram_small_triangle;
    }

    @Override
    public int getColorAttr() {
        return R.attr.tangramSmallTriangleColor;
    }

}
