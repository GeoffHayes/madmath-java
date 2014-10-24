/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage math-centric structures (i.e. matrices) and algorithms.
 * 
 * Matrix algorithms taken from:
 *
 * 1.  Numerical Linear Algebra, Lloyd N. Trefethen and David Bau, SIAM 1997.
 * 2.  Introductory Linear Algebra with Applications, Bernard Kolman,
 *     Macmillan 1993.
 */
package com.madmath.math;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * A class used to represent matrices with double data type elements.
 * 
 */
public class Matrix
{
    /**
     * Exception for matrices that have not had memory allocated to them.
     */
    public class MatrixNotCreatedException extends Exception
    {
        /**
         * Class constructor.  
         */
        MatrixNotCreatedException()
        {
            super();
        }
        
        /**
         * Class constructor. 
         * 
         * @param   msg   String message that can be specified at time of 
         *                exception creation.
         */
        MatrixNotCreatedException(String msg)
        {
            super(msg);
        }
    };
    
    /**
     * Exception for invalid row-column pair index into the matrix.
     */
    public class MatrixInvalidIndexException extends Exception
    {
        /**
         * Class constructor.  
         */
        MatrixInvalidIndexException()
        {
            super();
        }
        
        /**
         * Class constructor. 
         * 
         * @param   msg   String message that can be specified at time of 
         *                exception creation.
         */
        MatrixInvalidIndexException(String msg)
        {
            super(msg);
        }
    };
    
    /**
     * Exception for invalid double value being used arithmetically against 
     * the matrix.
     */
    public class MatrixInvalidDoubleException extends Exception
    {
        /**
         * Class constructor.  
         */
        MatrixInvalidDoubleException()
        {
            super();
        }
        
        /**
         * Class constructor. 
         * 
         * @param   msg   String message that can be specified at time of 
         *                exception creation.
         */
        MatrixInvalidDoubleException(String msg)
        {
            super(msg);
        }
    };
    
    /**
     * Exception for incompatible dimensions when one matrix is being applied
     * arithmetically against another matrix, or if the dimension of the matrix
     * prevents some operation (i.e. inversion) from being applied to it.
     */
    public class MatrixIncompatibleDimensionsException extends Exception
    {
        /**
         * Class constructor.  
         */
        MatrixIncompatibleDimensionsException()
        {
            super();
        }
        
        /**
         * Class constructor. 
         * 
         * @param   msg   String message that can be specified at time of 
         *                exception creation.
         */
        MatrixIncompatibleDimensionsException(String msg)
        {
            super(msg);
        }
    };
    
    /**
     * Exception for a matrix that is not invertible.
     */
    public class MatrixNotInvertibleException extends Exception
    {
        /**
         * Class constructor.  
         */
        MatrixNotInvertibleException()
        {
            super();
        }
        
        /**
         * Class constructor. 
         * 
         * @param   msg   String message that can be specified at time of 
         *                exception creation.
         */
        MatrixNotInvertibleException(String msg)
        {
            super(msg);
        }
    };
    
    /**
     * Exception for a matrix that cannot be LUP factorizable.
     */
    public class MatrixLUPFactorizationFailureException extends Exception
    {
        /**
         * Class constructor.  
         */
        MatrixLUPFactorizationFailureException()
        {
            super();
        }
        
        /**
         * Class constructor. 
         * 
         * @param   msg   String message that can be specified at time of 
         *                exception creation.
         */
        MatrixLUPFactorizationFailureException(String msg)
        {
            super(msg);
        }
    };
    
    /**
     * Default matrix constructor; intializes data members to
     * default values
     */
    public Matrix()
    {
        _rows     = 0;
        _cols     = 0;
        _data     = null;
        _tempData = null;
    }
  
