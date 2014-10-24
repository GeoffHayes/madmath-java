/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package that contains custom Android views.
 */
package com.madmath.views;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.madmath.measures.Angle;
import com.madmath.measures.Time;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Align;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

/**
 * Abstract class for 2D plots that can be derived from this type.  This view is
 * composed of a 2D plot and surrounding area used for labels along the x and y
 * axes.
 *
 */
public abstract class Plot2DView extends View
{
    //! Enumerated type to represent the different motions.
    protected enum MOTION_TYPE
    {
        NONE,         /**< Enumeration for no motion. */
        DOWN,         /**< Enumeration for a (press) down motion. */
        UP,           /**< Enumeration for a (lifting) up motion. */
        SWIPE_LEFT,   /**< Enumeration for a swipe left motion. */
        SWIPE_RIGHT,  /**< Enumeration for a swipe right motion. */
        MOVE,         /**< Enumeration for a move motion. */
        PINCH_OPEN,   /**< Enumeration for a pinch open (away) motion. */
        PINCH_CLOSE,  /**< Enumeration for a pinch close (to) motion. */
        MULTI_MOVE    /**< Enumeration for a multi-pointer move motion. */
    };
    
    /**
     * Abstract class that encapsulates data that is to be plotted
     * on a 2D plot.
     */
    static public class Plot2DData
    {
        /**
         * Default class constructor.
         */
        public Plot2DData()
        {
            _coord       = new PointF();
            _timestamp   = new Time();
            _isHooked    = false;
            _isDisplayed = false;
        }
        
        /**
         * Copy constructor.
         */
        public Plot2DData(final Plot2DData copy)
        {
            _coord       = new PointF(copy._coord.x,
                                      copy._coord.y);
            _timestamp   = new Time(copy._timestamp);
            _isHooked    = copy._isHooked;
            _isDisplayed = copy._isDisplayed;
        }
        
        //! (x,y) coordinate of data that has been plotted.
        public PointF _coord;
        
        //! Data timestamp.
        public Time _timestamp;
        
        //! Indicates whether data has been hooked by the user or not.
        public boolean _isHooked;
        
        //! Indicates whether data has been displayed on the plot or not.
        public boolean _isDisplayed;
    };
    
    /**
     * Abstract class that encapsulates configuration data for the view.
     */
    static public class Plot2DConfigData
    {
        /**
         * Default class constructor.
         */
        public Plot2DConfigData()
        {
            _drawGrid = false;
        }
        
        /**
         * Copy constructor.
         */
        public Plot2DConfigData(final Plot2DConfigData copy)
        {
            _drawGrid = copy._drawGrid;
        }

        //! Indicates whether grid lines should be drawn or not.
        public boolean _drawGrid;
    };
    
    /**
     * Class that encapsulates previously touched coordinate data specific
     * to a pointer.
     */
    protected class PointerData
    {
        /**
         * Default class constructor.
         */
        public PointerData()
        {
            _point      = new PointF();
            _isSet      = false;
            _motionType = MOTION_TYPE.NONE;
            _numMoves   = 0;
        }
        
        //! The previously touched point on the plot.
        PointF _point;
        
        //! Indicates whether the point has been set (true) or not (false).
        boolean _isSet;
        
        //! Indicates the motion event type.
        MOTION_TYPE _motionType;
        
        //! Counts the number of times that the pointer has been moved since a DOWN event.
        int _numMoves;
    }
    
    /**
     * Class constructor.
     * 
     * @param   context   Information to global information about application
     *                    environment.
     */
    public Plot2DView(Context context)
    {
         super(context);
         
         _context = context;
    }
    
