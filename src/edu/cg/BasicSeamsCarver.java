package edu.cg;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;


public class BasicSeamsCarver extends ImageProcessor {
	
	// An enum describing the carving scheme used by the seams carver.
	// VERTICAL_HORIZONTAL means vertical seams are removed first.
	// HORIZONTAL_VERTICAL means horizontal seams are removed first.
	// INTERMITTENT means seams are removed intermittently : vertical, horizontal, vertical, horizontal etc.
	public static enum CarvingScheme {
		VERTICAL_HORIZONTAL("Vertical seams first"),
		HORIZONTAL_VERTICAL("Horizontal seams first"),
		INTERMITTENT("Intermittent carving");
		
		public final String description;
		
		private CarvingScheme(String description) {
			this.description = description;
		}
	}
	
	// A simple coordinate class which assists the implementation.
	protected class Coordinate{
		public int X;
		public int Y;
		public Coordinate(int X, int Y) {
			this.X = X;
			this.Y = Y;
		}
	}
	
	protected class Seam{
		private int[] pixels;
		private int[] seamShifts;
		private int tailPosition;
	
		public Seam(int length){
			pixels = new int[length];
			seamShifts = new int[length];
			tailPosition = length - 1;
		}
	
		public void addPixelToTail(int j){
			pixels[tailPosition] = j;
			tailPosition --;
		}
	
		public void shiftSeam(int i, int shift){
			seamShifts[i] = shift;
		}
	
		public int getPixCol(int i){
			return pixels[i];
		}
	
		public int getColShift(int i){
			return seamShifts[i];
		}
	}
	
	private int numberOfHorizontalSeamsToCarve;
	private int numberOfVerticalSeamsToCarve;
	private long[][] energyMatrix;
	private long[][] costMatrix;
	private int[][] imageMatrix;
	private char[][] trackingtMatrix;
	private int currWidthorHight;
	private int numOfSeams;
	private boolean horizontalOperation; // (True for adding seams and False for removing seams)
	private boolean verticalOperation; // (True for adding seams and False for removing seams)

