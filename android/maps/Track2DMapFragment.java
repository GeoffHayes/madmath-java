/*
 * 2013-10-10   Geoff Hayes     Initial Release.
 */

/**
 * Package that contains custom Android maps.
 */
package com.madmath.maps;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import com.madmath.geocoordinates.Position;
import com.madmath.geocoordinates.Vincenty;
import com.madmath.measures.Angle;
import com.madmath.measures.Distance;
import com.madmath.measures.Speed;
import com.madmath.measures.Time;

import com.madmath.tracking.Track2DPV;
import com.madmath.utilities.MeasureUtils;
import com.madmath.utilities.StringUtils;
import com.madmath.views.Plot2DPositionView;

/**
 * Class that is used to plot 2D tracks on a Google map.
 *
 */
public class Track2DMapFragment extends MapFragment
{
    /**
     * Class that provides resource ids to images that will be drawn on the plot.
     *
     */
    public static class SymbolResourceIds
    {
        public SymbolResourceIds()
        {
            _symHistoricHooked = 0;
            _symSelfHooked     = 0;
            _symHistoric       = 0;
            _symSelf           = 0;
            _symDeparture      = 0;
        }
        
        public SymbolResourceIds(final SymbolResourceIds copy)
        {
            _symHistoricHooked = copy._symHistoricHooked;
            _symSelfHooked     = copy._symSelfHooked;
            _symHistoric       = copy._symHistoric;
            _symSelf           = copy._symSelf;
            _symDeparture      = copy._symDeparture;
        }
        
        public void copy(final SymbolResourceIds obj)
        {
            _symHistoricHooked = obj._symHistoricHooked;
            _symSelfHooked     = obj._symSelfHooked;
            _symHistoric       = obj._symHistoric;
            _symSelf           = obj._symSelf;
            _symDeparture      = obj._symDeparture;
        }
        
        //! Resource id for the departure symbol.
        public int _symDeparture;
        
        //! Resource id for the self symbol.
        public int _symSelf;
        
        //! Resource id for the historic symbol.
        public int _symHistoric;
        
        //! Resource id for the hooked self symbol.
        public int _symSelfHooked;
        
        //! Resource id for the hooked historic symbol.
        public int _symHistoricHooked;
    };
    
    /**
     * Class constructor.
     *
     */
    public Track2DMapFragment()
    {
        super();
        
        _symResourceIds    = new SymbolResourceIds();
        _appContext        = null;
        _latestTrkData  = new Plot2DPositionView.TrackData();
        _rangeAziData      = new Vincenty.GetRangeAzimuthData();
        
        _plotTrkData = new Vector<Plot2DPositionView.TrackData>(
                VECTOR_INITIAL_CAPACITY, VECTOR_CAPACITY_INCREMENT);
        _markerData    = new LinkedList<Marker>();
        _polylineData  = new LinkedList<Polyline>();
        _markerOptions = new MarkerOptions();
        _distanceUnits = Distance.DISTANCE_TYPE.KILOMETRES;
        
        // initialize the marker options object
        if (_markerOptions != null)
        {
            _markerOptions.anchor(0.5f, 0.5f);
            _markerOptions.visible(true);
        }
    }
    
    /**
     * Sets the resource IDs for the map.
     * 
     * @param   symResourceIds   The resource IDs to copy into the instance.
     */
    public void setSymbolResourceIds(final SymbolResourceIds symResourceIds)
    {
        if (_symResourceIds == null)
        {
            _symResourceIds = new SymbolResourceIds(symResourceIds);
        }
        else
        {
            _symResourceIds.copy(symResourceIds);
        }
    }
    
    /**
     * Sets the context used for toast popups.
     * 
     * @param   appContext   The context from the parent activity.
     */
    public void setContext(Context appContext)
    {
        _appContext = appContext;
    }
    
