//   Rectangle.java
//   Java Spatial Index Library
//   Copyright (C) 2002-2005 Infomatiq Limited
// 
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
// 
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA

package it.unibo.alchemist.external.com.infomatiq.jsi;

import java.io.Serializable;

//CHECKSTYLE:OFF

/**
 * Currently hardcoded to 2 dimensions, but could be extended.
 */
public class Rectangle implements Serializable {

    private static final long serialVersionUID = 7712457826520134642L;
    /**
     * use primitives instead of arrays for the coordinates of the rectangle, to
     * reduce memory requirements.
     */
    public double minX, minY, maxX, maxY;

    public Rectangle() {
        minX = Double.MAX_VALUE;
        minY = Double.MAX_VALUE;
        maxX = -Double.MAX_VALUE;
        maxY = -Double.MAX_VALUE;
    }

    /**
     * Constructor.
     * 
     * @param x1
     *            coordinate of any corner of the rectangle
     * @param y1
     *            (see x1)
     * @param x2
     *            coordinate of the opposite corner
     * @param y2
     *            (see x2)
     */
    public Rectangle(final double x1, final double y1, final double x2, final double y2) {
        set(x1, y1, x2, y2);
    }

    /**
     * Sets the size of the rectangle.
     * 
     * @param x1
     *            coordinate of any corner of the rectangle
     * @param y1
     *            (see x1)
     * @param x2
     *            coordinate of the opposite corner
     * @param y2
     *            (see x2)
     */
    public final void set(final double x1, final double y1, final double x2, final double y2) {
        minX = Math.min(x1, x2);
        maxX = Math.max(x1, x2);
        minY = Math.min(y1, y2);
        maxY = Math.max(y1, y2);
    }

    /**
     * Sets the size of this rectangle to equal the passed rectangle.
     */
    public void set(final Rectangle r) {
        minX = r.minX;
        minY = r.minY;
        maxX = r.maxX;
        maxY = r.maxY;
    }

    /**
     * Make a copy of this rectangle
     * 
     * @return copy of this rectangle
     */
    public Rectangle copy() {
        return new Rectangle(minX, minY, maxX, maxY);
    }

    /**
     * Determine whether an edge of this rectangle overlies the equivalent edge
     * of the passed rectangle
     */
    public boolean edgeOverlaps(final Rectangle r) {
        return minX == r.minX || maxX == r.maxX || minY == r.minY || maxY == r.maxY;
    }

    /**
     * Determine whether this rectangle intersects the passed rectangle
     * 
     * @param r
     *            The rectangle that might intersect this rectangle
     * 
     * @return true if the rectangles intersect, false if they do not intersect
     */
    public boolean intersects(final Rectangle r) {
        return maxX >= r.minX && minX <= r.maxX && maxY >= r.minY && minY <= r.maxY;
    }

    /**
     * Determine whether or not two rectangles intersect
     * 
     * @param r1MinX
     *            minimum X coordinate of rectangle 1
     * @param r1MinY
     *            minimum Y coordinate of rectangle 1
     * @param r1MaxX
     *            maximum X coordinate of rectangle 1
     * @param r1MaxY
     *            maximum Y coordinate of rectangle 1
     * @param r2MinX
     *            minimum X coordinate of rectangle 2
     * @param r2MinY
     *            minimum Y coordinate of rectangle 2
     * @param r2MaxX
     *            maximum X coordinate of rectangle 2
     * @param r2MaxY
     *            maximum Y coordinate of rectangle 2
     * 
     * @return true if r1 intersects r2, false otherwise.
     */
    public static boolean intersects(final double r1MinX, final double r1MinY, final double r1MaxX, final double r1MaxY, final double r2MinX, final double r2MinY, final double r2MaxX, final double r2MaxY) {
        return r1MaxX >= r2MinX && r1MinX <= r2MaxX && r1MaxY >= r2MinY && r1MinY <= r2MaxY;
    }

    /**
     * Determine whether this rectangle contains the passed rectangle
     * 
     * @param r
     *            The rectangle that might be contained by this rectangle
     * 
     * @return true if this rectangle contains the passed rectangle, false if it
     *         does not
     */
    public boolean contains(final Rectangle r) {
        return maxX >= r.maxX && minX <= r.minX && maxY >= r.maxY && minY <= r.minY;
    }

    /**
     * Determine whether or not one rectangle contains another.
     * 
     * @param r1MinX
     *            minimum X coordinate of rectangle 1
     * @param r1MinY
     *            minimum Y coordinate of rectangle 1
     * @param r1MaxX
     *            maximum X coordinate of rectangle 1
     * @param r1MaxY
     *            maximum Y coordinate of rectangle 1
     * @param r2MinX
     *            minimum X coordinate of rectangle 2
     * @param r2MinY
     *            minimum Y coordinate of rectangle 2
     * @param r2MaxX
     *            maximum X coordinate of rectangle 2
     * @param r2MaxY
     *            maximum Y coordinate of rectangle 2
     * 
     * @return true if r1 contains r2, false otherwise.
     */
    public static boolean contains(final double r1MinX, final double r1MinY, final double r1MaxX, final double r1MaxY, final double r2MinX, final double r2MinY, final double r2MaxX, final double r2MaxY) {
        return r1MaxX >= r2MaxX && r1MinX <= r2MinX && r1MaxY >= r2MaxY && r1MinY <= r2MinY;
    }

