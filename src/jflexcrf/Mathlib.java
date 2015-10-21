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
 * The Class Mathlib.
 */
public class Mathlib {
    
    /**
     * Mult.
     *
     * @param size the size
     * @param x the x
     * @param A the a
     * @param y the y
     * @param isTransposed the is transposed
     */
    public static void mult(int size, DoubleVector x, DoubleMatrix A, 
		DoubleVector y, boolean isTransposed) {
	// isTransposed = false:	x = A * y
	// isTransposed = true:		x^t = y^t * A^t
	
	int i, j;
	
	if (!isTransposed) {
	    // for beta
	    // x = A * y
	    for (i = 0; i < size; i++) {	
		x.vect[i] = 0;
		for (j = 0; j < size; j++) {
		    x.vect[i] += A.mtrx[i][j] * y.vect[j];
		}
	    }
	    
	} else {
	    // for alpha
	    // x^t = y^t * A^t
	    for (i = 0; i < size; i++) {
		x.vect[i] = 0;
		for (j = 0; j < size; j++) {
		    x.vect[i] += y.vect[j] * A.mtrx[j][i];
		}
	    }	    
	}	
    }

} // end of class Mathlib

