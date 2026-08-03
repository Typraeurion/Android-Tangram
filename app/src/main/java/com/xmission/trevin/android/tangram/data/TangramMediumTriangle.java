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
 * {@link TangramSmallTriangle}s placed back-to-back.
 * This has a length of 6 on the orthogonal sides and
 * 6&#8730;2&#773; on the hypotenuse.  It&rsquo;s default
 * orientation is with the hypotenuse on the bottom.
 *
 * @author Trevin Beattie
 */
public class TangramMediumTriangle extends TangramPiece {

    public static final String JSON_NAME = "medium_triangle";

    public static final TPoint[] VERTICES = new TPoint[] {
            new TPoint(0,  0, 0, -2),
            new TPoint(0,  3, 0,  1),
            new TPoint(0, -3, 0,  1)
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
