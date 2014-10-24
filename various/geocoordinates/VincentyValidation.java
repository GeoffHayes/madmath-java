/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage geographic coordinate systems (classes, structures, algorithms).
 */
package com.madmath.geocoordinates;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.Math;


/**
 * Class to validate the Vincenty library.
 */
public class VincentyValidation {

    /**
     * Helper class to manage test statistics.
     */
    public class TestStats
    {
        public TestStats()
        {
            numTests=0;
            numTestsPassed=0;
        }
        int numTests;
        int numTestsPassed;
    };
    
    //! Path to data files to be used for validation of the matrix class.
    private static final String DATA_PATH  = 
            "/Users/geoff/Development/java/vincenty/data/";
    
    //!  Epsilon bound uses to determine if two numbers are close enough to one another.
    private static final double EPSILON      = 0.0000000001;  
    private static final double EPSILON_MTRS = 0.001; 
    
    /**
     * Function to test for the Vincenty latitude and longitude (getLatLong) and
     * range and azimuth (getRangeAzimuth) functions.
     *
     * @param   stats  The statistics gathered from running the test.
     */
    public static void VincentyTest(TestStats stats)
    {       
        // initialize the input parameters
        stats.numTests       = 0;
        stats.numTestsPassed = 0;
        
        // get the path and file name
        String pathAndFile = new String(DATA_PATH);
        pathAndFile += "double/vincentyDataTest.bin";
        
        DataInputStream din = null;
        
        try
        {
            // open the data input stream
             din = new DataInputStream(new FileInputStream(pathAndFile));
             
            // declare the Vincenty objects
             Vincenty.GetLatLonData latLonData         = new Vincenty.GetLatLonData();
             Vincenty.GetRangeAzimuthData rangeAziData = new Vincenty.GetRangeAzimuthData();
            
            // read data from file
            while(true)
            {
                // read the position data
                final double refLatRads = din.readDouble();
                final double refLonRads = din.readDouble();
                final double objLatRads = din.readDouble();
                final double objLonRads = din.readDouble();
                
                // read the Matlab truth data
                final double rangeMtrsTruth = din.readDouble();
                final double fwdAziRadTruth = din.readDouble();
                final double revAziRadTruth = din.readDouble();
                
                stats.numTests++;
                
                // calculate the range and azimuth given the two positions
                Position refPosition = new Position(refLatRads, refLonRads, 
                        Position.POS_VALUE_TYPE.RADIANS);
                
                Position objPosition = new Position(objLatRads, objLonRads,
                        Position.POS_VALUE_TYPE.RADIANS);
                
                try
                {
                    Vincenty.GetRangeAzimuth(refPosition, objPosition, 
                            rangeAziData);
                    
                    // calculate the latitude and longitude given the reference position and
                    // range and azimuth
                    Vincenty.GetLatLon(refPosition,  rangeAziData._rangeMtrs,
                            rangeAziData._fwdAzimuthRads, latLonData);
                    
                    // validate
                    boolean diffFound = false;
                    
                    if (Math.abs(rangeAziData._rangeMtrs - rangeMtrsTruth) > EPSILON_MTRS)
                    {
                        System.out.println("range diff, truth: " + rangeMtrsTruth + 
                                " actual: " + rangeAziData._rangeMtrs);
                        diffFound = true;
                    }
                    
                    if (Math.abs(rangeAziData._fwdAzimuthRads - fwdAziRadTruth) > EPSILON)
                    {
                        System.out.println("fwdAzi diff, truth: " + fwdAziRadTruth + 
                                " actual: " + rangeAziData._fwdAzimuthRads);
                        diffFound = true;
                    }
                    
                    if (Math.abs(rangeAziData._revAzimuthRads -
                            revAziRadTruth) > EPSILON)
                    {
                        System.out.println("revAzi diff, truth: " + revAziRadTruth + 
                                " actual: " + rangeAziData._revAzimuthRads);
                        diffFound = true;
                    }
                    
                    final double objLatActual =  latLonData._position.
                            getLatitude(Position.POS_VALUE_TYPE.RADIANS);
                    
                    if (Math.abs(objLatActual - objLatRads) > EPSILON)
                    {
                        System.out.println("objLat diff, truth: " + objLatRads + 
                                " actual: " + objLatActual );
                        diffFound = true;
                    }
                    
                    final double objLonActual =  latLonData._position.
                            getLongitude(Position.POS_VALUE_TYPE.RADIANS);
                    
                    if (Math.abs(objLonActual - objLonRads) > EPSILON)
                    {
                        System.out.println("objLon diff, truth: " + objLonRads + 
                                " actual: " + objLonActual );
                        diffFound = true;
                    }
    
                    if (!diffFound)
                    {
                        stats.numTestsPassed++;
                    }       
                    
                }
                catch(final Exception e)
                {
                    System.out.println("Exception at test " + stats.numTests + ": "  +
                    		e.toString());
                }
            }
        }
        catch(final FileNotFoundException e)
        {
            System.out.println("Could not open file " + pathAndFile);
        }
        catch(final EOFException e)
        {
            // deliberately left blank
        }
        catch(final Exception e)
        {
            System.out.println("VincentyTest exception raised: " + e.toString());
        }
        
        if (din != null)
        {
            try 
            {
                din.close();
            } 
            catch (IOException e) 
            {
                // deliberately left blank
            }
        }
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) 
    {
        VincentyValidation vin = new VincentyValidation();
        
        TestStats stats = vin.new TestStats();
        
        VincentyValidation.VincentyTest(stats);
        System.out.println("Testing Vincenty direct and inverse methods.........." + 
                stats.numTestsPassed  + "/" + stats.numTests);

    }

}