    /**
     * Matrix constructor for a rows x columns matrix.
     * 
     * @param   rows   The number of rows in the matrix.
     * @param   cols   The number of columns in the matrix.
     * 
     * @note    The number of rows and cols must be positive (>0).
     * @throws  MatrixNotCreatedException
     */
    public Matrix(final int rows, final int cols) throws MatrixNotCreatedException
    {
        init(rows,cols);
    }
    
    /**
     * Vector constructor a rows x 1 matrix (vector).
     * 
     * @param   rows   The number of rows in the matrix.
     * 
     * @note    The number of rows must be positive (>0).
     * @throws  MatrixNotCreatedException
     */
    public Matrix(final int rows) throws MatrixNotCreatedException
    {
       this(rows, 1);
    }
    
    /**
     * Matrix copy constructor.
     * 
     * @param   mtx   Matrix to copy.
     * 
     * @throws  MatrixNotCreatedException
     */
    public Matrix(final Matrix mtx) throws MatrixNotCreatedException
    {
        if(mtx._rows > 0 && mtx._cols > 0)
        {
            _rows = mtx._rows;
            _cols = mtx._cols;
            
            try
            {
                _data     = new double[_rows*_cols];
                
                System.arraycopy(mtx._data, 0, _data, 0, _rows*_cols);
            }
            catch(final Exception e)
            {
                throw new MatrixNotCreatedException("Memory couldn't be allocated for matrix.");
            }
        }
        else
        {
            throw new MatrixNotCreatedException("Matrix to copy is of zero dimension");
        }
    }
    
    
    /**
     * Resizes the matrix given the new number of rows and columns.
     * 
     * @param   rows   The new number of rows in the matrix.
     * @param   cols   The new number of columns in the matrix.
     * 
     * @throws MatrixNotCreatedException
     */
    public void resize(final int rows, final int cols) throws MatrixNotCreatedException
    {
        // do we need to resize?
        if (rows*cols == _rows*_cols)
        {
            _rows = rows;
            _cols = cols;
        }
        else if (_data!=null && rows*cols <= _data.length)
        {
            // still have enough memory
            _rows = rows;
            _cols = cols;
        }
        else
        {
            init(rows,cols);
        }
    }
    
    /**
     * Resizes the vector given the new number of rows.
     * 
     * @param   rows   The new number of rows in the matrix.
     * 
     * @throws MatrixNotCreatedException
     */
    public void resize(final int rows) throws MatrixNotCreatedException
    {
        this.resize(rows, 1);
    }
    
    /**
     * Returns the number of rows in the matrix.
     * 
     * @return  Positive integer number of rows in the matrix.
     */
    public int getRows()
    {
        return _rows;
    }
    
    /**
     * Returns the number of columns in the matrix.
     * 
     * @return  Positive integer number of columns in the matrix.
     */
    public int getCols()
    {
        return _cols;
    }
    
    /**
     * Returns the matrix element given by the (row,column) pair.
     * 
     * @param   row  The "row" index into the matrix.
     * @param   col  The "column" index into the matrix.
     * 
     * @return  The matrix element at the (row,column) pair.
     * 
     * @throws MatrixInvalidIndexException
     */
    public double at(final int row, final int col) throws MatrixInvalidIndexException
    {
        if (row > 0 && row <= _rows && col > 0 && col <= _cols)
        {
            return _data[(row-1)*_cols + (col-1)];
        }
        else
        {
            throw new MatrixInvalidIndexException();
        }
    }
    
    /** 
     * Sets the value of the matrix at the given position.
     * 
     * @param   row  The "row" index into the matrix.
     * @param   col  The "column" index into the matrix.
     * @param   val  The new value to be set at (row,col).
     * 
     * @throws MatrixInvalidIndexException
     */
    public void setAt(final int row, final int col, final double val)
        throws MatrixInvalidIndexException
    {
        if (row > 0 && row <= _rows && col > 0 && col <= _cols)
        {
            _data[(row-1)*_cols + (col-1)] = val;
        }
        else
        {
            throw new MatrixInvalidIndexException();
        }
    }
    