    /**
     * Determine whether this rectangle is contained by the passed rectangle
     * 
     * @param r
     *            The rectangle that might contain this rectangle
     * 
     * @return true if the passed rectangle contains this rectangle, false if it
     *         does not
     */
    public boolean containedBy(final Rectangle r) {
        return r.maxX >= maxX && r.minX <= minX && r.maxY >= maxY && r.minY <= minY;
    }

    /**
     * Return the distance between this rectangle and the passed point. If the
     * rectangle contains the point, the distance is zero.
     * 
     * @param p
     *            Point to find the distance to
     * 
     * @return distance beween this rectangle and the passed point.
     */
    public double distance(final Point p) {
        double distanceSquared = 0;

        double temp = minX - p.x;
        if (temp < 0) {
            temp = p.x - maxX;
        }

        if (temp > 0) {
            distanceSquared += (temp * temp);
        }

        temp = minY - p.y;
        if (temp < 0) {
            temp = p.y - maxY;
        }

        if (temp > 0) {
            distanceSquared += (temp * temp);
        }

        return Math.sqrt(distanceSquared);
    }

    /**
     * Return the distance between a rectangle and a point. If the rectangle
     * contains the point, the distance is zero.
     * 
     * @param minX
     *            minimum X coordinate of rectangle
     * @param minY
     *            minimum Y coordinate of rectangle
     * @param maxX
     *            maximum X coordinate of rectangle
     * @param maxY
     *            maximum Y coordinate of rectangle
     * @param pX
     *            X coordinate of point
     * @param pY
     *            Y coordinate of point
     * 
     * @return distance beween this rectangle and the passed point.
     */
    public static double distance(final double minX, final double minY, final double maxX, final double maxY, final double pX, final double pY) {
        return (double) Math.sqrt(distanceSq(minX, minY, maxX, maxY, pX, pY));
    }

    public static double distanceSq(final double minX, final double minY, final double maxX, final double maxY, final double pX, final double pY) {
        double distanceSqX = 0;
        double distanceSqY = 0;

        if (minX > pX) {
            distanceSqX = minX - pX;
            distanceSqX *= distanceSqX;
        } else if (pX > maxX) {
            distanceSqX = pX - maxX;
            distanceSqX *= distanceSqX;
        }

        if (minY > pY) {
            distanceSqY = minY - pY;
            distanceSqY *= distanceSqY;
        } else if (pY > maxY) {
            distanceSqY = pY - maxY;
            distanceSqY *= distanceSqY;
        }

        return distanceSqX + distanceSqY;
    }

    /**
     * Return the distance between this rectangle and the passed rectangle. If
     * the rectangles overlap, the distance is zero.
     * 
     * @param r
     *            Rectangle to find the distance to
     * 
     * @return distance between this rectangle and the passed rectangle
     */

    public double distance(final Rectangle r) {
        double distanceSquared = 0;
        double greatestMin = Math.max(minX, r.minX);
        double leastMax = Math.min(maxX, r.maxX);
        if (greatestMin > leastMax) {
            distanceSquared += ((greatestMin - leastMax) * (greatestMin - leastMax));
        }
        greatestMin = Math.max(minY, r.minY);
        leastMax = Math.min(maxY, r.maxY);
        if (greatestMin > leastMax) {
            distanceSquared += ((greatestMin - leastMax) * (greatestMin - leastMax));
        }
        return (double) Math.sqrt(distanceSquared);
    }

    /**
     * Calculate the area by which this rectangle would be enlarged if added to
     * the passed rectangle. Neither rectangle is altered.
     * 
     * @param r
     *            Rectangle to union with this rectangle, in order to compute
     *            the difference in area of the union and the original rectangle
     * 
     * @return enlargement
     */
    public double enlargement(final Rectangle r) {
        final double enlargedArea = (Math.max(maxX, r.maxX) - Math.min(minX, r.minX)) * (Math.max(maxY, r.maxY) - Math.min(minY, r.minY));

        return enlargedArea - area();
    }

