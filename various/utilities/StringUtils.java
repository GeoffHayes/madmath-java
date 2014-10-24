/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage miscellaneous utilities.
 */
package com.madmath.utilities;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Class that provides static string manipulation utilities/tools.

 */
public class StringUtils 
{
    
    /**
     * Pads (with zeros) the double value according to the number of digits in
     * the integer portion, and the number of digits in the decimal portion.
     * 
     * @param   val       The value to be padded with zeros and converted to a
     *                    string.
     * @param   intSize   The number of integers in the integer portion, padded
     *                    to intSize.  If zero, then all digits in the integer
     *                    part of the number are shown.
     *                    
     * @param   decSize   The number of integers in the decimal portion, padded
     *                    to decSize.
     *                    
     * @return  Returns the double value padded with zeros and digit-limited
     *          according to the requested integer size and decimal size.
     *          
     * @warning Intended for small double values only; unexpected string formatting
     *          may occur for large numbers.
     */
    public static String Pad(final double val, final int intSize, final int decSize)
    {
        final boolean isNegative = (val<0.0);
        
        String str = String.valueOf(Math.abs(val));
        

        int maxInt = 1;
        
        for(int i=1;i<=Math.abs(intSize);i++)
        {
            maxInt *= 10;
        }

        maxInt--;  // some integer that is 0, 9, 99, etc.

        int index = str.indexOf('.');
        
        if (index >= 0)
        {
            int strIntSize = index;
            if (strIntSize > intSize && intSize != 0)
            {
                // need to replace the integer portion of the string with the
                // max integer allowed given the intSize
                str = String.valueOf(maxInt) + str.substring(index);
            }
            else if (intSize != 0)
            {
                // need to pad with zeros
                while(strIntSize < intSize)
                {
                    str = "0" + str;
                    strIntSize++;
                }
            }
            
            // recalculate the index of the decimal
            index = str.indexOf('.');
            
            // just grab the decimal portion up to decSize
            if (decSize > 0)
            {   
                final int strLen = str.length();
                int lenAfterDec  = strLen-index-1;
                str = str.substring(0,Math.min(index+decSize+1,strLen));

                // pad?
                while (lenAfterDec < decSize)
                {
                    str = str + '0';
                    lenAfterDec++;
                }
            }
            else
            {
                // ignore the decimal portion
                str = str.substring(0,index);
            }
        }
        
        if (isNegative)
        {
            str = "-" + str;
        }

        return str;
    }
    
    /**
     * Pads (with zeros) the integer value according to the number of digits in
     * the integer portion.
     * 
     * @param   val       The value to be padded with zeros and converted to a
     *                    string.
     * @param   intSize   The number of integers in the integer portion, padded
     *                    to intSize.
     *                    
     * @return  Returns the integer value padded with zeros and digit-limited
     *          according to the requested integer size and decimal size.
     */
    public static String Pad(final int val, final int intSize)
    {
        String str = new String("");
        
        int maxInt = 1;
        
        for(int i=1;i<=Math.abs(intSize);i++)
        {
            maxInt *= 10;
        }

        int intPart  = val;
        
        maxInt--;  // some integer that is 0, 9, 99, etc.
        
        if (intPart > maxInt)
        {
            intPart = maxInt; 
        }
        else
        {
            // need to pad with zeros
            int temp = 10;
            while (temp < maxInt)
            {
                if (intPart < temp)
                {  
                    str += "0";
                }
                temp *= 10;
            } 
        }
        
        str += Integer.toString(intPart) ;
        
        return str;
    }
    
    //! Enumerated type to represent the different formats for the string representation of time.
    public enum DATETIME_FORMAT_TYPE
    {
        HH_MM_SS,       /**< Enumeration for the HH:MM:SS time format. */
        YYYYMMDD_HHMMSS /**< Enumeration for the year/month/day_hour/minute/sec format. */
    };
    
    /**
     * Converts a timestamp to the format HH:MM:SS where HH is 
     * hours, MM is minutes, and SS is seconds.
     * 
     * @param   timestampMsecs   The input timestamp in milliseconds.
     * @param   format           The format used for the conversion of the
     *                           timestamp into the string equivalent.
     * 
     * @return  The timestamp as a string for the desired format.
     */
    public static String toString(final double timestampMsecs, final DATETIME_FORMAT_TYPE format)
    {
        final Date date = new Date((long) timestampMsecs);
        String str      = new String("");
        
        switch (format)
        {
            case HH_MM_SS:
            { 
                _dateTimeFormatter.applyLocalizedPattern("HH:mm:ss");
                str = _dateTimeFormatter.format(date);
                break;
            }
            case YYYYMMDD_HHMMSS:
            {
                _dateTimeFormatter.applyLocalizedPattern("yyyyMMdd_HHmmss");
                str = _dateTimeFormatter.format(date);
                break;
            }
        }
        
        return str;
    }
    
    //! Static simple data formatter to be used internally.
    private static final SimpleDateFormat _dateTimeFormatter = new SimpleDateFormat();
};