    /**
     * Returns the vector element given by the row index.
     * 
     * @param   row  The "row" index into the matrix.
     * 
     * @return  The matrix element at the row index.
     * 
     * @throws MatrixInvalidIndexException
     */
    public double at(final int row) throws MatrixInvalidIndexException
    {
        return this.at(row,1);
    }
    
    /** 
     * Sets the value of the vector at the given position.
     * 
     * @param   row  The "row" index into the matrix.
     * @param   val  The new value to be set at (row,1).
     * 
     * @throws MatrixInvalidIndexException
     */
    public void setAt(final int row, final double val)
        throws MatrixInvalidIndexException
    {
        this.setAt(row,1,val);
    }
    
    @Override
    /**
     * Compares the contents of two matrices and returns whether they
     * are identical.
     * 
     * @param   obj   Object to compare to.
     */
    public boolean equals(Object obj)
    {
        boolean areEqual = false;
        
        if (obj instanceof Matrix)
        {
            final Matrix mtx = (Matrix)obj;
            
            if(_cols == mtx._cols && _rows == mtx._rows)
            {
                areEqual = true;
                
                for (int i=0;i<_rows*_cols; ++i)
                {
                    if (_data[i] != mtx._data[i])
                    {
                        areEqual = false;
                        break;
                    }
                }
            }
        }
        
        return areEqual;
    }
    
    /**
     * Compares the contents of two matrices to within some tolerance and returns
     * whether the two are identical (or near-identical).
     * 
     * @param   mtx       The matrix to compare to.
     * @param   tol       The tolerance allowed (anything greater is rejected).
     * @param   verbose   If true, writes differences to the console.
     * 
     * @retval True if the two matrices are considered identical or near-identical.
     * @retval False if at least the difference between two co-located elements of
     *         the two matrices exceeds the tolerance.
     */
    public boolean compare(final Matrix mtx, final double tol, final boolean verbose)
    {
        boolean nearIdentical = true;
        
        // ensure they are both of the same dimension
        if (mtx == null || mtx.getRows() != _rows || mtx.getCols() != _cols)
        {
            nearIdentical = false;
        }
        else
        {
            for (int i=0;i<_rows*_cols; ++i)
            {
                if (Math.abs(_data[i] - mtx._data[i])>tol)
                {
                    nearIdentical = false;
                    
                    if (verbose)
                    {
                        System.out.println("::compare diff at i= " + i);
                        System.out.println(mtx.toString());
                        System.out.println(this.toString());
                    }
                    
                    break;
                }
            }
        }
        
        return nearIdentical;
    }
    
    /**
     * Copies the contents of the passed matrix.
     * 
     * @param   mtx   The matrix to copy.
     * 
     * @throws  MatrixNotCreatedException
     */
    public void copy(final Matrix mtx) 
            throws MatrixNotCreatedException
    {
        // check for self-assignment
        if (this != mtx)
        {
            this.resize(mtx._rows, mtx._cols);
            
            System.arraycopy(mtx._data, 0, _data, 0, _rows*_cols);
        }
    }
    
    /**
     * Copies the contents of an array into the matrix given the input
     * number of rows and columns.
     *
     * @param   numRows   The number of rows in the data array.
     * @param   numCols   The number of columns in the data array.
     * @param   data      The array data to be copied into the matrix.
     *
     * @throws  MatrixNotCreatedException
     */
    public void copy (int numRows, int numCols, final byte[] data) 
            throws MatrixNotCreatedException
    {
        // allocate memory only if there is a difference in the array size
        // from the matrix size
        if (_rows*_cols != numRows*numCols)
        {
            try
            {
                init(numRows,numCols);
            }
            catch(final MatrixNotCreatedException e)
            {
                throw e;
            }
        }

        _rows = numRows;
        _cols = numCols;
        
        // now copy the data
        ByteBuffer buf = ByteBuffer.wrap(data);
        
        for(int i=0; i<_rows*_cols; ++i)
        {
            _data[i] = buf.getDouble(i*8);
        }
    }

    
    @Override
    /**
     * Returns a string representation of the matrix.
     * 
     * @return The matrix as a string.
     */
    public String toString()
    {
        String output = new String("Matrix " + _rows + "x" + _cols + " :\n");
        
        NumberFormat nf = DecimalFormat.getInstance();
        nf.setMinimumFractionDigits(8);
        nf.setMaximumFractionDigits(8);
        
        for (int i=0; i<_rows; ++i)
        {
            for (int j=0; j<_cols; ++j)
            {
                output = output + nf.format(_data[i*_cols + j]) + "\t";
            }
            output = output + "\n";
        }
        output = output + "\n";
        
        return output;
    }
    
