/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage geographic coordinate systems (classes, structures, algorithms).
 */
package com.madmath.geocoordinates;

import java.lang.Exception;
import java.lang.Math;

/**
 * Vincenty's formulae are two related iterative methods used in geodesy to 
 * calculate the distance between two points on the surface of a spheroid, 
 * developed by Thaddeus Vincenty (1975a) They are based on the assumption 
 * that the figure of the Earth is an oblate spheroid, and hence are more 
 * accurate than methods such as great-circle distance which assume a 
 * spherical Earth.
 * 
 * The first (direct) method computes the location of a point which is a 
 * given distance and azimuth (direction) from another point. The second 
 * (inverse) method computes the geographical distance and azimuth between 
 * two given points. They have been widely used in geodesy because they are
 * accurate to within 0.5 mm (0.020?) on the Earth ellipsoid.
 *
 * http://en.wikipedia.org/wiki/Vincenty's_formulae
 * http://www.movable-type.co.uk/scripts/latlong-vincenty-direct.html
 * http://www.movable-type.co.uk/scripts/latlong-vincenty.html
 * http://www.sosmath.com/trig/Trig5/trig5/trig5.html
 * http://williams.best.vwh.net/avform.htm
 * http://www.ga.gov.au/earth-monitoring/geodesy.html
 *
 *[A] Direct and Inverse Solutions of Geodesics on the Ellipsoid with
 *    Application of Nested Equations, T. Vincenty, April 1975 (Survey
 *    Review XXII)
 *
 *[B] Geodetic Inverse Solution Between Antipodal Points, T. Vincenty,
 *    August 1975.
 */
public final class Vincenty 
{
    /**
     * Exception for no latitude and longitude solution.
     */
    public static class NoLatLonSolutionException extends Exception
    {
        /**
         * Class constructor.  
         */
        NoLatLonSolutionException()
        {
            super();
        }
        
        /**
         * Class constructor. 
         * 
         * @param   msg   String message that can be specified at time of 
         *                exception creation.
         */
        NoLatLonSolutionException(String msg)
        {
            super(msg);
        }
    };
    
    /**
     * Exception for no range and azimuth solution.
     */
    public static class NoRangeAzimuthSolutionException extends Exception
    {
        /**
         * Class constructor.  
         */
        NoRangeAzimuthSolutionException()
        {
            super();
        }
        
        /**
         * Class constructor. 
         * 
         * @param   msg   String message that can be specified at time of 
         *                exception creation.
         */
        NoRangeAzimuthSolutionException(String msg)
        {
            super(msg);
        }
    };
    
    
    /**
     * Class encapsulating data to be returned via a call to the getLatLon
     * function.
     */
    public static class GetLatLonData
    {
        // Default constructor
        public GetLatLonData()
        {
            _position  = new Position();
            _revAzimuthRads      = 0.0;
        }

        //! The calculated (via GetLatLong) position.
        public Position _position;  
        
        //! The calculated reverse azimuth in radians, positive from north.
        /**
         * The reverse azimuth is that from the second position to the first.
         */
        public double   _revAzimuthRads;
    };
    
    /**
     * Class encapsulating data to be returned via a call to the getRangeAzimuth
     * function.
     */
    public static class GetRangeAzimuthData
    {
        // Default constructor
        public GetRangeAzimuthData()
        {
            _rangeMtrs                = 0.0;
            _fwdAzimuthRads = 0.0;
            _revAzimuthRads = 0.0;
        }
        
        //! The calculated forward azimuth in radians (positive from north).
        /**
         * The forward azimuth is that from the first position to the second.
         */
        public double   _fwdAzimuthRads;
        
        //! The calculated reverse azimuth in radians (positive from north).
        /**
         * The reverse azimuth is that from the second position to the first.
         */
        public double   _revAzimuthRads;
        
        //! The calculated range in metres between the two positions.
        public double   _rangeMtrs;
    };
    
