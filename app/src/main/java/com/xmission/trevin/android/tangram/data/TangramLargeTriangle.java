/*
 * Copyright © 2026 Trevin Beattie
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.xmission.trevin.android.tangram.data;

import com.xmission.trevin.android.tangram.R;

/**
 * A right isosceles triangle which is the size of two
 * {@link TangramMediumTriangle}s placed back-to-back.
 * This has a length of 6&#8730;2&#773; on the orthogonal sides
 * and 12 on the hypotenuse.  It&rsquo;s default orientation
 * is with the hypotenuse on the bottom.
 *
 * @author Trevin Beattie
 */
public class TangramLargeTriangle extends TangramPiece {

    public static final String JSON_NAME = "large_triangle";

    public static final TPoint[] VERTICES = new TPoint[] {
            new ImmutableTPoint( 0, 0, -4, 0),
            new ImmutableTPoint( 6, 0,  2, 0),
            new ImmutableTPoint(-6, 0,  2, 0)
    };

    public String getJsonName() {
        return JSON_NAME;
    }

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