    /**
     * Writes the matrix to a data stream.
     * 
     * @param   stream   The stream to write the data to.
     * 
     * @throws IOException 
     */
    public void write(DataOutputStream stream) throws IOException
    {
        // write the number of rows and columns
        stream.writeInt(_rows);
        stream.writeInt(_cols);
        
        // write out each element in the matrix
        for(int i=0; i<_rows*_cols; ++i)
        {
            stream.writeDouble(_data[i]);
        }
    }
    
    /**
     * Reads the matrix from a data stream.
     * 
     * @param   stream   The stream to read the data from.
     * 
     * @throws IOException 
     */
    public void read(DataInputStream stream) throws IOException
    {
        // write the number of rows and columns
        _rows = stream.readInt();
        _cols = stream.readInt();
        
        // write out each element in the matrix
        for(int i=0; i<_rows*_cols; ++i)
        {
            _data[i] = stream.readDouble();
        }
    }
    
    /**
     * Assigns a common value to all elements in the matrix.
     * 
     * @param   val   The value common to all elements in the matrix.
     * 
     * @throws  MatrixInvalidDoubleException
     */
    public void assign(final double val) throws MatrixInvalidDoubleException
    {
        if (!Double.isNaN(val) && !Double.isInfinite(val))
        {
            for (int i=0;i<_rows*_cols;++i)
            {
                _data[i] = val;
            }
        }
        else
        {
            throw new MatrixInvalidDoubleException();
        }
    }
    
    /**
     * Adds a common value to all elements in the matrix.
     * 
     * @param   val   The value to be added to all elements in the matrix.
     * 
     * @throws  MatrixInvalidDoubleException
     */
    public void add(final double val)  throws MatrixInvalidDoubleException
    {
        if (!Double.isNaN(val) && !Double.isInfinite(val))
        {
            for (int i=0;i<_rows*_cols;++i)
            {
                _data[i] += val;
            }
        }
        else
        {
            throw new MatrixInvalidDoubleException();
        }
    }
    
    /**
     * Subtracts a common value to all elements in the matrix.
     * 
     * @param   val   The value to be subtracted from all elements in the matrix.
     * 
     * @throws  MatrixInvalidDoubleException
     */
    public void sub(final double val)  throws MatrixInvalidDoubleException
    {
        if (!Double.isNaN(val) && !Double.isInfinite(val))
        {
            for (int i=0;i<_rows*_cols;++i)
            {
                _data[i] -= val;
            }
        }
        else
        {
            throw new MatrixInvalidDoubleException();
        }
    }
    
    /**
     * Multiplies a common value against all elements in the matrix.
     * 
     * @param   val   The value to be multiplied against all elements in the matrix.
     * 
     * @throws  MatrixInvalidDoubleException
     */
    public void mult(final double val)  throws MatrixInvalidDoubleException
    {
        if (!Double.isNaN(val) && !Double.isInfinite(val))
        {
            for (int i=0;i<_rows*_cols;++i)
            {
                _data[i] *= val;
            }
        }
        else
        {
            throw new MatrixInvalidDoubleException();
        }
    }
    
