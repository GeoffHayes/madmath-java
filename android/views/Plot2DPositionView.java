/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package that contains custom Android views.
 */
package com.madmath.views;

import java.util.Vector;

import com.madmath.utilities.MeasureUtils;
import com.madmath.utilities.StringUtils;
import com.madmath.geocoordinates.Position;
import com.madmath.geocoordinates.Vincenty;
import com.madmath.measures.Angle;
import com.madmath.measures.Distance;
import com.madmath.measures.Speed;
import com.madmath.measures.Time;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.Toast;

/**
 * Class that is used to plot 2D positions within a view/widget.
 *
 */
public class Plot2DPositionView extends Plot2DView
{   
    /**
     * Class that encapsulates track data for display on a 2D plot.
     */
    static public class TrackData extends Plot2DView.Plot2DData
    {
        /**
         * Default class constructor.
         */
        public TrackData()
        {
            super();
            
            _speed          = new Speed();
            _heading         = new Angle();
            _position       = new Position();
            _dist           = new Distance();
            _distCumulative = new Distance();
            _deadReckoned   = false;
        }
        
        /**
         * Copy constructor.
         */
        public TrackData(final TrackData copy)
        {
            super(copy);
            
            _speed          = new Speed(copy._speed);
            _heading         = new Angle(copy._heading);
            _position       = new Position(copy._position);
            _dist           = new Distance(copy._dist);
            _distCumulative = new Distance(copy._distCumulative);
            _deadReckoned   = copy._deadReckoned;
        }
        
        //! Track course.
        public Angle _heading;
        
        //! Track speed.
        public Speed _speed;
        
        //! Track position latitude and longitude position.
        public Position _position;
        
        //! Track distance from previous update.
        public Distance _dist;
        
        //! Cumulative track distance from track initialization.
        public Distance _distCumulative;
        
        //! Indicates if track has been dead-reckoned (true) or not (false).
        public boolean _deadReckoned;
    };
    
    /**
     * Class that encapsulates point of interest data for display on a 2D plot.
     */
    static public class PoiData extends Plot2DView.Plot2DData
    {
        /**
         * Default class constructor.
         */
        public PoiData()
        {
            super();
            
            _position = new Position();
            _name     = null;
        }
        
        /**
         * Copy constructor.
         */
        public PoiData(final PoiData copy)
        {
            super(copy);
            
            _position = new Position(copy._position);
            _name     = new String(copy._name);
        }
        
        //! POI position latitude and longitude position.
        public Position _position;
        
        //! POI name.
        public String _name;
    };
    
