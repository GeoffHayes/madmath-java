/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package that contains custom Android views.
 */
package com.madmath.views;


import com.madmath.measures.Time;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Abstract class for 2D plots that have a time axis along the x-axis.  This view
 * is composed of a 2D plot and surrounding area used for labels along the x and
 * y axes.
 *
 */
public abstract class Plot2DXAxisTimeView extends Plot2DView
{
    /**
     * Class constructor.
     * 
     * @param   context   Information to global information about application
     *                    environment.
     */
    public Plot2DXAxisTimeView(Context context)
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
    public Plot2DXAxisTimeView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        
        _context = context;
    }
    
    /**
     * Synchronizes the x-axis to the new timestamps.  Forces a redraw of the
     * plot.
     * 
     * @param   minTime        The minimum timestamp for the x-axis.
     * @param   maxTime        The maximum timestamp for the x-axis.
     * @param   vertRefCoord   The vertical line reference coordinate.
     * @param   inHistMode     Indicates if the view should be in history mode (true)
     *                         or not (false).
     */
    public void synchXAxis(
            final Time minTime, 
            final Time maxTime, 
            final double vertRefCoord, 
            final boolean inHistMode)
    {
        _xAxisMinTimestamp.copy(minTime);
        _xAxisMaxTimestamp.copy(maxTime);
        _inHistoryMode = inHistMode;
        
        _xAxisNumSecondsDisplayed = _xAxisMaxTimestamp.get(Time.TIME_TYPE.SECONDS) -
                _xAxisMinTimestamp.get(Time.TIME_TYPE.SECONDS);
        
        final double xAxisRange = _maxXCoord-_minXCoord;
        
        _xAxisGridLineSpacing = xAxisRange/(_xAxisMaxTimestamp.get(Time.TIME_TYPE.SECONDS)-
                _xAxisMinTimestamp.get(Time.TIME_TYPE.SECONDS))*
                _xAxisMinTimeSecondsDisp;
        
        _refVertLineXCoord = vertRefCoord;
        
        invalidate();
        
        // time synch with partner views
        timeUpdatePartnerViews(false);
    }
    
    /**
     * Update the minimum and maximum timestamps for partner views.
     * 
     * @param   passToXaxisViews  Indicates if the time update should be passed
     *                            to other instances of the Plot2DAxisTimeView 
     *                            class (true) or not (false).
     */
    protected void timeUpdatePartnerViews(final boolean passToXaxisViews)
    {
        for(int i=0;i<_partnerViews.size();++i)
        {
            final Plot2DView vw = _partnerViews.get(i);
            
            if (vw instanceof Plot2DXAxisTimeView && passToXaxisViews)
            {
                final Plot2DXAxisTimeView timeVw = 
                        (Plot2DXAxisTimeView)vw;
                
                timeVw.synchXAxis(
                        _xAxisMinTimestamp, _xAxisMaxTimestamp, 
                        _refVertLineXCoord, _inHistoryMode);
            }
            else if (vw instanceof Plot2DPositionView)
            {
                vw.setTimestampRange(_xAxisMinTimestamp, _xAxisMaxTimestamp);
            }
        }
    }
  
    /**
     * Initialize data members with default values.
     */
    @Override
    protected void init(Context context)
    {
        super.init(context);
        
        // initialize time related objects for the x-axis
        _xAxisMinTimestamp = new Time();
        _xAxisMaxTimestamp = new Time();

        // get the size of the text for a default string along the x-axis
        _xAxisTextPaint.getTextBounds("00:00:00", 0, 8, _xAxisBounds);  
        
        // disable history mode (used when scrolling back in time)
        _inHistoryMode = false;
        
        // set the min and max allowed seconds to display along the x-axis
        _xAxisMinTimeSecondsDisp = MIN_ALLOWED_SECONDS_DISPLAYED;
        _xAxisMaxTimeSecondsDisp = MAX_ALLOWED_SECONDS_DISPLAYED;
        
        // default the number of seconds to display along the x-axis
        _xAxisNumSecondsDisplayed = DEFAULT_NUM_SECONDS_DISPLAYED;
    }
    
    //! The left (oldest) timestamp on the plot.
    protected Time _xAxisMinTimestamp;
    
    //! The right (newest) timestamp on the plot.
    protected Time _xAxisMaxTimestamp;
    
    //! Indicates whether the view is in history mode (and so user is swiping back in time).
    protected boolean _inHistoryMode;
    
    //! Indicates the minimum number of seconds to display along the x-axis.
    protected double _xAxisMinTimeSecondsDisp;
    
    //! Indicates the maximum number of seconds to display along the x-axis.
    protected double _xAxisMaxTimeSecondsDisp;
    
    //! The number of seconds displayed along the x-axis.
    protected double _xAxisNumSecondsDisplayed;
    
    //! Minimum allowed number of seconds of data to be shown along x-axis of plot.
    private static final double MIN_ALLOWED_SECONDS_DISPLAYED = 60.0;
    
    //! Maximum number of seconds shown in the view.
    private static final double DEFAULT_NUM_SECONDS_DISPLAYED = 10.0*60.0;
    
    //! Maximum allowed number of seconds of data to be shown along x-axis of plot.
    private static final double MAX_ALLOWED_SECONDS_DISPLAYED = 1.5*3600.0;
}
