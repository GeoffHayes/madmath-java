/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage the SelfTrackerApp activity and supporting classes.
 */
package com.madmath.selftrackerapp;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Xml;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
 
import com.madmath.selftrackerapp.R;
import com.madmath.geocoordinates.Position;
import com.madmath.math.Matrix;
import com.madmath.measures.Angle;
import com.madmath.measures.Distance;
import com.madmath.measures.Speed;
import com.madmath.measures.Time;
import com.madmath.tracking.KalmanPredictFilter;
import com.madmath.tracking.KalmanUpdateFilter;
import com.madmath.tracking.Observation2DP;
import com.madmath.tracking.Track2DPV;
import com.madmath.utilities.StringUtils;
import com.madmath.views.Plot2DBearingView;
import com.madmath.views.Plot2DPositionView;
import com.madmath.views.Plot2DSpeedView;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.Vector;

import org.xmlpull.v1.XmlPullParser;

//! Class to encapsulate the activity for the Self Tracker application.
/**
 * An activity is a single, focused thing that the user can do. Almost all 
 * activities interact with the user, so the Activity class takes care of 
 * creating a window for you in which you can place your UI with
 * setContentView(View). While activities are often presented to the user as 
 * full-screen windows, they can also be used in other ways: as floating 
 * windows (via a theme with windowIsFloating set) or embedded inside of another
 * activity (using ActivityGroup). There are two methods almost all subclasses 
 * of Activity will implement:
 * 
 *      onCreate(Bundle) is where you initialize your activity. Most importantly, 
 * 	    here you will usually call setContentView(int) with a layout resource 
 * 	    defining your UI, and using findViewById(int) to retrieve the widgets in 
 *      that UI that you need to interact with programmatically.
 *
 *      onPause() is where you deal with the user leaving your activity. Most 
 *      importantly, any changes made by the user should at this point be 
 *      committed (usually to the ContentProvider holding the data).  To be of 
 *      use with Context.startActivity(), all activity classes must have a 
 *      corresponding <activity> declaration in their package's 
 *      AndroidManifest.xml.
 *
 *      See http://developer.android.com/reference/android/app/Activity.html for
 *      details.
 */
public class SelfTrackerActivity extends Activity 
{
    /**
     * Class to handle configuration data.
     */
    public class AppConfigData 
    {
        /**
         * Class constructor.
         */
        public AppConfigData()
        {
            _updateRateMSec         = DEFAULT_UPDATE_RATE_MSEC;
            _displayRateMSec        = DEFAULT_DISPLAY_RATE_MSEC;
            _useLocPosnAccuracy     = true;
            _obsPosnAccuracyMtrs    = DEFAULT_OBS_POSN_ACC_MTRS;
            _trkVeloAccuracyMtrsSec = DEFAULT_TRK_VELO_ACC_MTRSEC;
            _trkProcessNoise        = 0.0;
            
        }
        //! The (observation) update rate (milliseconds).
        public long _updateRateMSec;
        
        //! The (track) display update rate (milliseconds) on the UI.
        public long _displayRateMSec;
        
        //! Indicator as to whether to use the Location position accuracy or not for an observation.
        public boolean _useLocPosnAccuracy;
        
        //! The observation position accuracy (metres) if _useLocaPosnAccuracy is false or in demo mode.
        public double _obsPosnAccuracyMtrs;
        
        //! The track velocity accuracy (metres per second).
        public double _trkVeloAccuracyMtrsSec;
        
        //! The track prediction process noise (unitless).
        public double _trkProcessNoise;

        //! Default tracker update rate (milliseconds).
        private static final long DEFAULT_UPDATE_RATE_MSEC = 5000;
        
        //! Default tracker display rate (milliseconds).
        private static final long DEFAULT_DISPLAY_RATE_MSEC = 10000;
        
        //! Default track velocity accuracy (metres per second).
        private static final double DEFAULT_TRK_VELO_ACC_MTRSEC = 1.5;
        
        //! Default observation position accuracy (metres).
        private static final double DEFAULT_OBS_POSN_ACC_MTRS = 50.0;
    };
    
    /**
     * Class constructor; initializes data members specific to this class.
     */
    public SelfTrackerActivity ()
    {
        // invoke the base class constructor
        super();
        
        // initialize data members that are specific to this base class
        _mode          = APP_MODE.STOP;
        _appConfigData = new AppConfigData();
        
        _obsList = new Vector<Observation2DP>(
                VECTOR_INITIAL_CAPACITY, VECTOR_CAPACITY_INCREMENT);
        _trkList = new Vector<Track2DPV>(
                VECTOR_INITIAL_CAPACITY, VECTOR_CAPACITY_INCREMENT);
        
        _dstUtil  = new Distance();
        _spdUtil  = new Speed();
        _timeUtil = new Time();
        _brgUtil  = new Angle();
        
        // instantiate the data "messages" used for publishing to the views
        _publishedSpdData = new Plot2DSpeedView.SpeedData();
        _publishedBrgData = new Plot2DBearingView.BearingData();
        _publishedTrkData = new Plot2DPositionView.TrackData();
    }
    
    /**
     * Responds to a click of the start action bar menu item.
     * 
     * @param   menu   Reference to action bar start menu item.
     */
    public boolean onStartActionItemClick(MenuItem menu)
    {
        _mode = APP_MODE.START;
        
        invalidateOptionsMenu();
        
        try
        {
            // default the state of the runnable
            _genObsRunnable.startUp();
            
            // remove any update route runnable from the message queue
            _msgHandler.removeCallbacks(_genObsRunnable);
            
            // add the runnable with zero delay
            _msgHandler.postDelayed(_genObsRunnable, 0);
            
            // clear all tracks and observations
            _obsList.clear();
            _trkList.clear();
            
            // clear all utilities
            _dstUtil.set(0.0);
            _spdUtil.set(0.0);
            _timeUtil.set(0.0);
            _brgUtil.set(0.0);
            
            // clear data in all views
            final Plot2DPositionView vwPos = 
                    ((Plot2DPositionView) findViewById(R.id.plot_2D_position));
            
            if (vwPos != null)
            {
                vwPos.reset();
            }

            final Plot2DSpeedView vwSpd = 
                    ((Plot2DSpeedView) findViewById(R.id.plot_2D_speed));
            
            if (vwSpd != null)
            {
                vwSpd.reset();
            }
            
            final Plot2DBearingView vwBrg = 
                    ((Plot2DBearingView) findViewById(R.id.plot_2D_bearing));
            
            if (vwBrg != null)
            {
                vwBrg.reset();
            }
        }
        catch (Exception e)
        {
            android.util.Log.e(TAG, "onStartActionItemClick - exception: " + e.toString());
        }
        
        return true;
        
    }

