/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package that contains custom Android views.
 */
package com.madmath.views;

import com.madmath.measures.Distance;
import com.madmath.measures.Speed;
import com.madmath.measures.Time;
import com.madmath.utilities.MeasureUtils;
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
 * Class that is used to plot 2D speeds within a view/widget.
 *
 */
public class Plot2DSpeedView extends Plot2DXAxisTimeView
{      
    /**
     * Class that encapsulates speed data that is displayed on this plot.
     */
    static public class SpeedData extends Plot2DView.Plot2DData
    {
        /**
         * Default class constructor.
         */
        public SpeedData()
        {
            super();
            _speed     = new Speed();
        }
        
        /**
         * Copy constructor.
         */
        public SpeedData(final SpeedData copy)
        {
            super(copy);
            _speed     = new Speed(copy._speed);
        }
        
        //! Data speed.
        public Speed _speed;
    };
    
    /**
     * Class constructor.
     * 
     * @param   context   Information to global information about application
     *                    environment.
     */
    public Plot2DSpeedView(Context context)
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
    public Plot2DSpeedView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        
        init(context);
    }
   
    /**
     * Adds new speed data to the instance.  Will force a redraw of the widget.
     * 
     * @param   data   New speed data.
     */
    public void add(final SpeedData data)
    {
        if (_plotData.isEmpty())
        {
            _endPlotObj = new SpeedData(data);
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
                    _endPlotObj = new SpeedData(data);
                    _plotData.add(_endPlotObj);
                    invalidate();
                }
            }
        }
    }
    
    @Override
    protected void doZoom(final MOTION_TYPE motion, final boolean isHorizMotion)
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
            final double yAxisMinSpeed = _yAxisMinSpeed.get(
                    _speedUnits);
            final double yAxisMaxSpeed = _yAxisMaxSpeed.get(
                    _speedUnits);
            final double yAxisElapsedSpeed = yAxisMaxSpeed -
                    yAxisMinSpeed;
            final double speedDiff = yAxisElapsedSpeed*_zoomScaleFactor;

            switch (motion)
            {
                case PINCH_CLOSE:
                {
                    // zoom out if there is still room to expand along the y-axis
                    if (yAxisElapsedSpeed < MAX_ALLOWED_SPEED_DISPLAYED)
                    {
                        double newMinSpeed = yAxisMinSpeed - speedDiff/2.0;
                        newMinSpeed = Math.max(newMinSpeed, MIN_ALLOWED_SPEED);
                        
                        double newMaxSpeed = yAxisMaxSpeed + speedDiff/2.0;
                        newMaxSpeed = Math.min(newMaxSpeed, MAX_ALLOWED_SPEED);
                        
                        _yAxisMinSpeed.set(newMinSpeed, _speedUnits);
                        _yAxisMaxSpeed.set(newMaxSpeed, _speedUnits);
                        
                        doRedraw = true; 
                    }
                    break;
                }
                case PINCH_OPEN:
                {
                    // zoom in if there is still room to zoom/pinch along the y-axis
                    if (yAxisElapsedSpeed > MIN_ALLOWED_SPEED_DISPLAYED)
                    {                 
                        double newMinSpeed = yAxisMinSpeed + speedDiff/2.0;
                        newMinSpeed = Math.min(newMinSpeed, MAX_ALLOWED_SPEED);
                        
                        double newMaxSpeed = yAxisMaxSpeed - speedDiff/2.0;
                        newMaxSpeed = Math.max(newMaxSpeed, MIN_ALLOWED_SPEED);
                        
                        _yAxisMinSpeed.set(newMinSpeed, _speedUnits);
                        _yAxisMaxSpeed.set(newMaxSpeed, _speedUnits);
      
                        doRedraw = true; 
                    }
                    break;
                }
                default:
                {
                    // intentionally left blank
                }
            }
        }
        
        if(doRedraw)
        {
            final double xAxisRange = _maxXCoord-_minXCoord;
            
            _xAxisGridLineSpacing = xAxisRange/(_xAxisMaxTimestamp.get(Time.TIME_TYPE.SECONDS)-
                    _xAxisMinTimestamp.get(Time.TIME_TYPE.SECONDS))*
                    _xAxisMinTimeSecondsDisp;
            
            // for the y-axis, we do something similar with respect to the
            // default bearings shown
            final double yAxisRange = _maxYCoord-_minYCoord;
            
            _yAxisGridLineSpacing = yAxisRange/
                    (_yAxisMaxSpeed.get(_speedUnits)-
                    _yAxisMinSpeed.get(_speedUnits))*
                    MIN_ALLOWED_SPEED_DISPLAYED;
            
            invalidate();
        }
    }

    /**
     * Gets the distance units.
     */
    public Distance.DISTANCE_TYPE getDistanceUnits()
    {
        return _distanceUnits;
    }
    
    /**
     * Sets the distance and units and forces a redraw of the plot if the units
     * have changed.
     * 
     * @param   units   The new distance units.
     * 
     * @note    The speed units are kept in synch with the distance units.
     */
    public void setDistanceUnits(final Distance.DISTANCE_TYPE units)
    {
        setSpeedUnits(MeasureUtils.ToSpeed(units));
    }
    
    /**
     * Gets the speed units.
     */
    public Speed.SPEED_TYPE getSpeedUnits()
    {
        return _speedUnits;
    }
    
    /**
     * Sets the speed and units and forces a redraw of the plot if the units
     * have changed.
     * 
     * @param   units   The new speed units.
     * 
     * @note    The distance units are kept in synch with the speed units.
     */
    public void setSpeedUnits(final Speed.SPEED_TYPE units)
    {
        if (!_speedUnits.equals(units))
        {
            _speedUnits    = units;
            _distanceUnits = MeasureUtils.ToDistance(_speedUnits);
            
            // ensure the new maximum speed is within the correct bounds
            if (_yAxisMaxSpeed.get(_speedUnits) > MAX_ALLOWED_SPEED)
            {
                _yAxisMaxSpeed.set(MAX_ALLOWED_SPEED,_speedUnits);
            }
            
            
            // since the units have changed, reset the grid line spacing
            // to match
            _yAxisGridLineSpacing = (_maxYCoord-_minYCoord)/
                    (_yAxisMaxSpeed.get(_speedUnits)-
                    _yAxisMinSpeed.get(_speedUnits))*
                    MIN_ALLOWED_SPEED_DISPLAYED;

            invalidate();
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
        
        final float yAxisMaxSpeed = (float)_yAxisMaxSpeed.get(_speedUnits);
        final float yAxisMinSpeed = (float)_yAxisMinSpeed.get(_speedUnits);
        
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
            
            _yAxisGridLineSpacing = yAxisRange/(_yAxisMaxSpeed.get(_speedUnits)-
                    _yAxisMinSpeed.get(_speedUnits))*
                    MIN_ALLOWED_SPEED_DISPLAYED;
        }
        
        if (_drawGrid)
        {
            drawGrid(canvas);
        }
        
        boolean timestampChange = false;
        
        // draw the speed data if we have at least two
        // speeds
        if (_plotData.size() > 1)
        {
            SpeedData prevData = (SpeedData)_plotData.get(0);
                
            // ensure that the speed is within the correct range
            double spdStart = 
                    prevData._speed.get(_speedUnits);
            
            spdStart = Math.max(spdStart,yAxisMinSpeed);
            
            //float startX = _minXCoord;
            double startY = _minYCoord + height/yAxisMaxSpeed*spdStart;
            
            startY = Math.min(startY, _maxYCoord);
            
            final SpeedData endData   = (SpeedData)_endPlotObj;
            final SpeedData startData = (SpeedData)_plotData.get(0);
                  
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

        canvas.drawText(StringUtils.Pad(_yAxisMinSpeed.get(_speedUnits),3,0) + " ", 
                getPaddingLeft(), 
                getPaddingTop()+_bgBounds.height(), 
                _yAxisTextPaint);

        canvas.drawText(StringUtils.Pad(_yAxisMaxSpeed.get(_speedUnits),3,0) + " ", 
                getPaddingLeft(), 
                (float)getPaddingTop()+_yAxisBounds.height(), 
                _yAxisTextPaint);
        
        canvas.drawText(StringUtils.toString(_xAxisMinTimestamp.get(),
                StringUtils.DATETIME_FORMAT_TYPE.HH_MM_SS), 
                (float)paddingLeft, 
                (float)paddingTop+_bgBounds.height()+_xAxisBounds.height(), 
                _xAxisTextPaint);

        canvas.drawText(StringUtils.toString(_xAxisMaxTimestamp.get(),
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
            SpeedData prevData = (SpeedData)_plotData.get(0);
            
            double startX = _minXCoord;
            double startY = _maxYCoord;
            
            final double yAxisMaxSpeed = _yAxisMaxSpeed.get(_speedUnits);
            final double yAxisMinSpeed = _yAxisMinSpeed.get(_speedUnits);

            final double minTimestampSecs = 
                    _xAxisMinTimestamp.get(Time.TIME_TYPE.SECONDS);
            
            final double maxTimestampSecs = 
                    _xAxisMaxTimestamp.get(Time.TIME_TYPE.SECONDS);
            
            final double timeScaleFactor = (_maxXCoord-_minXCoord)/
                    (maxTimestampSecs - minTimestampSecs);
            
            final double spdScaleFactor = (_maxYCoord-_minYCoord)/
                    (yAxisMaxSpeed-yAxisMinSpeed);
            
            boolean canvasUpdated = false;
            
            int i=1;
            for (i=1; i<_plotData.size(); ++i)
            {
                boolean exitEarly = false;
                
                final SpeedData data = (SpeedData)_plotData.get(i);
                
                data._isDisplayed = false;
                
                final double startTimestampSecs = 
                        prevData._timestamp.get(Time.TIME_TYPE.SECONDS);
                
                final double stopTimestampSecs = 
                        data._timestamp.get(Time.TIME_TYPE.SECONDS);
               
                // ensure that the speed is within the correct range
                double spdStart = prevData._speed.get(_speedUnits);
                
                spdStart = Math.max(spdStart,yAxisMinSpeed);
                spdStart = Math.min(spdStart,yAxisMaxSpeed);
                
                if (!canvasUpdated)
                {
                    startY = _maxYCoord - spdScaleFactor*(spdStart-yAxisMinSpeed);
                    startY = Math.min(startY, _maxYCoord);
                    startY = Math.max(startY, _minYCoord);
                }
                
                double spdStop = 
                        (float)data._speed.get(_speedUnits);
                
                spdStop = Math.max(spdStop,yAxisMinSpeed);
                spdStop = Math.min(spdStop,yAxisMaxSpeed);

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
                            ((spdStop - spdStart)*spdScaleFactor)/
                            ((elapsedTimeSecs)*timeScaleFactor);
                    
                    // the start time fits within the plot, but the
                    // end time is outside of the plot so we need to
                    // adjust the elapsed time stamp accordingly
                    final double adjElapsedTimeSecs = maxTimestampSecs - 
                            startTimestampSecs;
                    
                    // the start coordinate is fine, but the stop
                    // coordinate is now on the edge at maxTimestampSecs
                    stopX = _maxXCoord;
                    
                    stopY = startY - adjElapsedTimeSecs*timeScaleFactor*slope;

                    stopY = Math.max(stopY,  _minYCoord);
                    stopY = Math.min(stopY,  _maxYCoord);
                    
                    // can skip any subsequent speed since we have
                    // painted until the end of the plot
                    exitEarly = true;
                }
                else if (startTimestampSecs >= minTimestampSecs && 
                         stopTimestampSecs  <=  maxTimestampSecs)
                {
                    // both times fit within the plot, so no adjustments
                    // to the elapsed time is needed
                    stopX = startX + timeScaleFactor*elapsedTimeSecs;
                    
                    stopY = _maxYCoord - (spdStop-yAxisMinSpeed)*spdScaleFactor;
                    
                    stopY = Math.max(stopY,  _minYCoord);
                    stopY = Math.min(stopY,  _maxYCoord);
                }
                else if (startTimestampSecs < minTimestampSecs)
                {
                    final double slope =
                            ((spdStop - spdStart)*spdScaleFactor)/
                            ((elapsedTimeSecs)*timeScaleFactor);
                    
                    final double adjElapsedTimeSecs = minTimestampSecs - 
                            startTimestampSecs;
                    
                    // the start coordinate is then at the minimum
                    // timestamp (i.e. the left of the plot)
                    startX = _minXCoord;
                    
                    startY = _maxYCoord - (spdStart-yAxisMinSpeed)*spdScaleFactor;
                    
                    startY = Math.max(startY,  _minYCoord);
                    startY = Math.min(startY,  _maxYCoord);
                    
                    startY -= adjElapsedTimeSecs*timeScaleFactor*slope;

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
                        
                        stopY = startY - adjElapsedStopTimeSecs*timeScaleFactor*slope;
                        
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
                        
                        stopY = startY - adjElapsedStopTimeSecs*timeScaleFactor*slope;
                        
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
                    drawLine((float)startX, (float)startY, (float)stopX, (float)stopY, _dataPaint, canvas);
                    
                    // save the coordinates to the data objects for later hooking
                    prevData._coord.x = (float) startX;
                    prevData._coord.y = (float) startY;
                    
                    data._coord.x = (float) stopX;
                    data._coord.y = (float) stopY;
                    
                    data._isDisplayed = true;
                    
                    startX = stopX;
                    startY = stopY;

                    canvasUpdated = true;
                    
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
                final SpeedData data = (SpeedData)_plotData.get(i);
                
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
     * Responds to a click of the speed units label.
     * 
     */
    public void onSpeedUnitClick()
    {
        switch(_speedUnits)
        {
            case KILOMETRES_PER_HOUR:
            {
                _speedUnits = Speed.SPEED_TYPE.METRES_PER_SEC;
                break;
            }
            case METRES_PER_SEC:
            {
                _speedUnits = Speed.SPEED_TYPE.MILES_PER_HOUR;
                break;
            }
            case MILES_PER_HOUR:
            {
                _speedUnits = Speed.SPEED_TYPE.YARDS_PER_SEC;
                break;
            }
            case YARDS_PER_SEC:
            {
                _speedUnits = Speed.SPEED_TYPE.KILOMETRES_PER_HOUR;
                break;
            }
            default:
            {
                // unhandled
                break;
            }
        }  
    }
    
    @Override
    protected void dispHookedDataMsg()
    {
        // cast the object to a object
        SpeedData spdData = (SpeedData)_hookedObj;
        
        spdData._isHooked = true;
        
        if (_hookedDataToast != null)
        {
            // close the message in case it is already showing
            _hookedDataToast.cancel();
        }

        CharSequence message = "";
        message = StringUtils.Pad(spdData._speed.get(
                          _speedUnits), 0, 2) + " " + Speed.ToStringAbbrev(_speedUnits) 
                          + "\n" +
                  StringUtils.toString(spdData._timestamp.get(
                          Time.TIME_TYPE.MILLISECONDS), 
                          StringUtils.DATETIME_FORMAT_TYPE.HH_MM_SS);;

        _hookedDataToast = Toast.makeText(_context, message, Toast.LENGTH_LONG);
        _hookedDataToast.setGravity(Gravity.TOP | Gravity.LEFT, 
                                    this.getLeft()+(int)spdData._coord.x,
                                    this.getTop()+(int)spdData._coord.y);
        _hookedDataToast.show();
    }
        
    @Override
    protected void handleSwipe(final double deltaX, final double deltaY)
    {
        boolean invalidatePlot = false;
        
        // get the start and end speed data objects from the list
        
        if (!_plotData.isEmpty())
        {  
            final SpeedData startData = (SpeedData)_plotData.get(0);
            final SpeedData endData   = (SpeedData)_endPlotObj;
            
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
                //else if(deltaX > 0.0 && _inHistoryMode)
                else if(deltaX > 0.0)
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
            
            if (!doHorizontalSwipe)
            {
                // adjust the y-axis speed data if possible
                final double yAxisMaxSpeed  = _yAxisMaxSpeed.get(_speedUnits);
                final double yAxisMinSpeed  = _yAxisMinSpeed.get(_speedUnits);
                final double yAxisSpeedDiff = yAxisMaxSpeed-yAxisMinSpeed; 
    
                final double speedAxisScaleFactor = 
                        (yAxisMaxSpeed - yAxisMinSpeed)/(_maxYCoord-_minYCoord);
                
                final double speedDiff = deltaY*speedAxisScaleFactor;
                
                if (deltaY > 0.0)
                {
                    // is there data to swipe past to?
                    if (yAxisMaxSpeed < MAX_ALLOWED_SPEED)
                    {
                        // we can swipe up since the maximum allowed speed is not
                        // yet reached - how far do we go?
                        final double newMaxSpeed = 
                                Math.min(MAX_ALLOWED_SPEED, yAxisMaxSpeed+speedDiff);
    
                        // set the new min and max speeds for the plot
                        _yAxisMinSpeed.set(newMaxSpeed-yAxisSpeedDiff,_speedUnits);
                        _yAxisMaxSpeed.set(newMaxSpeed, _speedUnits);
    
                        invalidatePlot = true;
                    }
                }
                else if(deltaY < 0.0)
                {
                    // is there data to swipe past to?
                    if (yAxisMinSpeed > MIN_ALLOWED_SPEED)
                    {
                        // we can swipe down since the minimum allowed speed is not
                        // yet reached - how far do we go?
                        final double newMinSpeed = 
                                Math.max(MIN_ALLOWED_SPEED, yAxisMinSpeed+speedDiff);
                        
                        // set the new min and max speeds for the plot
                        _yAxisMinSpeed.set(newMinSpeed,_speedUnits);
                        _yAxisMaxSpeed.set(newMinSpeed+yAxisSpeedDiff, _speedUnits);
    
                        invalidatePlot = true;
                    }
                }
                
                if (invalidatePlot)
                {
                    if (_yAxisMinSpeed.get(_speedUnits) <= MIN_ALLOWED_SPEED)
                    {
                        _refHorizLineYCoord = _maxYCoord;
                    }
                    else
                    {
                        // shift the grid line
                        _refHorizLineYCoord += deltaY;
                    }
                }
            }
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

        // default the speed and distance types
        _distanceUnits = Distance.DISTANCE_TYPE.METRES;
        _speedUnits    = Speed.SPEED_TYPE.METRES_PER_SEC;
        
        // initialize speed related objects for the y-axis
        _yAxisMinSpeed = new Speed(MIN_ALLOWED_SPEED,     _speedUnits);
        _yAxisMaxSpeed = new Speed(DEFAULT_SPEED, _speedUnits);

        // override the axis scaling members
        _xAxisScale = (float)_xAxisNumSecondsDisplayed;
        _yAxisScale = DEFAULT_SPEED - MIN_ALLOWED_SPEED;
    }
    
    //! The currently set minimum speed along the y-axis.
    private Speed _yAxisMinSpeed;
    
    //! The currently set maximum speed along the y-axis.
    private Speed _yAxisMaxSpeed;
    
    //! The selected distance units for the plot and hooked data.
    private Distance.DISTANCE_TYPE _distanceUnits;
    
    //! The selected speed units for the plot and hooked data (tied to _distanceType).
    private Speed.SPEED_TYPE _speedUnits;
    
    //! Maximum history kept in seconds.
    private static final double MAX_HISTORY_SECS = 6.0*3600.0;
 
    //! Maximum allowed speed (unitless).
    private static final float MAX_ALLOWED_SPEED = 500.0f;
    
    //! Minimum allowed speed (unitless).
    private static final float MIN_ALLOWED_SPEED = 0.0f;
    
    //! Maximum allowed speed range to be displayed.
    private static final double MAX_ALLOWED_SPEED_DISPLAYED = MAX_ALLOWED_SPEED - 
            MIN_ALLOWED_SPEED;
    
    //! Minimum allowed speed range to be displayed.
    private static final double MIN_ALLOWED_SPEED_DISPLAYED = 25.0;
    
    //! Default maximum speed (unitless).
    private static final float DEFAULT_SPEED = 25.0f;
}
