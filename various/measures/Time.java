/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage math-centric structures (matrices) and algorithms.
 */
package com.madmath.measures;

import com.madmath.utilities.StringUtils;


/**
 * Encapsulates a time type of o sbject that can be used to easily convert a
 * time value from one unit type to another.
 */
public class Time 
{

    //! Enumerated type to represent the different units for any given time value.
    public enum TIME_TYPE
    {
        HOURS,        /**< Enumeration for hours. */
        MINUTES,      /**< Enumeration for minutes. */
        SECONDS,      /**< Enumeration for seconds. */
        MILLISECONDS, /**< Enumeration for milliseconds. */
        NANOSECONDS   /**< Enumeration for nanoseconds. */
    };
    
    /**
     * Default class constructor.
     */
    public Time()
    {
        this(0.0,TIME_TYPE.SECONDS);
    }
    
    /**
     * Class copy constructor.
     * 
     * @param   copy   The data to copy.
     */
    public Time(final Time copy)
    {
        this(copy._timeMilliseconds,TIME_TYPE.MILLISECONDS);
    }
    
    /**
     * Class constructor to initialize the time object data members.
     * 
     * @param   value  Time value in units given by second parameter.
     * @param   units  Unit type for time value.
     */
    public Time(final double value, final TIME_TYPE units)
    {
        _timeMilliseconds = convertToMillisecondsd(value, units);
    }
    
    /**
     * Sets the object with a new time given the specified units.
     * 
     * @param   value  Time value in units given by second parameter.
     * @param   units  Unit type for time value.
     */
    public void set(final double value, final TIME_TYPE units)
    {
        _timeMilliseconds = convertToMillisecondsd(value, units);
    }
    
    /**
     * Sets the object with a new time given the default units of
     * milliseconds.
     * 
     * @param   value  Time value in units given by second parameter.
     */
    public void set(final double value)
    {
        _timeMilliseconds = 
                convertToMillisecondsd(value, TIME_TYPE.MILLISECONDS);
    }
    
    /**
     * Copies the contents of one time object to the other.
     * 
     * @param   copy   The data to copy.
     */
    public void copy(final Time copy)
    {
        this._timeMilliseconds = copy._timeMilliseconds;
    }
    
    /**
     * Gets the time of the object in the default units of milliseconds.
     * 
     * @return  The time of the object in milliseconds.
     */
    public double get()
    {
        return _timeMilliseconds;
    }
    
    /**
     * Gets the time of the object given the specified units.
     * 
     * @param   units   Unit type for time value.
     * 
     * @retval  The time of the object converted to the specified units.
     */
    public double get(final TIME_TYPE units)
    {
        double convertedTime = 0.0;
        
        switch (units)
        {
            case HOURS:
            {
                convertedTime = _timeMilliseconds/HR_TO_MSEC;
                break;
            }
            case MINUTES:
            {
                convertedTime = _timeMilliseconds/MIN_TO_MSEC;
                break;
            }
            case SECONDS:
            {
                convertedTime = _timeMilliseconds/SEC_TO_MSEC;
                break;
            }
            case MILLISECONDS:
            {
                convertedTime = _timeMilliseconds;
                break;
            }
            case NANOSECONDS:
            {
                convertedTime = _timeMilliseconds/NSEC_TO_MSEC;
                break;
            }
        }
        
        return convertedTime;
    }
    
    @Override
    /**
     * Returns the textual string representation of the time object.
     */
    public String toString()
    {
        return "Time: " + this._timeMilliseconds + " milliseconds";
    }
    
    /**
     * Writes the Time type value to string given the unit type and format.
     * 
     * @param   units     Unit type for time value.
     * @param   intSize   The number of integers in the integer portion, padded
     *                    to intSize.
     * @param   decSize   The number of integers in the decimal portion, padded
     *                    to decSize.
     */
    public String toString(final TIME_TYPE units, 
                           final int intSize, 
                           final int decSize)
    {

        double time = get(units);
        
        return StringUtils.Pad(time, intSize, decSize);
    }
    
    /**
     * Function to convert the input time in whatever units to milliseconds.
     *
     * @param   value  Time value in units given by second parameter.
     * @param   units  Unit type for time value.
     * 
     * @retval  The input time converted to milliseconds.
     */
    private double convertToMillisecondsd(final double value, final TIME_TYPE units)
    {
        double timeInMilliseconds = 0.0;
        
        switch (units)
        {
            case HOURS:
            {
                timeInMilliseconds = value*HR_TO_MSEC;
                break;
            }
            case MINUTES:
            {
                timeInMilliseconds = value*MIN_TO_MSEC;
                break;
            }
            case SECONDS:
            {
                timeInMilliseconds = value*SEC_TO_MSEC;
                break;
            }
            case MILLISECONDS:
            {
                timeInMilliseconds = value;
                break;
            }
            case NANOSECONDS:
            {
                timeInMilliseconds = value*NSEC_TO_MSEC;
                break;
            }
        }
        
        return timeInMilliseconds;
    }
    
    
    //! The time of the object in milliseconds.
    double _timeMilliseconds;

    //! Conversion constant from nanoseconds to milliseconds
    private static final double NSEC_TO_MSEC = 1.0e-6;
    
    //! Conversion constant from seconds to milliseconds.
    private static final double SEC_TO_MSEC = 1000;
    
    //! Conversion constant from minutes to milliseconds
    private static final double MIN_TO_MSEC = 60.0*SEC_TO_MSEC;
    
    //! Conversion constant from hours to milliseconds.
    private static final double HR_TO_MSEC = 60.0*MIN_TO_MSEC;
    
}
