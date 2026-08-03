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
 * The smallest piece of a Tangram: a right isosceles triangle.
 * In order to avoid non-binary division to get the centroid,
 * it&rsquo;s scaled to a length of 3&#8730;2&#773; on the
 * orthogonal sides and 6 on the hypotenuse.  It&rsquo;s
 * default orientation is with the hypotenuse on the bottom.
 *
 * @author Trevin Beattie
 */
public class TangramSmallTriangle extends TangramPiece {

    public static final String JSON_NAME = "small_triangle";

    public static final TPoint[] VERTICES = new TPoint[] {
            new TPoint( 0, 0, -2, 0),
            new TPoint( 3, 0,  1, 0),
            new TPoint(-3, 0,  1, 0)
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