    /**
     * Responds to a click of the start action bar menu item.
     * 
     * @param   menu   Reference to action bar pause menu item.
     */
    public boolean onPauseActionItemClick(MenuItem menu)
    {
        if (_mode.equals(APP_MODE.START))
        {
            _mode = APP_MODE.PAUSE;
            
            menu.setTitle("RESUME");

            // remove any update route runnable from the message queue
            _msgHandler.removeCallbacks(_genObsRunnable);
            
        }
        else if(_mode.equals(APP_MODE.PAUSE))
        {
            _mode = APP_MODE.START;

            try
            {
                // remove any update route runnable from the message queue
                _msgHandler.removeCallbacks(_genObsRunnable);
                
                // add the runnable with zero delay
                _msgHandler.postDelayed(_genObsRunnable, 0);
                
                menu.setTitle("PAUSE");
            }
            catch (Exception e)
            {
                android.util.Log.e(TAG, "onPauseActionItemClick - exception: " + e.toString());
            }
            
        }
        else
        {
            android.util.Log.e(TAG,"onPauseActionItemClick - invalid mode");
        }
        
        return true;
    }
    
    /**
     * Responds to a click of the stop action bar menu item.
     * 
     * @param   menu   Reference to action bar start menu item.
     */
    public boolean onStopActionItemClick(MenuItem menu)
    {
        _mode = APP_MODE.STOP;
        
        _genObsRunnable.shutDown();
        
        _msgHandler.removeCallbacks(_genObsRunnable);
        
        invalidateOptionsMenu();
        
        return true;
    }
    
    /**
     * Responds to a click of the save action bar menu item.
     * 
     * @param   menu   Reference to action bar start menu item.
     */
    public boolean onSaveActionItemClick(MenuItem menu)
    {  
        try
        {
            final long currentTimeMSecs = System.currentTimeMillis();
            
            final String obsFilename = StringUtils.toString(
                    currentTimeMSecs, StringUtils.DATETIME_FORMAT_TYPE.YYYYMMDD_HHMMSS) + "_obs.dat";
            
            final String trkFilename = StringUtils.toString(
                    currentTimeMSecs, StringUtils.DATETIME_FORMAT_TYPE.YYYYMMDD_HHMMSS) + "_trk.dat";

            // get the public external storage directory for files saved via this activity
            File path = Environment.getExternalStoragePublicDirectory(DATA_DIR);
            
            // make sure the directory exists
            path.mkdirs();
            
            // write the track data to file
            writeTrackData(path, trkFilename);
            
            // write the observation data to file
            writeObservationData(path, obsFilename);
        }
        catch(final Exception e)
        {
            android.util.Log.e(TAG,"onSaveActionItemClick exception: " + e.toString());
        }

        return true;
    }
    