    /**
     * Performs element-by-element addition of two identically sized matrices.
     * 
     * @param   mtx   The matrix to add.
     * 
     * @throws  MatrixIncompatibleDimensionsException
     */
    public void add(final Matrix mtx) throws MatrixIncompatibleDimensionsException
    {
        if (_rows == mtx._rows && _cols == mtx._cols)
        {
            for (int i=0; i<_rows*_cols; ++i)
            {
                _data[i] += mtx._data[i];
            }
        }
        else
        {
            throw new MatrixIncompatibleDimensionsException();
        }
    }
    
    /**
     * Performs element-by-element subtraction of two identically sized matrices.
     * 
     * @param   mtx   The matrix to subtract.
     * 
     * @throws  MatrixIncompatibleDimensionsException
     */
    public void sub(final Matrix mtx) throws MatrixIncompatibleDimensionsException
    {
        if (_rows == mtx._rows && _cols == mtx._cols)
        {
            for (int i=0; i<_rows*_cols; ++i)
            {
                _data[i] -= mtx._data[i];
            }
        }
        else
        {
            throw new MatrixIncompatibleDimensionsException();
        }
    }
    
    /**
     * Performs matrix multiplication of two matrices provided that they are of
     * compatible dimensions (i.e. the number of columns in the "left" matrix is
     * identical to the number of rows in the "right" matrix (left and right 
     * referring to the position of the matrix relative to the * operator).
     * 
     * @param   mtx   The matrix to multiply.
     * 
     * @throws  MatrixIncompatibleDimensionsException
     */
    public void mult(final Matrix mtx) throws MatrixIncompatibleDimensionsException, MatrixNotCreatedException
    {
        if (_cols == mtx._rows)
        {
            try
            {
                int tempRows = _rows;
                int tempCols = mtx._cols;
                
                if (_tempData==null || 
                        _tempData.length < tempRows*tempCols)
                {
                    _tempData = new double[tempRows*tempCols];
                }
                
                for (int i=0; i<tempRows; ++i)
                {
                    for (int j=0; j<tempCols; ++j)
                    {
                        double  sum = 0.0;

                        for (int k=0; k<_cols; ++k)
                        {
                            sum += (_data[i*_cols+k])*(mtx._data[j + k*mtx._cols]);
                        }

                        _tempData[i*tempCols + j] = sum;
                    }
                }

                // copy the temp data into the self matrix
                this.resize(tempRows,tempCols);
                System.arraycopy(_tempData, 0, _data, 0, _rows*_cols);
            }
            catch(final MatrixNotCreatedException e)
            {
                throw e;
            }
        }
        else
        {
            throw new MatrixIncompatibleDimensionsException();
        }
    }
    
    /**
     * Converts the matrix into its transpose.
     */
    public void t() 
        throws MatrixNotCreatedException
    {
        int tempRows = _cols;
        int tempCols = _rows;
        
        if (_tempData==null || 
                _tempData.length < tempRows*tempCols)
        {
            _tempData = new double[tempRows*tempCols];
        }
        
        for (int i=0; i<_rows; ++i)
        {
            for (int j=0; j<_cols; ++j)
            {
                _tempData[j*_rows + i] = _data[i*_cols + j];
            }
        }
        
        this.resize(tempRows,tempCols);
        System.arraycopy(_tempData, 0, _data, 0, _rows*_cols);
    }
    
    /**
     * Returns a boolean indicating if the matrix is symmetric or not.  A
     * matrix is considered symmetric if it is square and row i is identical
     * to column i.
     *
     * @retval   true if the matrix is symmetric.
     * @retval   false if the matrix is not symmetric.
     */
    public boolean isSymmetric() 
    {
        boolean status = (_rows==_cols) && _rows>0;

        if (status)
        {
            for (int i=0; i<_rows; ++i)
            {
                for (int j=i; j<_cols; ++j)
                {
                    if (_data[i*_cols+j] !=
                            _data[j*_rows+i])
                    {
                        status = false;
                        break;
                    }
                }
                
                if (!status)
                {
                    break;
                }
            }
        }

        return status;
    }
    
