package com.xmission.trevin.android.tangram.data;

import com.xmission.trevin.android.tangram.R;

/**
 * A right isosceles triangle which is the size of two
 * {@link TangramMediumTriangle}s placed back-to-back.
 * This has a length of 6&#8730;2&#773; on the orthogonal sides
 * and 12 on the hypotenuse.  It&rsquo;s default orientation
 * is with the hypotenuse on the bottom.
 */
public class TangramLargeTriangle extends TangramPiece {

    public static final TPoint[] VERTICES = new TPoint[] {
            new TPoint( 0, 0, -4, 0),
            new TPoint( 6, 0,  2, 0),
            new TPoint(-6, 0,  2, 0)
    };

    public boolean canFlip() {
        return false;
    }

    @Override
    protected TPoint[] getShapeVertices() {
        return VERTICES;
    }

    public float getMaxRadius() {
        return 6.32455532f;
    }

    @Override
    public int getDrawableId() {
        return R.drawable.tangram_large_triangle;
    }

    @Override
    public int getColorAttr() {
        return R.attr.tangramLargeTriangleColor;
    }

}
