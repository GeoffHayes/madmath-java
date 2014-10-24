/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package that contains custom Android views.
 */
package com.madmath.views;

import com.madmath.measures.Angle;
import com.madmath.measures.Time;
import com.madmath.utilities.StringUtils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.Toast;

/**
 * Class that is used to plot 2D bearings on a plot.
 *
 */
public class Plot2DBearingView extends Plot2DXAxisTimeView
{      
    /**
     * Class that encapsulates bearing data that is displayed on this plot.
     */
    static public class BearingData extends Plot2DView.Plot2DData
    {
        /**
         * Default class constructor.
         */
        public BearingData()
        {
            super();
            _bearing   = new Angle();
        }
        
        /**
         * Copy constructor.
         */
        public BearingData(final BearingData copy)
        {
            super(copy);
            _bearing   = new Angle(copy._bearing);
        }
        
        //! Data bearing (positive from north).
        public Angle _bearing;
    };
    
    /**
     * Class constructor.
     * 
     * @param   context   Information to global information about application
     *                    environment.
     */
    public Plot2DBearingView(Context context)
    {
         super(context);
         
         init(context);
    }
    
    /**
     * Class constructor.
     * 
     * @param   context   Information to global information about application
     *                    environment.
     * @param   attrs     Interface to retrieve data from XML files.
     */
    public Plot2DBearingView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        
        init(context);
    }
    
    /**
     * Adds new bearing data to the instance.  Will force a redraw of the widget.
     * 
     * @param   data   New bearing data.
     */
    public void add(final BearingData data)
    {
        if (_plotData.isEmpty())
        {
            _endPlotObj = new BearingData(data);
            _plotData.add(_endPlotObj);
            invalidate();
        }
        else
        {
            double elapsedTimeSecs = data._timestamp.get(Time.TIME_TYPE.SECONDS) - 
                    _plotData.get(0)._timestamp.get(Time.TIME_TYPE.SECONDS);
            
            if (elapsedTimeSecs > 0.0)
            {
                while (elapsedTimeSecs > MAX_HISTORY_SECS)
                {
                    _plotData.remove(0);
                    
                    final double oldestTimeSec = 
                            _plotData.get(0)._timestamp.get(Time.TIME_TYPE.SECONDS);
                    
                    final double xAxisMinTimeSec = 
                            _xAxisMinTimestamp.get(Time.TIME_TYPE.SECONDS);
                    
                    // validate the min and max x-axis timestamps
                    if (xAxisMinTimeSec < oldestTimeSec)
                    {
                        // update the min and max x-axis timestamps
                        _xAxisMinTimestamp.set(oldestTimeSec, 
                                              Time.TIME_TYPE.SECONDS);
                        _xAxisMaxTimestamp.set(oldestTimeSec+_xAxisNumSecondsDisplayed, 
                                              Time.TIME_TYPE.SECONDS);
                        
                        // time synch with partner views
                        timeUpdatePartnerViews(false);
                    }

                    elapsedTimeSecs = data._timestamp.get(Time.TIME_TYPE.SECONDS) - 
                            oldestTimeSec;
                    
                    if (elapsedTimeSecs < 0.0)
                    {
                        break;
                    }
                }
                
                if (elapsedTimeSecs >= 0.0)
                {
                    _endPlotObj = new BearingData(data);
                    _plotData.add(_endPlotObj);
                    invalidate();
                }
            }
        }
    }
    
    @Override
    protected void doZoom(final MOTION_TYPE motion, final boolean isHorizMotion)
    {
        try
        {
            boolean doRedraw = false;
            
            if (isHorizMotion)
            {
                final double xAxisMinTimeSecs = _xAxisMinTimestamp.get(
                        Time.TIME_TYPE.SECONDS);
                final double xAxisMaxTimeSecs = _xAxisMaxTimestamp.get(
                        Time.TIME_TYPE.SECONDS);
                final double xAxisElapsedTimeSecs = xAxisMaxTimeSecs -
                        xAxisMinTimeSecs;
                final double timeDiffSecs = xAxisElapsedTimeSecs*_zoomScaleFactor;
                
                final double oldestTimeSecs = _plotData.get(0)._timestamp.get(
                        Time.TIME_TYPE.SECONDS);
    
                switch (motion)
                {
                    case PINCH_CLOSE:
                    {
                        // zoom out if there is still room to expand along the x-axis
                        if (xAxisElapsedTimeSecs < _xAxisMaxTimeSecondsDisp)
                        {
                            double newMinTimeSecs = xAxisMinTimeSecs - timeDiffSecs/2.0;
                            newMinTimeSecs = Math.max(newMinTimeSecs, oldestTimeSecs);
                            
                            double newMaxTimeSecs = xAxisMaxTimeSecs + timeDiffSecs/2.0;
                            newMaxTimeSecs = Math.min(newMaxTimeSecs, 
                                                      newMinTimeSecs+_xAxisMaxTimeSecondsDisp);
                            
                            _xAxisMinTimestamp.set(newMinTimeSecs, Time.TIME_TYPE.SECONDS);
                            _xAxisMaxTimestamp.set(newMaxTimeSecs, Time.TIME_TYPE.SECONDS);
                            
                            _xAxisNumSecondsDisplayed = newMaxTimeSecs-newMinTimeSecs;
                            
                            doRedraw = true; 
                        }
                        break;
                    }
                    case PINCH_OPEN:
                    {
                        // zoom in if there is still room to zoom/pinch along the x-axis
                        if (xAxisElapsedTimeSecs > _xAxisMinTimeSecondsDisp)
                        {
                            final double newestTimeSecs = _endPlotObj._timestamp.get(
                                    Time.TIME_TYPE.SECONDS);
                            
                            double newMinTimeSecs = xAxisMinTimeSecs + timeDiffSecs/2.0;
                            newMinTimeSecs = Math.min(newMinTimeSecs, newestTimeSecs);
                            
                            double newMaxTimeSecs = xAxisMaxTimeSecs - timeDiffSecs/2.0;
                            newMaxTimeSecs = Math.max(newMaxTimeSecs, 
                                                      newMinTimeSecs+_xAxisMinTimeSecondsDisp);
                            
                            _xAxisMinTimestamp.set(newMinTimeSecs, Time.TIME_TYPE.SECONDS);
                            _xAxisMaxTimestamp.set(newMaxTimeSecs, Time.TIME_TYPE.SECONDS);
                            
                            _xAxisNumSecondsDisplayed = newMaxTimeSecs-newMinTimeSecs;
          
                            doRedraw = true; 
                        }
                        break;
                    }
                    default:
                    {
                        // intentionally left blank
                    }
                }
                
                // time synch with partner views
                timeUpdatePartnerViews(true);
            }
            else
            {
                // comment out for now as is not to be used for bearing plots
//                final double yAxisMinBrgDegs = _yAxisMinBearing.get(
//                        Angle.ANGLE_TYPE.DEGREES);
//                final double yAxisMaxBrgDegs = _yAxisMaxBearing.get(
//                        Angle.ANGLE_TYPE.DEGREES);
//                final double yAxisElapsedBrgDegs = yAxisMaxBrgDegs -
//                        yAxisMinBrgDegs;
//                final double bearingDiff = yAxisElapsedBrgDegs*_zoomScaleFactor;
//    
//                switch (motion)
//                {
//                    case PINCH_CLOSE:
//                    {
//                        // zoom out if there is still room to expand along the y-axis
//                        if (yAxisElapsedBrgDegs < MAX_ALLOWED_BEARING_DISPLAYED_DEGS)
//                        {
//                            double newMinBrg = yAxisMinBrgDegs - bearingDiff/2.0;
//                            newMinBrg = Math.max(newMinBrg, MIN_ALLOWED_BEARING_DEGS);
//                            
//                            double newMaxBrg = yAxisMaxBrgDegs + bearingDiff/2.0;
//                            newMaxBrg = Math.min(newMaxBrg, MAX_ALLOWED_BEARING_DEGS);
//                            
//                            _yAxisMinBearing.set(newMinBrg, Angle.ANGLE_TYPE.DEGREES);
//                            _yAxisMaxBearing.set(newMaxBrg, Angle.ANGLE_TYPE.DEGREES);
//                            
//                            doRedraw = true;  
//                        }
//                        break;
//                    }
//                    case PINCH_OPEN:
//                    {
//                        // zoom in if there is still room to zoom/pinch along the y-axis
//                        if (yAxisElapsedBrgDegs > MIN_ALLOWED_BEARING_DISPLAYED_DEGS)
//                        {                 
//                            double newMinBrg = yAxisMinBrgDegs + bearingDiff/2.0;
//                            newMinBrg = Math.min(newMinBrg, MAX_ALLOWED_BEARING_DEGS);
//                            
//                            double newMaxBrg = yAxisMaxBrgDegs - bearingDiff/2.0;
//                            newMaxBrg = Math.max(newMaxBrg, MIN_ALLOWED_BEARING_DEGS);
//                            
//                            _yAxisMinBearing.set(newMinBrg, Angle.ANGLE_TYPE.DEGREES);
//                            _yAxisMaxBearing.set(newMaxBrg, Angle.ANGLE_TYPE.DEGREES);
//          
//                            doRedraw = true; 
//                        }
//                        break;
//                    }
//                    default:
//                    {
//                        // intentionally left blank
//                    }
//                }
            }
            
            if(doRedraw)
            {
                final double xAxisRange = _maxXCoord-_minXCoord;
                
                _xAxisGridLineSpacing = xAxisRange/
                        (_xAxisMaxTimestamp.get(Time.TIME_TYPE.SECONDS)-
                        _xAxisMinTimestamp.get(Time.TIME_TYPE.SECONDS))*
                        _xAxisMinTimeSecondsDisp;
                
                // for the y-axis, we do something similar with respect to the
                // default bearings shown
                final double yAxisRange = _maxYCoord-_minYCoord;
                
                _yAxisGridLineSpacing = yAxisRange/(_yAxisMaxBearing.get(Angle.ANGLE_TYPE.DEGREES)-
                        _yAxisMinBearing.get(Angle.ANGLE_TYPE.DEGREES))*
                        DEFAULT_BEARING_SPACING_DEGS;
                
                invalidate();
            }
        }
        catch(final Exception e)
        {
            android.util.Log.e(
                    this.getClass().getSimpleName(),
                    "doZoom exception: " + e.toString());
        }
    }

    @Override
    /**
     * Redraws the canvas/plot on change to the size of the view.
     * 
     * @param   w      The new width of the view.
     * @param   h      The new height of the view.
     * @param   oldw   The previous width (before the size change) of the view.
     * @param   oldh   The previous height (before the size change) of the view.
     */
    protected void onSizeChanged(int w, int h, int oldw, int oldh) 
    {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // set dimensions, account for padding
        float xpad = getPaddingLeft() + getPaddingRight();
        float ypad = getPaddingTop() + getPaddingBottom();

        float ww = w - xpad - _yAxisBounds.width();
        float hh = h - ypad - _xAxisBounds.height();

        _bgBounds = new RectF(
                0.0f,
                0.0f,
                ww,
                hh);
        
        _bgBounds.offsetTo(
                getPaddingLeft()+_yAxisBounds.width(), 
                getPaddingTop());

        invalidate();
    }
        
    @Override
    /**
     * Updates the canvas with plot information.
     * 
     * @param   canvas   The canvas to draw on.
     */
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        
        canvas.drawRect(_bgBounds, _bgPaint);
        
        final double width  = _bgBounds.width();
        final double height = _bgBounds.height();
        
        final double paddingLeft = getPaddingLeft() +
                _yAxisBounds.width();
        
        final double paddingTop  = getPaddingTop();
        
        _minXCoord = paddingLeft;
        _minYCoord = paddingTop;
        _maxXCoord = _minXCoord+width;
        _maxYCoord = _minYCoord+height;
        
        // set the initial reference coordinates for the grid
        if (!_refLineCoordsSet)
        {
            _refLineCoordsSet   = true;
            _refVertLineXCoord  = _minXCoord;
            _refHorizLineYCoord = _maxYCoord;
            
            // evenly space the vertical lines along the x-axis
            final double xAxisRange = _maxXCoord-_minXCoord;
            
            _xAxisGridLineSpacing = xAxisRange/
                    _xAxisNumSecondsDisplayed*
                    _xAxisMinTimeSecondsDisp;
            
            // for the y-axis, we do something similar with respect to the
            // default bearings shown
            final double yAxisRange = _maxYCoord-_minYCoord;
            
            _yAxisGridLineSpacing = yAxisRange/
                    (_yAxisMaxBearing.get(Angle.ANGLE_TYPE.DEGREES)-
                    _yAxisMinBearing.get(Angle.ANGLE_TYPE.DEGREES))*
                    DEFAULT_BEARING_SPACING_DEGS;
        }
        
        if (_drawGrid)
        {
            drawGrid(canvas);
        }
        
        boolean timestampChange = false;
        
        // draw the bearing data if we have at least two
        // bearings
        if (_plotData.size() > 1)
        {
            BearingData prevData = (BearingData)_plotData.get(0);
                
            // ensure that the bearing is within the correct range
            double brgStartDegs = 
                    prevData._bearing.get(Angle.ANGLE_TYPE.DEGREES);
            
            brgStartDegs = Math.max(brgStartDegs,
                                    _yAxisMinBearing.get(Angle.ANGLE_TYPE.DEGREES));
            
            //float startX = _minXCoord;
            double startY = _minYCoord + 
                    height/_yAxisMaxBearing.get(Angle.ANGLE_TYPE.DEGREES)*brgStartDegs;
            
            startY = Math.min(startY, _maxYCoord);
            
            final BearingData endData   = (BearingData)_endPlotObj;
            final BearingData startData = (BearingData)_plotData.get(0);
                  
            if (!_inHistoryMode)
            {
                final float elapsedTimeSecs = (float)Math.abs(
                        endData._timestamp.get(Time.TIME_TYPE.SECONDS) -
                        startData._timestamp.get(Time.TIME_TYPE.SECONDS));
                
                if (elapsedTimeSecs >= _xAxisNumSecondsDisplayed)
                {
                    _xAxisMaxTimestamp.copy(endData._timestamp);
                    _xAxisMinTimestamp.set(
                            _xAxisMaxTimestamp.get(Time.TIME_TYPE.SECONDS) -
                            _xAxisNumSecondsDisplayed, Time.TIME_TYPE.SECONDS);
                }
                else
                {
                    _xAxisMinTimestamp.copy(startData._timestamp);
                    _xAxisMaxTimestamp.set(
                            _xAxisMinTimestamp.get(Time.TIME_TYPE.SECONDS) +
                            _xAxisNumSecondsDisplayed, Time.TIME_TYPE.SECONDS);
                }
                
                timestampChange = true;
            }
            
            plotDataWithinTimestamps(canvas);
        }
        else if (_plotData.size() == 1)
        {
            _xAxisMinTimestamp.copy(_plotData.get(0)._timestamp);
            _xAxisMaxTimestamp.set(
                    _xAxisMinTimestamp.get(Time.TIME_TYPE.SECONDS) +
                    _xAxisNumSecondsDisplayed, Time.TIME_TYPE.SECONDS);
            
            timestampChange = true;
        }
        
        // add the vertical axis labels
        final char degree = '\u00B0';
        
        canvas.drawText(
                StringUtils.Pad(
                _yAxisMaxBearing.get(Angle.ANGLE_TYPE.DEGREES),3,0)+degree, 
                getPaddingLeft(), 
                getPaddingTop()+_bgBounds.height(), 
                _yAxisTextPaint);
        
        canvas.drawText(
                "  0"+degree, 
                getPaddingLeft(), 
                getPaddingTop()+_bgBounds.height()/2, 
                _yAxisTextPaint);
        
        canvas.drawText(
                StringUtils.Pad(
                _yAxisMinBearing.get(Angle.ANGLE_TYPE.DEGREES),3,0)+degree, 
                getPaddingLeft(), 
                (float)getPaddingTop()+_yAxisBounds.height(), 
                _yAxisTextPaint);
        
        canvas.drawText(
                StringUtils.toString(_xAxisMinTimestamp.get(),
                StringUtils.DATETIME_FORMAT_TYPE.HH_MM_SS), 
                (float)paddingLeft, 
                (float)paddingTop+_bgBounds.height()+_xAxisBounds.height(), 
                _xAxisTextPaint);
        
        canvas.drawText(StringUtils.toString(
                _xAxisMaxTimestamp.get(),
                StringUtils.DATETIME_FORMAT_TYPE.HH_MM_SS), 
                (float)(_maxXCoord-_xAxisBounds.width()), 
                (float)paddingTop+_bgBounds.height()+_xAxisBounds.height(), 
                _xAxisTextPaint);
        
        if (timestampChange)
        {
            timeUpdatePartnerViews(false);
        }
    }
    
    /**
     * Plots data within the already calculated left and right timestamps for the plot.
     * 
     * @warning  Assumes that there is at least two elements in the data list.
     */
    private void plotDataWithinTimestamps(Canvas canvas)
    {
        // data is plotted between the already calculated left and right
        // timestamps
        if (!_plotData.isEmpty())
        {
            BearingData prevData = (BearingData)_plotData.get(0);
            
            double startX = _minXCoord;
            double startY = _maxYCoord;
            
            final double yAxisMaxBrgDegs = 
                    _yAxisMaxBearing.get(Angle.ANGLE_TYPE.DEGREES);
            
            final double yAxisMinBrgDegs = 
                    _yAxisMinBearing.get(Angle.ANGLE_TYPE.DEGREES);

            final double minTimestampSecs = 
                    _xAxisMinTimestamp.get(Time.TIME_TYPE.SECONDS);
            
            final double maxTimestampSecs = 
                    _xAxisMaxTimestamp.get(Time.TIME_TYPE.SECONDS);
            
            final double timeScaleFactor = (_maxXCoord-_minXCoord)/
                    (maxTimestampSecs - minTimestampSecs);
            
            final double brgScaleFactor = (_maxYCoord-_minYCoord)/
                    (yAxisMaxBrgDegs-yAxisMinBrgDegs);
            
            boolean canvasUpdated = false;
            
            int i = 1;
            for (i=1; i<_plotData.size(); ++i)
            {
                boolean exitEarly = false;
                
                final BearingData data = (BearingData)_plotData.get(i);
                
                data._isDisplayed = false;
                
                final double startTimestampSecs = 
                        prevData._timestamp.get(Time.TIME_TYPE.SECONDS);
                
                final double stopTimestampSecs = 
                        data._timestamp.get(Time.TIME_TYPE.SECONDS);
               
                // ensure that the bearing is within the correct range
                double brgStartDegs = 
                        prevData._bearing.get(Angle.ANGLE_TYPE.DEGREES);
                
                brgStartDegs = Math.max(brgStartDegs,yAxisMinBrgDegs);
                brgStartDegs = Math.min(brgStartDegs,yAxisMaxBrgDegs);
                
                if (!canvasUpdated)
                {
                    startY = _minYCoord + brgScaleFactor*(brgStartDegs-yAxisMinBrgDegs);
                    startY = Math.min(startY, _maxYCoord);
                    startY = Math.max(startY, _minYCoord);
                }
                
                double brgStopDegs = 
                        data._bearing.get(Angle.ANGLE_TYPE.DEGREES);
                
                brgStopDegs = Math.max(brgStopDegs,yAxisMinBrgDegs);
                brgStopDegs = Math.min(brgStopDegs,yAxisMaxBrgDegs);

                double elapsedTimeSecs = 
                        stopTimestampSecs - startTimestampSecs;

                double stopX = 0.0;
                double stopY = 0.0;
                
                // should we draw this?
                boolean fitsWithinWindow = true;
                
                if (startTimestampSecs > maxTimestampSecs)
                {
                    // the starting time is greater than the max plot time
                    // so exit early
                    fitsWithinWindow = false;
                    exitEarly        = true;
                }
                else if (startTimestampSecs >= minTimestampSecs && 
                         stopTimestampSecs  >  maxTimestampSecs)
                {
                    final double slope = 
                            ((brgStopDegs - brgStartDegs)*brgScaleFactor)/
                            ((elapsedTimeSecs)*timeScaleFactor);
                    
                    // the start time fits within the plot, but the
                    // end time is outside of the plot so we need to
                    // adjust the elapsed time stamp accordingly
                    final double adjElapsedTimeSecs = maxTimestampSecs - 
                            startTimestampSecs;
                    
                    // the start coordinate is fine, but the stop
                    // coordinate is now on the edge at maxTimestampSecs
                    stopX = _maxXCoord;
                  
                    stopY = startY + adjElapsedTimeSecs*timeScaleFactor*slope;

                    stopY = Math.max(stopY,  _minYCoord);
                    stopY = Math.min(stopY,  _maxYCoord);
                    
                    // can skip any subsequent bearings since we have
                    // painted until the end of the plot
                    exitEarly = true;
                }
                else if (startTimestampSecs >= minTimestampSecs && 
                         stopTimestampSecs  <=  maxTimestampSecs)
                {
                    // both times fit within the plot, so no adjustments
                    // to the elapsed time is needed
                    stopX = startX + timeScaleFactor*elapsedTimeSecs;
                    
                    stopY = _minYCoord + (brgStopDegs-yAxisMinBrgDegs)*brgScaleFactor;
                    
                    stopY = Math.max(stopY,  _minYCoord);
                    stopY = Math.min(stopY,  _maxYCoord);
                }
                else if (startTimestampSecs < minTimestampSecs)
                {
                    final double slope =
                            ((brgStopDegs - brgStartDegs)*brgScaleFactor)/
                            ((elapsedTimeSecs)*timeScaleFactor);
                    
                    final double adjElapsedTimeSecs = minTimestampSecs - 
                            startTimestampSecs;
                    
                    // the start coordinate is then at the minimum
                    // timestamp (i.e. the left of the plot)
                    startX = _minXCoord;
                    
                    startY = _minYCoord + (brgStartDegs-yAxisMinBrgDegs)*brgScaleFactor;
                    
                    startY = Math.max(startY,  _minYCoord);
                    startY = Math.min(startY,  _maxYCoord);
                    
                    startY += adjElapsedTimeSecs*timeScaleFactor*slope;
                    
                    startY = Math.max(startY,  _minYCoord);
                    startY = Math.min(startY,  _maxYCoord);

                    if (stopTimestampSecs >= minTimestampSecs &&
                        stopTimestampSecs >  maxTimestampSecs)
                    {
                        // so the plot window is enclosed by the start
                        // and end times, so the elapsed time is just
                        // the length of the window
                        final double adjElapsedStopTimeSecs = _xAxisNumSecondsDisplayed;

                        stopX  = _maxXCoord;
                        
                        stopY = startY + adjElapsedStopTimeSecs*timeScaleFactor*slope;
                        
                        stopY = Math.max(stopY,  _minYCoord);
                        stopY = Math.min(stopY,  _maxYCoord);   

                        // exit early since the complete window is painted
                        exitEarly = true;
                    }
                    else if (stopTimestampSecs >= minTimestampSecs &&
                             stopTimestampSecs <=  maxTimestampSecs)
                    {
                        // the end time fits within the window but
                        // the elapsed time needs to be adjusted
                        final double adjElapsedStopTimeSecs = stopTimestampSecs - 
                                minTimestampSecs;

                        stopX = startX + timeScaleFactor*adjElapsedStopTimeSecs;
                        
                        stopY = startY + adjElapsedStopTimeSecs*timeScaleFactor*slope;
                        
                        stopY = Math.max(stopY,  _minYCoord);
                        stopY = Math.min(stopY,  _maxYCoord);
                    }
                    else
                    {
                        // the start and end times are both outside of
                        // the window, so cannot be used
                        fitsWithinWindow = false;
                    }
                    
                }

                if (stopX >= _maxXCoord)
                {
                    exitEarly = true;
                    stopX     = _maxXCoord;  
                }
                
                if (Math.abs(stopX-startX) < 1.0)
                {
                    fitsWithinWindow = false;
                }
               
                // draw the line only if it has been fitted for the
                // window
                if (fitsWithinWindow)
                {
                    drawSmartLine(
                            (float)startX, (float)startY, 
                            (float)stopX, (float)stopY, _dataPaint,canvas);
                    
                    // save the coordinates to the data objects for later hooking
                    prevData._coord.x = (float) startX;
                    prevData._coord.y = (float) startY;
                    
                    data._coord.x = (float) stopX;
                    data._coord.y = (float) stopY;
                    
                    data._isDisplayed = true;
                    
                    startX = stopX;
                    startY = stopY;
                    
                    // draw squares around the points to make it clear where the
                    // data is, hooking where necessary
                    if(prevData._isHooked)
                    {
                        _dataPaint.setColor(_hookedColour);
                    }
                    drawPositionSquare(prevData,_semiPosSqDiagnonalLen,_dataPaint,canvas);
                    
                    if(data._isHooked)
                    {
                        _dataPaint.setColor(_hookedColour);
                    }
                    else
                    {
                        _dataPaint.setColor(_dataColour);
                    }
                    drawPositionSquare(data,_semiPosSqDiagnonalLen,_dataPaint,canvas);
                    
                    _dataPaint.setColor(_dataColour);
                    
                    canvasUpdated = true;
                }        
    
                if (exitEarly)
                {
                    break;
                }

                // save the previous data for the next iteration
                prevData = data;
            }
            
            // need to reset the is displayed flag for the remaining elements
            // in the container
            for (;i<_plotData.size(); ++i)
            {
                final BearingData data = (BearingData)_plotData.get(i);
                
                // assume that on first element that isn't displayed, that those
                // which follow are not displayed either
                if (!data._isDisplayed)
                {
                    break;
                }
                
                data._isDisplayed = false;
            }
        }
    }
    
    /**
     * Draws a line between two positions, ensuring that the line does not extend
     * outside of the plot.  Intended to handle the all combinations of both points
     * in the plot, both outside of the plot, or one of the two outside of the 
     * plot.
     * 
     * A "smart" line is drawn whereby the assumption is that it must start at the
     * from position and end at the to position but need not take the easiest path
     * and so can reach its destination by crossing outside of the plot.  Intended
     * for plots that are circular in the y-axis.
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
    private void drawSmartLine(
            final float fromX, final float fromY, 
            final float toX,   final float toY, 
            final Paint paint, Canvas canvas)
    {
        final double yAxisMaxBrgDegs = 
                _yAxisMaxBearing.get(Angle.ANGLE_TYPE.DEGREES);
        final double yAxisMinBrgDegs = 
                _yAxisMinBearing.get(Angle.ANGLE_TYPE.DEGREES);
    
        final double brgScaleFactor = (_maxYCoord-_minYCoord)/
                (yAxisMaxBrgDegs-yAxisMinBrgDegs);
        
        final double fromBrgDegs = 
                (fromY-_minYCoord)/brgScaleFactor + yAxisMinBrgDegs;
        
        final double toBrgDegs = 
                (toY-_minYCoord)/brgScaleFactor + yAxisMinBrgDegs;
        
        final double diffDegs = Math.abs(fromBrgDegs-toBrgDegs);
        
        // we want to draw the shortest lines, taking into account the wrap around
        // 180 degrees
        if (diffDegs > 180.0)
        {
            // draw two lines
            final float midX = (toX+fromX)/2.0f;
            float newYA = 0.0f;
            float newYB = 0.0f;
            
            if (fromBrgDegs > 0.0)
            {
                newYA = (float)_maxYCoord;
                newYB = (float)_minYCoord;
            }
            else
            {
                newYA = (float)_minYCoord;
                newYB = (float)_maxYCoord;
            }

            drawLine(fromX, fromY, midX, newYA, _dataPaint,canvas);
            drawLine(midX, newYB, toX, toY, _dataPaint,canvas);
        }
        else
        {    
            drawLine(fromX, fromY, toX, toY, _dataPaint,canvas);
        }
    }
    
    @Override
    /**
     * Measure the view and its content to determine the measured width and the 
     * measured height. This method is invoked by measure(int, int) and should 
     * be overriden by subclasses to provide accurate and efficient measurement 
     * of their contents.
     * 
     * CONTRACT: When overriding this method, you must call 
     * setMeasuredDimension(int, int) to store the measured width and height of 
     * this view. Failure to do so will trigger an IllegalStateException, thrown 
     * by measure(int, int). Calling the superclass' onMeasure(int, int) is a 
     * valid use.
     *
     * The base class implementation of measure defaults to the background size, 
     * unless a larger size is allowed by the MeasureSpec. Subclasses should 
     * override onMeasure(int, int) to provide better measurements of their content.
     *
     * If this method is overridden, it is the subclass's responsibility to make 
     * sure the measured height and width are at least the view's minimum height 
     * and width (getSuggestedMinimumHeight() and getSuggestedMinimumWidth()).
     * 
     * @param   widthMeasureSpec    Horizontal space requirements as imposed by the 
     *                              parent.
     * @param   heightMeasureSpec   Vertical space requirements as imposed by the 
     *                              parent.
     */
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) 
    {
        // Try for a width based on our minimum
        int minw = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();

        int w = Math.max(minw, MeasureSpec.getSize(widthMeasureSpec));

        int minh = (w) + getPaddingBottom() + getPaddingTop();
        int h = Math.min(MeasureSpec.getSize(heightMeasureSpec), minh);

        setMeasuredDimension(w, h);
    }
    
    @Override
    protected void dispHookedDataMsg()
    {
        // cast the object to a object
        BearingData brgData = (BearingData)_hookedObj;
        
        brgData._isHooked = true;
        
        if (_hookedDataToast != null)
        {
            // close the message in case it is already showing
            _hookedDataToast.cancel();
        }

        CharSequence message = "";
        message = StringUtils.Pad(brgData._bearing.get(
                Angle.ANGLE_TYPE.DEGREES_POS),3,0) + '\u00B0' + "\n" +
                  StringUtils.toString(brgData._timestamp.get(
                          Time.TIME_TYPE.MILLISECONDS), 
                          StringUtils.DATETIME_FORMAT_TYPE.HH_MM_SS);;

        _hookedDataToast = Toast.makeText(_context, message, Toast.LENGTH_LONG);
        _hookedDataToast.setGravity(Gravity.TOP | Gravity.LEFT, 
                                    this.getLeft()+(int)brgData._coord.x,
                                    this.getTop()+(int)brgData._coord.y);
        _hookedDataToast.show();
    }
    
    @Override
    protected void handleSwipe(final double deltaX, final double deltaY)
    {
        boolean invalidatePlot = false;
        
        // get the start and end bearing data objects from the list
        
        if (!_plotData.isEmpty())
        {  
            final BearingData startData = (BearingData)_plotData.get(0);
            final BearingData endData   = (BearingData)_endPlotObj;
            
            // get the needed timestamps for the x-axis
            final double startTimestampSecs   = startData._timestamp.get(Time.TIME_TYPE.SECONDS);
            final double endTimestampSecs     = endData._timestamp.get(Time.TIME_TYPE.SECONDS);
            final double elapsedTimestampSecs = endTimestampSecs - startTimestampSecs;
            final double minTimestampSecs     = _xAxisMinTimestamp.get(Time.TIME_TYPE.SECONDS);
            final double maxTimestampSecs     = _xAxisMaxTimestamp.get(Time.TIME_TYPE.SECONDS);
            
            final double timeAxisScaleFactor = 
                    (maxTimestampSecs - minTimestampSecs)/(_maxXCoord-_minXCoord);
            
            // only do a horizontal or vertical depending upon which delta is larger
            boolean doHorizontalSwipe = Math.abs(deltaX) >= Math.abs(deltaY);
    
            if (doHorizontalSwipe && (elapsedTimestampSecs > _xAxisNumSecondsDisplayed ||
                    minTimestampSecs > startTimestampSecs))
            {
                final double timeDiffSecs = deltaX*timeAxisScaleFactor;
                
                if (deltaX < 0.0)
                {
                    // is there data to swipe past to?
                    if (startTimestampSecs < minTimestampSecs)
                    {
                        // we can swipe to the left since there is older 
                        // data to display - how far back do we go?
                        final double newMinTimestampSecs = 
                                Math.max(startTimestampSecs, minTimestampSecs+timeDiffSecs);
    
                        _inHistoryMode  = true;
                        
                        // set the new min and max timestamps for the plot
                        _xAxisMinTimestamp.set(newMinTimestampSecs, 
                                Time.TIME_TYPE.SECONDS);
                        _xAxisMaxTimestamp.set(newMinTimestampSecs+_xAxisNumSecondsDisplayed, 
                                Time.TIME_TYPE.SECONDS);
    
                        invalidatePlot = true;
                    }
                }
                else if(deltaX > 0.0 && _inHistoryMode)
                {
                    // is there data to swipe up to?
                    if (endTimestampSecs > maxTimestampSecs)
                    {
                        // we can swipe to the right since there is newer 
                        // data to display - how far forward do we go?
                        final double newMaxTimestampSecs = 
                                Math.min(endTimestampSecs, maxTimestampSecs+timeDiffSecs);
    
                        // flip out of history mode if the user has swiped to the end of
                        // the plot
                        _inHistoryMode  = !(newMaxTimestampSecs>=endTimestampSecs);
                        
                        // set the new min and max timestamps for the plot
                        _xAxisMinTimestamp.set(newMaxTimestampSecs-_xAxisNumSecondsDisplayed, 
                                Time.TIME_TYPE.SECONDS);
                        _xAxisMaxTimestamp.set(newMaxTimestampSecs, 
                                Time.TIME_TYPE.SECONDS);
    
                        invalidatePlot = true;
                    }
                }
                
                if (invalidatePlot)
                {
                    // shift the grid line
                    _refVertLineXCoord  -= deltaX;

                    // time synch with partner views
                    timeUpdatePartnerViews(true);
                }
            }
            
//            // deliberately avoid vertical swipes as we don't allow the y-axis to be pinched
//            if (!doHorizontalSwipe && 1==0)
//            {
//                // adjust the y-axis bearing data if possible
//                final double yAxisMaxBrgDegs  = _yAxisMaxBearing.get(Angle.ANGLE_TYPE.DEGREES);
//                final double yAxisMinBrgDegs  = _yAxisMinBearing.get(Angle.ANGLE_TYPE.DEGREES);
//                final double yAxisBrgDiff = yAxisMaxBrgDegs-yAxisMinBrgDegs; 
//    
//                final double brgAxisScaleFactor = 
//                        (yAxisMaxBrgDegs - yAxisMinBrgDegs)/(_maxYCoord-_minYCoord);
//                
//                final double brgDiff = deltaY*brgAxisScaleFactor;
//                
//                if (deltaY > 0.0)
//                {
//                    // is there data to swipe past to?
//                    if (yAxisMaxBrgDegs < MAX_ALLOWED_BEARING_DEGS)
//                    {
//                        // we can swipe up since the maximum allowed bearing is not
//                        // yet reached - how far do we go?
//                        final double newMaxBrgDegs = 
//                                Math.min(MAX_ALLOWED_BEARING_DEGS, yAxisMaxBrgDegs+brgDiff);
//    
//                        // set the new min and max bearings for the plot
//                        _yAxisMinBearing.set(newMaxBrgDegs-yAxisBrgDiff,Angle.ANGLE_TYPE.DEGREES);
//                        _yAxisMaxBearing.set(newMaxBrgDegs, Angle.ANGLE_TYPE.DEGREES);
//    
//                        invalidatePlot = true;
//                    }
//                }
//                else if(deltaY < 0.0)
//                {
//                    // is there data to swipe past to?
//                    if (yAxisMinBrgDegs > MIN_ALLOWED_BEARING_DEGS)
//                    {
//                        // we can swipe down since the minimum allowed bearing is not
//                        // yet reached - how far do we go?
//                        final double newMinBrgDegs = 
//                                Math.max(MIN_ALLOWED_BEARING_DEGS, yAxisMinBrgDegs+brgDiff);
//                        
//                        // set the new min and max bearings for the plot
//                        _yAxisMinBearing.set(newMinBrgDegs,Angle.ANGLE_TYPE.DEGREES);
//                        _yAxisMaxBearing.set(newMinBrgDegs+yAxisBrgDiff, Angle.ANGLE_TYPE.DEGREES);
//    
//                        invalidatePlot = true;
//                    }
//                }
//                     
//                if (_yAxisMinBearing.get(Angle.ANGLE_TYPE.DEGREES) <= MIN_ALLOWED_BEARING_DEGS)
//                {
//                    _refHorizLineYCoord = _maxYCoord;
//                }
//                else
//                {
//                    // shift the grid line
//                    _refHorizLineYCoord += deltaY;
//                }
//            }
        }
        
        if (invalidatePlot)
        {
            invalidate();
        }
    }
    
    @Override
    protected void handleVerticalAxisTouch(final double xCoord, 
                                           final double yCoord)
    {
        // deliberately left blank
    }

    /**
     * Initialize data members with default values.
     */
    @Override
    protected void init(Context context)
    {
        super.init(context);
        
        // set and override the colours where necessary
        _dataColour   = Color.GREEN;
        _hookedColour = Color.WHITE;
        
        // set the style and colour
        _dataPaint.setStyle(Paint.Style.STROKE);
        _dataPaint.setColor(_dataColour);
        
        // get the size of the text for a default string along the y-axis
        _yAxisTextPaint.getTextBounds("-000"+'\u00B0', 0, 5, _yAxisBounds);
        
        // initialize bearing related objects for the y-axis
        _yAxisMinBearing = new Angle(MIN_ALLOWED_BEARING_DEGS,Angle.ANGLE_TYPE.DEGREES);
        _yAxisMaxBearing = new Angle(MAX_ALLOWED_BEARING_DEGS,Angle.ANGLE_TYPE.DEGREES);

        // override the axis scaling members
        _xAxisScale = (float)_xAxisNumSecondsDisplayed;
        _yAxisScale = MAX_ALLOWED_BEARING_DEGS - MIN_ALLOWED_BEARING_DEGS;
    }

    //! The currently set minimum bearing along the axis.
    private Angle _yAxisMinBearing;
    
    //! The currently set maximum bearing  along the axis.
    private Angle _yAxisMaxBearing;

    //! Maximum allowed bearing (degrees).
    private static final float MAX_ALLOWED_BEARING_DEGS = 180.0f;
    
    //! Minimum allowed bearing (degrees).
    private static final float MIN_ALLOWED_BEARING_DEGS = -180.0f;
    
    //! Maximum allowed bearing range to be displayed.
    private static final double MAX_ALLOWED_BEARING_DISPLAYED_DEGS = MAX_ALLOWED_BEARING_DEGS - 
            MIN_ALLOWED_BEARING_DEGS;
    
    //! Minimum allowed bearing range to be displayed.
    private static final double MIN_ALLOWED_BEARING_DISPLAYED_DEGS = 360.0;
    
    //! Default bearing spacing (degrees).
    private static final double DEFAULT_BEARING_SPACING_DEGS = 90.0;
    
    //! Maximum history kept in seconds.
    private static final double MAX_HISTORY_SECS = 6.0*3600.0;
}