    /**
     * Resets data members to a state where the map fragment is considered to
     * be refreshed.
     */
    public void reset()
    {
        // reset the map camera (do this before the members are reset)
        _map.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder()
                                  .target(_camPos.target)      
                                  .zoom(_camPos.zoom)                  
                                  .bearing(0.0f)             
                                  .build()));
        
        // reset the data members
        _isSelfCentred             = true;
        _isMapBearingSlavedToTrk   = false;
        _hookedMarker              = null;
        _latestMarker              = null;
        _isAutomaticCameraMove     = true;
        _showHistory               = true;
        _showConnections           = true;
        _plotTrkData.clear();
        _markerData.clear();
        _polylineData.clear();
        _map.clear();
        _latestTrkData._dist.set(0.0);
        _latestTrkData._distCumulative.set(0.0);
    }
    
    /**
     * Toggles the show history indicator and forces a refresh of the map (with
     * or without the history points, given the new state of the indicator).
     */
    public synchronized void toggleShowHistory()
    {
        _showHistory = !_showHistory;
        showHistory();
    }
    
    /**
     * Returns whether history is being shown (true) or not (false).
     */
    public boolean isShowHistory()
    {
        return _showHistory;
    }
    
    /**
     * Toggles the show connection indicator and forces a refresh of the map (with
     * or without the connections, given the new state of the indicator).
     */
    public synchronized void toggleShowConnections()
    {
        _showConnections= !_showConnections;
        showConnections();
    }
    
    /**
     * Returns whether connections are being shown (true) or not (false).
     */
    public boolean isShowConnections()
    {
        return _showConnections;
    }
    
    /**
     * Sets the distance units (as determined by the caller) which affects the
     * toast popup messages displayed on hooked data.
     * 
     * @param   units   The new distance units.
     */
    public void setDistanceUnits(final Distance.DISTANCE_TYPE units)
    {
        _distanceUnits = units;
    }
    
    /**
     * Enables the self centre of the map against the current/most recent self
     * position.
     */
    public synchronized void enableSelfCentre()
    {  
        _isSelfCentred         = true;
        _isAutomaticCameraMove = true;
        
        if (_latestMarker != null)
        {           
            if (_isMapBearingSlavedToTrk)
            {
                _map.animateCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                          .target(_latestMarker.getPosition())      
                                          .zoom(_map.getCameraPosition().zoom)                  
                                          .bearing((float)
                                                _latestTrkData._heading.get(Angle.ANGLE_TYPE.DEGREES_POS))             
                                          .build()));
                
                // since the map bearing is slaved to the track heading
                _latestMarker.setRotation(0.0f);
            }
            else
            {
                _map.animateCamera(CameraUpdateFactory.newLatLng(
                        _latestMarker.getPosition()));
            }
        }
    }
    
    /**
     * Toggles the tying of the map bearing with the current track heading.
     */
    public synchronized void toggleMapBearing()
    {
        _isAutomaticCameraMove = true;

        _isMapBearingSlavedToTrk = !_isMapBearingSlavedToTrk;
        
        if (_latestMarker != null)
        {
            if (_isMapBearingSlavedToTrk)
            {
                // only rotate if self-centred
                if (_isSelfCentred)
                {
                    _latestMarker.setRotation(0.0f);
                    
                    _map.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                              .target(_map.getCameraPosition().target)      
                                              .zoom(_map.getCameraPosition().zoom)                  
                                              .bearing((float)_latestTrkData._heading.get(Angle.ANGLE_TYPE.DEGREES_POS))             
                                              .build()));
                }
            }
            else
            {
                _latestMarker.setRotation(
                        (float)_latestTrkData._heading.get(
                                Angle.ANGLE_TYPE.DEGREES_POS));      
                
                _map.animateCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                          .target(_map.getCameraPosition().target)      
                                          .zoom(_map.getCameraPosition().zoom)                  
                                          .bearing(0.0f)             
                                          .build()));
            }
        }
    }
    
    /**
     * Returns whether the map bearing is slaved to the track (true) or not (false).
     */
    public boolean isMapBearingSlavedToTrack()
    {
        return _isMapBearingSlavedToTrk;
    }
    
    /**
     * Toggles the map type through its different types.
     */
    public synchronized void toggleMapType()
    {
        if (_map != null)
        {
            int mapType = _map.getMapType();
            mapType++;
            if (mapType > GoogleMap.MAP_TYPE_HYBRID)
            {
                mapType = GoogleMap.MAP_TYPE_NONE;
            }
            _map.setMapType(mapType);
        }
    }
    
    /**
     * Updates the map with the latest 2DPV track data.
     * 
     * @param   trk   The latest track to update the map with.
     */
    public synchronized void updateMap(final Track2DPV trk)
    {
        if (_map != null)
        {
            if (!_markerData.isEmpty())
            {
                try
                {
                    Vincenty.GetRangeAzimuth(
                            _latestTrkData._position, trk.getPosition(), _rangeAziData);
                    
                    // set the distance and the cumulative distance
                    _latestTrkData._dist.set(
                            _rangeAziData._rangeMtrs, Distance.DISTANCE_TYPE.METRES);
                    _latestTrkData._distCumulative.add(
                            _rangeAziData._rangeMtrs, Distance.DISTANCE_TYPE.METRES);

                }
                catch(final Exception e)
                {
                    android.util.Log.e(
                            this.getClass().getSimpleName(),
                            "updateMap exception: " + e.toString());
                }
            }
            
            try
            {   
                final double trkSpeedMps    = trk.getSpeed();
                final double trkBrgRads     = trk.getHeadingRads();
                final double trkPrevBrgDegs = _latestTrkData._heading.get(Angle.ANGLE_TYPE.DEGREES_POS);
                
                // update the remaining track data
                _latestTrkData._heading.set(trkBrgRads, Angle.ANGLE_TYPE.RADIANS);
                _latestTrkData._speed.set(trkSpeedMps, Speed.SPEED_TYPE.METRES_PER_SEC);
                _latestTrkData._position.copy(trk.getPosition());
                _latestTrkData._timestamp.set(trk.getLastUpdateTime(), Time.TIME_TYPE.SECONDS);
                _latestTrkData._deadReckoned = !trk.receivedUpdate();
                
                // fix the heading if the track is considered to be stationary
                if (trkSpeedMps <= MAX_STATIONARY_SPEED_MPS)
                {
                    _latestTrkData._heading.set(trkPrevBrgDegs,Angle.ANGLE_TYPE.DEGREES_POS);
                }
                
                final double latDegs = _latestTrkData._position.getLatitude(
                        Position.POS_VALUE_TYPE.DEGREES);
                
                final double lonDegs = _latestTrkData._position.getLongitude(
                        Position.POS_VALUE_TYPE.DEGREES);
                
                final LatLng pos = new LatLng(latDegs,lonDegs);
    
                final Marker nextToLastMarker = _latestMarker;
                
                // reset the previous marker icon
                if (nextToLastMarker != null)
                {
                    if (Integer.parseInt(nextToLastMarker.getTitle()) != 1)
                    {
                        nextToLastMarker.setRotation(0.0f);
                        nextToLastMarker.setIcon(BitmapDescriptorFactory
                            .fromResource(_symResourceIds._symHistoric));
                    }
                }
                        
                // update the marker options object with the position, icon and
                // icon rotation
                _markerOptions.position(pos);
                _markerOptions.rotation((float)
                        _latestTrkData._heading.get(Angle.ANGLE_TYPE.DEGREES_POS) - 
                        _map.getCameraPosition().bearing);
                _markerOptions.icon(
                        BitmapDescriptorFactory
                        .fromResource(_symResourceIds._symSelf));

                // change the icon to the departure icon if the container is empty
                if (_markerData.isEmpty())
                {
                    _markerOptions.icon(
                            BitmapDescriptorFactory
                            .fromResource(_symResourceIds._symDeparture));
                }
                else
                {
                    nextToLastMarker.setVisible(_showHistory);
                }
                
                _latestMarker = _map.addMarker(_markerOptions);
                
                if (nextToLastMarker!=null && _latestMarker!=null)
                {
                    // add a new polyline
                    PolylineOptions line = 
                            new PolylineOptions()
                    .add(nextToLastMarker.getPosition(), _latestMarker.getPosition())
                    .width(2)
                    .visible(_showConnections)
                    .color(Color.BLUE);

                    _polylineData.add(_map.addPolyline(line));
                }
    
                _markerData.add(_latestMarker);
                
                if (_hookedMarker!=null)
                {
                    if(Integer.parseInt(_hookedMarker.getTitle()) > 1)
                    {
                        _hookedMarker.setIcon(BitmapDescriptorFactory
                                .fromResource(_symResourceIds._symHistoricHooked));
                    }
                }
    
                if(_isSelfCentred)
                {
                    _isAutomaticCameraMove = true;
                    
                    float camBrg = 0.0f;
                    
                    if (_isMapBearingSlavedToTrk)
                    {
                        camBrg = (float)
                                _latestTrkData._heading.get(Angle.ANGLE_TYPE.DEGREES_POS);
                        
                        // since the map bearing is slaved to the track heading, set the
                        // marker rotation to zero
                        _latestMarker.setRotation(0.0f);
                    }
                    
                    _map.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                              .target(pos)      
                                              .zoom(_map.getCameraPosition().zoom)                  
                                              .bearing(camBrg)             
                                              .build()));
                }
                
                // add the published/plotted track to the container
                _plotTrkData.add(new Plot2DPositionView.TrackData(_latestTrkData));
                
                // set the track id within the title of the marker
                _latestMarker.setTitle(Integer.toString(_plotTrkData.size()));
            }
            catch(final Exception e)
            {
                android.util.Log.e(
                        this.getClass().getSimpleName(),
                        "updateMap exception: " + e.toString());
            }
        }
    }
    
    /**
     * Initializes the map object.
     */
    public void initMap()
    {
        // create the map object if necessary
        if (_map == null)   
        {
            _map = getMap();
        }
        
        if (_map != null)
        {
            _map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            
            _latLng = new LatLng(45.423606,-75.686917);

            _camPos = 
                    new CameraPosition.Builder()
                        .target(_latLng)      
                        .zoom(14)                  
                        .bearing(0)               
                        .tilt(0)                  
                        .build();     
            
            _map.animateCamera(CameraUpdateFactory.newCameraPosition(_camPos));
            
            // set the UI settings for the map
            UiSettings uiSettings = _map.getUiSettings();
            uiSettings.setCompassEnabled(true);
            uiSettings.setRotateGesturesEnabled(false);
            uiSettings.setScrollGesturesEnabled(true);
            uiSettings.setTiltGesturesEnabled(false);
            uiSettings.setZoomControlsEnabled(false);
            uiSettings.setZoomGesturesEnabled(true);
            
            // handle a click
            _map.setOnMapClickListener(
                    new GoogleMap.OnMapClickListener() 
                    {
                        @Override
                        public void onMapClick(LatLng point) 
                        {
                            // clear any previously hooked marker
                            if (_hookedMarker != null)
                            {
                                final int trkId = Integer.parseInt(_hookedMarker.getTitle());
                                
                                if (trkId == _plotTrkData.size())
                                {
                                    _hookedMarker.setIcon(
                                            BitmapDescriptorFactory
                                            .fromResource(_symResourceIds._symSelf));
                                }
                                else if (trkId > 1)
                                {                                    
                                    _hookedMarker.setIcon(
                                            BitmapDescriptorFactory
                                            .fromResource(_symResourceIds._symHistoric));
                                }
                                
                                // clear the hooked marker
                                _hookedMarker = null;
                            }
                        }
                    });
            
            _map.setOnMarkerClickListener(
                    new GoogleMap.OnMarkerClickListener() 
                    {
                        @Override
                        public boolean onMarkerClick(Marker marker) 
                        {
                            // clear any previously hooked marker
                            if (_hookedMarker != null)
                            {                   
                                final int trkId = Integer.parseInt(_hookedMarker.getTitle());
                                
                                if (trkId == _plotTrkData.size())
                                {
                                    _hookedMarker.setIcon(
                                            BitmapDescriptorFactory
                                            .fromResource(_symResourceIds._symSelf));
                                }
                                else if (trkId > 1)
                                {                                    
                                    _hookedMarker.setIcon(
                                            BitmapDescriptorFactory
                                            .fromResource(_symResourceIds._symHistoric));
                                }
                                
                            }
                            
                            // set the new hooked marker
                            _hookedMarker = marker;
                            
                            // set the new icon colour for the hooked marker if not the
                            // departure
                            final int trkId = Integer.parseInt(_hookedMarker.getTitle());
                            
                            if (trkId == _plotTrkData.size() && trkId > 1)
                            {
                                _hookedMarker.setIcon(
                                        BitmapDescriptorFactory
                                        .fromResource(_symResourceIds._symSelfHooked));
                            }
                            else if (trkId > 1)
                            {
                                _hookedMarker.setIcon(
                                        BitmapDescriptorFactory
                                        .fromResource(_symResourceIds._symHistoricHooked));
                            }
                            else
                            {
                                _hookedMarker = null;
                            }
                            
                            // do we have a track for this id?
                            String message = "No information available";
                            
                            if (trkId > 0 && trkId <= _plotTrkData.size())
                            {
                             
                                final Plot2DPositionView.TrackData trkData = 
                                        _plotTrkData.get(trkId-1);
                                
                                if (trkData != null)
                                {
                                    final Speed.SPEED_TYPE speedUnits = 
                                            MeasureUtils.ToSpeed(_distanceUnits);
                                    
                                    message = trkData._position.toString() + "\n" + 
                                            StringUtils.Pad(trkData._heading.get(
                                                    Angle.ANGLE_TYPE.DEGREES_POS),3,0) + '\u00B0' + "/" +
                                            StringUtils.Pad(trkData._speed.get(
                                                    speedUnits), 0, 2) + " " + 
                                                    Speed.ToStringAbbrev(speedUnits) +"\n" +
                                            StringUtils.Pad(trkData._dist.get(
                                                    _distanceUnits), 0, 2) +"/" +
                                            StringUtils.Pad(trkData._distCumulative.get
                                                    (_distanceUnits), 0, 2) + " " + 
                                                    Distance.ToStringAbbrev(_distanceUnits) +"\n" +
                                            StringUtils.toString(trkData._timestamp.get(
                                                    Time.TIME_TYPE.MILLISECONDS), 
                                                    StringUtils.DATETIME_FORMAT_TYPE.HH_MM_SS);
                                }
                            }

                            // display a hooked message
                            if (_appContext != null)
                            {
                                Toast tst = 
                                        Toast.makeText(
                                                _appContext, 
                                                message, 
                                                Toast.LENGTH_LONG);
                                
                                tst.setGravity(Gravity.CENTER, 0,0);
                                tst.show();
                            }
                            
                            return true;
                        }
                    });
            
            _map.setOnCameraChangeListener(
                    new GoogleMap.OnCameraChangeListener()
                    {
                        @Override
                        public void onCameraChange(CameraPosition position) 
                        {
                            if (_isAutomaticCameraMove)
                            {
                                _isAutomaticCameraMove = false;
                            }
                            else
                            {
                                _isSelfCentred = false;
                            }
                        }
                    });
        }
        
        reset();
    }
    
    /**
     * Shows or hides the history points on the map given a change to the 
     * show history indicator.
     */
    private void showHistory()
    {
        Iterator<Marker> iter = _markerData.iterator();
        
        boolean firstSkipped = false;
        while(iter.hasNext())
        {
            Marker marker = iter.next();
            
            if (!firstSkipped)
            {
                // always show the departure
                firstSkipped = true;
                continue;
            }
            
            if (iter.hasNext())
            {
                marker.setVisible(_showHistory);
            }
            else
            {
                // always display the most recent marker
                marker.setVisible(true);
            }
        }
    }
    
    /**
     * Shows or hides the connections on the map given a change to the 
     * show connection indicator.
     */
    private void showConnections()
    {
        Iterator<Polyline> iter = _polylineData.iterator();
        
        while(iter.hasNext())
        {
            Polyline pLine = iter.next();
            pLine.setVisible(_showConnections);
        }
    }
    
    //! Container of plotted tracks.
    private Vector<Plot2DPositionView.TrackData> _plotTrkData;
    
    //! Container of markers.
    private List<Marker> _markerData;
    
    //! Most recent marker added to plot.
    private Marker _latestMarker;
    
    //! Container of polylines that join two markers together.
    private List<Polyline> _polylineData;
    
    //! Reference to a hooked marker.
    private Marker _hookedMarker;
    
    //! Google map object.
    private GoogleMap _map;
    
    //! Marker options object to be re-used for new markers added to the map.
    private MarkerOptions _markerOptions;
    
    // Google map camera position.
    private CameraPosition _camPos;
    
    // Latitude and longitude position for Google map.
    private LatLng _latLng;
    
    //! Indicates whether the camera move is due to an automatic change within the code.
    private boolean _isAutomaticCameraMove;
    
    //! Application context.
    private Context _appContext;
    
    //! Manages the currently set distance units.
    Distance.DISTANCE_TYPE _distanceUnits;
    
    //! Indicates whether the map is self-centred on the latest position or not.
    private boolean _isSelfCentred;
    
    //! Indicates whether the map bearing should be slaved to the current track heading or not.
    private boolean _isMapBearingSlavedToTrk;
    
    //! Set of symbol resource ids to be provided by the user of this fragment.
    SymbolResourceIds _symResourceIds;
    
    //! Reference to the last added track to the map.
    private Plot2DPositionView.TrackData _latestTrkData;
    
    //! Vincenty data object for range and azimuth.
    private Vincenty.GetRangeAzimuthData _rangeAziData;
    
    //! Indicates whether to show history or not.
    private boolean _showHistory;

    //! Indicates whether to show connections or not.
    private boolean _showConnections;
    
    //! Default number of observations and tracks to manage in the vector.
    /**
     * If we assume a minimum update rate of 5 seconds, then over three hours
     * this is 12*60*3=2160 objects.
     */
    private static final int VECTOR_INITIAL_CAPACITY = 2200;
    
    //! The increment for whenever a vector is resized upwards.
    /**
     * Add one-half hour of data: 12*30=360
     */
    private static final int VECTOR_CAPACITY_INCREMENT = 360;
    
    //! Maximum stationary speed for the tracked target (metres per second).
    private static final double MAX_STATIONARY_SPEED_MPS = 0.3;
};