    /**
     * Sets the matrix as the identity matrix i.e. ones along the diagonal
     * and zeros elsewhere.
     *
     * @throws  std::logic_error if the matrix is not square.
     */
    public void setAsIdentity() throws MatrixIncompatibleDimensionsException
    {
        if ((_rows==_cols) && _rows>0)
        {
            for (int i=0; i<_rows; ++i)
            {
                // set the element on the diagonal to 1
                _data[i*_rows + i] = 1.0; 
                
                for (int j=i+1; j<_cols; ++j)
                {
                     _data[i*_rows + j] = 0.0;
                     _data[j*_rows + i] = 0.0;
                }
            }
        }
        else
        {
            throw new MatrixIncompatibleDimensionsException();
        }
    }
    
    /**
     * Calculates the LU factorization of the given matrix where L is a unit
     * lower triangular matrix and U is an upper triangular matrix.  This
     * algorithm makes use of partial pivoting (is more stable) than that
     * without and so provides a permutation matrix P. i.e. PA = LU where A
     * is the self matrix.
     *
     * @param   L   Unit lower triangular matrix (i.e. ones along its
     *              diagonal).
     * @param   U   Upper triangular matrix.
     * @param   P   Permutation matrix indicating the permutated rows of the
     *              original matrix.
     *
     * @return  The number of permuatations that have been invoked against P.
     * 
     * @throws  MatrixIncompatibleDimensionsException
     * @throws  MatrixNotCreatedException
     * @throws  MatrixLUPFactorizationFailureException
     *
     * @note    Assumes that the "self" matrix is square.
     */
    public int getLUPFactorization(Matrix L, Matrix U, Matrix P) 
            throws MatrixIncompatibleDimensionsException, 
                   MatrixNotCreatedException,
                   MatrixLUPFactorizationFailureException
    {
        int numPerms = 0;

        // assume valid for square matrices only
        if ((_rows==_cols) && _rows>0)
        {
            try
            {
                U.copy(this); 
                L.resize(_rows,_cols);
                L.setAsIdentity();
                P.resize(_rows,_cols);
                P.setAsIdentity();
            }
            catch(MatrixNotCreatedException e)
            {
                throw e;
            }

            for (int k=0; k<_rows-1; ++k)
            {
                // find that row of U such that |u{ik}| is maximized
                double  maxUik        = java.lang.Math.abs(U._data[k*_rows + k]);
                int     maxAt         = k;
                boolean doInterchange = false;

                for (int i=k+1; i<_rows; ++i)
                {
                    if (java.lang.Math.abs(U._data[i*_rows + k]) > maxUik)
                    {
                        maxUik = java.lang.Math.abs(U._data[i*_rows + k]);
                        maxAt  = i;
                        doInterchange = true;
                    }
                }

                // interchange the two rows if necessary
                if (doInterchange)
                {
                    for (int i=k; i<_rows; ++i)
                    {
                        double temp = U._data[k*_rows+i];
                        U._data[k*_rows+i] = U._data[maxAt*_rows+i];
                        U._data[maxAt*_rows+i] = temp;
                    }

                    if (k>0)
                    {
                        for (int i=0; i<=k-1; ++i)
                        {
                            double temp = L._data[k*_rows+i];
                            L._data[k*_rows+i] = L._data[maxAt*_rows+i];
                            L._data[maxAt*_rows+i] = temp;
                        }
                    }

                    for (int i=0; i<_rows; ++i)
                    {
                        double temp = P._data[k*_rows+i];
                        P._data[k*_rows+i] = P._data[maxAt*_rows+i];
                        P._data[maxAt*_rows+i] = temp;
                    }

                    numPerms++;
                }

                for (int j=k+1; j<_rows; ++j)
                {
                    double denom = U._data[k*_rows+k];

                    if (java.lang.Math.abs(denom)==0.0)
                    {
                        // no solution
                        throw new MatrixLUPFactorizationFailureException();
                    }

                    double temp = U._data[j*_rows+k] / denom;

                    L._data[j*_rows+k] = temp;

                    for (int i=k;i<_rows;++i)
                    {
                        U._data[j*_rows+i] -= temp*U._data[k*_rows+i];
                    }
                }
            }
        }
        else
        {
            throw new MatrixIncompatibleDimensionsException("Matrix is not square");
        }
        
        return numPerms;
    }

