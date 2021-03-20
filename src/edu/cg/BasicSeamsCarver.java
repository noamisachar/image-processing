package edu.cg;

import java.awt.image.BufferedImage;


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
	
	private String widthOp;
	private String heightOp;
	private int numOfVerSeams;
	private int numOfHorSeams;
    private boolean[][] imageMask;
    private int[][] pixelOriginX;
    private int[][] pixelOriginY;
    private int[][] vSeamDirection;
    private int[][] hSeamDirection;
	private int numVSeamsRemoved = 0;
    private int numHSeamsRemoved = 0;
    private int[][] greyScale;
	
	public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
			int outWidth, int outHeight, RGBWeights rgbWeights) {
		super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);

		numOfVerSeams = Math.abs(outWidth - inWidth);
		numOfHorSeams = Math.abs(outHeight - inHeight);
        
		if (inWidth < 2 | inHeight < 2)
            throw new RuntimeException("Can not apply seam carving: workingImage is too small");

        if (numOfVerSeams > inWidth / 2 || numOfHorSeams > inHeight)
            throw new RuntimeException("Can not apply seam carving: too many seams...");

		if (outWidth > inWidth)
            widthOp = "up";
        else if (outWidth < inWidth)
			widthOp = "down";
        else
            widthOp = "none";

		if (outHeight > inHeight)
            heightOp = "up";
        else if (outHeight < inHeight)
			heightOp = "down";
        else
            heightOp = "none";

		BufferedImage carvingImage = greyscale();
        pixelOriginX = new int[inHeight][inWidth];
        pixelOriginY = new int[inHeight][inWidth];
        pushForEachParameters();
        setForEachInputParameters();
        forEach((y, x) -> pixelOriginX[y][x] = x);
        forEach((y, x) -> pixelOriginY[y][x] = y);
        popForEachParameters();
        vSeamDirection = new int[inHeight][inWidth];
        hSeamDirection = new int[inHeight][inWidth];
        greyScale = new int[inHeight][inWidth];
        pushForEachParameters();
        setForEachInputParameters();
        forEach((y, x) -> greyScale[y][x] = rgbToSingle(carvingImage.getRGB(x,y)));
        popForEachParameters();
	}

	private int rgbToSingle(int rgb) {
        return rgb & 0xFF;
    }


    private int getGreyscaleDiff(int y1, int x1,
                                 int y2, int x2) {
        return Math.abs(greyScale[y1][x1] - greyScale[y2][x2]);
    }

    private void setForEachCarvingParameters() {
        setForEachParameters(inWidth - numVSeamsRemoved, inHeight - numHSeamsRemoved);
    }
	
	public BufferedImage carveImage(CarvingScheme carvingScheme) {
		int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
		// TODO :  Perform Seam Carving. Overall you need to remove 'numberOfVerticalSeamsToCarve' vertical seams
				// and 'numberOfHorizontalSeamsToCarve' horizontal seams from the image.
				// Note you must consider the 'carvingScheme' parameter in your procedure.
				// Return the resulting image.
		throw new UnimplementedMethodException("carveImage");
	}
	
	public BufferedImage showSeams(boolean showVerticalSeams, int seamColorRGB) {
		int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
		// TODO :  Present either vertical or horizontal seams on the input image.
				// If showVerticalSeams = true, carve 'numberOfVerticalSeamsToCarve' vertical seams from the image.
				// Then, generate a new image from the input image in which you mark all of the vertical seams that
				// were chosen in the Seam Carving process. 
				// This is done by painting each pixel in each seam with 'seamColorRGB' (overriding its' previous value). 
				// Similarly, if showVerticalSeams = false, carve 'numberOfHorizontalSeamsToCarve' horizontal seams
				// from the image.
				// Then, generate a new image from the input image in which you mark all of the horizontal seams that
				// were chosen in the Seam Carving process.
		throw new UnimplementedMethodException("showSeams");
	}
}
