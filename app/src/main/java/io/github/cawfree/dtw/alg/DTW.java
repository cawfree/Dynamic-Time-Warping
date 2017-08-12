package io.github.cawfree.dtw.alg;

/**
 *  A class that implements the awesome Dynamic Time Warping algorithm.
 *  Absolutely all credit for this implementation goes to the developers of the Gesture and Activity Recognition Toolkit, (GART).
 *  @http://trac.research.cc.gatech.edu/GART/browser/GART/weka/edu/gatech/gart/ml/weka/DTW.java?rev=9
 **/
public final class DTW {

    /** Defines the result for a Dynamic Time Warping operation. */
    public static class Result {
        /* Member Variables. */
        private final int[][] mWarpingPath;
        private final double  mDistance;
        /** Constructor. */
        public Result(final int[][] pWarpingPath, final double pDistance) {
            // Initialize Member Variables.
            this.mWarpingPath = pWarpingPath;
            this.mDistance    = pDistance;
        }
        /* Getters. */
        public final int[][] getWarpingPath() { return this.mWarpingPath; }
        public final double     getDistance() { return this.mDistance;    }
    }

    /** Default constructor for a class which implements dynamic time warping. */
    public DTW() { }

	public DTW.Result compute(final float[] pSample, final float[] pTemplate) {
        // Declare Iteration Constants.
        final int lN = pSample.length;
        final int lM = pTemplate.length;
        // Ensure the samples are valid.
        if(lN == 0 || lM == 0) {
            // Assert a bad result.
            return new DTW.Result(new int[][]{ /* No path data. */ }, Double.NaN);
        }
        // Define the Scalar Qualifier.
              int lK = 1;
        // Allocate the Warping Path. (Math.max(N, M) <= K < (N + M).
        final int[][]    lWarpingPath  = new int[lN + lM][2];
        // Declare the Local Distances.
		final double[][] lL            = new double[lN][lM];
        // Declare the Global Distances.
		final double[][] lG            = new double[lN][lM];
        // Declare the MinimaBuffer.
        final double[]   lMinimaBuffer = new double[3];
        // Declare iteration variables.
        int i, j;
        // Iterate the Sample.
		for(i = 0; i < lN; i++) {
            // Fetch the Sample.
            final float lSample = pSample[i];
            // Iterate the Template.
			for(j = 0; j < lM; j++) {
                // Calculate the Distance between the Sample and the Template for this Index.
				lL[i][j] = this.getDistanceBetween(lSample, pTemplate[j]);
			}
		}

		// Initialize the Global.
		lG[0][0] = lL[0][0];
		
		for(i = 1; i < lN; i++) {
			lG[i][0] = lL[i][0] + lG[i - 1][0];
		}

		for(j = 1; j < lM; j++) {
			lG[0][j] = lL[0][j] + lG[0][j - 1];
		}

		for (i = 1; i < lN; i++) {
			for (j = 1; j < lM; j++) {
                // Accumulate the path.
				lG[i][j] = (Math.min(Math.min(lG[i-1][j], lG[i-1][j-1]), lG[i][j-1])) + lL[i][j];
			}
		}

		// Update iteration varaibles.
		i = lWarpingPath[lK - 1][0] = (lN - 1);
		j = lWarpingPath[lK - 1][1] = (lM - 1);

        // Whilst there are samples to process...
		while ((i + j) != 0) {
            // Handle the offset.
			if(i == 0) {
                // Decrement the iteration variable.
				j -= 1;
			}
			else if(j == 0) {
                // Decrement the iteration variable.
				i -= 1;
			}
			else {
                // Update the contents of the MinimaBuffer.
                lMinimaBuffer[0] = lG[i - 1][j];
                lMinimaBuffer[1] = lG[i][j - 1];
                lMinimaBuffer[2] = lG[i - 1][j - 1];
                // Calculate the Index of the Minimum.
				final int lMinimumIndex = this.getMinimumIndex(lMinimaBuffer);
                // Declare booleans.
                final boolean lMinIs0 = (lMinimumIndex == 0);
                final boolean lMinIs1 = (lMinimumIndex == 1);
                final boolean lMinIs2 = (lMinimumIndex == 2);
                // Update the iteration components.
                i -= (lMinIs0 || lMinIs2) ? 1 : 0;
                j -= (lMinIs1 || lMinIs2) ? 1 : 0;
			}
			// Increment the qualifier.
			lK++;
            // Update the Warping Path.
			lWarpingPath[lK - 1][0] = i;
			lWarpingPath[lK - 1][1] = j;
		}

        // Return the Result. (Calculate the Warping Path and the Distance.)
        return new DTW.Result(this.reverse(lWarpingPath, lK), ((lG[lN - 1][lM - 1]) / lK));
	}
	
	/** Changes the order of the warping path, in increasing order. */
	private int[][] reverse(final int[][] pPath, final int pK) {
        // Allocate the Path.
		final int[][] lPath = new int[pK][2];
        // Iterate.
		for(int i = 0; i < pK; i++) {
            // Iterate.
			for (int j = 0; j < 2; j++) {
                // Update the Path.
				lPath[i][j] = pPath[pK - i - 1][j];
			}
		}
		// Return the Allocated Path.
		return lPath;
	}
	
	/** Computes a distance between two points. */
	protected double getDistanceBetween(double p1, double p2) {
        // Calculate the square error.
		return (p1 - p2) * (p1 - p2);
	}

	/** Finds the index of the minimum element from the given array. */
	protected final int getMinimumIndex(final double[] pArray) {
		// Declare iteration variables.
        int    lIndex = 0;
		double lValue = pArray[0];
        // Iterate the Array.
		for(int i = 1; i < pArray.length; i++) {
            // .Is the current value smaller?
            final boolean lIsSmaller = pArray[i] < lValue;
            // Update the search metrics.
            lValue = lIsSmaller ? pArray[i] : lValue;
            lIndex = lIsSmaller ?         i : lIndex;
		}
		// Return the Index.
		return lIndex;
	}

}