    /**
     * Function returns the determinant of a matrix.
     *
     * @return   The determinant of the matrix; 0 may mean that no
     *           determinant exists.
     *
     * @throws   MatrixIncompatibleDimensionsException
     * @throws   MatrixNotCreatedException
     * @throws   MatrixLUPFactorizationFailureException
     */
    public double det() throws MatrixIncompatibleDimensionsException, 
                               MatrixNotCreatedException,
                               MatrixLUPFactorizationFailureException
    {
        double determ = 0;

        if (_rows==_cols)
        {
            // calculate the determinant using the LUP factorization
            int numPerms = 0;
            Matrix L = new Matrix();
            Matrix U = new Matrix();
            Matrix P = new Matrix();

            try
            {
                try
                {
                    numPerms = getLUPFactorization(L,U,P);
                }
                catch(final MatrixLUPFactorizationFailureException e)
                {
                    throw e;
                }

                // we have calculated P*A = L*U => A = P^-1*U*P
                // => determ(A) = determ(P^-1*U*P)
                // => determ(A) = determ(P^-1)*determ(L)*determ(U)
                // as P is a permuation matrix, then determ(P^-1) = (-1)^n
                // where n is the number of rows that have been permuted in
                // P
                // as L and U are triangular, the determinant of each is
                // just the product of the elements along the diagonal
                //
                // but since L is unit lower triangular, then its
                // determinant is just one and so can be ignored
                //
                // but since the determinant of the permutation matrix P is
                // just (-1)^numPerms, then if numPerms is positive then its
                // determinant is +1 (and so can be ignored) else it is
                // -1 and so has to be applied.
                double determU = U._data[0];

                for (int i=1;i<_rows;++i)
                {
                    determU *= U._data[i*_rows + i];
                }

                determ = determU;

                // if the number of permuations is odd, then negate the
                // calculated determinant
                if (numPerms % 2 != 0)
                {
                    determ = -determ;
                }
            }
            catch(MatrixNotCreatedException e)
            {
                throw e;
            }
            catch(MatrixIncompatibleDimensionsException e)
            {
                throw e;
            }
        }
        else
        {
            throw new MatrixIncompatibleDimensionsException("Matrix is not square.");
        }

        return determ;
    }
    
