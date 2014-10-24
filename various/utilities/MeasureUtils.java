/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage miscellaneous utilities.
 */
package com.madmath.utilities;

import com.madmath.measures.Distance;
import com.madmath.measures.Speed;

/**
 * Class which provides static methods/functions that are suitable for the
 * manipulation of Measures (distance, speed, time, etc.)
 */
public class MeasureUtils
{
    /**
     * Returns the equivalent speed units given the distance units.
     * 
     * @param   distUnits   The distance units.
     */
    public static Speed.SPEED_TYPE ToSpeed(final Distance.DISTANCE_TYPE distUnits)
    {
        Speed.SPEED_TYPE spdUnits = Speed.SPEED_TYPE.FEET_PER_SEC;
        
        switch (distUnits)
        {
            case KILOMETRES:
            {
                spdUnits = Speed.SPEED_TYPE.KILOMETRES_PER_HOUR;
                break;
            }
            case METRES:
            {
                spdUnits = Speed.SPEED_TYPE.METRES_PER_SEC;
                break;
            }
            case MILES:
            {
                spdUnits = Speed.SPEED_TYPE.MILES_PER_HOUR;
                break;
            }
            case YARDS:
            {
                spdUnits = Speed.SPEED_TYPE.YARDS_PER_SEC;
                break;
            }
            case FEET:
            {
                spdUnits = Speed.SPEED_TYPE.FEET_PER_SEC;
                break;
            }
            case NAUTICAL_MILES:
            {
                spdUnits = Speed.SPEED_TYPE.KNOTS;
                break;
            }
        }
        
        return spdUnits;
    }
    
    /**
     * Returns the equivalent distance units given the speed units.
     * 
     * @param   spdUnits   The speed units.
     */
    public static Distance.DISTANCE_TYPE ToDistance(final Speed.SPEED_TYPE spdUnits)
    {
        Distance.DISTANCE_TYPE distUnits = Distance.DISTANCE_TYPE.FEET;
        
        switch (spdUnits)
        {
            case KILOMETRES_PER_HOUR:
            {
                distUnits = Distance.DISTANCE_TYPE.KILOMETRES;
                break;
            }
            case METRES_PER_SEC:
            {
                distUnits = Distance.DISTANCE_TYPE.METRES;
                break;
            }
            case MILES_PER_HOUR:
            {
                distUnits = Distance.DISTANCE_TYPE.MILES;
                break;
            }
            case YARDS_PER_SEC:
            {
                distUnits = Distance.DISTANCE_TYPE.YARDS;
                break;
            }
            case FEET_PER_SEC:
            {
                distUnits = Distance.DISTANCE_TYPE.FEET;
                break;
            }
            case KNOTS:
            {
                distUnits = Distance.DISTANCE_TYPE.NAUTICAL_MILES;
                break;
            }
        }
        
        return distUnits;
    }
    
};
