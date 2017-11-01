/*
 Copyright (C) 2010 by
 * 
 * 	Cam-Tu Nguyen	ncamtu@ecei.tohoku.ac.jp ncamtu@gmail.com
 *  Xuan-Hieu Phan  pxhieu@gmail.com 
 
 *  College of Technology, Vietnamese University, Hanoi
 * 
 * 	Graduate School of Information Sciences
 * 	Tohoku University
 *
 *  JVnTextPro-v.2.0 is a free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * JVnTextPro-v.2.0 is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with  JVnTextPro-v.2.0); if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package jflexcrf;

// TODO: Auto-generated Javadoc
/**
 * The Class DoubleMatrix.
 */
public class DoubleMatrix {
    
    /** The mtrx. */
    public double[][] mtrx = null;
    
    /** The rows. */
    public int rows = 0;
    
    /** The cols. */
    public int cols = 0;
    
    /**
     * Instantiates a new double matrix.
     */
    public DoubleMatrix() {
    }
    
    /**
     * Instantiates a new double matrix.
     *
     * @param rows the rows
     * @param cols the cols
     */
    public DoubleMatrix(int rows, int cols) {
	this.rows = rows;
	this.cols = cols;
	mtrx = new double[rows][cols];
    }
    
    /**
     * Instantiates a new double matrix.
     *
     * @param rows the rows
     * @param cols the cols
     * @param mtrx the mtrx
     */
    public DoubleMatrix(int rows, int cols, double[][] mtrx) {
	this.rows = rows;
	this.cols = cols;	
	this.mtrx = new double[rows][cols];
    }
    
    /**
     * Instantiates a new double matrix.
     *
     * @param dm the dm
     */
    public DoubleMatrix(DoubleMatrix dm) {
	rows = dm.rows;
	cols = dm.cols;	
	mtrx = new double[rows][cols];
	
	for (int i = 0; i < rows; i++) {
	    for (int j = 0; j < cols; j++) {
		mtrx[i][j] = dm.mtrx[i][j];
	    }
	}
    }
    
    /**
     * Assign.
     *
     * @param val the val
     */
    public void assign(double val) {
	for (int i = 0; i < rows; i++) {
	    for (int j = 0; j < cols; j++) {
		mtrx[i][j] = val;
	    }
	}
    }
    
    /**
     * Assign.
     *
     * @param dm the dm
     */
    public void assign(DoubleMatrix dm) {
	for (int i = 0; i < rows; i++) {
	    for (int j = 0; j < cols; j++) {
		mtrx[i][j] = dm.mtrx[i][j];
	    }
	}
    }    
    
} // end of class DoubleMatrix