    /**
     * Calculates the inverse of the assumed square matrix using the LUP
     * factorization.  Note that it is assumed that the diagonal elements
     * along the L and U matrices are non-zero.  (If any should happen to
     * be zero, then the matrix is considered invertible.)
     *
     * @throws MatrixIncompatibleDimensionsException
     * @throws MatrixNotCreatedException
     * @throws MatrixNotInvertibleException
     * @throws MatrixLUPFactorizationFailureException
     * @throws MatrixInvalidDoubleException
     */
    public void i() throws MatrixIncompatibleDimensionsException, 
                             MatrixNotCreatedException,
                             MatrixNotInvertibleException,
                             MatrixLUPFactorizationFailureException,
                             MatrixInvalidDoubleException
    {
        if (_rows==_cols)
        {
            // calculate the LUP factorization
            Matrix L = new Matrix();
            Matrix U = new Matrix();
            Matrix P = new Matrix();

            try
            {
                getLUPFactorization(L,U,P);
            }
            catch(final MatrixLUPFactorizationFailureException e)
            {
                throw e;
            }

            try
            {
                // create the inverse matrix
                Matrix inverse = new Matrix(_rows, _cols);

                // create a vector to solve against
                Matrix b = new Matrix(_rows);

                // temp vectors
                Matrix y = new Matrix(_rows);
                Matrix x = new Matrix(_rows);                   

                // to solve the matrix inverse problem we will use forward
                // and backwards substitution to solve for it:
                // inv(A) = inv(L)*inv(U)*P
                // b will be the vector that will be used for each column in
                // the identity matrix
                for (int i=0; i<_rows; ++i)
                {
                    // set b as the ith column of the identity matrix
                    b.assign(0.0);
                    b._data[i] = 1.0;

                    // compute P*b
                    Matrix Pb = new Matrix(P);
                    Pb.mult(b);

                    // use forward substitution to solve for Ly = Pb
                    for (int j=0; j<_rows; ++j)
                    {
                        final double denom = L._data[j*_rows+j];

                        if (denom==0.0)
                        {
                            throw new MatrixNotInvertibleException();
                        }

                        y._data[j] = Pb._data[j]/denom;

                        for (int k=0; k<j; ++k)
                        {
                            y._data[j] -= L._data[j*_rows+k]*y._data[k]/denom;
                        }
                    }

                    // use backward substitution to solve for Ux = y
                    for (int j=_rows-1; j>=0; --j)
                    {
                        final double denom = U._data[j*_rows+j];

                        if (denom==0.0)
                        {
                            throw new MatrixNotInvertibleException();
                        }

                        x._data[j] = y._data[j]/denom;

                        for (int k=j+1; k<_rows; ++k)
                        {
                            x._data[j] -=
                                    U._data[j*_rows+k]*x._data[k]/denom;
                        }
                    }

                    // update the ith column of the inverse matrix with x
                    for (int j=0; j<_rows;++j)
                    {
                        inverse._data[j*_rows+i] = x._data[j];
                    }
                }

                this.copy(inverse);
            }
            catch(final MatrixNotCreatedException e)
            {
                throw e;
            }
            catch(final MatrixIncompatibleDimensionsException e)
            {
                throw e;
            }
            catch(final MatrixInvalidDoubleException e)
            {
                throw e;
            }
        }
        else
        {
            throw new MatrixIncompatibleDimensionsException();
        }
    }

    
    /**
     * Initializes the matrix to one specified for the given number of rows
     * columns.
     * 
     * @param   rows   The number of rows in the matrix.
     * @param   cols   The number of columns in the matrix.
     * 
     * @note    The number of rows and cols must be positive (>0).
     * @throws  Throws a MatrixNotCreatedException exception if the matrix
     *          cannot be created (i.e. memory not allocated).
     */
    private void init(final int rows, final int cols) throws MatrixNotCreatedException
    {
        _rows     = 0;
        _cols     = 0;
        _data     = null;
        
        // ensure that the rows and columns are valid
        if (rows > 0 && cols > 0)
        {
            // allocate memory only if necessary
            if ((_rows*_cols) != (rows*cols))
            {               
                try
                {
                    _data     = new double[rows*cols];
                }
                catch(final Exception e)
                {
                    // re-initialize data members
                    _rows     = 0;
                    _cols     = 0;
                    _data     = null;

                    System.out.print("Matrix::Matrix - exception when allocating memory: " +
                                     e.toString());
                    
                    throw new MatrixNotCreatedException("Memory couldn't be allocated for matrix.");
                }
            }
            
            _rows = rows;
            _cols = cols;
        }
        else
        {
            throw new MatrixNotCreatedException("Invalid rows and/or cols input parameters.");
        }
    }
 
    //! Indicates the number of rows in the matrix.
    int _rows;
    
    //! Indicates the number of columns in the matrix.
    int _cols;
    
    //! 2D array of matrix data.
    double[] _data;
    
    //! 2D array of temp data.
    /**
     * To be used in place of allocating memory for certain operations (mult, 
     * transpose, etc.).  Is initially the same size as that for the 
     * instantiated matrix but may grow as needed.
     */
    double[] _tempData;
}