    /**
     * Returns the WGS-84 latitude and longitude position of an object that is
     * a fixed range and azimuth from a reference position.
     * 
     * @param   ref          Reference position latitude and longitude.
     * @param   rangeMtrs    Distance from reference position to source in metres
     * @param   azimuthRads  Angle from reference position to source in radians
     *                       (positive with respect to north)
     * @param   data         The data (position and new azimuth) returned from this
     *                       funciton.
     * 
     * @retval  True if the latitude and longitude have been calculated.
     * @retval  False if the latitude and longitude have not been calculated.
     * 
     * @note    If the rangeMtrs is negative, then it is assumed invalid and so the 
     *          returned position is identical to the reference position.
     * 
     * @throws  NoLatLonSolutionException
     */
    public static boolean GetLatLon(final Position ref, final double rangeMtrs, 
                                    final double azimuthRads, GetLatLonData data)
        throws NoLatLonSolutionException
    {   
        final int MAX_ITERATIONS = 100;
        
        // assume that a range of up to 50 centimetres is close enough to
        // zero
        final double EPSILON_M     = 0.50;
        final double EPSILON       = Math.pow(10.0,-12.0);
        boolean success = false;
        final double a = SEMI_MAJOR_AXIS_M;
        final double b = SEMI_MINOR_AXIS_M;
        
        // flattening (inverse of INVERSE_FLATTENING)
        final double f = (a-b)/a;
        
        final double s      = rangeMtrs;
        final double alpha1 = azimuthRads;
        final double phi1   = ref.getLatitude(Position.POS_VALUE_TYPE.RADIANS);
        
        final double lambda1 = ref.getLongitude(Position.POS_VALUE_TYPE.RADIANS);

        // initialize the outputs
        data._position.setLatitude(0.0, Position.POS_VALUE_TYPE.RADIANS);
        data._position.setLongitude(0.0, Position.POS_VALUE_TYPE.RADIANS);
        
        data._revAzimuthRads = 0.0;

        // handle the range
        if (Math.abs(rangeMtrs) <= EPSILON_M)
        {
            // nothing to do - the position is the same as the reference
            // position
            data._position.copy(ref);
            success = true;
        }
        else if (rangeMtrs > 0.0)
        {
            // calculate the reduced latitude (U1)
            final double tanU1 = (1.0 - f)*Math.tan(phi1);
         
            // due to 1 + tan^2x = sec^2x where secx = 1/cosx
            final double cosU1 = 1/Math.sqrt(1 + Math.pow(tanU1,2.0)); 
            final double sinU1 = tanU1*cosU1;

            // calculate the angular distance on sphere from equator to reference
            // position (sig1 of [A])
            double cosAlpha1 = Math.cos(alpha1);
            double sinAlpha1 = Math.sin(alpha1);
            
            //tanSigma1 = tanU1/cosAlpha1;                                                     // [1]
            double sigma1    = Math.atan2(tanU1, cosAlpha1);
    
            // calculate the azimuth of the geodesic at the equator
            double sinAlpha = cosU1*sinAlpha1;                                                 // [2]
            double cosAlpha = Math.sqrt(1.0 - Math.pow(sinAlpha,2.0));
    
            double uSqrd = Math.pow(cosAlpha,2.0) * (Math.pow(a,2.0) - Math.pow(b,2.0))/Math.pow(b,2.0);
    
            double A = 1.0+(uSqrd/16384.0)*(4096.0+uSqrd*(-768.0+uSqrd*(320.0-175.0*uSqrd)));  // [3]
            double B = (uSqrd/1024.0)*(256.0+uSqrd*(-128.0 + uSqrd*(74.0-47.0*uSqrd)));        // [4]

            double sigma  = s/(b*A);
            double sigmaP = 2.0*Math.PI;  // previous value
    
            double cos2sigmaM = 0.0;
            
            // iterate until little change in sigma

            // force at least one iteration of the loop
            int iter = 0;
    
            while (Math.abs(sigma - sigmaP) > EPSILON  || iter == 0)
            {
                iter++;
                
                if (iter > MAX_ITERATIONS)
                {
                    throw new NoLatLonSolutionException("max iterations reached - no convergence");
                }
                    
                // calculate the angular distance on the sphere from the equator to the
                // midpoint of the line (sigmaM)
                cos2sigmaM = Math.cos(2.0*sigma1 + sigma);                                     // [5]
                
                double sinSigma = Math.sin(sigma);
                double cosSigma = Math.cos(sigma);
                
                double deltaSigma = B*sinSigma*(cos2sigmaM + B/4*(cosSigma*        
                    (-1.0+2.0*Math.pow(cos2sigmaM,2.0)) - B/6*cos2sigmaM*(-3.0+4.0*    
                    Math.pow(sinSigma,2.0))*(-3.0+4.0*Math.pow(cos2sigmaM,2.0))));             // [6]
                
                sigmaP = sigma;
                
                sigma = s/(b*A) + deltaSigma;
                
            }
    
            final double sinSigma = Math.sin(sigma);
            final double cosSigma = Math.cos(sigma);
    
            // calculate the numer(ator) and denom(inator) of the formula that is used
            // to determine the latitude (phi2)
            double denom = Math.pow(sinAlpha,2.0) + Math.pow((sinU1*sinSigma - cosU1*cosSigma*cosAlpha1),2.0);

            // no solution if the denominator is less than or equal to zero
            if (denom >= 0.0)
            {
                denom = Math.sqrt(denom);
        
                double numer = sinU1*cosSigma + cosU1*sinSigma*cosAlpha1;
        
                final double phi2 = Math.atan2(numer, (1.0-f)*denom);                          // [8]
        
                // calculate the numer(ator) and denom(inator) of the formula that is used
                // to determine the difference in longitude on an auxiliary sphere (lambda)
                denom = cosU1*cosSigma - sinU1*sinSigma*cosAlpha1;
        
                numer = sinSigma*sinAlpha1;
        
                double lambda = Math.atan2(numer, denom);                                     // [9]
        
                // calculate the difference in longitude (L)                                  // [10]
                double C = (f/16.0)*Math.pow(cosAlpha,2.0)*(4.0+f*(4.0-3.0*Math.pow(cosAlpha,2.0))); 
                double L = lambda-(1.0-C)*f*sinAlpha*(sigma+C*sinSigma*(cos2sigmaM+           // [11]
                    cosSigma*C*(-1.0+2.0*Math.pow(cos2sigmaM,2.0))));                            
        
                // calculate the reverse azimuth (note that this is slightly different from
                // the note, and the signs in the numerator and denominator have been
                // reversed like they have been in the getRangeAzimuth function call - so
                // [12] is different)
                double alpha2 = alpha1;
                denom  = sinU1*sinSigma - cosU1*cosSigma*cosAlpha1;
                alpha2 = Math.atan2(-sinAlpha, denom);                                        // [12]
                
                // successful!
                success = true;
                data._position.setLatitude(phi2, Position.POS_VALUE_TYPE.RADIANS);
                
                double lon = lambda1 + L;

                if (lon < -Math.PI)
                {
                    lon += 2.0*Math.PI;
                }
                else if (lon > Math.PI)
                {
                    lon -= 2.0*Math.PI;
                }
                data._position.setLongitude(lon, Position.POS_VALUE_TYPE.RADIANS);
        
                data._revAzimuthRads = alpha2;
            }
        }

        return success;
    }