	public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
			int outWidth, int outHeight, RGBWeights rgbWeights) {
		super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
		
		this.numberOfHorizontalSeamsToCarve = Math.abs(workingImage.getHeight() - outHeight);
		this.numberOfVerticalSeamsToCarve = Math.abs(workingImage.getWidth() - outWidth);
		this.costMatrix = new long[outWidth][outHeight];
		if (outHeight > workingImage.getHeight())
		{
			horizontalOperation = true;
		} else {
			horizontalOperation = false;
		}
		if (outWidth > workingImage.getWidth())
		{
			verticalOperation = true;
		} else {
			verticalOperation = false;
		}
	}

	public BufferedImage carveImage(CarvingScheme carvingScheme) {

		BufferedImage greyScaled = greyscale();
		calculateCostMatrix();
		/*
		1. create greyScaled image
		2. calculate costMatrix
		3. find min seam
		4. remove or add and update the pic and the costMatrix
		5. repeat 3,4
		*/
	}

	private BufferedImage showSeams(boolean showVerticalSeams, int seamColorRGB) {
		Seam[] seams = new Seam[numOfSeams];
		Set<Integer> seamsInRow;

		// remove and store seams
		for (int i = 0; i < numOfSeams; i++) {
			calculateEnergy();
			calculateCostMatrix();
			Seam seam = findMinSeam();

			logger.log("Storing seam");
			seams[i] = seam;
			removeSeam(seam);
		}

		convertSeamPositions(seams);
		BufferedImage outImage = newEmptyInputSizedImage();
		for (int i = 0; i < inHeight; i++) {
			seamsInRow = new HashSet<Integer>();

			// add seams positions per row
			for (int k = 0; k < numOfSeams; k++) {
				seamsInRow.add(seams[k].getPixCol(i) + seams[k].getColShift(i));
			}

			for (int j = 0; j < inWidth; j++) {
				if (seamsInRow.contains(j)) {
					outImage.setRGB(j, i, seamColorRGB);
				} else {
					outImage.setRGB(j, i, workingImage.getRGB((j), i));
				}
			}
		}
		return outImage;
	}

	private void calculateCostMatrix(){
		costMatrix = new long[inHeight][currWidth];
		trackingtMatrix = new char[inHeight][currWidth];
		long min, left, right, up;

		logger.log("Calculating cost matrix");

		// use dynamic programming to calculate minimal seam cost
		for (int i = 0; i < inHeight; i++) {
			for (int j = 0; j < currWidth; j++) {

				// first row
				if (i == 0) {
					costMatrix[i][j] = energyMatrix[i][j];
					trackingtMatrix[i][j] = 's';
				} else {
					up = costMatrix[i - 1][j] + forwardLookingCost(i, j, 'u');
					right = (j < currWidth - 1) ? costMatrix[i - 1][j + 1] + forwardLookingCost(i, j, 'r') : Long.MAX_VALUE;
					left = (j > 0) ? costMatrix[i - 1][j - 1] + forwardLookingCost(i, j, 'l') : Long.MAX_VALUE;
					min = Math.min(left, Math.min(up, right));
					costMatrix[i][j] = energyMatrix[i][j] + min;

					if (min == right) {
						trackingtMatrix[i][j] = 'r';
					} else if (min == up) {
						trackingtMatrix[i][j] = 'u';
					} else {
						trackingtMatrix[i][j] = 'l';
					}
				}
			}
		}
	}

	private long forwardLookingCost(int i, int j, char dir) {
		long res;

		if (j == currWidth - 1) {
			res = toGray(imageMatrix[i][j - 1]);
		} else if (j == 0) {
			res = toGray(imageMatrix[i][j + 1]);
		} else {
			res = Math.abs(toGray(imageMatrix[i][j + 1]) - toGray(imageMatrix[i][j - 1]));
		}

		switch (dir) {
			case 'l':
				res += Math.abs(toGray(imageMatrix[i - 1][j]) - toGray(imageMatrix[i][j - 1]));
				break;
			case 'r':
				res += Math.abs(toGray(imageMatrix[i - 1][j]) - toGray(imageMatrix[i][j + 1]));
				break;
			default:
				break;
		}
		return res;
	}

	private int toGray(int rgb){
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = (rgb & 0xFF);

		int grayLevel = (r + g + b) / 3;
		return grayLevel;
	}

	private void initialCalculations() {
		energyMatrix = new long[inHeight][currWidth];
		imageMatrix = new int[inHeight][currWidth];

		calculateEnergy();
		for (int i = 0; i < inHeight; i++) {
			for (int j = 0; j < currWidth; j++) {
				imageMatrix[i][j] = workingImage.getRGB(j, i);
			}
		}
	}

	private void calculateEnergy() {
		energyMatrix = new long[inHeight][currWidth];
		int nextCol, nextRow, currPix, nextColPix, nextRowPix;
		double di, dj;

		for (int i = 0; i < inHeight; i++) {
			for (int j = 0; j < currWidth; j++) {
				if (j == currWidth - 1) {
					nextCol = j - 1;
				} else{
					nextCol = j + 1;
				}

				if (i == inHeight - 1){
					nextRow = i - 1;
				} else {
					nextRow = i + 1;
				}

				currPix = toGray(imageMatrix[i][j]);
				nextColPix = toGray(imageMatrix[i][nextCol]);
				nextRowPix = toGray(imageMatrix[nextRow][j]);

				//calculate magnitude
				dj = Math.pow(currPix - nextColPix, 2);
				di = Math.pow(currPix - nextRowPix, 2);
				energyMatrix[i][j] = (int) Math.sqrt((di + dj) / 2);
			}
		}
	}

	private Seam findMinSeam() {
		Seam seam = new Seam(inHeight);
		int col = 0;
		long min = Long.MAX_VALUE;
		for (int j = 0; j < currWidth; j++) {
			if (costMatrix[inHeight - 1][j] <= min) {
				min = costMatrix[inHeight - 1][j];
				col = j;
			}
		}

		logger.log("Found min: " + min + " at index: " + col);
		logger.log("Backtracking");

		// backtrack
		for (int i = inHeight - 1; i >= 0; i--) {
			seam.addPixelToTail(col);
			switch (trackingtMatrix[i][col]) {
				case 'l':
					col = col - 1;
					break;
				case 'r':
					col = col + 1;
					break;
				case 's':
					break;
				default:
					break;
			}
		}
		return seam;
	}

	private void removeSeam(Seam seam) {
		currWidth--;
		int[][] tmpImageMatrix = new int[inHeight][currWidth];
		int shift, shiftCol;

		logger.log("Removing seam");
		for (int i = 0; i < inHeight; i++) {
			shift = 0;
			shiftCol = seam.getPixCol(i);
			for (int j = 0; j < currWidth; j++) {
				if (j == shiftCol) {
					shift = 1;
				}
				tmpImageMatrix[i][j] = imageMatrix[i][j + shift];
			}
		}
		imageMatrix = tmpImageMatrix;
	}

	private BufferedImage reduceImageWidth() {

		// remove seams
		for (int i = 0; i < numOfSeams; i++) {
			calculateEnergy();
			calculateCostMatrix();
			Seam seam = findMinSeam();
			removeSeam(seam);
		}

		BufferedImage outImage = newEmptyOutputSizedImage();
		for (int i = 0; i < inHeight; i++) {
			for (int j = 0; j < currWidth; j++) {
				outImage.setRGB(j, i, imageMatrix[i][j]);
			}
		}
		return outImage;
	}

	private BufferedImage increaseImageWidth() {
		Seam[] seams = new Seam[numOfSeams];
		Set<Integer>  seamsInRow;
		int shift;

		// remove and store seams
		for (int i = 0; i < numOfSeams; i++) {
			calculateEnergy();
			calculateCostMatrix();
			Seam seam = findMinSeam();

			logger.log("Storing seam");
			seams[i] = seam;
			removeSeam(seam);
		}

		convertSeamPositions(seams);
		BufferedImage outImage = newEmptyOutputSizedImage();
		for (int i = 0; i < inHeight; i++) {
			shift = 0;
			seamsInRow = new HashSet<Integer>();

			// add seams positions per row
			for (int k = 0; k < numOfSeams; k++) {
				seamsInRow.add(seams[k].getPixCol(i) + seams[k].getColShift(i));
			}

			for (int j = 0; j < outWidth; j++) {
				outImage.setRGB(j, i, workingImage.getRGB((j - shift), i));
				if (seamsInRow.contains(j)) {
					shift++;
				}
			}
		}
		return outImage;
	}
	
	private void convertSeamPositions(Seam[] seams) {
		int shift;

		for (int i = 0; i < inHeight; i++) {
			for (int k = 0; k < numOfSeams; k++) {
				shift = 0;
				for (int l = k - 1; l >= 0; l--) {

					//account for previous shifts
					if (seams[k].getPixCol(i) + shift >= seams[l].getPixCol(i)) {
						shift++;
					}
				}
				seams[k].shiftSeam(i, shift);
			}
		}
	}
}
