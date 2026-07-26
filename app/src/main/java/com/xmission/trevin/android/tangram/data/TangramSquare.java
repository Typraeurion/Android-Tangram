package com.xmission.trevin.android.tangram.data;

import com.xmission.trevin.android.tangram.R;

/**
 * A square which is the size of two {@link TangramSmallTriangle}s
 * placed face-to-face.  This has a length of 3&#8730;2&#773; on each
 * side.  It&rsquo;s default orientation is orthogonal to the axes.
 */
public class TangramSquare extends TangramPiece {

    public static final TPoint[] VERTICES = new TPoint[] {
            new TPoint(0, -1.5f, 0, -1.5f),
            new TPoint(0,  1.5f, 0, -1.5f),
            new TPoint(0,  1.5f, 0,  1.5f),
            new TPoint(0, -1.5f, 0,  1.5f)
    };

    public boolean canFlip() {
        return false;
    }

    @Override
    protected TPoint[] getShapeVertices() {
        return VERTICES;
    }

    public float getMaxRadius() {
        return 2.12132034f;
    }

    @Override
    public int getDrawableId() {
        return R.drawable.tangram_square;
    }

    @Override
    public int getColorAttr() {
        return R.attr.tangramSmallSquareColor;
    }

}