    /**
     * Writes the track data to file.
     */
    private void writeTrackData(final File path, final String filename)
    {
        try
        {
            // get the file to save
            File file = new File(path,filename);
            
            // get the output stream
            DataOutputStream os = new DataOutputStream(
                    new BufferedOutputStream(
                    new FileOutputStream(file)));
            
            Iterator<Track2DPV> iter = _trkList.iterator();
            
            while (iter.hasNext())
            {
                final Track2DPV data = iter.next();
                data.write(os);
            }

            os.close();
            
            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(this,
                    new String[] { file.toString() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                    android.util.Log.i("writeTrackData", "Scanned " + path + ":");
                    android.util.Log.i("writeTrackData", "-> uri=" + uri);
                }
            });
        }
        catch(final Exception e)
        {
            android.util.Log.e(TAG,"writeTrackData exception: " + e.toString());
        }
    }
    
    /**
     * Writes the observation data to file.
     */
    private void writeObservationData(final File path, final String filename)
    {
        try
        {
            // get the file to save
            File file = new File(path,filename);
            
            // get the output stream
            DataOutputStream os = new DataOutputStream(
                    new BufferedOutputStream(
                    new FileOutputStream(file)));
            
            Iterator<Observation2DP> iter = _obsList.iterator();
            
            while (iter.hasNext())
            {
                final Observation2DP data = iter.next();
                data.write(os);
            }

            os.close();
            
            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(this,
                    new String[] { file.toString() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                    android.util.Log.i("writeObservationData", "Scanned " + path + ":");
                    android.util.Log.i("writeObservationData", "-> uri=" + uri);
                }
            });
        }
        catch(final Exception e)
        {
            android.util.Log.e(TAG,"writeObservationData exception: " + e.toString());
        }
    }
    
    /**
     * Responds to a select of the 2D position plot top-level menu item.
     * 
     * @param   menu   Reference to menu item.
     */
    public boolean on2DPositionPlotSelect(MenuItem menu)
    {
        final Plot2DPositionView vw = 
                ((Plot2DPositionView) findViewById(R.id.plot_2D_position));
        
        if (vw != null)
        {
            switch(menu.getItemId())
            {
                case R.id.twodim_pos_mi_toggle_grid:
                {
                    vw.toggleDrawGrid();
                    break;
                }
                case R.id.twodim_pos_mi_self_centre:
                {
                    vw.setSelfCentre();
                    break;
                }
                case R.id.twodim_pos_mi_toggle_history:
                {
                    vw.toggleHistory();
                    break;
                }
                case R.id.twodim_pos_mi_toggle_connect:
                {
                    vw.toggleConnectTheDots();
                    break;
                }
                case R.id.twodim_pos_mi_toggle_poi:
                {
                    vw.toggleDrawPois();
                    break;
                }
                case R.id.twodim_pos_mi_toggle_colour_for_time:
                {
                    vw.toggleColourForTime();
                    break;
                }
                default:
                {
                    // intentionally left blank
                }
            }
        }
        
        return true;
    }
    
    /**
     * Responds to a select of the 2D speed plot top-level menu item.
     * 
     * @param   menu   Reference to menu item.
     */
    public boolean on2DSpeedPlotSelect(MenuItem menu)
    {
        final Plot2DSpeedView vw = 
                ((Plot2DSpeedView) findViewById(R.id.plot_2D_speed));
        
        if (vw != null)
        {
            switch(menu.getItemId())
            {
                case R.id.twodim_spd_mi_toggle_grid:
                {
                    vw.toggleDrawGrid();
                    break;
                }
                default:
                {
                    // intentionally left blank
                }
            }
        }
        
        return true;
    }
    
    /**
     * Responds to a select of the 2D bearing plot top-level menu item.
     * 
     * @param   menu   Reference to menu item.
     */
    public boolean on2DBearingPlotSelect(MenuItem menu)
    {
        final Plot2DBearingView vw = 
                ((Plot2DBearingView) findViewById(R.id.plot_2D_bearing));
        
        if (vw != null)
        {
            switch(menu.getItemId())
            {
                case R.id.twodim_brg_mi_toggle_grid:
                {
                    vw.toggleDrawGrid();
                    break;
                }
                default:
                {
                    // intentionally left blank
                }
            }
        }
        
        return true;
    }
    
    /**
     * Responds to a selection within the toggle units menu item.
     * 
     * @param   menu   Reference to menu item.
     */
    public boolean onToggleUnitsSelect(MenuItem menu)
    {

        Distance.DISTANCE_TYPE units = Distance.DISTANCE_TYPE.KILOMETRES;

        switch(menu.getItemId())
        {
            case R.id.st_actbar_mi_toggle_units_km:
            {
                units = Distance.DISTANCE_TYPE.KILOMETRES;
                break;
            }
            case R.id.st_actbar_mi_toggle_units_metres:
            {
                units = Distance.DISTANCE_TYPE.METRES;
                break;
            }
            case R.id.st_actbar_mi_toggle_units_miles:
            {
                units = Distance.DISTANCE_TYPE.MILES;
                break;
            }
            case R.id.st_actbar_mi_toggle_units_yards:
            {
                units = Distance.DISTANCE_TYPE.YARDS;
                break;
            }
            case R.id.st_actbar_mi_toggle_units_feet:
            {
                units = Distance.DISTANCE_TYPE.FEET;
                break;
            }
            default:
            {
                // intentionally left blank
            }
        }
        
        // change the units for the position plot
        final Plot2DPositionView vwPos = 
                ((Plot2DPositionView) findViewById(R.id.plot_2D_position));
        
        if (vwPos != null)
        { 
            vwPos.setDistanceUnits(units);
        }
        
        // change the units for the speed plot
        final Plot2DSpeedView vwSpd = 
                ((Plot2DSpeedView) findViewById(R.id.plot_2D_speed));
        
        if (vwSpd != null)
        { 
            vwSpd.setDistanceUnits(units);
        }
        
        invalidateOptionsMenu();
        
        return true;
    }
    
    /**
     * Responds to a selection within the mode menu item.
     * 
     * @param   menu   Reference to menu item.
     */
    public boolean onToggleModeSelect(MenuItem menu)
    {
        if (_genObsRunnable!=null)
        {
            switch(menu.getItemId())
            {
                case R.id.st_actbar_mi_mode_ftr:
                {
                    _genObsRunnable.toggleMode(
                            ObservationGenerationRunnable.SUPPORTED_MODE.FTR_DEMO);
                    invalidateOptionsMenu();
                    break;
                }
                case R.id.st_actbar_mi_mode_real:
                {
                    _genObsRunnable.toggleMode(
                            ObservationGenerationRunnable.SUPPORTED_MODE.REAL_DEMO);
                    invalidateOptionsMenu();
                    break;
                }
                default:
                {
                    // intentionally left blank
                }
            }
        }
        
        return true;
    }
    
    /**
     * Responds to a selection within the provider menu item.
     * 
     * @param   menu   Reference to menu item.
     */
    public boolean onToggleProviderSelect(MenuItem menu)
    {
        if (_genObsRunnable!=null)
        {
            switch(menu.getItemId())
            {
                case R.id.st_actbar_mi_provider_gps:
                {
                    _genObsRunnable.setProvider(
                            ObservationGenerationRunnable.POS_PROVIDER.GPS);
                    invalidateOptionsMenu();
                    break;
                }
                case R.id.st_actbar_mi_provider_net:
                {
                    _genObsRunnable.setProvider(
                            ObservationGenerationRunnable.POS_PROVIDER.NETWORK);
                    invalidateOptionsMenu();
                    break;
                }
                default:
                {
                    // intentionally left blank
                }
            }
        }
        
        return true;
    }
    
	@Override
	/**
	 * Called when the activity is first created. This is where you should do
	 * all of your normal static set up: create views, bind data to lists, etc. 
	 * This method also provides you with a Bundle containing the activity's 
	 * previously frozen state, if there was one.  Always followed by onStart().  
	 * 
	 * @param   savedInstanceState   If the activity is being re-initialized 
	 *                               after previously being shut down then this 
	 *                               Bundle contains the data it most recently 
	 *                               supplied in onSaveInstanceState(Bundle). 
	 *                               Note: Otherwise it is null.
	 */
	protected void onCreate(Bundle savedInstanceState) 
	{
	    super.onCreate(savedInstanceState);
	    
	    // create the handler
        _msgHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                updateUI(msg);
            }
        };
		
	    // inflate the activity's UI
	    setContentView(R.layout.activity_self_tracker);
	    
	    // initialize the UI
	    initialize();
	    
	    // register certain views for context menus
        final Plot2DPositionView vwPos = 
                ((Plot2DPositionView) findViewById(R.id.plot_2D_position));
        
        if (vwPos != null)
        {
            registerForContextMenu(vwPos);
        }

        final Plot2DSpeedView vwSpd = 
                ((Plot2DSpeedView) findViewById(R.id.plot_2D_speed));
        
        if (vwSpd != null)
        {
            registerForContextMenu(vwSpd);
        }
        
        final Plot2DBearingView vwBrg = 
                ((Plot2DBearingView) findViewById(R.id.plot_2D_bearing));
        
        if (vwBrg != null)
        {
            registerForContextMenu(vwBrg);
        }
        
        // create a two-way partnership between the bearing and speed views
        if (vwBrg != null && vwSpd != null)
        {
            vwBrg.addPartnerView(vwSpd);
            vwSpd.addPartnerView(vwBrg);
        }
        
        // partner the speed view with the position view
        if (vwPos != null && vwBrg != null)
        {
            vwSpd.addPartnerView(vwPos);
        }
        
        // fix the orientation of the app
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        
        // read the point of interest data
        readPoiData();

	    // write verbose message to the logger
	    android.util.Log.v(TAG, "onCreate");
	}
	
	@Override
	/**
	 * Called when the activity is becoming visible to the user.  Followed by 
	 * onResume() if the activity comes to the foreground, or onStop() if it 
	 * becomes hidden.
	 */
	protected void onStart()
	{
	    super.onStart();
		
	    // write verbose message to the logger
	    android.util.Log.v(TAG, "onStart");
	}
	
	@Override
	/**
	 * Called when the activity will start interacting with the user. At this 
	 * point your activity is at the top of the activity stack, with user input 
	 * going to it.  Always followed by onPause().
	 */
	protected void onResume()
	{
	    super.onResume();
		
	    // write verbose message to the logger
	    android.util.Log.v(TAG, "onResume");
	}
	
	@Override
	/**
	 * Called when the system is about to start resuming a previous activity. 
	 * This is typically used to commit unsaved changes to persistent data, stop 
	 * animations and other things that may be consuming CPU, etc. Implementations 
	 * of this method must be very quick because the next activity will not be 
	 * resumed until this method returns.  Followed by either onResume() if the 
	 * activity returns back to the front, or onStop() if it becomes invisible 
	 * to the user.
	 */
	protected void onPause()
	{
	    super.onPause();
		
	    // write verbose message to the logger
	    android.util.Log.v(TAG, "onPause");
	}
	
	@Override
	/**
	 * Called when the activity is no longer visible to the user, because 
	 * another activity has been resumed and is covering this one. This may 
	 * happen either because a new activity is being started, an existing one 
	 * is being brought in front of this one, or this one is being destroyed.
	 * Followed by either onRestart() if this activity is coming back to 
	 * interact with the user, or onDestroy() if this activity is going away.
	 */
	protected void onStop()
	{
	    super.onStop();
		
	    // write verbose message to the logger
	    android.util.Log.v(TAG, "onStop");
	}
	
	@Override
	/**
	 * Initialize the contents of the Activity's standard options menu. You should 
	 * place your menu items in to menu.  This is only called once, the first time 
	 * the options menu is displayed. To update the menu every time it is displayed, 
	 * see onPrepareOptionsMenu(Menu).
	 * 
	 * The default implementation populates the menu with standard system menu items. 
	 * These are placed in the CATEGORY_SYSTEM group so that they will be correctly 
	 * ordered with application-defined menu items. Deriving classes should always 
	 * call through to the base implementation.
	 * 
	 * You can safely hold on to menu (and any items created from it), making 
	 * modifications to it as desired, until the next time onCreateOptionsMenu() is 
	 * called.  When you add items to the menu, you can implement the Activity's 
	 * onOptionsItemSelected(MenuItem) method to handle them there.
	 * 
	 * @param   menu   The options menu in which you place your items.
	 */
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    // Inflate the menu; this adds items to the action bar if it is present.
	    getMenuInflater().inflate(R.menu.self_tracker_action_bar, menu);
		
	    android.util.Log.v(TAG, "onCreateOptionsMenu");
		
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	/**
	 * Prepare the Screen's standard options menu to be displayed. This is called 
	 * right before the menu is shown, every time it is shown. You can use this 
	 * method to efficiently enable/disable items or otherwise dynamically modify 
	 * the contents.
	 * 
	 * The default implementation updates the system menu items based on the 
	 * activity's state. Deriving classes should always call through to the base 
	 * class implementation.
	 * 
	 * @param   menu   The options menu as last shown or first initialized by 
	 *                 onCreateOptionsMenu().
	 *                 
	 * @retval  true if the menu is to be displayed.
	 * @retval  false if the menu is not to be displayed.
	 */
	public boolean onPrepareOptionsMenu(Menu menu)
	{
        switch (_mode)
        {
            case START:
            {
                menu.getItem(0).setEnabled(false);
                menu.getItem(1).setEnabled(true);
                menu.getItem(2).setEnabled(true);
                menu.getItem(3).setEnabled(false);
                break;
            }
            
            case PAUSE:
            {
                break;
            }
            
            case STOP:
            {
                menu.getItem(0).setEnabled(true);
                menu.getItem(1).setEnabled(false);
                menu.getItem(2).setEnabled(false);
                menu.getItem(3).setEnabled(!_obsList.isEmpty());
                break;
            }
        }
        
        // check the currently selected distance units using the position view
        final Plot2DPositionView vwPos = 
                ((Plot2DPositionView) findViewById(R.id.plot_2D_position));
        
        if (vwPos != null)
        {
            final Distance.DISTANCE_TYPE units = vwPos.getDistanceUnits();
            
            switch(units)
            {
                case KILOMETRES:
                {
                    final MenuItem mi = menu.findItem(R.id.st_actbar_mi_toggle_units_km);
                    mi.setChecked(true);
                    break;
                }
                case METRES:
                {
                    final MenuItem mi = menu.findItem(R.id.st_actbar_mi_toggle_units_metres);
                    mi.setChecked(true);
                    break;
                }
                case MILES:
                {
                    final MenuItem mi = menu.findItem(R.id.st_actbar_mi_toggle_units_miles);
                    mi.setChecked(true);
                    break;
                }
                case YARDS:
                {
                    final MenuItem mi = menu.findItem(R.id.st_actbar_mi_toggle_units_yards);
                    mi.setChecked(true);
                    break;
                }
                case FEET:
                {
                    final MenuItem mi = menu.findItem(R.id.st_actbar_mi_toggle_units_feet);
                    mi.setChecked(true);
                    break;
                }
                default:
                {
                    // intentionally left blank
                }
            }
        }
        
        // check the demo mode if enabled
        if (_genObsRunnable != null)
        {
            final ObservationGenerationRunnable.SUPPORTED_MODE mode = 
                    _genObsRunnable.getMode();
            
            switch(mode)
            {
                case REAL_DEMO:
                {
                    final MenuItem mi = menu.findItem(R.id.st_actbar_mi_mode_real);
                    if (mi!=null)
                    {
                        mi.setChecked(true);
                    }
                    break;
                }
                case FTR_DEMO:
                {
                    final MenuItem mi = menu.findItem(R.id.st_actbar_mi_mode_ftr);
                    if (mi!=null)
                    {
                        mi.setChecked(true);
                        mi.setEnabled(_mode==APP_MODE.STOP);
                    }
                    break;
                }
                case REAL_LIVE:
                {
                    // nothing to do
                    break;
                }
            }   
            
            // enable/disable the modes
            final MenuItem miReal = menu.findItem(R.id.st_actbar_mi_mode_real);
            miReal.setEnabled(_mode==APP_MODE.STOP);
            final MenuItem miFtr = menu.findItem(R.id.st_actbar_mi_mode_ftr);
            miFtr.setEnabled(_mode==APP_MODE.STOP);
            
            final ObservationGenerationRunnable.POS_PROVIDER provider =
                    _genObsRunnable.getProvider();
            
            switch(provider)
            {
                case GPS:
                {
                    final MenuItem mi = menu.findItem(R.id.st_actbar_mi_provider_gps);
                    mi.setChecked(true);
                    break;
                }
                case NETWORK:
                {
                    final MenuItem mi = menu.findItem(R.id.st_actbar_mi_provider_net);
                    mi.setChecked(true);
                }
            }
            
            // enable/disable the providers
            final MenuItem miGps = menu.findItem(R.id.st_actbar_mi_provider_gps);
            miGps.setEnabled(_mode==APP_MODE.STOP);
            final MenuItem miNet = menu.findItem(R.id.st_actbar_mi_provider_net);
            miNet.setEnabled(_mode==APP_MODE.STOP);

        }
	    
	    return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	/**
	 * Called to retrieve per-instance state from an activity before being killed
	 * so that the state can be restored in onCreate(Bundle) or 
	 * onRestoreInstanceState(Bundle) (the Bundle populated by this method will 
	 * be passed to both).
	 * 
	 * This method is called before an activity may be killed so that when it 
	 * comes back some time in the future it can restore its state. For example,
	 * if activity B is launched in front of activity A, and at some point 
	 * activity A is killed to reclaim resources, activity A will have a chance 
	 * to save the current state of its user interface via this method so that 
	 * when the user returns to activity A, the state of the user interface can 
	 * be restored via onCreate(Bundle) or onRestoreInstanceState(Bundle).
	 * 
	 * Do not confuse this method with activity lifecycle callbacks such as 
	 * onPause(), which is always called when an activity is being placed in 
	 * the background or on its way to destruction, or onStop() which is called 
	 * before destruction. One example of when onPause() and onStop() is called 
	 * and not this method is when a user navigates back from activity B to 
	 * activity A: there is no need to call onSaveInstanceState(Bundle) on B 
	 * because that particular instance will never be restored, so the system 
	 * avoids calling it. An example when onPause() is called and not 
	 * onSaveInstanceState(Bundle) is when activity B is launched in front of 
	 * activity A: the system may avoid calling onSaveInstanceState(Bundle) on 
	 * activity A if it isn't killed during the lifetime of B since the state of 
	 * the user interface of A will stay intact.
	 * 
	 * The default implementation takes care of most of the UI per-instance state 
	 * for you by calling onSaveInstanceState() on each view in the hierarchy that 
	 * has an id, and by saving the id of the currently focused view (all of which 
	 * is restored by the default implementation of onRestoreInstanceState(Bundle)). 
	 * If you override this method to save additional information not captured by 
	 * each individual view, you will likely want to call through to the default 
	 * implementation, otherwise be prepared to save all of the state of each 
	 * view yourself.
	 * 
	 * If called, this method will occur before onStop(). There are no guarantees 
	 * about whether it will occur before or after onPause().
	 */
	protected void onSaveInstanceState(Bundle outState)
	{
	    // save instance-specific state
	    //outState.putString("answer", state);
		
	    super.onSaveInstanceState(outState);
		
	    android.util.Log.v(TAG, "onSaveInstanceState");
	}
	
	@Override
	/**
	 * This method is called after onStart() when the activity is being re-initialized
	 * from a previously saved state, given here in savedInstanceState. Most 
	 * implementations will simply use onCreate(Bundle) to restore their state, but it 
	 * is sometimes convenient to do it here after all of the initialization has been 
	 * done or to allow subclasses to decide whether to use your default implementation. 
	 * The default implementation of this method performs a restore of any view state 
	 * that had previously been frozen by onSaveInstanceState(Bundle).
	 * 
	 * This method is called between onStart() and onPostCreate(Bundle). 
	 */
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
	    super.onRestoreInstanceState(savedInstanceState);
	    
	    android.util.Log.v(TAG, "onResotreInstanceState");
	}
	
	@Override
	/**
	 * The final call you receive before your activity is destroyed. This can 
	 * happen either because the activity is finishing (someone called finish() 
	 * on it, or because the system is temporarily destroying this instance of 
	 * the activity to save space. You can distinguish between these two 
	 * scenarios with the isFinishing() method.)  
	 * 
	 */
	protected void onDestroy() 
	{
	    super.onDestroy();
	    
	    if(_genObsRunnable != null)
	    {
	        _genObsRunnable.shutDown();
	        _msgHandler.removeCallbacks(_genObsRunnable);
	    }
		
	    // write verbose message to the logger
	    android.util.Log.v(TAG, "onDestroy");
	}
	
	/**
	 * Initializes the UI.
	 */
	private void initialize()
	{
	    // default members
	    _mode          = APP_MODE.STOP;
	    
	    // read the configuration data
	    readConfigData();

	    // instantiate the runnable
	    _genObsRunnable = new ObservationGenerationRunnable(
	            this, _appConfigData, _msgHandler);
	}
	
	   /**
     * Reads the configuration data from the xml file.
     */
    private void readConfigData()
    {
       try
       {
           File path                = Environment.getExternalStoragePublicDirectory(DATA_DIR);
           File pathAndFile         = new File(path,CONFIG_XML_FILENAME);
           DataInputStream inStream = new DataInputStream(new FileInputStream(pathAndFile));
           
           XmlPullParser parser = Xml.newPullParser();
           parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
           parser.setInput(inStream,null);
           
           while(parser.next() != XmlPullParser.END_DOCUMENT)
           {
               if (parser.getEventType() != XmlPullParser.START_TAG) {
                   continue;
               }
               
               final String name = parser.getName();
               
               if (name!=null && name.equals("updateRateSecs"))
               {
                   final int numAttrs = parser.getAttributeCount();

                   for(int i=0;i<numAttrs;++i)
                   {
                       final String attrName = parser.getAttributeName(i);

                       if (attrName.equals("value"))
                       {
                           _appConfigData._updateRateMSec = 
                                   Long.parseLong(parser.getAttributeValue(i))*1000;
                           
                           _appConfigData._updateRateMSec = Math.max(
                                   _appConfigData._updateRateMSec,AppConfigData.DEFAULT_UPDATE_RATE_MSEC);
                           
                           android.util.Log.v(
                                   TAG,"readConfigData - _updateRateMSec="+_appConfigData._updateRateMSec);
                           break;
                       }
                       
                   }
               }
               else if (name!=null && name.equals("displayRateSecs"))
               {
                   final int numAttrs = parser.getAttributeCount();

                   for(int i=0;i<numAttrs;++i)
                   {
                       final String attrName = parser.getAttributeName(i);

                       if (attrName.equals("value"))
                       {
                           _appConfigData._displayRateMSec = 
                                   Long.parseLong(parser.getAttributeValue(i))*1000;
                           
                           _appConfigData._displayRateMSec = Math.max(
                                   _appConfigData._displayRateMSec,AppConfigData.DEFAULT_DISPLAY_RATE_MSEC);
                           
                           android.util.Log.v(
                                   TAG,"readConfigData - _displayRateMSec="+_appConfigData._displayRateMSec);
                           break;
                       }
                   }
               }
               else if (name!=null && name.equals("useLocationAccuracy"))
               {
                   final int numAttrs = parser.getAttributeCount();

                   for(int i=0;i<numAttrs;++i)
                   {
                       final String attrName = parser.getAttributeName(i);

                       if (attrName.equals("value"))
                       {
                           _appConfigData._useLocPosnAccuracy = 
                                   Boolean.parseBoolean(parser.getAttributeValue(i));
                           android.util.Log.v(
                                   TAG,"readConfigData - _useLocPosnAccuracy="+
                                   _appConfigData._useLocPosnAccuracy);
                           break;
                       }
                   }
               }
               else if (name!=null && name.equals("obsPosnAccuracyMtrs"))
               {
                   final int numAttrs = parser.getAttributeCount();

                   for(int i=0;i<numAttrs;++i)
                   {
                       final String attrName = parser.getAttributeName(i);

                       if (attrName.equals("value"))
                       {
                           _appConfigData._obsPosnAccuracyMtrs = 
                                   Double.parseDouble(parser.getAttributeValue(i));
                           android.util.Log.v(
                                   TAG,"readConfigData - _obsPosnAccuracyMtrs="+
                                   _appConfigData._obsPosnAccuracyMtrs);
                           break;
                       }
                   }
               }
               else if (name!=null && name.equals("trkVeloAccuracyMtrsSec"))
               {
                   final int numAttrs = parser.getAttributeCount();

                   for(int i=0;i<numAttrs;++i)
                   {
                       final String attrName = parser.getAttributeName(i);

                       if (attrName.equals("value"))
                       {
                           _appConfigData._trkVeloAccuracyMtrsSec = 
                                   Double.parseDouble(parser.getAttributeValue(i));
                           android.util.Log.v(
                                   TAG,"readConfigData - _trkVeloAccuracyMtrsSec="+
                                   _appConfigData._trkVeloAccuracyMtrsSec);
                           break;
                       }
                   }
               }
               else if (name!=null && name.equals("trkProcessNoise"))
               {
                   final int numAttrs = parser.getAttributeCount();

                   for(int i=0;i<numAttrs;++i)
                   {
                       final String attrName = parser.getAttributeName(i);

                       if (attrName.equals("value"))
                       {
                           _appConfigData._trkProcessNoise = 
                                   Double.parseDouble(parser.getAttributeValue(i));
                           android.util.Log.v(
                                   TAG,"readConfigData - _trkProcessNoise="+
                                   _appConfigData._trkProcessNoise);
                           break;
                       }
                   }
               }
           }
           inStream.close();
       }
       catch(final Exception e)
       {
           android.util.Log.e(TAG,"readConfigData exception: " + e.toString());
       }
    }
	
	/**
	 * Reads the point of interest data from the xml file.
	 */
	private void readPoiData()
	{
	   try
	   {
	       final Plot2DPositionView vwPos = 
	                ((Plot2DPositionView) findViewById(R.id.plot_2D_position));
	        
	       if (vwPos != null)
	       {
	           Plot2DPositionView.PoiData poiData = new Plot2DPositionView.PoiData();
	           
    	       //XmlPullParser parser = getResources().getXml(R.xml.poi);
               File path                = Environment.getExternalStoragePublicDirectory(DATA_DIR);
               File pathAndFile         = new File(path,POI_XML_FILENAME);
               DataInputStream inStream = new DataInputStream(new FileInputStream(pathAndFile));
               
	           XmlPullParser parser = Xml.newPullParser();
	           parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
	           parser.setInput(inStream,null);
    	       
    	       while(parser.next() != XmlPullParser.END_DOCUMENT)
    	       {
    	           if (parser.getEventType() != XmlPullParser.START_TAG) {
    	               continue;
    	           }
    	           
    	           final String name = parser.getName();
    	           
    	           if (name!=null && name.equals("poi"))
    	           {
    	               final int numAttrs = parser.getAttributeCount();
    	               
    	               String poiName = null;
    	               String poiLat  = null;
    	               String poiLon  = null;
    	               
    	               for(int i=0;i<numAttrs;++i)
    	               {
    	                   final String attrName = parser.getAttributeName(i);
    
    	                   if (attrName.equals("name"))
    	                   {
    	                       poiName = parser.getAttributeValue(i);
    	                   }
    	                   else if(attrName.equals("lat"))
    	                   {
    	                       poiLat = parser.getAttributeValue(i);
    	                   }
                           else if(attrName.equals("lon"))
                           {
                               poiLon = parser.getAttributeValue(i);
                           }
    	               }
    	               
    	               poiData._name     = poiName;
    	               poiData._position.setLatitude(
    	                       Double.parseDouble(poiLat), Position.POS_VALUE_TYPE.DEGREES); 
    	               poiData._position.setLongitude(
                               Double.parseDouble(poiLon), Position.POS_VALUE_TYPE.DEGREES); 
    	               
    	               vwPos.add(poiData);
    	               
    	               android.util.Log.v(TAG,"readPoiData - " + poiName+" "+poiLat+" "+poiLon);
    	           }
    	       }
    	       
    	       inStream.close();
	       }
	   }
	   catch(final Exception e)
	   {
	       android.util.Log.e(TAG,"readPoiData exception: " + e.toString());
	   }
	}
	
    @Override
    /**
     * Called when a context menu for the view is about to be shown. Unlike 
     * onCreateOptionsMenu(Menu), this will be called every time the context menu
     * is about to be shown and should be populated for the view (or item inside 
     * the view for AdapterView subclasses, this can be found in the menuInfo)).
     * 
     * Use onContextItemSelected(android.view.MenuItem) to know when an item has 
     * been selected.
     * 
     * It is not safe to hold onto the context menu after this method returns.
     * 
     * @param   menu       The context menu that is being built.
     * @param   v          The view for which the context menu is being built.
     * @param   menuInfo   Extra information about the item for which the context 
     *                     menu should be shown. This information will vary depending 
     *                     on the class of v.
     */
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        // launch the menu for certain views only, displaying only if the view is 
        // stationary (i.e. the user isn't swiping or moving the plot)
        switch (v.getId())
        {
            case R.id.plot_2D_position:
            {
                Plot2DPositionView vw = (Plot2DPositionView)v;
                
                if(vw.isStationary(-1))
                {
                    super.onCreateContextMenu(menu, v, menuInfo);
                    build2DPositionViewContextMenu(vw, menu);
                }
                
                break;
            }
            case R.id.plot_2D_speed:
            {    
                Plot2DSpeedView vw = (Plot2DSpeedView)v;
                
                if(vw.isStationary(-1))
                {
                    super.onCreateContextMenu(menu, v, menuInfo);
                    build2DSpeedViewContextMenu(vw, menu);
                }
                
                break;
            }
            case R.id.plot_2D_bearing:
            {      
                Plot2DBearingView vw = (Plot2DBearingView)v;
                
                if(vw.isStationary(-1))
                {
                    super.onCreateContextMenu(menu, v, menuInfo);
                    build2DBearingViewContextMenu(vw, menu);
                }
                
                break;
            }
            default:
            {
                // intentionally left blank
            }
        }
    }

    @Override
    /**
     * This hook is called whenever an item in a context menu is selected. The 
     * default implementation simply returns false to have the normal processing 
     * happen (calling the item's Runnable or sending a message to its Handler as 
     * appropriate). You can use this method for any items for which you would like 
     * to do processing without those other facilities.
     * 
     * Use getMenuInfo() to get extra information set by the View that added this 
     * menu item.
     * 
     * Derived classes should call through to the base class for it to perform the 
     * default menu handling.
     * 
     * @param   item   The context menu item that was selected.
     * 
     * @retval  true to consume normal context menu processing here.
     * @retval  false to allow normal context menu processing to proceed.
     */
    public boolean onContextItemSelected(MenuItem item) 
    {  
	   return super.onContextItemSelected(item);  
	} 
	
	/**
	 * Builds the context menu for the Plot2DPositionView class.
	 * 
	 * @param   view   The view from which the context menu arises.
	 * @param   menu   The context menu that is updated with options
	 *                 specific to the view 
	 * 
	 */
	private void build2DPositionViewContextMenu(final Plot2DPositionView view, 
	                                            ContextMenu menu)
	{
	    // clear all pointers so that they are unaffected by this long click
	    view.clearPointers();
	    
	    // update the menu with items specific to this view
	    getMenuInflater().inflate(R.menu.two_dim_position_plot, menu);
    
	    // update the menu items given the state of the position view
	    MenuItem mi = menu.findItem(R.id.twodim_pos_mi_toggle_grid);
	    if (mi != null)
	    {
	        if (view.isDrawGrid())
	        {
	            mi.setTitle(R.string.hide_grid_txt);
	        }
	        else
	        {
	            mi.setTitle(R.string.show_grid_txt);
	        }
	    }
	    
	    mi = menu.findItem(R.id.twodim_pos_mi_toggle_colour_for_time);
        if (mi != null)
        {
            if (view.isColourForTime())
            {
                mi.setTitle(R.string.hide_colour_for_time_txt);
            }
            else
            {
                mi.setTitle(R.string.show_colour_for_time_txt);
            }
        }
        
        mi = menu.findItem(R.id.twodim_pos_mi_toggle_connect);
        if (mi != null)
        {
            if (view.isConnectDataDrawn())
            {
                mi.setTitle(R.string.hide_connect_txt);
            }
            else
            {
                mi.setTitle(R.string.show_connect_txt);
            }
        }
        
        mi = menu.findItem(R.id.twodim_pos_mi_toggle_history);
        if (mi != null)
        {
            if (view.isHistoryDrawn())
            {
                mi.setTitle(R.string.hide_history_txt);
            }
            else
            {
                mi.setTitle(R.string.show_history_txt);
            }
        }
        
        mi = menu.findItem(R.id.twodim_pos_mi_toggle_poi);
        if (mi != null)
        {
            if (view.isPoiDataDrawn())
            {
                mi.setTitle(R.string.hide_poi_txt);
            }
            else
            {
                mi.setTitle(R.string.show_poi_txt);
            }
        }
	}
	
	/**
     * Builds the context menu for the Plot2DSpeedView class.
     * 
     * @param   view   The view from which the context menu arises.
     * @param   menu   The context menu that is updated with options
     *                 specific to the view 
     * 
     */
    private void build2DSpeedViewContextMenu(final Plot2DSpeedView view, 
                                             ContextMenu menu)
    {
        // clear all pointers so that they are unaffected by this long click
        view.clearPointers();

        // update the menu with items specific to this view
        getMenuInflater().inflate(R.menu.two_dim_speed_plot, menu);
        
        // update the menu items given the state of the position view
        MenuItem mi = menu.findItem(R.id.twodim_spd_mi_toggle_grid);
        if (mi != null)
        {
            if (view.isDrawGrid())
            {
                mi.setTitle(R.string.hide_grid_txt);
            }
            else
            {
                mi.setTitle(R.string.show_grid_txt);
            }
        }
    }
    
    /**
     * Builds the context menu for the Plot2DBearingView class.
     * 
     * @param   view   The view from which the context menu arises.
     * @param   menu   The context menu that is updated with options
     *                 specific to the view 
     * 
     */
    private void build2DBearingViewContextMenu(final Plot2DBearingView view, 
                                               ContextMenu menu)
    {
        // clear all pointers so that they are unaffected by this long click
        view.clearPointers();
        
        // update the menu with items specific to this view
        getMenuInflater().inflate(R.menu.two_dim_bearing_plot, menu);
        
        // update the menu items given the state of the position view
        MenuItem mi = menu.findItem(R.id.twodim_brg_mi_toggle_grid);
        if (mi != null)
        {
            if (view.isDrawGrid())
            {
                mi.setTitle(R.string.hide_grid_txt);
            }
            else
            {
                mi.setTitle(R.string.show_grid_txt);
            }
        }
    }
	
	/**
	 * Updates the user interface (UI) with message data that has been published
	 * from the timer task.
	 * 
	 * @param   msg   Message that contains the published data.
	 */
	private void updateUI(Message msg)
	{
	    final ObservationPacket packet = 
	            (ObservationPacket)msg.obj;
	    final Observation2DP obs = packet.getObservation();
	    
	    try
	    {
	    
    	    if (obs != null)
    	    {
    	        _obsList.add(new Observation2DP(obs));  
    	    }
    	        
    	    if (_trkList.isEmpty())
    	    {
    	        if (obs != null)
    	        {
        	        _trkList.add(new 
        	                Track2DPV(
        	                        new KalmanPredictFilter(_appConfigData._trkProcessNoise),
        	                        new KalmanUpdateFilter(),
        	                        _appConfigData._trkVeloAccuracyMtrsSec));
        	            
        	        Track2DPV trk = _trkList.get(0);
            	    trk.init(obs);
            	    
            	    updateViews();
    	        }
    	    }
    	    else
    	    {
	            _trkList.add(new Track2DPV(
	                    _trkList.lastElement()));
	            
	            Track2DPV trk = _trkList.lastElement();
	            
	            trk.predict(packet.getTimestamp());
	            
	            if (obs != null)
	            {
	                if (!obs.getIsTimeOnly())
	                {
	                    trk.update(obs);
	                }
	            }

    	        updateViews();
    	    }
	    }
	    catch(final Exception e)
	    {
	        android.util.Log.e(TAG, "updateUI - exception: " + e.toString());
	    }
	}
	
	/**
	 * Updates the views with the latest track data.
	 */
	private void updateViews()
	{
	    if (!_trkList.isEmpty())
	    {
	        final Track2DPV trk = _trkList.lastElement();
	        
    	    // update the time
            final double elapsedTimeSecs = trk.getLastUpdateTime() - 
                    trk.getInitTime();
            
            _timeUtil.set(elapsedTimeSecs, Time.TIME_TYPE.SECONDS);
    	    
    	    // update the speed
    	    try
    	    {
    	        boolean displayData = false;
    	        
    	        if (_displayTime==null)
    	        {
    	            _displayTime = new Time();
    	            displayData = true;
    	        }
    	        else
    	        {
    	            final double elapsedDisplayTimeMSecs = 
    	                    Math.abs(trk.getLastUpdateTime()*1000.0 - 
    	                    _displayTime.get(Time.TIME_TYPE.MILLISECONDS));
    	            
    	            displayData = (elapsedDisplayTimeMSecs >= _appConfigData._displayRateMSec);
    	        }
    	        
    	        if (displayData)
    	        {
            	    final Matrix x = trk.getX();
            	    final double vx = x.at(Track2DPV.VX);
            	    final double vy = x.at(Track2DPV.VY);
            	    final double trkSpeedMps = Math.sqrt(vx*vx + vy*vy);
            	    _spdUtil.set(trkSpeedMps, Speed.SPEED_TYPE.METRES_PER_SEC);
            	    
                    _publishedSpdData._timestamp.set(trk.getLastUpdateTime(), Time.TIME_TYPE.SECONDS);
                    _publishedSpdData._speed.copy(_spdUtil);
                    
                    final Plot2DSpeedView vwSpeed = 
                            ((Plot2DSpeedView) findViewById(R.id.plot_2D_speed));
                    
                    if (vwSpeed != null)
                    {
                        vwSpeed.add(_publishedSpdData);
                    }
                    
                    final double trkBrg = Math.atan2(vx,vy);
                    
                    // only reset the bearing if the target is moving faster than the max
                    if (trkSpeedMps > MAX_STATIONARY_SPEED_MPS)
                    {
                        _brgUtil.set(trkBrg, Angle.ANGLE_TYPE.RADIANS);
                    }
   
                    _publishedBrgData._timestamp.set(trk.getLastUpdateTime(), Time.TIME_TYPE.SECONDS);
                    _publishedBrgData._bearing.copy(_brgUtil);
                    
                    final Plot2DBearingView vwBearing = 
                            ((Plot2DBearingView) findViewById(R.id.plot_2D_bearing));
                    
                    if (vwBearing != null)
                    {
                        vwBearing.add(_publishedBrgData);
                    } 

                    _publishedTrkData._heading.copy(_brgUtil);
                    _publishedTrkData._speed.copy(_spdUtil);
                    _publishedTrkData._position.copy(trk.getPosition());
                    _publishedTrkData._timestamp.set(trk.getLastUpdateTime(), Time.TIME_TYPE.SECONDS);
                    _publishedTrkData._deadReckoned = !trk.receivedUpdate();
                    
                    final Plot2DPositionView vwPos = 
                            ((Plot2DPositionView) findViewById(R.id.plot_2D_position));
                    
                    if (vwPos != null)
                    {
                        vwPos.add(_publishedTrkData);
                    }
                    
                    _displayTime.set(
                            trk.getLastUpdateTime(),Time.TIME_TYPE.SECONDS);
    	        }
    	    }
    	    catch(final Exception e)
    	    {
    	        // deliberately do nothing - leave speed as last recorded
    	    }

    	    // update the distance if there exists at least two observations
    	    if (_obsList.size()>=2)
    	    {
    	        try
    	        {
        	        final Observation2DP obsA = _obsList.elementAt(_obsList.size()-2);
        	        final Observation2DP obsB = _obsList.lastElement();
        	        
        	        final Matrix zA = obsA.getZ();
        	        final Matrix zB = obsB.getZ();
        	        
        	        final double xa = zA.at(Observation2DP.XPOS);
        	        final double ya = zA.at(Observation2DP.YPOS);
        	        final double xb = zB.at(Observation2DP.XPOS);
        	        final double yb = zB.at(Observation2DP.YPOS);
        	        
        	        final double distance = Math.sqrt((xa-xb)*(xa-xb) + 
        	                (ya-yb)*(ya-yb));
        	        
        	        _dstUtil.add(distance, Distance.DISTANCE_TYPE.METRES);   
    	        }
    	        catch(final Exception e)
    	        {
    	            // deliberately do nothing - leave distance as is
    	        }  
    	    }
	    }
	}
	
	//! enumerated type representing the different modes of the app
	private enum APP_MODE
	{
	    START, /**< Enumeration indicating the application has started. */
	    PAUSE, /**< Enumeration indicating the application has paused.  */
	    STOP   /**< Enumeration indicating the application has stopped. */
	};

	//! Tag string identifying class, used for logging purposes.
	private static final String TAG = "SelfTrackerActivity";
	
	//! Directory to save self-tracker data files to
	private static final String DATA_DIR = "Apps/SelfTracker";
	
	//! File that contains the POI data.
	private static final String POI_XML_FILENAME = "poi.xml";
	
	//! File that contains the config data.
	private static final String CONFIG_XML_FILENAME = "config.xml";
	
	//! Default number of observations and tracks to manage in the vector.
	/**
	 * If we assume a minimum update rate of 5 seconds, then over three hours
	 * this is 12*60*3=2160 objects.
	 */
	private static final int VECTOR_INITIAL_CAPACITY = 2200;
	
    //! Maximum stationary speed for the tracked target (metres per second).
    private static final double MAX_STATIONARY_SPEED_MPS = 0.3;
	
	//! The increment for whenever a vector is resized upwards.
	/**
	 * Add one-half hour of data: 12*30=360
	 */
	private static final int VECTOR_CAPACITY_INCREMENT = 360;

	//! Indicates the current mode of the app
	private APP_MODE _mode;

	//! Handler to handle messaging between the UI thread and the timer task.
	private Handler _msgHandler;
	
	//! Runnable used to generate observations to update the route.
	private ObservationGenerationRunnable _genObsRunnable;
	
	//! List of observations.
	private Vector<Observation2DP> _obsList;
	
	//! List of tracks.
	private Vector<Track2DPV> _trkList;
	
	//! Update time of the track published to and displayed on the views.
	private Time _displayTime;
	
	//! Distance utility to format a distance value for GUI display.
	private Distance _dstUtil;
	
	//! Speed utility to format a speed value for GUI display.
	private Speed _spdUtil;
	
	//! Time utility to format a time value for GUI display.
	private Time _timeUtil;
	
	//! Angle utility to format the bearing value for GUI display.
	private Angle _brgUtil;
	
	//! Storage for the app configuration data.
	private AppConfigData _appConfigData;
	
	//! Data to publish to the speed view.
	private Plot2DSpeedView.SpeedData _publishedSpdData;
	
	//! Data to publish to the bearing view.
	private Plot2DBearingView.BearingData _publishedBrgData;
	
	//! Data to publish to the position view.
	private Plot2DPositionView.TrackData _publishedTrkData;
}
