/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage target tracking structures (matrices) and algorithms.
 */
package com.madmath.tracking;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.madmath.math.Matrix;


/**
 * Class to validate the Track 2DPV filtering algorithms.
 */
public class TrackValidation2DPV 
{

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
            "/Users/geoff/Development/java/tracker/data/";
    
    //!  Epsilon bound uses to determine if two numbers are close enough to one another.
    private static final double EPSILON      = 0.0000000001;  
    
    /**
     * Function to test the Track2DPV target tracking ability using a linear 
     * Kalman filter (LKF).
     *
     * @param   stats  The statistics gathered from running the test.
     */
    public void lkfTrack2DPV(TestStats stats)
    {       
        int numBytes = 0;
        
        // initialize the input parameters
        stats.numTests       = 0;
        stats.numTestsPassed = 0;
        
        // get the path and file name
        String pathAndFile = new String(DATA_PATH);
        pathAndFile += "double/lkfValidation.bin";
        numBytes = 8;

        DataInputStream din = null;
        
        try
        {
            // create the filters
            KalmanPredictFilter pred = new KalmanPredictFilter(0.16);
            KalmanUpdateFilter  updt = new KalmanUpdateFilter();
            
            // create the track
            Track2DPV trk = new Track2DPV(pred,updt,1.5);
            
            // create the re-usable observation
            Observation2DP obs = new Observation2DP();

            // open the data input stream
            din = new DataInputStream(new FileInputStream(pathAndFile));
            
            // declare matrices
            Matrix z  = new Matrix();
            Matrix R  = new Matrix();
            Matrix F  = new Matrix();
            Matrix Q  = new Matrix();
            Matrix x  = new Matrix();
            Matrix P  = new Matrix();
            Matrix H  = new Matrix();
            Matrix K  = new Matrix();
            Matrix y  = new Matrix();
            Matrix S  = new Matrix();
            Matrix xp = new Matrix();
            Matrix Pp = new Matrix();

            // read data from file
            while(true)
            {
                // read the observation data
                final double initTimeSecs = din.readDouble();

                // increment the test counter
                stats.numTests++;
                
                // read the observation state vector
                boolean moreData = readMatrixData(din, z, numBytes);
                if (!moreData)
                {
                    break;
                }

                // read the observation covariance matrix
                moreData = readMatrixData(din, R, numBytes);
                if (!moreData)
                {
                    break;
                }

                // read the track data
                final double lastUpdateTimeSecs = din.readDouble();
                
                // read the transition matrix
                moreData = readMatrixData(din, F, numBytes);
                if (!moreData)
                {
                    break;
                }
                
                // read the process noise matrix
                moreData = readMatrixData(din, Q, numBytes);
                if (!moreData)
                {
                    break;
                }
                
                // read the predicted state vector
                moreData = readMatrixData(din, xp, numBytes);
                if (!moreData)
                {
                    break;
                }
                
                // read the predicted covariance matrix
                moreData = readMatrixData(din, Pp, numBytes);
                if (!moreData)
                {
                    break;
                }
                
                // read the mapping matrix
                moreData = readMatrixData(din, H, numBytes);
                if (!moreData)
                {
                    break;
                }
                
                // read the innovation vector
                moreData = readMatrixData(din, y, numBytes);
                if (!moreData)
                {
                    break;
                }
                
                // read the innovation covariance
                moreData = readMatrixData(din, S, numBytes);
                if (!moreData)
                {
                    break;
                }
                
                // read the Kalman gain matrix
                moreData = readMatrixData(din, K, numBytes);
                if (!moreData)
                {
                    break;
                }
                
                // read the track state vector
                moreData = readMatrixData(din, x, numBytes);
                if (!moreData)
                {
                    break;
                }
                
                // read the covariance matrix
                moreData = readMatrixData(din, P, numBytes);
                if (!moreData)
                {
                    break;
                }
                
                try
                {
                    boolean diffFound = false;
                    
                    // initialize the observation
                    obs.init(z, R, initTimeSecs);

                    // if the first test case, then initialize the track only
                    if (stats.numTests==1)
                    {
                        trk.init(obs);
                        
                        // validate
                        if (trk.getLastUpdateTime() != lastUpdateTimeSecs)
                        {
                            diffFound = true;
                        }
                        
                        final Matrix tx = trk.getX();
                        final Matrix tP = trk.getP();
                        
                        diffFound = diffFound || !tx.compare(x,EPSILON,true);
                        diffFound = diffFound || !tP.compare(P,EPSILON,true);
                    }
                    else
                    {
                        // predict the track forward in time
                        trk.predict(obs.getInitTime());
                        
                        // update the track with the observation
                        trk.update(obs);
                        
                        // validate
                        if (trk.getLastUpdateTime() != lastUpdateTimeSecs)
                        {
                            diffFound = true;
                        }
                        
                        // compare the F and Q matrices
                        final Matrix tF = pred.getF();
                        final Matrix tQ = pred.getQ();
                        final double tq = pred.getQNoise();
                        Q.mult(tq);

                        diffFound = diffFound || !tF.compare(F,EPSILON,true);
                        diffFound = diffFound || !tQ.compare(Q,EPSILON,true);
                        
                        // compare the predicted state vector and covariance matrix
                        final Matrix txp = pred.getX();
                        final Matrix tPp = pred.getP();

                        diffFound = diffFound || !txp.compare(xp,EPSILON,true);
                        diffFound = diffFound || !tPp.compare(Pp,EPSILON,true);  
                        
                        // compare the innovation vector and the innovation covariance
                        final Matrix ty = updt.getY();
                        final Matrix tS = updt.getS();

                        diffFound = diffFound || !ty.compare(y,EPSILON,true);
                        diffFound = diffFound || !tS.compare(S,EPSILON,true);  
                        
                        // compare the H matrix and the Kalman gain matrix
                        final Matrix tH = updt.getH();
                        final Matrix tK = updt.getK();

                        diffFound = diffFound || !tH.compare(H,EPSILON,true);
                        diffFound = diffFound || !tK.compare(K,EPSILON,true);    
                        
                        // compare the updated state vector and covariance matrix
                        final Matrix tx = trk.getX();
                        final Matrix tP = trk.getP();
  
                        diffFound = diffFound || !tx.compare(x,EPSILON,true);
                        diffFound = diffFound || !tP.compare(P,EPSILON,true);    
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
            System.out.println("lkfTrack2DPV exception raised: " + e.toString());
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
     * Reads matrix data from file
     */
    private boolean readMatrixData(DataInputStream din, Matrix mtx, final int numBytes)
    {
        boolean moreData = true;
        
        try
        {
            short numRows = din.readShort();
            short numCols = din.readShort();

            if (numRows > 0 && numCols > 0)
            {
                // create the matrix
                if(mtx==null)
                {
                    mtx = new Matrix(numRows,numCols);
                }
                else
                {
                    mtx.resize(numRows,numCols);
                }
                
                // read the data into a buffer
                int numBytesToRead = numRows*numCols*numBytes;
                byte data[] = new byte[numBytesToRead];
                din.read(data);
        
                // copy the buffer into the matrix
                mtx.copy(numRows,numCols,data);
            }
            else
            {
                mtx = new Matrix();
            }
        }
        catch(final EOFException e)
        {
            moreData = false;
        }
        catch(final Exception e)
        {
            System.out.println("TrackValidation2DPV::readMatrixData - exception: " + e.toString());
        }
        
        return moreData;
    }

    /**
     * Main program.
     */
    public static void main(String[] args) 
    {
        TrackValidation2DPV validator = new TrackValidation2DPV();
        
        TestStats stats = validator.new TestStats();
        
        validator.lkfTrack2DPV(stats);
        System.out.println("LKF Validaation on 2DPV track.........." + 
                stats.numTestsPassed  + "/" + stats.numTests);

    }

}