    /**
     * Class constructor.
     * 
     * @param   context   Information to global information about application
     *                    environment.
     * @param   attrs     Interface to retrieve data from XML files.
     */
    public Plot2DView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        
        _context = context;
    }

    /**
     * Returns whether the grid lines are drawn (true) or not (false).
     */
    public boolean isDrawGrid() 
    {
        return _drawGrid;
    }
    
    /**
     * Sets the background plot colour.
     * 
     * @param   newColour   The new colour for the plot.
     */
    public void setBgColour(int newColour)
    {
        _bgColour = newColour;
        _bgPaint.setColor(_bgColour);
    }
    
    /**
     * Returns true if the view is stationary and so a context menu can be
     * displayed. i.e. there is no movement along plot with the last known
     * pointer motion flagged/set to DOWN only.
     * 
     * @param   pointerId    Pointer id of the pointer data object if only 
     *                       concerned with that specific one; pass -1 to 
     *                       check all.
     */
    public boolean isStationary(final int pointerId)
    {
        boolean notMoving = false;
        
        if (pointerId >= 0)
        {
            final int index = _pointerData.indexOfKey(pointerId);
            
            if(index >= 0)
            {
                final PointerData data = _pointerData.get(index);
                
                // default notMoving to whether the daa is set or not
                notMoving = data._isSet;
                
                if (data._isSet && (data._motionType != MOTION_TYPE.DOWN &&
                        data._numMoves > MAX_NUM_MOVES_ALLOWED))
                {
                    notMoving = false;
                }
                    
            }
        }
        else
        {
            // iterate through all pointer data objects
            for (int i=0;i<_pointerData.size();++i)
            {
                final int index = _pointerData.keyAt(i);
                
                final PointerData data = _pointerData.get(index);
                
                if (!notMoving)
                {
                    notMoving = data._isSet;
                }

                if (data._isSet && (data._motionType != MOTION_TYPE.DOWN &&
                        data._numMoves > MAX_NUM_MOVES_ALLOWED))
                {
                    notMoving = false;
                    break;
                }
            }
        }

        return notMoving;
    }
    
    /**
     * Toggles the grid drawing on or off, depending upon current state.
     */
    public void toggleDrawGrid()
    {
        _drawGrid = !_drawGrid;
        invalidate();
    }
    
    /**
     * Performs a fixed clockwise rotation about the origin.
     */
    public void cwRotate()
    {
        _axisRotationAngle.add(Math.PI/36.0,Angle.ANGLE_TYPE.RADIANS);
        invalidate();
    }
    
    /**
     * Updates the configurable parameters which can be read in through an xml
     * file.
     */
    public void config(final Plot2DConfigData configData)
    {
        _drawGrid = configData._drawGrid;
    }
    
    /**
     * Passes a time range from one view to another.
     * 
     * @param   minTimestamp   The minimum timestamp of the range.
     * @param   maxTimestamp   The maximum timestamp of the range.
     */
    public void setTimestampRange(final Time minTimestamp, final Time maxTimestamp)
    {
        // intentionally left blank
    }
    
    /**
     * Clears all pointers, unsetting all and resetting the motion types
     * to NONE.
     */
    public void clearPointers()
    {
        for (int i=0;i<_pointerData.size();++i)
        {
            final int index = _pointerData.keyAt(i);
            PointerData data = _pointerData.get(index);
            data._isSet = false;
            data._motionType = MOTION_TYPE.NONE;
        }
    }
    
    /**
     * Resets the instance to its default state, clearing all lists etc.
     * 
     * @warning   Grid and zoom factors (if any exist) remain as is.
     */
    public void reset()
    {
        _plotData.clear();
        clearPointers();
        
        invalidate();
    }
    
    /**
     * Partners this view with the passed one.
     * 
     * @param   partnerView   View to partner with this one.
     */
    public void addPartnerView(Plot2DView partnerView)
    {
        // add if not self
        if (!this.equals(partnerView))
        {
            _partnerViews.add(partnerView);
        }
    }

    /**
     * Sets the indicator to draw the grid lines (true) or not (false).
     * 
     * @param   drawGrid   Indicates whether to draw grid lines (true) or
     *                     not (false).
     */
    public void setDisplayGrid(boolean drawGrid) 
    {
        _drawGrid = drawGrid;
         
        // indicates to the system that the view must be redrawn
        invalidate();
        
        // deliberately commented out because this will not affect layout
        
        // size or shape of layout may be affected, so request a new layout
        // requestLayout();
    }
    
    /**
     * Function to handle the zooming in or out of the plot.
     * 
     * @param   motion         The zoom motion, one of PINCH_OPEN or PINCH_CLOSE.
     * @param   isHorizMotion  Indicates whether the motion is horizontal (true)
     *                         or vertical (false).  
     */
    protected abstract void doZoom(final MOTION_TYPE motion, 
                                   final boolean isHorizMotion);
    
    /**
     * Draws a grid on the plot.
     * 
     * @param   canvas   The canvas to draw the grid on.
     */
    protected void drawGrid(Canvas canvas)
    {
        // draw the vertical lines given the reference position ensuring that they
        // are evenly spaced 
        final double vertLineSpacing = _xAxisGridLineSpacing;
        
        // ensure that the vertical line reference coordinate is within the plot
        if (_refVertLineXCoord > _maxXCoord)
        {
            final double diff  = _refVertLineXCoord - _maxXCoord;
            _refVertLineXCoord = _maxXCoord - (_xAxisGridLineSpacing-diff); 
        }
        else if (_refVertLineXCoord < _minXCoord)
        {
            final double diff  = _minXCoord - _refVertLineXCoord;
            _refVertLineXCoord = _minXCoord + (_xAxisGridLineSpacing-diff); 
        }
        
        // ensure that the grid lines are bounded appropriately
        while(_refVertLineXCoord<_minXCoord)
        {
            _refVertLineXCoord += _xAxisGridLineSpacing;
        }
        
        while(_refVertLineXCoord>_maxXCoord)
        {
            _refVertLineXCoord -= _xAxisGridLineSpacing;
        }

        // draw all vertical lines that start at _refVertLineXCoord and end at the
        // _maxXCoord
        double currentXCoord = _refVertLineXCoord;
        
        while (currentXCoord <= _maxXCoord)
        {
            canvas.drawLine((float)currentXCoord, (float)_minYCoord, 
                            (float)currentXCoord, (float)_maxYCoord,
                            _gridPaint);
            
            currentXCoord += vertLineSpacing;
        }
        
        // draw all vertical lines that start at _refVertLineXCoord and end at the
        // _minXCoord
        currentXCoord = _refVertLineXCoord - vertLineSpacing;
        
        while (currentXCoord >= _minXCoord)
        {
            canvas.drawLine((float)currentXCoord, (float)_minYCoord, 
                            (float)currentXCoord, (float)_maxYCoord,
                            _gridPaint);
            
            currentXCoord -= vertLineSpacing;
        }
        
        // draw the horizontal lines given the reference position ensuring that they
        // are evenly spaced
        final double horizLineSpacing = _yAxisGridLineSpacing;

        // ensure that the vertical line reference coordinate is within the plot
        if (_refHorizLineYCoord > _maxYCoord)
        {
            final double diff   = _refHorizLineYCoord - _maxYCoord;
            _refHorizLineYCoord = _maxYCoord - (horizLineSpacing-diff);       
        }
        else if (_refHorizLineYCoord < _minYCoord)
        {
            final double diff   = _minYCoord - _refHorizLineYCoord;
            _refHorizLineYCoord = _minYCoord + (horizLineSpacing-diff);
        }

        // ensure that the grid lines are bounded appropriately
        while(_refHorizLineYCoord<_minYCoord)
        {
            _refHorizLineYCoord += _yAxisGridLineSpacing;
        }
        
        while(_refHorizLineYCoord>_maxYCoord)
        {
            _refHorizLineYCoord -= _yAxisGridLineSpacing;
        }

        // draw all horizontal lines that start at _refHorizLineYCoord and end at the
        // _maxYCoord
        double currentYCoord = _refHorizLineYCoord;
        
        while (currentYCoord <= _maxYCoord)
        {
            canvas.drawLine((float)_minXCoord, (float)currentYCoord,
                            (float)_maxXCoord, (float)currentYCoord, 
                            _gridPaint);
            
            currentYCoord += horizLineSpacing;
        }
        
        // draw all horizontal lines that start at _refHorizLineYCoord and end at the
        // _minYCoord
        currentYCoord = _refHorizLineYCoord - horizLineSpacing;
        
        while (currentYCoord >= _minYCoord)
        {
            canvas.drawLine((float)_minXCoord, (float)currentYCoord,
                            (float)_maxXCoord, (float)currentYCoord, 
                            _gridPaint);
            
            currentYCoord -= horizLineSpacing;
        }
    }
    
    /**
     * Draws a square on the canvas centred on the object position.
     * 
     * @param   data     The object of whose position is to be centred within
     *                   a square.
     * @param   length   The length from the centre of the square to any corner.
     * @param   paint    The paint object used to draw the square.
     * @param   canvas   The canvas to draw the square on.
     * 
     * @warning Assumes that the x and y axes are scaled identically.
     * 
     */
    protected void drawPositionSquare(final Plot2DData data,
                                      final double length, 
                                      final Paint paint,
                                      Canvas canvas)
    {
        // angle from the centre of the square to one of the corners
        final double angle = Math.PI/4.0;
        
        double x = data._coord.x;
        double y = data._coord.y;
        
        // only draw if the coordinate is in the plot
        if (x >= _minXCoord && x <= _maxXCoord && 
            y >= _minYCoord && y <= _maxYCoord)
        {
            final double xCoordDelta = length*Math.sin(angle);
            final double yCoordDelta = length*Math.cos(angle);
            
            final double left   = Math.max(x-xCoordDelta, _minXCoord);
            final double top    = Math.max(y-yCoordDelta, _minYCoord);
            final double right  = Math.min(x+xCoordDelta, _maxXCoord);
            final double bottom = Math.min(y+yCoordDelta, _maxYCoord);
            
            canvas.drawRect(
                    (float)left, (float)top, (float)right, (float)bottom,paint);
            
            data._isDisplayed = true;
        }
    }
    
    /**
     * Draws an X on the canvas centred on the object position.
     * 
     * @param   data     The object of whose position is to be centred within
     *                   a square.
     * @param   length   The length from the centre of the square to any corner.
     * @param   paint    The paint object used to draw the square.
     * @param   canvas   The canvas to draw the square on.
     * 
     * @warning Assumes that the x and y axes are scaled identically.
     * 
     */
    protected void drawPositionX(final Plot2DData data,
                                 final double length, 
                                 final Paint paint,
                                 Canvas canvas)
    {
        // angle from the centre of the X to any of its ends
        final double angle = Math.PI/4.0;
        
        double x = data._coord.x;
        double y = data._coord.y;
        
        // only draw if the coordinate is in the plot
        if (x >= _minXCoord && x <= _maxXCoord && 
            y >= _minYCoord && y <= _maxYCoord)
        {
            final double xCoordDelta = length*Math.sin(angle);
            final double yCoordDelta = length*Math.cos(angle);
            
            // draw the north-west to south-east line
            canvas.drawLine(
                    (float)(x-xCoordDelta),
                    (float)(y-yCoordDelta),
                    (float)(x+xCoordDelta),
                    (float)(y+yCoordDelta),
                    paint);
            
            // draw the south-west to north-east line
            canvas.drawLine(
                    (float)(x-xCoordDelta),
                    (float)(y+yCoordDelta),
                    (float)(x+xCoordDelta),
                    (float)(y-yCoordDelta),
                    paint);
            
            data._isDisplayed = true;
        }
    }
    
    /**
     * Draws an angled line from a position on a plot for some fixed length.
     * 
     * @param   data     The object of whose position is to be the start of the
     *                   line.
     * @param   length   The length from the centre of the square to any corner.
     * @param   angle    The angle of the line (relative to north).
     * @param   paint    The paint object used to draw the line.
     * @param   canvas   The canvas to draw the line on.
     */
    protected void drawAngledLine(final Plot2DData data, final Angle angle, 
                                  final double length, final Paint paint,
                                  Canvas canvas)
    {
        final double angleRads = angle.get(Angle.ANGLE_TYPE.RADIANS);
        
        double x = data._coord.x;
        double y = data._coord.y;
        
        // only draw if the coordinate is in the plot
        if (x >= _minXCoord && x <= _maxXCoord && 
            y >= _minYCoord && y <= _maxYCoord)
        {
            // calculate the end x and y end positions (subtract on y because
            // of the north-south values are from 0 to _maxYCoord)
            final double xEndCoord = x + length*Math.sin(angleRads);
            final double yEndCoord = y - length*Math.cos(angleRads);
            
            canvas.drawLine((float)x, (float)y, (float)xEndCoord, (float)yEndCoord, paint);
        }
    }
    
    /**
     * Draws a line between two positions, ensuring that the line does not extend
     * outside of the plot.  Intended to handle the all combinations of both points
     * in the plot, both outside of the plot, or one of the two outside of the 
     * plot.
     * 
     * @param   fromPos  The from position end of the line.
     * @param   toPos    The to position end of the line.
     * @param   paint    The paint object used to draw the line.
     * @param   canvas   The canvas to draw the line on.
     * 
     * @warning Assumes that the x and y axes are scaled identically.
     */
    protected void drawLine(
            final Plot2DData fromPos, final Plot2DData toPos, final Paint paint,
            Canvas canvas)
    {
        drawLine(
                fromPos._coord.x, fromPos._coord.y, 
                toPos._coord.x, toPos._coord.y, paint, canvas);
    }
    
    /**
     * Draws a line between two positions, ensuring that the line does not extend
     * outside of the plot.  Intended to handle the all combinations of both points
     * in the plot, both outside of the plot, or one of the two outside of the 
     * plot.
     * 
     * @param   fromX    The x-coordinate of the from position.
     * @param   fromY    The y-coordinate of the from position.
     * @param   toX      The x-coordinate of the to position.
     * @param   toY      The y-coordinate of the to position.
     * @param   paint    The paint object used to draw the line.
     * @param   canvas   The canvas to draw the line on.
     * 
     * @warning Assumes that the x and y axes are scaled identically.
     */
    protected void drawLine(
            final float fromX, final float fromY, 
            final float toX,   final float toY, 
            final Paint paint, Canvas canvas)
    {
        // check to see if the points are displayed i.e. their coordinates
        // fit within the plot
        final boolean isFromDisp = 
                ((fromX>=_minXCoord && fromX<=_maxXCoord) &&
                 (fromY>=_minYCoord && fromY<=_maxYCoord));
        
        final boolean isToDisp = 
                ((toX>=_minXCoord && toX<=_maxXCoord) &&
                 (toY>=_minYCoord && toY<=_maxYCoord));
                
        if (isFromDisp && isToDisp)
        {
            // since both points have been displayed within the plot, then
            // the line drawn between the two points will be displayed on
            // the plot
            canvas.drawLine(fromX, fromY, toX, toY, paint);
        }
        else
        {
            // check for a vertical line
            if (toX == fromX)
            {
                // if the x-coordinate is within the range for this axis then
                // draw the line
                if (toX > _minXCoord && toX < _maxXCoord)
                {
                    boolean doLine = true;
                    
                    double xA      = toX;
                    double xB      = xA;
                    double yA      = 0.0;
                    double yB      = 0.0;

                    if(!isFromDisp && !isToDisp)
                    {
                        // ensure that both points are on opposite sides of the plot
                        if ((toY<_minYCoord && fromY>_maxYCoord) || 
                            (toY>_maxYCoord && fromY<_minYCoord))
                        {
                            yA = _minYCoord;
                            yB = _maxYCoord;
                        }
                        else
                        {
                            doLine = false;
                        }
                    }
                    else if (!isFromDisp)
                    {
                        // the toPos is displayed on the plot
                        yA = toY;
                        
                        if (fromY < _minYCoord)
                        {
                            yB = _minYCoord;
                        }
                        else
                        {
                            yB = _maxYCoord;
                        }
                    }
                    else
                    {
                        // the fromPos is displayed on the plot
                        yA = fromY;
                        
                        if (toY < _minYCoord)
                        {
                            yB = _minYCoord;
                        }
                        else
                        {
                            yB = _maxYCoord;
                        }
                    }
                    
                    if(doLine)
                    {
                        canvas.drawLine(
                            (float)xA, (float)yA, 
                            (float)xB, (float)yB, paint);
                    }
                }
            }
            // check for a horizontal line
            else if (toY == fromY)
            {               
                // if the y-coordinate is within the range for this axis then
                // draw the line
                if (toY > _minYCoord && toY < _maxYCoord)
                {
                    boolean doLine = true;
                    
                    double xA      = 0.0;
                    double xB      = 0.0;
                    double yA      = toY ;
                    double yB      = yA;
                    
                    if(!isFromDisp && !isToDisp)
                    {
                        // ensure that both points are on opposite sides of the plot
                        if ((toX<_minXCoord && fromX>_maxXCoord) || 
                            (toX>_maxXCoord && fromX<_minXCoord))
                        {
                            xA = _minXCoord;
                            xB = _maxXCoord;
                        }
                        else
                        {
                            doLine = false;
                        }
                    }
                    else if (!isFromDisp)
                    {
                        // the toPos is displayed on the plot
                        xA = toX;
                        
                        if (fromX < _minXCoord)
                        {
                            xB = _minXCoord;
                        }
                        else
                        {
                            xB = _maxXCoord;
                        }
                    }
                    else
                    {
                        // the fromPos is displayed on the plot
                        xA = fromX;
                        
                        if (toX < _minXCoord)
                        {
                            xB = _minXCoord;
                        }
                        else
                        {
                            xB = _maxXCoord;
                        }
                    }
                    
                    if(doLine)
                    {
                        canvas.drawLine(
                            (float)xA, (float)yA, 
                            (float)xB, (float)yB, paint);
                    }
                }
            }
            else
            {
                // since one or both points haven't been drawn on the plot, we 
                // need to  see if the line drawn between these two points 
                // intersect the plot in any way
                final double slope = 
                         (toY - fromY)/
                         (toX - fromX);
                
                // does the line given by the above slope intersect exactly two of
                // the sides of the grid given by the equations:
                //
                //   i) x=_minXCoord
                //  ii) x=_maxXCoord
                // iii) y=_minYCoord
                //  iv) y=_maxYCoord
                
                final double minXOnLine = Math.min(fromX, toX);
                final double maxXOnLine = Math.max(fromX, toX);
                
                // check for x=_minXCoord and x=_maxXCoord
                final double y1 = slope*_minXCoord - slope*toX + toY;
                final double y2 = slope*_maxXCoord - slope*toX + toY;
                
                // check for y=_minYCoord and y=_maxYCoord
                final double x1 = (_minYCoord - toY + slope*toX)/slope;
                final double x2 = (_maxYCoord - toY + slope*toX)/slope;
                
                double yA = 0.0;
                double yB = 0.0;
                double xA = 0.0;
                double xB = 0.0;
                
                boolean pointAExists = false;
                boolean pointBExists = false;
                
                if (isToDisp)
                {
                    pointAExists = true;
                    yA = toY;
                    xA = toX;
                }
                else if(isFromDisp)
                {
                    pointAExists = true;
                    yA = fromY;
                    xA = fromX;
                }
                
                if (y1 > _minYCoord && y1 < _maxYCoord)
                {
                    if (!pointAExists)
                    {
                        yA = y1;
                        xA = _minXCoord;
                        pointAExists = (xA >= minXOnLine && xA <= maxXOnLine);
                    }
                    else if(!pointBExists)
                    {
                        yB = y1;
                        xB = _minXCoord;
                        pointBExists = (xB >= minXOnLine && xB <= maxXOnLine);
                    }
                }
                
                if (y2 > _minYCoord && y2 < _maxYCoord)
                {
                    if (!pointAExists)
                    {
                        yA = y2;
                        xA = _maxXCoord;
                        pointAExists = (xA >= minXOnLine && xA <= maxXOnLine);
                    }
                    else if(!pointBExists)
                    {
                        yB = y2;
                        xB = _maxXCoord; 
                        pointBExists = (xB >= minXOnLine && xB <= maxXOnLine);
                    }
                }
                
                if (x1 > _minXCoord && x1 < _maxXCoord)
                {
                    if (!pointAExists)
                    {
                        yA = _minYCoord;
                        xA = x1;
                        pointAExists = (xA >= minXOnLine && xA <= maxXOnLine);
                    }
                    else if (!pointBExists)
                    {
                        yB = _minYCoord;
                        xB = x1;
                        pointBExists = (xB >= minXOnLine && xB <= maxXOnLine);
                    }
                }
                
                if (x2 > _minXCoord && x2 < _maxXCoord)
                {
                    if (!pointAExists)
                    {
                        yA = _maxYCoord;
                        xA = x2;
                        pointAExists = (xA >= minXOnLine && xA <= maxXOnLine);
                    }
                    else if (!pointBExists)
                    {
                        yB = _maxYCoord;
                        xB = x2;
                        pointBExists = (xB >= minXOnLine && xB <= maxXOnLine);
                    }
                }
                
                if (pointAExists && pointBExists)
                {
                    canvas.drawLine(
                            (float)xA, (float)yA, 
                            (float)xB, (float)yB, paint);
                }
                else if(isFromDisp || isToDisp)
                {
                    android.util.Log.e(
                            this.getClass().getSimpleName(),
                            "Failed to draw line fromX=" + fromX +
                            " fromY=" + fromY + " toX=" + toX + " toY=" + toY);
                }
            }
        }
    }

    /**
     * Rotates the given coordinate, applying the calculated angle of rotation
     * such that the new coordinate has been rotated clockwise about the origin.
     * 
     * @param   data   The data containing the coordinate.
     */
    protected void cwRotate(Plot2DData data)
    {
        final double x = data._coord.x - _originXCoord;
        final double y = data._coord.y - _originYCoord;
        final double rotAngleRads = 
                _axisRotationAngle.get(Angle.ANGLE_TYPE.RADIANS);
        
        data._coord.x = (float)(Math.cos(rotAngleRads)*x - 
                Math.sin(rotAngleRads)*y) + (float) _originXCoord;
        
        data._coord.y = (float)(Math.sin(rotAngleRads)*x + 
                Math.cos(rotAngleRads)*y) + (float) _originYCoord;
    }
    
    /**
     * Returns whether the user touch event position is within the plot
     * (i.e. bounded by the min and max x and y coordinates) or not.
     * 
     * @param   xCoord   The x-coordinate of the touched position.
     * @param   yCoord   The y-coordinate of the touched position.
     * 
     * @retval  true if the touched position is within the plot.
     * @retval  false if the touched position is outside the plot.
     */
    protected boolean isWithinPlot(final double xCoord, final double yCoord)
    {
        boolean isInPlot = false;
        
        if (xCoord >= _minXCoord && xCoord <= _maxXCoord && 
            yCoord >= _minYCoord && yCoord <= _maxYCoord)
        {
            isInPlot = true;
        }
        
        return isInPlot;
    }
    
    /**
     * Handles a single pointer event on a move action.
     * 
     * @param   event   The motion event.
     */
    protected void handleSinglePointerEventMove(final MotionEvent event)
    {
        final int actionIndex = event.getActionIndex();
        final int pointerId = event.getPointerId(actionIndex);
        final float xCoord  = event.getX(actionIndex);
        final float yCoord  = event.getY(actionIndex);
        
        final boolean eventIsInPlot = isWithinPlot(xCoord,yCoord);

        // ignore data for events outside of the plot (for now)
        if (eventIsInPlot)
        {
            PointerData data = _pointerData.get(pointerId);
            
            if (data._isSet)
            {
                // calculate the difference between the previous coordinate
                // and the current (note that we negate the y difference
                // because the north to south running y-axis starts at 0 and
                // ends at some positive number)
                final double deltaX = data._point.x - xCoord;
                final double deltaY = -(data._point.y - yCoord);
                
                // if both deltas are less than some small number, then assume
                // that no move has taken place and so don't cause a change to
                // motion type
                if (Math.abs(deltaX) >= 1.5 || Math.abs(deltaY) >= 1.5)
                {
                    if (deltaX < 0.0f && data._motionType==MOTION_TYPE.DOWN)
                    {
                        data._motionType=MOTION_TYPE.SWIPE_LEFT;
                    }
                    else if (deltaX > 0.0f && data._motionType==MOTION_TYPE.DOWN)
                    {
                        data._motionType=MOTION_TYPE.SWIPE_RIGHT;
                    }
                    
                    handleSwipe(deltaX,deltaY);
                    
                    data._numMoves++;
                }
            }
            
            data._isSet = true;
            data._point.x = xCoord;
            data._point.y = yCoord;
        }
    }
    
    /**
     * Handles a single pointer event on an up action.
     * 
     * @param   event   The motion event.
     */
    protected void handleSinglePointerEventUp(final MotionEvent event)
    {
        final int actionIndex = event.getActionIndex();
        final int pointerId = event.getPointerId(actionIndex);
        final float xCoord  = event.getX(actionIndex);
        final float yCoord  = event.getY(actionIndex);
        
        final boolean eventIsInPlot = isWithinPlot(xCoord,yCoord);
        
        PointerData data = _pointerData.get(pointerId);

        if (eventIsInPlot)
        {
            // handle the case for a down followed by an up event or if there
            // have been no more than MAX_NUM_MOVES_ALLOWED
            // this corresponds to the user clicking on the plot and
            // a potential hook of the data
            if (isStationary(pointerId) && data._motionType != MOTION_TYPE.MULTI_MOVE)
            {
                doHookOperation(xCoord,yCoord);
            }
        }
        else
        {
            // has the user clicked within the vertical axis?
            if (xCoord < (getPaddingLeft()+ _yAxisBounds.width()))
            {
                handleVerticalAxisTouch(xCoord,yCoord);
            }
        }
        
        data._motionType = MOTION_TYPE.NONE;
        data._isSet      = false;
        data._numMoves   = 0;
    }
    
    /**
     * Handles a multi-pointer event on an up action; clears the action
     * event only.
     * 
     * @param   event   The motion event.
     */
    protected void handleMultiPointerEventUp(final MotionEvent event)
    {
        // only handle the the two pointer event
        if (event.getPointerCount() == MAX_NUM_POINTERS_ALLOWED)
        {
            final int actionIndex = event.getActionIndex();
            final int pointerId   = event.getPointerId(actionIndex);
            
            if(_pointerData.indexOfKey(pointerId)>=0)
            {
                PointerData data = _pointerData.get(pointerId);
                
                data._isSet      = false;
                data._motionType = MOTION_TYPE.NONE;
                data._numMoves   = 0;
            }
            else
            {
                android.util.Log.e(
                        this.getClass().getSimpleName(),
                        "handleMultiPointerEventUp - non-existant pointerId=" +
                        pointerId);
            }
        }
    }
    
    /**
     * Invokes the hooking operation which can be overidden by derived classes.
     * 
     * @param   xCoord       The pressed x-coordinate within the plot.
     * @param   yCoord       The pressed y-coordinate within the plot.
     */
    protected void doHookOperation(final float xCoord, final float yCoord)
    {
        boolean wasPrevObjHooked = (_hookedObj != null);
        
        // unhook any previously hooked object
        if(wasPrevObjHooked)
        {
            _hookedObj._isHooked = false;
            _hookedObj           = null;
        }
        
        final boolean isObjHooked = hookData(_plotData,xCoord,yCoord);
        
        // redraw if no object hooked and we have cleared a hooked object
        if (!isObjHooked && wasPrevObjHooked)
        {
            invalidate();
        }
        else if (isObjHooked)
        {
            // displays the hooked data message
            dispHookedDataMsg();
            
            // redraw the plot
            invalidate();
        }
    }
    
    /**
     * Hooks the data object that is closest to the pressed position given the
     * "nearness" criteria.  Clears any previously hooked object.
     * 
     * @param   plotData     The container of objects to iterate over.
     * @param   xCoord       The pressed x-coordinate within the plot.
     * @param   yCoord       The pressed y-coordinate within the plot.
     * 
     * @retval  true if an object has been hooked.
     * @retval  false if no object was hooked.
     *          
     * @note    Any previously hooked object is cleared.
     */
    protected boolean hookData(
             final List<Plot2DData> plotData,
             final float xCoord, 
             final float yCoord)
    {
        // default the min distance
        double minDistance = Math.sqrt(
                Math.pow(_maxXCoord-_minXCoord,2.0f) +
                Math.pow(_maxYCoord-_minYCoord,2.0f));
        
        // if an object has already been hooked, then use that hooked object
        // distance as the minimum distance
        if (_hookedObj != null)
        {
            minDistance = _hookedObjDist;
        }
        
        
        Plot2DData nearestObj = null;

        for(int i=0;i<plotData.size();++i)
        {
            final Plot2DData data = plotData.get(i);
            
            data._isHooked = false;
            
            // skip this data point if not displayed on the plot
            if (!data._isDisplayed)
            {
                continue;
            }
            
            // calculate the distance to the pressed position
            final double distance = Math.sqrt(
                    Math.pow(xCoord-data._coord.x,2.0f) + 
                    Math.pow(yCoord-data._coord.y,2.0f));

            // use <= on the minDistance condition to handle the case
            // where we want to find the most recent that is as close
            // to the hooked position as the older data
            if (distance <= _hookingGateSize && distance <= minDistance)
            {
                minDistance = distance;
                nearestObj = data;
                
                // clear any already hooked object
                if (_hookedObj != null)
                {
                    _hookedObj._isHooked = false;
                }
                
            }
        }
        
        // has data object been found close to the pressed position
        if (nearestObj != null)
        {
            // save the hooked object
            _hookedObj     = nearestObj;
            _hookedObjDist = minDistance;
        }
        
        return (_hookedObj != null);
    }
    
    /**
     * Displays the hooked data message toast view for the hooked object.
     */
    protected abstract void dispHookedDataMsg();
    
    /**
     * Function to be overriden by derived classes in order to handle
     * swiping behaviour specific to the plot.
     * 
     * @param   deltaX   The delta/change/displacement in x from the previous
     *                   motion event position to the current one.
     * @param   deltaY   The delta/change/displacement in y from the previous
     *                   motion event position to the current one.
     */
    protected abstract void handleSwipe(final double deltaX, final double deltaY);
    
    /**
     * Handles a multi-pointer event on a move action which causes a zoom of 
     * the plot.
     * 
     * @param   event   The motion event.
     */
    protected void handleMultiPointerEventMove(final MotionEvent event)
    {
        // only handle the the two pointer event
        if (event.getPointerCount() == MAX_NUM_POINTERS_ALLOWED)
        {
            final int pointerIdA = event.getPointerId(0);
            final int pointerIdB = event.getPointerId(1);
            
            if(_pointerData.indexOfKey(pointerIdA)>=0 && 
                    _pointerData.indexOfKey(pointerIdB)>=0)
            {
                final float xCoordA = event.getX(0);
                final float yCoordA = event.getY(0);
                
                final boolean eventIsInPlotA = isWithinPlot(xCoordA,yCoordA);
                
                final float xCoordB = event.getX(1);
                final float yCoordB = event.getY(1);
                
                final boolean eventIsInPlotB = isWithinPlot(xCoordB,yCoordB);
                
                PointerData dataA = _pointerData.get(pointerIdA);
                PointerData dataB = _pointerData.get(pointerIdB);
                
                if (dataA._isSet && dataB._isSet && eventIsInPlotA && eventIsInPlotB)
                {
                    //dumpEvent(event);
                    
                    // check for fixed positions
                    final boolean isAFixed = (Math.sqrt(
                            Math.pow(dataA._point.x - xCoordA,2.0) + 
                            Math.pow(dataA._point.y - yCoordA,2.0)) == 0.0);
                    
                    final boolean isBFixed = (Math.sqrt(
                            Math.pow(dataB._point.x - xCoordB,2.0) + 
                            Math.pow(dataB._point.y - yCoordB,2.0)) == 0.0);
    
                    if (isAFixed || isBFixed)
                    {
                        if (!isAFixed && isBFixed)
                        {
                            final double prevBrgFromOrg = 
                                    Math.atan2(dataA._point.x-_originXCoord,dataA._point.y-_originYCoord);
                            
                            final double currBrgFromOrg = 
                                    Math.atan2(xCoordA-_originXCoord,yCoordA-_originYCoord);  
                            
                            //_axisAngleRotationRads += -(currBrgFromOrg - prevBrgFromOrg)/5.0;
                            
                            //invalidate();
                        }
                        else if (!isBFixed && isAFixed)
                        {
                            final double prevBrgFromOrg = 
                                    Math.atan2(dataB._point.x-_originXCoord,dataB._point.y-_originYCoord);
                            
                            final double currBrgFromOrg = 
                                    Math.atan2(xCoordB-_originXCoord,yCoordB-_originYCoord); ;  
                            
                            //_axisAngleRotationRads += -(currBrgFromOrg - prevBrgFromOrg)/5.0;
    
                            //invalidate();
                        }
                    }
                    else
                    {
                        // historical data exists for both pointers so we can calculate
                        // a historic distance
                        final double histDistance = Math.sqrt(
                                Math.pow(dataA._point.x - dataB._point.x,2.0) + 
                                Math.pow(dataA._point.y - dataB._point.y,2.0));
    
                        final double currDistance = Math.sqrt(
                                Math.pow(xCoordA - xCoordB,2.0) + 
                                Math.pow(yCoordA - yCoordB,2.0));
                        
                        // estimate direction given the current and past pointer positions
                        double angleA = Math.toDegrees(Math.atan2(
                                dataA._point.x-xCoordA, dataA._point.y-yCoordA));
                        
                        double angleB = Math.toDegrees(Math.atan2(
                                dataB._point.x-xCoordB, dataB._point.y-yCoordB));
                        
                        if (angleA < 0.0)
                        {
                            angleA += 360.0;
                        }
                        
                        if (angleB < 0.0)
                        {
                            angleB += 360.0;
                        }
                        
                        final boolean isHorizMove = isHorizontalMotion(angleA,angleB);
                        
                        if (currDistance > histDistance)
                        {
                            // the pointers are moving away from each other which is
                            // equivalent to a zoom in
                            doZoom(MOTION_TYPE.PINCH_OPEN, isHorizMove);
                        }
                        else if(currDistance < histDistance)
                        {
                            // the pointers are moving towards one another which is
                            // equivalent to a zoom out
                            doZoom(MOTION_TYPE.PINCH_CLOSE, isHorizMove);
                        }
                    }
                }
                
                // save updates to the data if within the plot
                if (eventIsInPlotA)
                {
                    dataA._isSet      = true;
                    dataA._motionType = MOTION_TYPE.MULTI_MOVE;
                    dataA._point.x    = xCoordA;
                    dataA._point.y    = yCoordA;
                    dataA._numMoves++;
                }
                
                if (eventIsInPlotB)
                {
                    dataB._isSet      = true;
                    dataB._motionType = MOTION_TYPE.MULTI_MOVE;
                    dataB._point.x    = xCoordB;
                    dataB._point.y    = yCoordB;
                    dataB._numMoves++;
                }
            }
            else
            {
                android.util.Log.e(
                        this.getClass().getSimpleName(),
                        "handleMultiPointerEventUp - non-existant pointerIdA=" +
                        pointerIdA + " or pointerIdB=" + pointerIdB);
            }     
        }
    }
    
    /**
     * Function to determine if two angles indicate a horizontal swipe along the
     * view (within some tolerance) or a vertical.
     * 
     * @param   angleADegs   First angle in degrees, positive from north.
     * @param   angleBDegs   Second angle in degrees, positive from north.
     * 
     * @retval  true if the two angles indicate a horizontal swiping motion.
     * @retval  false if the two angles indicate a vertical swiping motion.
     */
    private boolean isHorizontalMotion(final double angleADegs, final double angleBDegs)
    {
        boolean isHoriz = false;

        if ((Math.abs(Math.abs(angleADegs)-90.0) <= 45.0) ||
                (Math.abs(Math.abs(angleADegs)-270.0) <= 45.0) &&
            (Math.abs(Math.abs(angleBDegs)-90.0) <= 45.0) ||
                (Math.abs(Math.abs(angleBDegs)-270.0) <= 45.0))
        {
            // so both angles are within 45 degrees of either 90 degrees or 270
            // this roughly corresponds to a horizontal swipe
            isHoriz = true;
        }
        
        return isHoriz;
    }
    
    /** 
     * Shows an event in the LogCat view, for debugging purposes. 
     *
     * @param   event   The motion event to display.
     */
    private void dumpEvent(final MotionEvent event) 
    {
       String names[] = { "DOWN" , "UP" , "MOVE" , "CANCEL" , "OUTSIDE" ,
          "POINTER_DOWN" , "POINTER_UP" , "7?" , "8?" , "9?" };
       StringBuilder sb = new StringBuilder();
       int action = event.getAction();
       int actionCode = action & MotionEvent.ACTION_MASK;
       sb.append("event ACTION_" ).append(names[actionCode]);
       if (actionCode == MotionEvent.ACTION_POINTER_DOWN
             || actionCode == MotionEvent.ACTION_POINTER_UP) {
          sb.append("(pid " ).append(
          action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
          sb.append(")" );
       }
       sb.append("[" );
       for (int i = 0; i < event.getPointerCount(); i++) {
          sb.append("#" ).append(i);
          sb.append("(pid " ).append(event.getPointerId(i));
          sb.append(")=" ).append((int) event.getX(i));
          sb.append("," ).append((int) event.getY(i));
          if (i + 1 < event.getPointerCount())
             sb.append(";" );
       }
       sb.append("]" );
       android.util.Log.d("EVENTDUMP", sb.toString());
    }
    
    @Override
    /**
     * Response to the user touching the view display.
     * 
     * @param   event   The motion event which describes the user's action
     *                  against the view.
     *                  
     * @retval  true if the event has been handled.
     * @retval  false if the event has not been handled.
     */
    public boolean onTouchEvent(MotionEvent event)
    {
        super.onTouchEvent(event);

        try
        {
            final int numPointers = event.getPointerCount();
    
            if (numPointers <= MAX_NUM_POINTERS_ALLOWED)
            {
                switch (event.getActionMasked()) 
                {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                    {  
                        final int actionIndex = event.getActionIndex();
                        final int pointerId   = event.getPointerId(actionIndex);
                        
                        if(_pointerData.indexOfKey(pointerId) < 0)
                        {
                            _pointerData.append(pointerId, new PointerData());
                        }

                        PointerData data = _pointerData.get(pointerId);
                        data._isSet      = true;
                        data._motionType = MOTION_TYPE.DOWN;
                        data._point.x    = event.getX(actionIndex);
                        data._point.y    = event.getY(actionIndex);
                        data._numMoves   = 0;
    
                        break;
                    }
                    case MotionEvent.ACTION_MOVE:
                    {
                        if (numPointers==1)
                        { 
                            handleSinglePointerEventMove(event);
                        }
                        else
                        {
                            handleMultiPointerEventMove(event);
                        }
    
                        break;
                    }
                    case MotionEvent.ACTION_POINTER_UP:
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                    {
                        if (numPointers==1)
                        { 
                            handleSinglePointerEventUp(event);
                        }
                        else
                        {
                            handleMultiPointerEventUp(event);
                        }
    
                        break;
                    }
                    default:
                    {
                      // intentionally left blank
                    }
                }
            }
        }
        catch(final Exception e)
        {
            android.util.Log.e(
                    this.getClass().getSimpleName(),
                    "onTouchEvent exception:" + e.toString());
        }

        return true;
    }
    
    /**
     * Function to be overriden by derived classes in order to handle
     * a click within the vertical axis area outside of the plot.
     * 
     * @param   xCoord   The x-coordinate of the touch within the vertical axis.
     * @param   yCoord   The y-coordinate of the touch within the vertical axis.
     */
    protected abstract void handleVerticalAxisTouch(final double xCoord, 
                                                    final double yCoord);
    
    /**
     * Initialize data members with default values.
     * 
     * @param   context   The application context.
     */
    protected void init(Context context)
    {
        // initialize grid related members
        _drawGrid           = false;
        _refLineCoordsSet   = false;
        _refVertLineXCoord  = 0.0;
        _refHorizLineYCoord = 0.0;
        
        // default the colors
        _hookedColour    = Color.WHITE;
        _dataColour      = Color.GREEN;
        _gridColour      = Color.LTGRAY;
        _bgColour        = Color.BLACK;
        _xAxisTextColour = Color.WHITE;
        _yAxisTextColour = Color.WHITE;
        
        _gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        _gridPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        _gridPaint.setColor(_gridColour);
        _gridPaint.setStrokeWidth(1.0f);
        
        // initialize the pointer data array for multi-touch pointers
        _pointerData = new SparseArray<PointerData>(MAX_NUM_POINTERS_ALLOWED);

        // initialize the min and max bounds for the x and y axes
        _minXCoord  = 0.0;
        _maxXCoord  = 0.0;
        _minYCoord  = 0.0;
        _maxYCoord  = 0.0;
        
        // initialize the paint objects for the background and the data
        _bgBounds     = new RectF();
        _bgPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
        _bgPaint.setStyle(Paint.Style.FILL);
        _bgPaint.setColor(_bgColour);
        
        // default the colour to white
        _dataPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        _dataPaint.setStyle(Paint.Style.STROKE);
        _dataPaint.setColor(_dataColour);
        
        // initialize the data container
        _plotData   = new LinkedList<Plot2DData>();
        _endPlotObj = null;
        
        // initialize the hooked data objects
        _hookedObj     = null;
        _hookedObjDist = 0.0;
        
        // set the bounds for the x and y axes
        _xAxisBounds = new Rect();
        _yAxisBounds = new Rect();
        
        // set the paints for the axes
        _xAxisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        _xAxisTextPaint.setStyle(Paint.Style.STROKE);
        _xAxisTextPaint.setColor(_xAxisTextColour);
        _xAxisTextPaint.setTextAlign(Align.LEFT);
        
        _yAxisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        _yAxisTextPaint.setStyle(Paint.Style.STROKE);
        _yAxisTextPaint.setColor(_yAxisTextColour);
        _yAxisTextPaint.setTextAlign(Align.LEFT);
        
        // set the axes scale values
        _xAxisScale            = 1.0f;
        _yAxisScale            = 1.0f;
        
        // default the origin coordinates
        _originXCoord = 0.0;
        _originYCoord = 0.0;
        
        // default the hooking gate size
        _hookingGateSize = 50.0;
        
        // default the spacing between grid lines
        _xAxisGridLineSpacing = 0.0;
        _yAxisGridLineSpacing = 0.0;
        
        // default the zoom scale factor to 5%
        _zoomScaleFactor = 0.05;
        
        // set the semi-length of the diagonal of the position square drawn 
        // on the plot
        _semiPosSqDiagnonalLen = 2.0;
        
        // default the angle of rotation to zero
        _axisRotationAngle = new Angle();
        _axisRotationAngle.set(0.0);
        
        // intialize the partner view list
        _partnerViews = new Vector<Plot2DView>();
    }
    
    //! Colour for a hooked object.
    protected int _hookedColour;
    
    //! Colour for a data object.
    protected int _dataColour;
    
    //! Colour for the grid lines.
    protected int _gridColour;
    
    //! Colour for the plot background.
    protected int _bgColour;
    
    //! Colour for the x-axis text.
    protected int _xAxisTextColour;
    
    //! Colour for the y-axis text.
    protected int _yAxisTextColour;
 
    //! Indicator on whether to draw grid lines on the plot.
    protected boolean _drawGrid;
    
    //! Indicates whether the reference horizontal and vertical coordinates have been set.
    protected boolean _refLineCoordsSet;
    
    //! The x-coordinate of a vertical grid line that is used as a reference for all others.
    protected double _refVertLineXCoord;
    
    //! The y-coordinate of a horizontal grid line that is used as a reference for all others.
    protected double _refHorizLineYCoord;
    
    //! The previously touched (by user) (x,y) coordinates for up to two pointers.
    protected SparseArray<PointerData> _pointerData;
    
    //! Paint object used for painting the grid lines.
    protected Paint _gridPaint;
    
    //! The minimum x-coordinate for the plot.
    protected double _minXCoord;
    
    //! The maximum x-coordinate for the plot.
    protected double _maxXCoord;
    
    //! The minimum y-coordinate for the plot.
    protected double _minYCoord;
    
    //! The maximum y-coordinate for the plot.
    protected double _maxYCoord;
    
    //! Paint object used for drawing the background on the canvas.
    protected Paint _bgPaint;
    
    //! Bounds for the background.
    protected RectF _bgBounds;
    
    //! Paint object used for drawing the position data on the canvas.
    protected Paint _dataPaint;
    
    //! Container of plot data objects.
    protected List<Plot2DData> _plotData;
    
    //! Reference to the last element added to the plot data container.
    protected Plot2DData _endPlotObj;
    
    //! Reference to the hooked data element.
    protected Plot2DData _hookedObj;
    
    //! Distance from touched position on display to the hooked object.
    protected double _hookedObjDist;
    
    //! Bounds for the text along the y-axis.
    protected Rect _yAxisBounds;
    
    //! Bounds for the text along the x-axis.
    protected Rect _xAxisBounds;
    
    //! Paint object used for writing text on the x-axis of the plot.
    protected Paint _xAxisTextPaint;
    
    //! Paint object used for writing text on the y-axis of the plot.
    protected Paint _yAxisTextPaint;
    
    //! Scale value for the x-axis in to be determined (by derived class) units.
    protected float _xAxisScale;
    
    //! Scale value for the y-axis in to be determined (by derived class) units.
    protected float _yAxisScale;
    
    //! The plot origin x-coordinate where the origin is the centre of the plot.
    protected double _originXCoord;
    
    //! The plot origin x-coordinate where the origin is the centre of the plot.
    protected double _originYCoord;
    
    //! The axis angle of rotation, positive from north.
    protected Angle _axisRotationAngle;

    //! The gate size around a 2D data object position that is used for hooking.
    protected double _hookingGateSize;
    
    //! Hooked data message window.
    protected Toast _hookedDataToast;
    
    //! A reference to the application context.
    protected Context _context;
    
    //! The x-axis spacing between vertical grid lines.
    protected double _xAxisGridLineSpacing;
    
    //! The y-axis spacing between horizontal grid lines.
    protected double _yAxisGridLineSpacing;
    
    //! The zoom scale factor independent of axis.
    protected double _zoomScaleFactor;

    //! The semi-length of the diagnoal of the position square.
    protected double _semiPosSqDiagnonalLen;
    
    //! Reference to a list of views partnered with this one.
    protected Vector<Plot2DView> _partnerViews;
      
    //! Maximum number of pointers allowed for multi-touch on the plot.
    protected static final int MAX_NUM_POINTERS_ALLOWED = 2;
    
    //! Maximum number of moves allowed before declaring that the pointer is stationary.
    protected static final int MAX_NUM_MOVES_ALLOWED = 3;
}
