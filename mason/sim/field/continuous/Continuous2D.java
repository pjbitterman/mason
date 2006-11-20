/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.continuous;
import sim.field.*;
import sim.util.*;
import java.util.*;


/**
   A storage facility for objects located in a continuous 2D environment.  This facility relates
   objects with 2D double tuples (in the form of Double2D).  The facility extends SparseField, and like
   other objects which extend SparseField (such as SparseGrid2D), the facility can only relate any given
   object with a single location at a time -- that is, an object cannot hold two locations at once in 
   the Continuous2D field.

   <p>Because hashtable lookups are more expensive than just storing the object, we suggest that you ALSO
   store the location of an object in the object itself, so you can read from the object rather than having
   to call getObjectLocation(object).

   <p>The Continuous2D has been arranged to make neighborhood lookup information reasonably efficient.
   It discretizes the space into grid buckets.  The discretization size of the buckets is
   provided in the constructor and cannot be changed thereafter.  If the discretization was 0.7,
   for example, then one bucket would be (0,0) to (under 0.7, under 0.7), another bucket would be (0,0,0.7)
   to (under 0.7, under 1.4), etc.
   
   <p>You can use Continuous2D to look up objects in a given region by asking for objects within the
   enclosing buckets, then rummaging through the buckets to find the individuals actually in the desired
   region.  The trick here is to come up with a good bucket size.  If the bucket size is much larger than
   the typical size of a neighborhood lookup, then a typical lookup will include large numbers of objects
   you don't care about; in the worst case, this is an O(n) lookup for something that could have been much
   smaller.  On the other hand, if the bucket size is much smaller than the typical size of a neighborhood
   lookup, then you have to do lots of bucket lookups to cover your range; many if not most of these buckets
   could be empty.  This can also be highly inefficient.

   <p>Stored objects are best thought of as one of two types: <b>point objects</b> and <b>non-point objects</b>.
   A point object is represented in space by a single point.  It has no area or volume.  A non-point object
   has area or volume.  You specify whether or not your objects are point or non-point objects when calling
   getObjectsWithinDistance().  The distinction matters when you care about this function returning either all
   the objects whose point location is within the distance range, or returning all the (non-point) the objects which
   could possibly overlap with the range.  

   <p>This distinction also is important when determining the discretization size
   of your grid.  If your objects are point objects, you have no minimum bound on the discretization size.  But if
   the object are non-point location objects (that is, they have dimensions of width, height, etc.), and
   you care about this overlap when you do distance lookups, then you have a minimum bound on your
   discretization.  In this case, you want to make certain that your discretization is at LEAST larger than
   the LARGEST dimension of any object you plan on putting in the Continuous2D.  The idea here is that if an
   any part of an object fell within the bounding box for your distance lookup task 
   (see getObjectsWithinDistance(...)), you're guaranteed that the stored location of the object must be within
   a bounding box 1 discretization larger in each direction.

   <p>Okay, so that gives you the minimum discretization you should use.  What about the maximum discretization?
   It depends largely on the number of objects expected to occupy a given discretized bucket region, and on what
   kind of lookups you need to do for objects within a given distance.  Searching through one bucket is a hash
   table lookup.  A smaller discretization returns a more accurate sample of objects within the requested
   bounding box, but requires more hash table lookups.  If you have <b>point location</b> objects, and 
   your field is very dense (LOTS of objects in a bucket on average), then we recommend a
   discretization equal to the maximum range distance you are likely to look up; but if your field is very sparse,
   then we recommend a discretization equal to twice the maximum range distance.  You have to tune it.  If you
   have <b>non-point-location</b> objects, then you have two choices.  One approach is to assume a discretization 
   equal to the maximum range distance, but when doing lookups with getObjectsWithinDistance(...), you need to
   state that you're using non-point-location objects.  If you're fairly sparse and your objects aren't big, you
   can set the discretization to twice the maximum range distance, and you should be safe calling getObjectsWithinDistance()
   pretending that your objects are point-location; this saves you a lot of hash table lookups.

   <p>At any rate, do NOT go below the minimum discretization rules. 

   <p>But wait, you say, I have objects of widely varying sizes.  Or I have many different neighborhood lookup
   range needs.  Never fear.  Just use multiple Continuous2Ds of different discretizations.  Depending on your
   needs, you can put all the objects in all of the Continuous2Ds (making different range lookups efficient)
   or various-sized classes of objects in their own Continuous2Ds perhaps.  You have to think this through based
   on your needs.  If all the objects were in all of the Continuous2Ds, you'd think that'd be inefficient in
   moving objects around.  Not really: if the discretizations doubled (or more) each time, you're looking at 
   typically an O(ln n) number of Continuous2Ds, and a corresponding number of lookups.

   <p>Continuous2D objects have a width and a height, but this is used for two functions: first, to determine
   the bounds for toroidal functions.  Second, to determine the bounds for drawing on the screen in a portrayal.
   Otherwise, width and height are not used.  If your space is bounded, you should set the width and height to
   those bounds.  If it's unbounded, then you should set the width and height to the bounds you would like
   displayed on-screen.
   
   <p><b>Warning about getObjectsAtLocation() and numObjectsAtLocation()</b>  
   Because this class uses its superclass (the SparseField) to store the <i>discretized region</i>,
   getObjectsAtLocation(...) and numObjectsAtLocation(...) will not work as you might expect.  The Sparse
   Field is storing Int2Ds (the discretized grid locations), not Double2Ds.  While you could get all the
   objects in the same discretization cell as a given Double2D location with
   getObjectsAtLocation(discretize(theDouble2D)), almost certainly you're going to retain sanity better
   by using the neighborhood functions (getObjectsWithinDistance(...)).  
   The same goes for getObjectsAtLocationOfObject() and numObjectsAtLocationOfObject().
*/