    /**
     * Calculate the area by which a rectangle would be enlarged if added to the
     * passed rectangle..
     * 
     * @param r1MinX
     *            minimum X coordinate of rectangle 1
     * @param r1MinY
     *            minimum Y coordinate of rectangle 1
     * @param r1MaxX
     *            maximum X coordinate of rectangle 1
     * @param r1MaxY
     *            maximum Y coordinate of rectangle 1
     * @param r2MinX
     *            minimum X coordinate of rectangle 2
     * @param r2MinY
     *            minimum Y coordinate of rectangle 2
     * @param r2MaxX
     *            maximum X coordinate of rectangle 2
     * @param r2MaxY
     *            maximum Y coordinate of rectangle 2
     * 
     * @return enlargement
     */
    public static double enlargement(final double r1mnX, final double r1mnY, final double r1mxX, final double r1mxY, final double r2MinX, final double r2MinY, final double r2MaxX, final double r2MaxY) {
        double r1MinX = r1mnX;
        double r1MinY = r1mnY;
        double r1MaxX = r1mxX;
        double r1MaxY = r1mxY;
        final double r1Area = (r1MaxX - r1MinX) * (r1MaxY - r1MinY);

        if (r1Area == Double.POSITIVE_INFINITY) {
            return 0; // cannot enlarge an infinite rectangle...
        }

        if (r2MinX < r1MinX) {
            r1MinX = r2MinX;
        }
        if (r2MinY < r1MinY) {
            r1MinY = r2MinY;
        }
        if (r2MaxX > r1MaxX) {
            r1MaxX = r2MaxX;
        }
        if (r2MaxY > r1MaxY) {
            r1MaxY = r2MaxY;
        }

        final double r1r2UnionArea = (r1MaxX - r1MinX) * (r1MaxY - r1MinY);

        if (r1r2UnionArea == Double.POSITIVE_INFINITY) {
            // if a finite rectangle is enlarged and becomes infinite,
            // then the enlargement must be infinite.
            return Double.POSITIVE_INFINITY;
        }
        return r1r2UnionArea - r1Area;
    }

    /**
     * Compute the area of this rectangle.
     * 
     * @return The area of this rectangle
     */
    public double area() {
        return (maxX - minX) * (maxY - minY);
    }

    /**
     * Compute the area of a rectangle.
     * 
     * @param minX
     *            the minimum X coordinate of the rectangle
     * @param minY
     *            the minimum Y coordinate of the rectangle
     * @param maxX
     *            the maximum X coordinate of the rectangle
     * @param maxY
     *            the maximum Y coordinate of the rectangle
     * 
     * @return The area of the rectangle
     */
    public static double area(final double minX, final double minY, final double maxX, final double maxY) {
        return (maxX - minX) * (maxY - minY);
    }

    /**
     * Computes the union of this rectangle and the passed rectangle, storing
     * the result in this rectangle.
     * 
     * @param r
     *            Rectangle to add to this rectangle
     */
    public void add(final Rectangle r) {
        if (r.minX < minX) {
            minX = r.minX;
        }
        if (r.maxX > maxX) {
            maxX = r.maxX;
        }
        if (r.minY < minY) {
            minY = r.minY;
        }
        if (r.maxY > maxY) {
            maxY = r.maxY;
        }
    }

    /**
     * Computes the union of this rectangle and the passed point, storing the
     * result in this rectangle.
     * 
     * @param p
     *            Point to add to this rectangle
     */
    public void add(final Point p) {
        if (p.x < minX) {
            minX = p.x;
        }
        if (p.x > maxX) {
            maxX = p.x;
        }
        if (p.y < minY) {
            minY = p.y;
        }
        if (p.y > maxY) {
            maxY = p.y;
        }
    }

    /**
     * Find the the union of this rectangle and the passed rectangle. Neither
     * rectangle is altered
     * 
     * @param r
     *            The rectangle to union with this rectangle
     */
    public Rectangle union(final Rectangle r) {
        final Rectangle union = this.copy();
        union.add(r);
        return union;
    }

    /**
     * Determine whether this rectangle is equal to a given object. Equality is
     * determined by the bounds of the rectangle.
     * 
     * @param o
     *            The object to compare with this rectangle
     */
    @Override
    public boolean equals(final Object o) {
        boolean equals = false;
        if (o instanceof Rectangle) {
            final Rectangle r = (Rectangle) o;
            if (minX == r.minX && minY == r.minY && maxX == r.maxX && maxY == r.maxY) {
                equals = true;
            }
        }
        return equals;
    }
    
    @Override
    public int hashCode(){
        final int mix = Float.floatToRawIntBits((float)minX);
        final int max = Float.floatToRawIntBits((float)maxX);
        final int miy = Float.floatToRawIntBits((float)minY);
        final int may = Float.floatToRawIntBits((float)maxY);
        return mix ^ max ^ may ^ miy;
    }

    /**
     * Determine whether this rectangle is the same as another object
     * 
     * Note that two rectangles can be equal but not the same object, if they
     * both have the same bounds.
     * 
     * @param o
     *            The object to compare with this rectangle.
     */
    public boolean sameObject(final Object o) {
        return super.equals(o);
    }

    /**
     * Return a string representation of this rectangle, in the form: (1.2,
     * 3.4), (5.6, 7.8)
     * 
     * @return String String representation of this rectangle.
     */
    public String toString() {
        return "(" + minX + ", " + minY + "), (" + maxX + ", " + maxY + ")";
    }

    /**
     * Utility methods (not used by JSI); added to enable this to be used as a
     * generic rectangle class
     */
    public double width() {
        return maxX - minX;
    }

    public double height() {
        return maxY - minY;
    }

    public double aspectRatio() {
        return width() / height();
    }

    public Point centre() {
        return new Point((minX + maxX) / 2, (minY + maxY) / 2);
    }

}