    /**
     * Returns the range and azimuth of an object from a reference position 
     * given the latitude and longitude pair for each.
     * 
     * @param   ref      Reference position latitude and longitude.
     * @param   pos      The object position latitude and longitude.
     * @param   data     The data (range, forward and reverse azimuths) returned
     *                   from this funciton.
     * 
     * @retval  True if the range and azimuths have been calculated.
     * @retval  False if the range and azimuths have not been calculated.
     * 
     * @throws  NoRangeAzimuthSolutionException
     */
    public static boolean GetRangeAzimuth(final Position ref, final Position pos, 
                                          GetRangeAzimuthData data)
        throws NoRangeAzimuthSolutionException
    {
        final int MAX_ITERATIONS = 100;
        final double EPSILON = Math.pow(10.0,-12.0);
        boolean success = false;
        final double a = SEMI_MAJOR_AXIS_M;
        final double b = SEMI_MINOR_AXIS_M;
        
        // flattening (inverse of INVERSE_FLATTENING)
        final double f = (a-b)/a;
       
        final double phi1    = ref.getLatitude(Position.POS_VALUE_TYPE.RADIANS);
        final double phi2    = pos.getLatitude(Position.POS_VALUE_TYPE.RADIANS);
        final double lambda1 = ref.getLongitude(Position.POS_VALUE_TYPE.RADIANS);
        final double lambda2 = pos.getLongitude(Position.POS_VALUE_TYPE.RADIANS);
        
        // initialize the outputs
        data._fwdAzimuthRads = 0.0;
        data._revAzimuthRads = 0.0;
        data._rangeMtrs                = 0.0;
        
        // capture the case where the two geogpraphic positions are coincident
        if (Math.abs(phi1-phi2)<EPSILON && Math.abs(lambda1-lambda2)<EPSILON)
        {
            // the positions are considered identical so the range and azimuth(s)
            // are zero
            success = true;
        }
        else
        {
            // calculate the reduced latitude (U1)
            final double tanU1 = (1.0 - f)*Math.tan(phi1);
            
            // due to 1 + tan^2x = sec^2x where secx = 1/cosx
            final double cosU1 = 1/Math.sqrt(1 + Math.pow(tanU1,2.0)); 
            final double sinU1 = tanU1*cosU1;
            final double U1    = Math.atan2(sinU1,cosU1);
            
            // calculate the reduced latitude (U2)
            final double tanU2 = (1.0 - f)*Math.tan(phi2);
            
            // due to 1 + tan^2x = sec^2x where secx = 1/cosx
            final double cosU2  = 1/Math.sqrt(1 + Math.pow(tanU2,2.0)); 
            final double sinU2 = tanU2*cosU2;
            final double U2    = Math.atan2(sinU2,cosU2);
            
            // calculate the difference in longitude, positive east
            double L = lambda2 - lambda1;
            
            if (L > Math.PI)
            {
                L = L - 2.0*Math.PI;
            }
            else if (L < -Math.PI)
            {
                L = L + 2.0*Math.PI;
            }
            
            double lambda  = L;                                                                // [13]
            double lambdaP = 2*Math.PI; 
            
            // iterate until little change in lambda
            
            // force at least one iteration of the loop
            int iter = 0;
        
            if (Math.abs(lambda) > Math.PI)
            {
                double LP = 0.0;
                
                // anti-podal points must be handled separately
                if (L > 0.0)
                {
                    LP = 2.0*Math.PI - L;
                }
                else if (L < 0)
                {
                    LP = -2.0*Math.PI - L;
                }
        
                lambdaP    = 0.0;
                double cosSqAlpha = 0.5;
                double cos2sigmaM = 0.0;
                double sigma      = Math.PI - Math.abs(U1+U2);
                double sinAlpha   = Math.sqrt(1-cosSqAlpha);
                double sinAlphaP  = Math.PI;
                double cosLambdaP = 0.0;
        
                // force at least one iteration of the loop
                while (Math.abs(sinAlpha-sinAlphaP)>EPSILON || iter == 0)
                {
                    iter++;
                    
                    if (iter > MAX_ITERATIONS)
                    {
                        throw new NoRangeAzimuthSolutionException("max iterations reached - no convergence");
                    }
        
                    double C = (1/16.0)*f*cosSqAlpha*(4.0+f*(4.0-3.0*cosSqAlpha));
        
                    if (cosSqAlpha == 0.0)
                    {
                        throw new NoRangeAzimuthSolutionException("divide by zero error");
                    }
        
                    cos2sigmaM = Math.cos(sigma) - 2.0*sinU1*sinU2/cosSqAlpha;
        
                    double D = (1.0-C)*f*(sigma+C*Math.sin(sigma)*(cos2sigmaM + C*Math.cos(sigma)* 
                        (-1.0+2.0*Math.pow(cos2sigmaM,2.0))));
        
                    if (D==0.0)
                    {
                        throw new NoRangeAzimuthSolutionException("divide by zero error");
                    }
        
                    sinAlphaP  = sinAlpha;
                    sinAlpha   = (LP-lambdaP)/D;
                    
                    if (Math.abs(sinAlpha) > 1.0)
                    {
                        if (sinAlpha < 0)
                        {
                            sinAlpha = -1.0;
                        }
                        else
                        {
                            sinAlpha = 1.0;
                        }
                    }
                    
                    cosSqAlpha = 1.0 - Math.pow(sinAlpha,2.0);
        
                    double denom = cosU1*cosU2;
        
                    if (denom == 0.0)
                    {
                        throw new NoRangeAzimuthSolutionException("divide by zero error");
                    }
        
                    double sinLambdaP = sinAlpha*Math.sin(sigma)/denom;
                    cosLambdaP        = Math.sqrt(1.0 - Math.pow(sinLambdaP,2.0));
        
                    lambdaP    = Math.atan2(sinLambdaP,cosLambdaP);
        
                    double sinSqSigma = Math.pow((cosU2*Math.sin(lambdaP)),2.0) + 
                        Math.pow((cosU1*sinU2+sinU1*cosU2*Math.cos(lambdaP)),2.0);
        
                    double cosSqSigma = 1.0 - sinSqSigma;
        
                    sigma = Math.atan2(Math.sqrt(sinSqSigma), Math.sqrt(cosSqSigma));
                }
        
                double sinAlpha1 = sinAlpha/cosU1;
                double cosAlpha1 = Math.sqrt(1.0-Math.pow(sinAlpha1,2.0));
                
                if ((cosU1*sinU2+sinU1*cosU2*cosLambdaP) < 0.0)
                {
                    cosAlpha1 = -cosAlpha1;
                }
        
                final double azimuth1 = Math.atan2(sinAlpha1,cosAlpha1);
        
                final double denom = -sinU1*Math.sin(sigma) + cosU1*Math.cos(sigma)*Math.cos(azimuth1);
        
                if (Math.abs(denom) == 0.0)
                {
                    throw new NoRangeAzimuthSolutionException("divide by zero error");
                }
        
                final double azimuth2 = Math.atan2(sinAlpha,denom);   
                
                // now calculate the distance
                final double e = (a*a - b*b)/(b*b);
                final double E = Math.sqrt(1.0 + e*cosSqAlpha);
                final double F = (E-1.0)/(E+1.0);
                final double A = (1.0+0.25*F*F)/(1.0-F);
                final double B = F*(1.0-3/(8*F*F));
                final double delSigma = B*Math.sin(sigma)*(cos2sigmaM+0.25*B*
                        (Math.cos(sigma)*(-1.0+2.0*Math.pow(cos2sigmaM,2.0)) -
                        1/6*B*cos2sigmaM*(-3.0+4.0*Math.pow(Math.sin(sigma),2.0))*
                        (-3.0+4.0*Math.pow(cos2sigmaM,2.0))));
                final double s = (1.0-f)*a*A*(sigma-delSigma);
                
                data._rangeMtrs                = s;
                data._fwdAzimuthRads = azimuth1;
                data._revAzimuthRads = azimuth2;
                success = true;
            }      
            else
            {
                double sigma      = 0.0;
                double cosAlpha   = 0.0;
                double sinSigma   = 0.0;
                double cosSigma   = 0.0;
                double cos2sigmaM = 0.0;
                
                while (Math.abs(lambda - lambdaP) > EPSILON || iter == 0)
                {
                    iter++;
                    
                    if (iter > MAX_ITERATIONS)
                    {
                        throw new NoRangeAzimuthSolutionException("max iterations reached - no convergence");
                    }        

                    final double sinLambda = Math.sin(lambda);
                    final double cosLambda = Math.cos(lambda);
        
                    final double sinSqSigma = Math.pow((cosU2*sinLambda),2.0) + Math.pow((cosU1*sinU2 -
                        sinU1*cosU2*cosLambda),2.0);                                           // [14]
        
                    sinSigma = Math.sqrt(sinSqSigma);
                    cosSigma = sinU1*sinU2 + cosU1*cosU2*cosLambda;               // [15]
        
                    if (Math.abs(cosSigma) == 0.0)
                    {
                        throw new NoRangeAzimuthSolutionException("divide by zero error");
                    }
        
                    sigma = Math.atan2(sinSigma, cosSigma);
        
                    final double tanSigma = sinSigma/cosSigma;                                 // [16]
        
                    if (Math.abs(sinSigma) == 0.0)
                    {
                        throw new NoRangeAzimuthSolutionException("divide by zero error");
                    }
        
                    final double sinAlpha = cosU1*cosU2*sinLambda/sinSigma;                    // [17]
        
                    cosAlpha = Math.sqrt(1.0 - Math.pow(sinAlpha,2.0));
        
                    if (Math.abs(cosAlpha) == 0.0)
                    {
                        throw new NoRangeAzimuthSolutionException("divide by zero error");
                    }
        
                    cos2sigmaM = cosSigma - 2.0*sinU1*sinU2/Math.pow(cosAlpha,2.0); // [18]
        
                    // store the previous result
                    lambdaP = lambda;
        
                    final double C = (f/16.0)*cosAlpha*cosAlpha*(4.0+f*(4.0-3.0*cosAlpha*cosAlpha)); //[10]
                    lambda = L + (1.0-C)*f*sinAlpha*(sigma+C*sinSigma*(cos2sigmaM+                   // [11]
                        cosSigma*C*(-1.0+2.0*cos2sigmaM*cos2sigmaM)));  
        
                    sigma = Math.atan2(sinSigma, cosSigma);   
                }
        
                final double uSqrd = cosAlpha*cosAlpha*(a*a - b*b)/(b*b);
        
                final double A = 1.0+(uSqrd/16384.0)*(4096.0+uSqrd*(-768.0+uSqrd*(320.0-175.0*uSqrd))); // [3]
                final double B = (uSqrd/1024.0)*(256.0+uSqrd*(-128.0 + uSqrd*(74.0-47.0*uSqrd)));       // [4]
        
                final double deltaSigma = B*sinSigma*(cos2sigmaM + B/4*(cosSigma* 
                        (-1.0+2.0*cos2sigmaM*cos2sigmaM) - B/6*cos2sigmaM*(-3.0+4.0* 
                        sinSigma*sinSigma)*(-3.0+4.0*cos2sigmaM*cos2sigmaM)));                          // [6]
        
                final double s = b*A*(sigma - deltaSigma);
        
                double denom = cosU1*sinU2 - sinU1*cosU2*Math.cos(lambda);
        
                if (Math.abs(denom) == 0.0)
                {
                    throw new NoRangeAzimuthSolutionException("divide by zero error");
                }
        
                final double azimuth1 = Math.atan2(cosU2*Math.sin(lambda),denom);              // [20]
                
                // for azimuth2 (from P2 back to P1) we set lambda to be the
                // negative of itself; with this in mind, sin(-lambda) =
                // -sin(lambda) and cos(-lambda) = cos(lambda); this results in
                // something a little different than [21] in the note
                denom = sinU1*cosU2 - cosU1*sinU2*Math.cos(lambda);
        
                if (Math.abs(denom) == 0.0)
                {
                    throw new NoRangeAzimuthSolutionException("divide by zero error");
                }
        
                final double azimuth2 = Math.atan2(-cosU1*Math.sin(lambda),denom);             //[21]
                
                data._rangeMtrs                = s;
                data._fwdAzimuthRads = azimuth1;
                data._revAzimuthRads = azimuth2;
                success = true;
            }
        }

        return success;
    }
    
    //! The length of the semi-major axis of the spheroid earth in metres.
    private final static double SEMI_MAJOR_AXIS_M  = 6378137.0;
    
    //! The length of the semi-minor axis of the spheroid earth in metres.
    private final static double SEMI_MINOR_AXIS_M  = 6356752.314245;
    
    //! Inverse flattening factor (unitless).
    private final static double INVERSE_FLATTENING = 298.257223563;
}
