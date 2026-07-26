package com.xmission.trevin.android.tangram.data;

import com.xmission.trevin.android.tangram.R;

/**
 * A square which is the size of two {@link TangramSmallTriangle}s
 * placed with their short sides together with one rotated 180&deg;
 * relative to the other.  Its sides have lengths of 3&#8730;2&#773;
 * on the short edge and 6 on the long edge.  It&rsquo;s default
 * orientation is with the long edge on top and bottom, and angled
 * 45&deg; counter-clockwise.
 */
public class TangramParallelogram extends TangramPiece {

    public static final TPoint[] VERTICES = new TPoint[] {
            new TPoint(-4.5f, 0, -1.5f, 0),
            new TPoint( 1.5f, 0, -1.5f, 0),
            new TPoint( 4.5f, 0,  1.5f, 0),
            new TPoint(-1.5f, 0,  1.5f, 0)
    };

    public boolean canFlip() {
        return true;
    }

    @Override
    protected TPoint[] getShapeVertices() {
        return VERTICES;
    }

    public float getMaxRadius() {
        return 4.74341649f;
    }

    @Override
    public int getDrawableId() {
        return R.drawable.tangram_parallelogram;
    }

    @Override
    public int getColorAttr() {
        return R.attr.tangramParallelogramColor;
    }

}