    /**
     * Class constructor.
     * 
     * @param   context   Information to global information about application
     *                    environment.
     */
    public Plot2DPositionView(Context context)
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
    public Plot2DPositionView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        
        init(context);
    }
    
    /**
     * Returns whether the history is drawn (true) or not (false).
     */
    public boolean isHistoryDrawn() 
    {
        return _drawHistory;
    }
    
    /**
     * Sets the self as the origin or centre of the plot.
     */
    public void setSelfCentre()
    {
        if (!_isSelfPlotOrigin)
        {
            _isSelfPlotOrigin = true;
            invalidate();
        }
    }

    /**
     * Toggles the history point drawing on or off.
     */
    public void toggleHistory()
    {
        _drawHistory = !_drawHistory;
        invalidate();
    }
    
    /**
     * Toggles the colouring of data within the timestamp range.
     */
    public void toggleColourForTime()
    {
        _colourForTime = !_colourForTime;
        invalidate();
    }
    
    /**
     * Returns whether the position data is coloured for time or not.
     */
    public boolean isColourForTime()
    {
        return _colourForTime;
    }
    
    /**
     * Toggles the drawing of POIs on or off.
     */
    public void toggleDrawPois()
    {
        _drawPois = !_drawPois;
        invalidate();
    }
    
    /**
     * Returns whether the POI data is drawn or not.
     */
    public boolean isPoiDataDrawn()
    {
        return _drawPois;
    }
    
    /**
     * Toggles the connection of the dots (history points) drawing on or off.
     */ 
    public void toggleConnectTheDots()
    {
        _connectTheDots = !_connectTheDots;
        invalidate();
    }
    
    /**
     * Returns whether the connections between positions are drawn or not.
     */
    public boolean isConnectDataDrawn()
    {
        return _connectTheDots;
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
        if (!_distanceUnits.equals(units))
        {
            _distanceUnits = units;
            _speedUnits    = MeasureUtils.ToSpeed(_distanceUnits);
            invalidate();
        }
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
            invalidate();
        }
    }

    /**
     * Adds new track data to the instance.  Will force a redraw of the widget.
     * 
     * @param   data   New track data.
     */
    public void add(final TrackData data)
    {
        if (_plotData.isEmpty())
        {
            // create the new track data object and add to the container
            _endPlotObj = new TrackData(data);
            _plotData.add(_endPlotObj);
            
            // default the distances
            TrackData trk = (TrackData)_endPlotObj;
            
            trk._dist.set(0.0);
            trk._distCumulative.set(0.0);
            
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
                    
                    elapsedTimeSecs = data._timestamp.get(Time.TIME_TYPE.SECONDS) - 
                            _plotData.get(0)._timestamp.get(Time.TIME_TYPE.SECONDS);
                    
                    if (elapsedTimeSecs < 0.0)
                    {
                        break;
                    }
                }
                
                if (elapsedTimeSecs >= 0.0)
                {
                    // store the previous end object for Vincenty calc
                    TrackData prevEnd = null;
                    if(_endPlotObj!=null)
                    {
                        prevEnd = (TrackData)_endPlotObj;
                    }
                    
                    // create the new track data object and add to the container
                    _endPlotObj = new TrackData(data);
                    _plotData.add(_endPlotObj);
                    
                    // calculate the distance between the two objects and 
                    // update the cumulative distance
                    if (prevEnd != null)
                    {
                        try
                        {
                            TrackData trk = (TrackData)_endPlotObj;
                            
                            Vincenty.GetRangeAzimuth(
                                    prevEnd._position, trk._position, _rangeAziData);
                            
                            // set the distance and the cumulative distance
                            trk._dist.set(
                                    _rangeAziData._rangeMtrs, Distance.DISTANCE_TYPE.METRES);
                            trk._distCumulative.copy(prevEnd._distCumulative);
                            trk._distCumulative.add(
                                    _rangeAziData._rangeMtrs, Distance.DISTANCE_TYPE.METRES);
                        }
                        catch(final Exception e)
                        {
                            android.util.Log.e(
                                    this.getClass().getSimpleName(),
                                    "add exception: " + e.toString());
                        }
                    }
                    
                    invalidate();
                }
            }
        }
    }
    
    /**
     * Adds new pointer of interet data to the instance.
     * 
     * @param   data   New track data.
     */
    public void add(final PoiData data)
    {
        _poiData.add(new PoiData(data));
    }
    
    @Override
    protected void doZoom(final MOTION_TYPE motion, final boolean isHorizMotion)
    {
        switch (motion)
        {
            case PINCH_CLOSE:
            {
                if (_zoomScaleFactorExp < _maxZoomScaleFactorExp)
                {
                    _zoomScaleFactorExp++;
                }
                break;
            }
            case PINCH_OPEN:
            {
                if (_zoomScaleFactorExp > _minZoomScaleFactorExp)
                {
                    _zoomScaleFactorExp--;
                }
                else if(_zoomScaleFactorExp > _absMinZoomScaleFactorExp)
                {
                    _zoomScaleFactorExp--;
                }
                break;
            }
            default:
            {
                // intentionally left blank
            }
        }
        
        //_zoomScaleFactorExp = Math.max(_zoomScaleFactorExp,0);
        
        _xAxisScale         = (float)Math.pow(_zoomScaleFactor, 
                                              _zoomScaleFactorExp);
        
        _xAxisScale *= _xAxisScaleUnitMapping;
        
        // assume that the the x-axis is exactly _xAxisScale units but the y-axis is
        // some fraction of it
        _yAxisScale = (float)(
                (_maxYCoord-_minYCoord)/(_maxXCoord-_minXCoord)*_xAxisScale);
        
        // adjust the grid line spacing
        final double minRange = Math.min(_maxXCoord-_minXCoord,
                _maxYCoord-_minYCoord);
        
        if (_zoomScaleFactorExp>=0)
        {
            _xAxisGridLineSpacing = minRange/_xAxisScale*MIN_ALLOWED_SCALE_MTRS;
            _gridPaint.setColor(_gridColour);
        }
        else
        {
            _xAxisGridLineSpacing = minRange/_xAxisScale*ABS_MIN_ALLOWED_SCALE_MTRS;
            _gridPaint.setColor(_extraZoomedInGridColour);
        }
        _yAxisGridLineSpacing = _xAxisGridLineSpacing;

        invalidate();
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

        //
        // Set dimensions for plot
        //
        // Account for padding
        float xpad = (float) (getPaddingLeft() + getPaddingRight())+
                _yAxisBounds.width();
        float ypad = getPaddingTop() + getPaddingBottom();

        float ww = w - xpad;
        float hh = h - ypad;
        
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
        
        final float width  = _bgBounds.width();
        final float height = _bgBounds.height();
        
        // note that the x and y axes have the same scale value since the plot
        // is for position
        _yAxisScale = _xAxisScale;
        
        final float paddingLeft = (float)getPaddingLeft() + _yAxisBounds.width();
        final float paddingTop  = getPaddingTop();
        
        _minXCoord = paddingLeft;
        _maxXCoord = _minXCoord+width;
        
        _minYCoord = paddingTop;
        _maxYCoord = _minYCoord+height;

        _originXCoord = (_maxXCoord+_minXCoord)/2.0f;
        _originYCoord = (_maxYCoord+_minYCoord)/2.0f;
        
        // set the initial reference coordinates for the grid and the spacing
        if (!_refLineCoordsSet)
        {
            _refLineCoordsSet     = true;
            _refVertLineXCoord    = _minXCoord;
            _refHorizLineYCoord   = _minYCoord;
            
            // we assume that on startup, the plot has been zoomed in 
            // to the finest "resolution" and so the smallest axis will
            // determine the spacing between one grid line (which will
            // correspond to _xAxisScale units)
            final double minRange = Math.min(_maxXCoord-_minXCoord,
                                             _maxYCoord-_minYCoord);
            _xAxisGridLineSpacing = minRange/_xAxisScale*_xAxisScaleUnitMapping;
            _yAxisGridLineSpacing = _xAxisGridLineSpacing;
        }
        
        if (_drawGrid)
        {
            drawGrid(canvas);
        }
        
        // draw the track data if we have at least two
        // tracks
        if (!_plotData.isEmpty())
        {
            // grab the most recent/latest track
            final TrackData latestTrk = (TrackData)_endPlotObj;

            TrackData prevTrk   = null;

            // set the origin if self has been set to the origin
            if (_isSelfPlotOrigin)
            {
                _plotOrigin.copy(latestTrk._position);
            }
            
            boolean atFirstTrack    = true;
            
            final double minColourTimeSecs = 
                    _minTimestampOfInterest.get(Time.TIME_TYPE.SECONDS);
            final double maxColourTimeSecs = 
                    _maxTimestampOfInterest.get(Time.TIME_TYPE.SECONDS);
            
            final int numElems = _plotData.size();
            
            for(int i=0;i<numElems;++i)
            {
                final TrackData trk = (TrackData)_plotData.get(i);
                
                boolean doColourForTime = false;
                
                // if there is no next track, then we are at the most recent one
                if(i==(numElems-1) && _isSelfPlotOrigin)
                {
                    // the origin position is in the middle of the plot
                    latestTrk._coord.x = (float)_originXCoord;
                    latestTrk._coord.y = (float)_originYCoord;
                    latestTrk._isDisplayed = true;
                    
                    // choose the colour for the track
                    if (latestTrk._isHooked)
                    {
                        _dataPaint.setColor(_hookedColour);
                        _crsPaint.setColor(_hookedColour);
                    }
                    else if (latestTrk._deadReckoned)
                    {
                        _dataPaint.setColor(_deadReckColour);
                        _crsPaint.setColor(_deadReckColour);
                    }
                    else
                    {
                        if (_plotData.size()==1)
                        {
                            _dataPaint.setColor(_departureColour);
                        }
                        else
                        {
                            _dataPaint.setColor(_dataColour);
                            _crsPaint.setColor(_dataColour);
                        }
                    }
                    
                    // replace the colour if the colour for time is enabled
                    if (_colourForTime)
                    {
                        final double trkTimestampSecs = 
                                latestTrk._timestamp.get(Time.TIME_TYPE.SECONDS);
                        
                        if (trkTimestampSecs>=minColourTimeSecs && 
                                trkTimestampSecs<=maxColourTimeSecs)
                        {
                            _dataPaint.setColor(_colourForTimeColour);
                            _crsPaint.setColor(_colourForTimeColour);
                        }
                    }
                    
                    // draw an X for the departure position
                    if (_plotData.size()==1)
                    {
                        drawPositionX(latestTrk,_semiPosSqDiagnonalLen*1.5,_dataPaint,canvas);
                    }
                    else
                    {
                        drawPositionSquare(latestTrk,_semiPosSqDiagnonalLen,_dataPaint,canvas);
                        drawAngledLine(latestTrk,latestTrk._heading,_headingLineLen,_crsPaint,canvas);
                    }
                }
                else
                {
                    // determine the range and bearing from the origin
                    try
                    {
                        Vincenty.GetRangeAzimuth(_plotOrigin, trk._position, _rangeAziData);
                        double trkX = _rangeAziData._rangeMtrs*Math.sin(_rangeAziData._fwdAzimuthRads);
                        double trkY = _rangeAziData._rangeMtrs*Math.cos(_rangeAziData._fwdAzimuthRads);
          
                        trkX = _originXCoord + trkX*(_maxXCoord-_minXCoord)/_xAxisScale;
                        trkY = _originYCoord - trkY*(_maxYCoord-_minYCoord)/_yAxisScale;
                        
                        trk._coord.x = (float)trkX;
                        trk._coord.y = (float)trkY;

                        trk._isDisplayed = false;

                        // choose the colour for the track
                        if (trk._isHooked)
                        {
                            _dataPaint.setColor(_hookedColour);
                            _crsPaint.setColor(_hookedColour);
                        }
                        else if(trk._deadReckoned)
                        {
                            _dataPaint.setColor(_deadReckColour);
                            _crsPaint.setColor(_deadReckColour);
                        }
                        else
                        {
                            if (atFirstTrack)
                            {
                                _dataPaint.setColor(_departureColour);
                            }
                            else
                            {
                                _dataPaint.setColor(_dataColour);
                                _crsPaint.setColor(_dataColour);
                            }
                        }

                        // replace the colour if the colour for time is enabled
                        // and the track is not hooked
                        if (_colourForTime)
                        {
                            final double trkTimestampSecs = 
                                    trk._timestamp.get(Time.TIME_TYPE.SECONDS);
                            
                            if (trkTimestampSecs>=minColourTimeSecs && 
                                    trkTimestampSecs<=maxColourTimeSecs)
                            {
                                if (!trk._isHooked)
                                {
                                    _dataPaint.setColor(_colourForTimeColour);
                                    _crsPaint.setColor(_colourForTimeColour);
                                }
                                doColourForTime = true;
                            }
                        }
                        
                        // draw an X for the departure position
                        if (atFirstTrack)
                        {
                            atFirstTrack = false;
                            drawPositionX(trk,_semiPosSqDiagnonalLen*1.5,_dataPaint,canvas);
                        }
                        else
                        {
                            if (i==(numElems-1))
                            {
                                drawPositionSquare(trk,_semiPosSqDiagnonalLen,_dataPaint,canvas);
                                drawAngledLine(trk,trk._heading,_headingLineLen,_crsPaint,canvas);
                            }
                            else if (_drawHistory)
                            {
                                drawPositionSquare(trk,_semiPosSqDiagnonalLen,_dataPaint,canvas);
                            } 
                        }
                    }
                    catch(final Exception e)
                    {
                        android.util.Log.e(
                                this.getClass().getSimpleName(),
                                "onDraw exception: " + e.toString());
                    }
                }
                
                try
                {
                    if (prevTrk != null)
                    {
                        // draw a line from the previous track to this one if enabled
                        if (_connectTheDots && _drawHistory)
                        {
                            // only reset the colour if not already the colour for time
                            if(doColourForTime)
                            {
                                _dataPaint.setColor(_colourForTimeColour);
                            }
                            drawLine(prevTrk,trk,_dataPaint,canvas);
                        }
                    }
                    else
                    {
                        trk._dist.set(0.0);
                        trk._distCumulative.set(0.0);
                    }
                    
                    prevTrk = trk;
                }
                catch(final Exception e)
                {
                    android.util.Log.e(
                            this.getClass().getSimpleName(),
                            "onDraw exception: " + e.toString());
                }
            }
        }
        
        if (_drawPois)
        {
            drawPois(canvas);
        }
        
        // set the boundaries about the origin
        setBoundaries(_plotOrigin);
    }
    
    @Override
    public void setTimestampRange(final Time minTimestamp, final Time maxTimestamp)
    {
        _minTimestampOfInterest.copy(minTimestamp);
        _maxTimestampOfInterest.copy(maxTimestamp);
        invalidate();
    }
    
    /**
     * Draws the point of interests (POIs) on the plot given the minimum and
     * maximum latitude and longitude bounds for the plot.
     * 
     * @param   canvas   The canvas to draw the POIs on.
     */
    private void drawPois(Canvas canvas)
    {
        try
        {            
            final double minLatDegs = _plotMinLatitude.get(Angle.ANGLE_TYPE.DEGREES);
            final double maxLatDegs = _plotMaxLatitude.get(Angle.ANGLE_TYPE.DEGREES);
            final double minLonDegs = _plotMinLongitude.get(Angle.ANGLE_TYPE.DEGREES);
            final double maxLonDegs = _plotMaxLongitude.get(Angle.ANGLE_TYPE.DEGREES);
            
            for(int i=0;i<_poiData.size();++i)
            {
                final PoiData data = (PoiData)_poiData.get(i);
                
                final double poiLatDegs = 
                        data._position.getLatitude(Position.POS_VALUE_TYPE.DEGREES);
                final double poiLonDegs = 
                        data._position.getLongitude(Position.POS_VALUE_TYPE.DEGREES);
                
                if(poiLatDegs > minLatDegs && poiLatDegs < maxLatDegs && 
                        poiLonDegs > minLonDegs && poiLonDegs < maxLonDegs)
                {
                    Vincenty.GetRangeAzimuth(_plotOrigin, data._position, _rangeAziData);
                    double x = _rangeAziData._rangeMtrs*Math.sin(_rangeAziData._fwdAzimuthRads);
                    double y = _rangeAziData._rangeMtrs*Math.cos(_rangeAziData._fwdAzimuthRads);

                    x = _originXCoord + x*(_maxXCoord-_minXCoord)/_xAxisScale;
                    y = _originYCoord - y*(_maxYCoord-_minYCoord)/_yAxisScale;
                    
                    data._coord.x = (float)x;
                    data._coord.y = (float)y;

                    data._isDisplayed = true;
                    
                    if (data._isHooked)
                    {
                        _poiPaint.setColor(_hookedColour);
                    }
                    else
                    {
                        _poiPaint.setColor(_poiColour);
                    }
                    
                    canvas.drawCircle(
                            data._coord.x, data._coord.y, (float)_semiPosSqDiagnonalLen, _poiPaint);
                }
                
            }
        }
        catch(final Exception e)
        {
            android.util.Log.e(
                    this.getClass().getSimpleName(),
                    "drawPois exception: " + e.toString());
        }
    }
    
    @Override
    protected void handleVerticalAxisTouch(final double xCoord, 
                                           final double yCoord)
    {
        // intentionally left blank
    }
    
    @Override
    protected void dispHookedDataMsg()
    {
        // determine the hooked class type and display as appropriate
        if (_hookedObj.getClass().getSimpleName().equals("TrackData"))
        {
            dispHookedDataMsg((TrackData)_hookedObj);
        }
        else if (_hookedObj.getClass().getSimpleName().equals("PoiData"))
        {
            dispHookedDataMsg((PoiData)_hookedObj);
        }
    }
    
    /**
     * Displays the hooked track message.
     * 
     * @param   trk   The track that has been hooked.
     */
    private void dispHookedDataMsg(final TrackData trk)
    {
        trk._isHooked = true;
        
        if (_hookedDataToast != null)
        {
            // close the message in case it is already showing
            _hookedDataToast.cancel();
        }

        CharSequence message = "";
        message = trk._position.toString() + "\n" + 
                  StringUtils.Pad(trk._heading.get(
                          Angle.ANGLE_TYPE.DEGREES_POS),3,0) + '\u00B0' + "/" +
                  StringUtils.Pad(trk._speed.get(
                          _speedUnits), 0, 2) + " " + 
                          Speed.ToStringAbbrev(_speedUnits) +"\n" +
                  StringUtils.Pad(trk._dist.get(
                          _distanceUnits), 0, 2) +"/" +
                  StringUtils.Pad(trk._distCumulative.get
                          (_distanceUnits), 0, 2) + " " + 
                          Distance.ToStringAbbrev(_distanceUnits) +"\n" +
                  StringUtils.toString(trk._timestamp.get(
                          Time.TIME_TYPE.MILLISECONDS), 
                          StringUtils.DATETIME_FORMAT_TYPE.HH_MM_SS);

        _hookedDataToast = Toast.makeText(_context, message, Toast.LENGTH_LONG);
        _hookedDataToast.setGravity(Gravity.TOP | Gravity.LEFT, 
                                    this.getLeft()+(int)trk._coord.x,
                                    this.getTop()+(int)trk._coord.y);
        _hookedDataToast.show();
    }
    
    /**
     * Displays the hooked POI message.
     * 
     * @param   poi   The POI that has been hooked.
     */
    private void dispHookedDataMsg(final PoiData poi)
    {
        poi._isHooked = true;
        
        if (_hookedDataToast != null)
        {
            // close the message in case it is already showing
            _hookedDataToast.cancel();
        }

        CharSequence message = "";
        message =
                poi._name + "\n" +
                poi._position.toString();

        _hookedDataToast = Toast.makeText(_context, message, Toast.LENGTH_LONG);
        _hookedDataToast.setGravity(Gravity.TOP | Gravity.LEFT, 
                                    this.getLeft()+(int)poi._coord.x,
                                    this.getTop()+(int)poi._coord.y);
        _hookedDataToast.show();
    }
    
    @Override
    protected void handleSwipe(final double deltaX, final double deltaY)
    {
        final double diffXMetres = deltaX*_xAxisScale/(_maxXCoord-_minXCoord);
        final double diffYMetres = deltaY*_yAxisScale/(_maxYCoord-_minYCoord);
        
        // calculate the range from the previous position to the current position
        final double rangeMtrs = Math.sqrt(diffXMetres*diffXMetres + 
                diffYMetres*diffYMetres);

        // calculate the bearing from the current position to the previous position
        double bearingRads = Math.atan2(diffXMetres,diffYMetres);
        
        try
        {
            Vincenty.GetLatLon(_plotOrigin, rangeMtrs, bearingRads, _latLonData);
            _plotOrigin.copy(_latLonData._position);
            
            // shift the grid lines
            _refVertLineXCoord  -= deltaX;
            _refHorizLineYCoord += deltaY;
            
            invalidate();
        }
        catch(final Exception e)
        {
            // do nothing; leave the origin as before
        }

        _isSelfPlotOrigin = false;
    }
    
    /**
     * Invokes the hooking operation which can be overidden by derived classes.
     * 
     * @param   xCoord       The pressed x-coordinate within the plot.
     * @param   yCoord       The pressed y-coordinate within the plot.
     */
    @Override
    protected void doHookOperation(final float xCoord, final float yCoord)
    {
        boolean isObjHooked      = false;
        boolean wasPrevObjHooked = (_hookedObj != null);
        
        // unhook any previously hooked object
        if (wasPrevObjHooked)
        {
            _hookedObj._isHooked = false;
            _hookedObj           = null;
        }
        
        // attempt to hook the POIs first
        if (_drawPois)
        {
            isObjHooked = hookData(_poiData,xCoord,yCoord);
        }
        
        // attempt to hook an object in the data list
        isObjHooked = hookData(_plotData,xCoord,yCoord);
        
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
     * Resets the instance to its default state, clearing all lists etc.
     * 
     * @warning   Grid and zoom factors (if any exist) remain as is.
     */
    @Override
    public void reset()
    {
        super.reset();
        
        _isSelfPlotOrigin = true;
        _drawHistory      = true;
    }
    
    /**
     * Sets the plot boundary latitude and longtiudes given the origin.
     * 
     * @param   originPos   The latitude and longitude position origin of the
     *                      plot.
     */
    private void setBoundaries(final Position originPos)
    {
        // calculate the range from the origin to the east-west and north-
        // south boundaries
        final double eastWestRangeMtrs   = _xAxisScale/2.0;
        final double northSouthRangeMtrs = _yAxisScale/2.0;
        
        try
        {
            // calculate the west boundary longitude
            Vincenty.GetLatLon(originPos, eastWestRangeMtrs, -Math.PI/2.0, _latLonData);
            _plotMinLongitude.set(
                    _latLonData._position.getLongitude(Position.POS_VALUE_TYPE.RADIANS));
            
            // calculate the east boundary longitude
            Vincenty.GetLatLon(originPos, eastWestRangeMtrs, Math.PI/2.0, _latLonData);
            _plotMaxLongitude.set(
                    _latLonData._position.getLongitude(Position.POS_VALUE_TYPE.RADIANS));
            
            // calculate the north boundary latitude
            Vincenty.GetLatLon(originPos, northSouthRangeMtrs, Math.PI, _latLonData);
            _plotMinLatitude.set(
                    _latLonData._position.getLatitude(Position.POS_VALUE_TYPE.RADIANS));
            
            // calculate the south boundary latitude
            Vincenty.GetLatLon(originPos, northSouthRangeMtrs, 0.0, _latLonData);
            _plotMaxLatitude.set(
                    _latLonData._position.getLatitude(Position.POS_VALUE_TYPE.RADIANS));
        }
        catch(final Exception e)
        {
            // clear all boundaries
            _plotMinLongitude.set(0.0);
            _plotMaxLongitude.set(0.0);
            _plotMinLatitude.set(0.0);
            _plotMaxLatitude.set(0.0);
            
            android.util.Log.e(
                    this.getClass().getSimpleName(),
                    "setBoundaries exception: " + e.toString());
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
    
    /**
     * Initialize data members with default values.
     */
    @Override
    protected void init(Context context)
    {
        super.init(context);
        
        // set booleans and override where necessaru
        _drawGrid       = true;
        _drawHistory    = true;
        _connectTheDots = false; 
        _colourForTime  = false;
        
        // set and override colours where necessary
        _departureColour         = Color.MAGENTA;
        _dataColour              = Color.GREEN;
        _hookedColour            = Color.WHITE;
        _deadReckColour          = Color.CYAN;
        _poiColour               = Color.YELLOW;
        _extraZoomedInGridColour = Color.DKGRAY;
        _colourForTimeColour     = Color.RED;

        // set the style and colour
        _dataPaint.setStyle(Paint.Style.FILL);
        _dataPaint.setColor(_dataColour);
        
        _crsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        _crsPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        _crsPaint.setColor(_dataColour);
        _crsPaint.setStrokeWidth(2.5f);
        
        _poiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        _poiPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        _poiPaint.setColor(_poiColour);

        // get the size of the text for a default string along the y-axis
        _yAxisTextPaint.getTextBounds("-000"+'\u00B0', 0, 5, _yAxisBounds);
        
        // set the zoom scale factors
        _zoomScaleFactorExp    = 0;
        _zoomScaleFactor       = DEFAULT_ZOOM_SCALE_BASE;
        _absMinZoomScaleFactorExp = -42;
        _minZoomScaleFactorExp    = 0;
        _maxZoomScaleFactorExp    = 81;
        
        // override the axis scaling members
        _xAxisScale = MIN_ALLOWED_SCALE_MTRS;
        _yAxisScale = MIN_ALLOWED_SCALE_MTRS;
        _xAxisScaleUnitMapping = MIN_ALLOWED_SCALE_MTRS;
        _yAxisScaleUnitMapping = MIN_ALLOWED_SCALE_MTRS;
        
        _rangeAziData = new Vincenty.GetRangeAzimuthData();
        _latLonData   = new Vincenty.GetLatLonData();
        
        // initialize the origin and boundaries
        _isSelfPlotOrigin = true;
        _plotOrigin       = new Position();
        _plotMinLatitude  = new Angle();
        _plotMaxLatitude  = new Angle();
        _plotMinLongitude = new Angle();
        _plotMaxLongitude = new Angle();
        
        // default the speed and distance types
        _distanceUnits = Distance.DISTANCE_TYPE.METRES;
        _speedUnits    = Speed.SPEED_TYPE.METRES_PER_SEC;
        
        // override the semi-length of the diagonal of the position square drawn 
        // on the plot
        _semiPosSqDiagnonalLen = 5.0;
        
        // set the length of the course line
        _headingLineLen = 3.0*_semiPosSqDiagnonalLen;
        
        // initialize the POI data list
        _poiData     = new Vector<Plot2DData>();
        
        // initialize the time range of interest bounds
        _minTimestampOfInterest = new Time();
        _maxTimestampOfInterest = new Time();
    }
    
    //! Colour for the departure position.
    private int _departureColour;
    
    //! Colour for a dead-reckoned data object.
    private int _deadReckColour;
    
    //! Colour for a POI data object.
    private int _poiColour;
    
    //! Colour for data within the timestamp range.
    private int _colourForTimeColour;
    
    //! Colour for extra zoomed in grid lines.
    private int _extraZoomedInGridColour;
    
    //! Indicator on whether to draw history points or not on the plot.
    private boolean _drawHistory;
    
    //! Indicator on whether to connect the history points or not on the plot
    private boolean _connectTheDots;
    
    //! Indicator on whether to draw the POIs or not on the plot.
    private boolean _drawPois;
    
    //! Indicator on whether to colour data given the timestamp range.
    private boolean _colourForTime;

    //! Paint object used for drawing the course line on the canvas.
    private Paint _crsPaint;
    
    //! Paint object used for drawing the POIs on the canvas.
    private Paint _poiPaint;

    //! Vincenty data object for range and azimuth.
    private Vincenty.GetRangeAzimuthData _rangeAziData;
    
    //! Vincenty data object for a new position relative to another.
    private Vincenty.GetLatLonData _latLonData;
    
    //! Indicates whether the self/user should be the origin on the plot.
    private boolean _isSelfPlotOrigin;
    
    //! The latitude and longitude origin position.
    private Position _plotOrigin;
    
    //! The plot minimum latitude.
    private Angle _plotMinLatitude;
    
    //! The plot maximum latitude.
    private Angle _plotMaxLatitude;
    
    //! The plot minimum longitude.
    private Angle _plotMinLongitude;
    
    //! The plot maximum longitude.
    private Angle _plotMaxLongitude;

    //! The length of the heading line drawn from the position square.
    private double _headingLineLen;
    
    //! Zoom scale factor exponent.
    private int _zoomScaleFactorExp;
    
    //! X-axis scale factor mapping from the unitless to the desired units.
    private float _xAxisScaleUnitMapping;
    
    //! Y-axis scale factor mapping from the unitless to the desired units.
    private float _yAxisScaleUnitMapping;
    
    //! Minimum allowed zoom scale factor exponent.
    private int _minZoomScaleFactorExp;
    
    //! Absolute minimum allowed zoom scale factor exponent.
    private int _absMinZoomScaleFactorExp;
    
    //! Maximum allowed zoom scale factor exponent.
    private int _maxZoomScaleFactorExp;
    
    //! The selected distance units for the plot and hooked data.
    private Distance.DISTANCE_TYPE _distanceUnits;
    
    //! The selected speed units for the plot and hooked data (tied to _distanceType).
    private Speed.SPEED_TYPE _speedUnits;
    
    //! List of point of interest data objects.
    private Vector<Plot2DData> _poiData;
    
    //! Minimum timestamp for the data range of interest.
    private Time _minTimestampOfInterest;
    
    //! Maximum timestamp for the data range of interest.
    private Time _maxTimestampOfInterest;
 
    //! Maximum history kept in seconds.
    private static final double MAX_HISTORY_SECS = 6.0*3600.0;
    
    //! Absolute minimum allowed scale value.
    private static final float ABS_MIN_ALLOWED_SCALE_MTRS = 125.0f;
    
    //! Minimum allowed scale value.
    private static final float MIN_ALLOWED_SCALE_MTRS = 1000.0f;
    
    //! Default zoom scale factor base.
    private static final double DEFAULT_ZOOM_SCALE_BASE = 1.05;
}