public /*strictfp*/ class Continuous2D extends SparseField
    {
    /** Where we store the Double2D values hashed by object */
    public HashMap doubleLocationHash = new HashMap();
    
    public double width;
    public double height;
    
    public final double discretization;
    
    /** Provide expected bounds on the SparseContinuous2D */
    public Continuous2D(final double discretization, double width, double height)
        {
        this.discretization = discretization;
        this.width = width;
        this.height = height;
        }

    public final Double2D getObjectLocation(Object obj)
        {
        return (Double2D) doubleLocationHash.get(obj);
        }
    
    public final Int2D discretize(final Double2D location)
        {
        return new Int2D((int)(location.x / discretization), (int)(location.y / discretization));
        }
    
    public final boolean setObjectLocation(Object obj, final Double2D location)
        {
        boolean result = super.setObjectLocation(obj, discretize(location));
        if (result) doubleLocationHash.put(obj,location);
        return result;
        }
        
    public final Bag clear()
        {
        doubleLocationHash = new HashMap();
        return super.clear();
        }
        
    public final Object remove(final Object obj)
        {
        Object result = super.remove(obj);
        doubleLocationHash.remove(obj);
        return result;
        }
    
    /** Get the width */
    public double getWidth() { return width; }
    
    /** Get the height */
    public double getHeight() { return height; }
    
    /** Toroidal x */
	// slight revision for more efficiency
	public final double tx(double x) 
        { 
		final double width = this.width;
		if (x >= 0 && x < width) return x;  // do clearest case first
		x = x % width;
		if (x < 0) x = x + width;
		return x;
		}
	
    /** Toroidal y */
	// slight revision for more efficiency
    public final double ty(double y) 
        { 
		final double height = this.height;
		if (y >= 0 && y < height) return y;  // do clearest case first
		y = y % height;
		if (y < 0) y = y + height;
		return y;
		}

    /*
    public final double tx(final double x) 
        { 
        final double width = this.width; 
        if (x >= 0) return (x % width); 
        final double width2 = (x % width) + width; 
        if (width2 < width) return width2;
        return 0;
        }
	*/
    
    /*
    public final double ty(final double y) 
        { 
        final double height = this.height; 
        if (y >= 0) return (y % height); 
        final double height2 = (y % height) + height;
        if (height2 < height) return height2;
        return 0;
        }
	*/
	

    /** Simple [and fast] toroidal x.  Use this if the values you'd pass in never stray
        beyond (-width ... width * 2) not inclusive.  It's a bit faster than the full
        toroidal computation as it uses if statements rather than two modulos.
        The following definition:<br>
        { double width = this.width; if (x >= 0) { if (x < width) return x; return x - width; } return x + width; } <br>
        ...produces the shortest code (24 bytes) and is inlined in Hotspot for 1.4.1.   However removing
        the double width = this.width; is likely to be a little faster if most objects are within the
        toroidal region. */
    public double stx(final double x) 
        { if (x >= 0) { if (x < width) return x; return x - width; } return x + width; }
    
    /** Simple [and fast] toroidal y.  Use this if the values you'd pass in never stray
        beyond (-height ... height * 2) not inclusive.  It's a bit faster than the full
        toroidal computation as it uses if statements rather than two modulos.
        The following definition:<br>
        { double height = this.height; if (y >= 0) { if (y < height) return y ; return y - height; } return y + height; } <br>
        ...produces the shortest code (24 bytes) and is inlined in Hotspot for 1.4.1.   However removing
        the double height = this.height; is likely to be a little faster if most objects are within the
        toroidal region. */
    public double sty(final double y) 
        { if (y >= 0) { if (y < height) return y ; return y - height; } return y + height; }
        
    
    // some efficiency to avoid width lookups
    double _stx(final double x, final double width) 
        { if (x >= 0) { if (x < width) return x; return x - width; } return x + width; }

    /** Minimum toroidal distance between two values in the X dimension. */
    public double tdx(final double x1, final double x2)
        {
        double width = this.width;
        if (Math.abs(x1-x2) <= width / 2)
            return x1 - x2;  // no wraparounds  -- quick and dirty check
        
        double dx = _stx(x1,width) - _stx(x2,width);
        if (dx * 2 > width) return dx - width;
        if (dx * 2 < -width) return dx + width;
        return dx;
        }
    
    // some efficiency to avoid height lookups
    double _sty(final double y, final double height) 
        { if (y >= 0) { if (y < height) return y ; return y - height; } return y + height; }

    /** Minimum toroidal distance between two values in the Y dimension. */
    public double tdy(final double y1, final double y2)
        {
        double height = this.height;
        if (Math.abs(y1-y2) <= height / 2)
            return y1 - y2;  // no wraparounds  -- quick and dirty check

        double dy = _sty(y1,height) - _sty(y2,height);
        if (dy * 2 > height) return dy - height;
        if (dy * 2 < -height) return dy + height;
        return dy;
        }
    
    /** Minimum Toroidal Distance Squared between two points. This computes the "shortest" (squared) distance between two points, considering wrap-around possibilities as well. */
    public double tds(final Double2D d1, final Double2D d2)
        {
        double dx = tdx(d1.x,d2.x);
        double dy = tdy(d1.y,d2.y);
        return (dx * dx + dy * dy);
        }

    /** Minimum Toroidal difference vector between two points.  This subtracts the second point from the first and produces the minimum-length such subtractive vector, considering wrap-around possibilities as well*/
    public Double2D tv(final Double2D d1, final Double2D d2)
        {
        return new Double2D(tdx(d1.x,d2.x),tdy(d1.y,d2.y));
        }

    /** Returns a bag containing AT LEAST those objects within the bounding box surrounding the
        specified distance of the specified position.  The bag could include other objects than this.
        In this case we include the object if
        any part of the bounding box could overlap into the desired region.  To do this, if nonPointObjects is
        true, we extend the search space by one extra discretization in all directions.  For small distances within
        a single bucket, this returns nine bucket's worth rather than 1, so if you know you only care about the
        actual x/y points stored, rather than possible object overlap into the distance sphere you specified,
        you'd want to set nonPointObjects to FALSE. [assumes non-toroidal, point objects] */
    public Bag getObjectsWithinDistance( final Double2D position, final double distance)
        { return getObjectsWithinDistance(position,distance,false,false, null); }

    /** Returns a bag containing AT LEAST those objects within the bounding box surrounding the
        specified distance of the specified position.  The bag could include other objects than this.
        If toroidal, then wrap-around possibilities are also considered.
        In this case we include the object if
        any part of the bounding box could overlap into the desired region.  To do this, if nonPointObjects is
        true, we extend the search space by one extra discretization in all directions.  For small distances within
        a single bucket, this returns nine bucket's worth rather than 1, so if you know you only care about the
        actual x/y points stored, rather than possible object overlap into the distance sphere you specified,
        you'd want to set nonPointObjects to FALSE. [assumes point objects] */
    public Bag getObjectsWithinDistance( final Double2D position, final double distance, final boolean toroidal)
        { return getObjectsWithinDistance(position,distance,toroidal,false, null); }

    /** Returns a bag containing AT LEAST those objects within the bounding box surrounding the
        specified distance of the specified position.  The bag could include other objects than this.
        If toroidal, then wrap-around possibilities are also considered.
        If nonPointObjects, then it is presumed that
        the object isn't just a point in space, but in fact fills an area in space where the x/y point location
        could be at the extreme corner of a bounding box of the object.  In this case we include the object if
        any part of the bounding box could overlap into the desired region.  To do this, if nonPointObjects is
        true, we extend the search space by one extra discretization in all directions.  For small distances within
        a single bucket, this returns nine bucket's worth rather than 1, so if you know you only care about the
        actual x/y points stored, rather than possible object overlap into the distance sphere you specified,
        you'd want to set nonPointObjects to FALSE. */
        
    public Bag getObjectsWithinDistance( final Double2D position, final double distance, final boolean toroidal,
                                         final boolean nonPointObjects)
        { return getObjectsWithinDistance(position, distance, toroidal, nonPointObjects, null); }
    
    /** Puts into the result Bag (and returns it) AT LEAST those objects within the bounding box surrounding the
        specified distance of the specified position.  If the result Bag is null, then a Bag is created.
        
        <p>The bag could include other objects than this.
        If toroidal, then wrap-around possibilities are also considered.
        If nonPointObjects, then it is presumed that
        the object isn't just a point in space, but in fact fills an area in space where the x/y point location
        could be at the extreme corner of a bounding box of the object.  In this case we include the object if
        any part of the bounding box could overlap into the desired region.  To do this, if nonPointObjects is
        true, we extend the search space by one extra discretization in all directions.  For small distances within
        a single bucket, this returns nine bucket's worth rather than 1, so if you know you only care about the
        actual x/y points stored, rather than possible object overlap into the distance sphere you specified,
        you'd want to set nonPointObjects to FALSE. */
    
    public Bag getObjectsWithinDistance( final Double2D position, final double distance, final boolean toroidal,
                                         final boolean nonPointObjects, Bag result)
        {
        double discDistance = distance / discretization;
        double discX = position.x / discretization;
        double discY = position.y / discretization;
        
        if (nonPointObjects)
            {
            // We assume that the discretization is larger than the bounding
            // box width or height for the object in question.  In this case, then
            // we can just increase the range by 1 in each direction and we are
            // guaranteed to have the location of the object in our collection.
            discDistance++;
            }
        
        // we're using StrictMath.floor instead of Math.floor because
        // Math.floor just calls StrictMath.floor, and so using the
        // StrictMath version may help in the inlining (one function
        // to inline, not two).  They should be identical in function anyway.
        
        int minX = (int) StrictMath.floor(discX - discDistance);
        int maxX = (int) StrictMath.floor(discX + discDistance);
        int minY = (int) StrictMath.floor(discY - discDistance);
        int maxY = (int) StrictMath.floor(discY + discDistance);

        final int expectedBagSize = 1;  // in the future, pick a smarter bag size?
        if (result!=null)
            {
            result.clear();
            // result.resize(expectedBagSize);  // presently 1, why bother?
            }
        else result = new Bag(expectedBagSize);
        Bag temp;
    
        MutableInt2D speedyMutableInt2D = this.speedyMutableInt2D;  // a little faster (local)

        // do the loop
        if( toroidal )
            {
            final int iWidth = (int)(StrictMath.ceil(width / discretization));
            final int iHeight = (int)(StrictMath.ceil(height / discretization));

            // we promote to longs so that maxX - minX can't totally wrap around by accident
            if ((long)maxX - (long)minX >= iWidth)  // total wrap-around.
                { minX = 0; maxX = iWidth-1; }
            if ((long)maxY - (long)minY >= iHeight) // similar
                { minY = 0; maxY = iHeight-1; }

            // okay, now tx 'em.
            final int tmaxX = toroidal(maxX,iWidth);
            final int tmaxY = toroidal(maxY,iHeight);
            final int tminX = toroidal(minX,iWidth);
            final int tminY = toroidal(minY,iHeight);
                        
            int x = tminX ;
            do
                {
                int y = tminY;
                do
                    {
                    // grab location
                    speedyMutableInt2D.x=x; speedyMutableInt2D.y=y;
                    temp = super.getObjectsAtLocation(speedyMutableInt2D);
                    if( temp != null && !temp.isEmpty())
                        {
                        // a little efficiency: add if we're 1, addAll if we're > 1, 
                        // do nothing if we're <= 0 (we're empty)
                        final int n = temp.numObjs;
                        if (n==1) result.add(temp.objs[0]);
                        else result.addAll(temp);
                        }

                    // update y
                    if( y == tmaxY )
                        break;
                    if( y == iHeight-1 )
                        y = 0;
                    else
                        y++;
                    }
                while(true);

                // update x
                if( x == tmaxX )
                    break;
                if( x == iWidth-1 )
                    x = 0;
                else
                    x++;
                }
            while(true);
            }
        else
            {
            // for non-toroidal, it is easier to do the inclusive for-loops
            for(int x = minX; x<= maxX; x++)
                for(int y = minY ; y <= maxY; y++)
                    {
                    // grab location
                    speedyMutableInt2D.x=x; speedyMutableInt2D.y=y;
                    temp = super.getObjectsAtLocation(speedyMutableInt2D);
                    if( temp != null && !temp.isEmpty())
                        {
                        // a little efficiency: add if we're 1, addAll if we're > 1, 
                        // do nothing if we're <= 0 (we're empty)
                        final int n = temp.numObjs;
                        if (n==1) result.add(temp.objs[0]);
                        else result.addAll(temp);
                        }
                    }
            }

        return result;
        }
        
    // used internally in getObjectsWithinDistance.  Note similarity to
    // AbstractGrid2D's tx method
    final int toroidal(final int x, final int width) 
        { 
        if (x >= 0) return (x % width); 
        final int width2 = (x % width) + width; 
        if (width2 < width) return width2;
        return 0;
        }

    MutableInt2D speedyMutableInt2D = new MutableInt2D();

    }


