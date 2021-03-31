package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;


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
    protected class Coordinate {
        public int X;
        public int Y;

        public Coordinate(int X, int Y) {
            this.X = X;
            this.Y = Y;
        }
    }

	BufferedImage originalImage;
	private BufferedImage greyscaled;
	private int numOfVerticalSeams;
    private int numOfHorizontalSeams;

    private double[][] costMatrix;
    private int[][] carved;
    private int[][] backTrack;
    private int currWidth;
    private int currHeight;
    private Coordinate[][] originalCoordinates;
    private ArrayList<Coordinate[]> horizontalCoordinates;
    private ArrayList<Coordinate[]> verticalCoordinates;

    public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
                            int outWidth, int outHeight, RGBWeights rgbWeights) {
        super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
		
		this.greyscaled = this.greyscale();
        this.originalImage = this.greyscale();

        this.currWidth = getForEachWidth();
        this.currHeight = getForEachHeight();
        this.costMatrix = new double[currHeight][currWidth];
        this.backTrack = new int[currHeight][currWidth];

        this.numOfHorizontalSeams = workingImage.getWidth() - outWidth;
        this.numOfVerticalSeams = workingImage.getHeight() - outHeight;

        this.horizontalCoordinates = new ArrayList<Coordinate[]>();
        this.verticalCoordinates = new ArrayList<Coordinate[]>();
        this.originalCoordinates = new Coordinate[currHeight][currWidth];

        this.initMatrices();
    }

	private void initMatrices(){
		this.carved = new int[this.currHeight][this.currWidth];
        for(int x = 0; x < currWidth; x++){
            for(int y = 0; y < currHeight; y++){
                this.originalCoordinates[y][x] = new Coordinate(x, y);
				this.carved[y][x] = (new Color(this.greyscaled.getRGB(x,y))).getBlue();
            }
        }
    }

    private BufferedImage reconstructImage(){
        BufferedImage ans = newEmptyOutputSizedImage();

        for(int x = 0; x < currWidth; x++){
            for(int y = 0; y < currHeight; y++){
                ans.setRGB(x,y,this.workingImage.getRGB(this.originalCoordinates[y][x].X,this.originalCoordinates[y][x].Y));
            }
        }
        return ans;
    }

    private void removeSeams(int numOfVerticalSeams, int numOfHorizontalSeams, CarvingScheme carvingScheme){
        if (carvingScheme == CarvingScheme.VERTICAL_HORIZONTAL) {
            removeVertical(numOfVerticalSeams);
            removeHorizontal(numOfHorizontalSeams);
        }
        else if (carvingScheme == CarvingScheme.HORIZONTAL_VERTICAL) {
            removeHorizontal(numOfHorizontalSeams);
            removeVertical(numOfVerticalSeams);

        }else{
            this.removeIntermittently(numOfVerticalSeams, numOfHorizontalSeams);
        }
    }

    private void removeVertical(int numOfVerticalSeams){
        for (int i = 0; i < numOfVerticalSeams; i++) {
            removeMinVerticalSeam();
        }
    }

    private void removeHorizontal(int numOfHorizontalSeams){
        for (int i = 0; i < numOfHorizontalSeams; i++) {
            removeMinHorizontalSeam();
        }
    }
    private void removeIntermittently(int numOfVerticalSeams, int numOfHorizontalSeams) {

        while(numOfVerticalSeams > 0 || numOfHorizontalSeams > 0){
            if (numOfVerticalSeams > 0 ) {
                removeMinVerticalSeam();
                numOfVerticalSeams--;
            }

            if (numOfHorizontalSeams > 0){
                removeMinHorizontalSeam();
                numOfHorizontalSeams--;
            }
        }
    }

    private void removeMinVerticalSeam() {

    	this.computeCosts("vertical");
        double min = Double.MAX_VALUE;
    	int idx = -1;
    	for(int x = 0; x < this.currWidth; x++){
    		if(this.costMatrix[this.currHeight-1][x] < min){
    			min = costMatrix[currHeight-1][x];
    			idx = x;
			}
		}


    	Coordinate[] seamToRemove = new Coordinate[this.currHeight];
    	for(int y = this.currHeight - 1; y >= 0; y--){
    		seamToRemove[y] = this.originalCoordinates[y][idx];
    		this.verticalShift(y, idx);
    		idx = idx + this.backTrack[y][idx];
		}

    	this.verticalCoordinates.add(seamToRemove);
    	this.currWidth--;
    }

    private void removeMinHorizontalSeam() {

        this.computeCosts("horizontal");
        double min = Double.MAX_VALUE;
        int idx = -1;

        for(int y = 0; y < this.currHeight; y++){
            if(this.costMatrix[y][this.currWidth-1] < min){
                min = costMatrix[y][this.currWidth-1];
                idx = y;
            }
        }

        Coordinate[] seamToRemove = new Coordinate[this.currWidth];
        for(int x = this.currWidth - 1; x >= 0; x--){
            seamToRemove[x] = this.originalCoordinates[idx][x];
            this.horizontalShift(idx, x);
            idx = idx + this.backTrack[idx][x];
        }
        this.horizontalCoordinates.add(seamToRemove);
        this.currHeight--;
    }

	private void verticalShift(int y, int idx) {
		this.originalCoordinates[y][idx] = null;
    	for(int x = idx; x < currWidth - 1; x++){
    		this.originalCoordinates[y][x] = this.originalCoordinates[y][x+1];
            this.carved[y][x] = carved[y][x+1];
		}
	}

	private  void horizontalShift(int idx, int x){
        this.originalCoordinates[idx][x] = null;
        for(int y = idx; y < currHeight - 1; y++){
            this.originalCoordinates[y][x] = this.originalCoordinates[y+1][x];
            this.carved[y][x] = carved[y+1][x];
        }
    }


	private void computeCosts(String direction) {
        if (direction == "vertical") {
            for (int y = 0; y < this.currHeight; y++) {
                for (int x = 0; x < this.currWidth; x++) {
                    this.minVertical(y, x);
                }
            }
        }else{
            for (int x = 0; x < this.currWidth; x++){
                for (int y = 0; y < this.currHeight; y++)  {
                    this.minHorizontal(y, x);
                }
            }
        }
    }

    private void minVertical(int y, int x) {
        int origin = 0;
        int cLeft = 255;
        int cUp = 255;
        int cRight = 255;

        if (y > 0) {
            double tUp = this.costMatrix[y - 1][x];
            double tRight = Double.MAX_VALUE / 2;
            double tLeft = Double.MAX_VALUE / 2;

            if (x > 0 && x < this.currWidth - 1) {
                cRight = Math.abs(this.carved[y][x - 1] - this.carved[y][x + 1]);
                cLeft = cRight;
                cUp = cRight;
            }

            if (x > 0) {
                cLeft += Math.abs(this.carved[y][x - 1] - this.carved[y - 1][x]);
                tLeft = this.costMatrix[y - 1][x - 1];
            }

            if (x  < this.currWidth - 1) {
                cRight += Math.abs(this.carved[y - 1][x] - this.carved[y][x + 1]);
                tRight = this.costMatrix[y - 1][x + 1];
            }

            double costRight = tRight + cRight;
            double costLeft = tLeft + cLeft;
            double costUp = tUp + cUp;

            double min = 0;

            if (costRight < costUp && costRight < costLeft  && x < this.currWidth - 1) {
                origin = 1;
                min = costRight;
            }
            else if (costLeft < costUp && costLeft < costRight && x > 0) {
                origin = - 1;
                min = costLeft;
            }else{
                min = costUp;
                origin = 0;
            }

            this.costMatrix[y][x] = this.pixelEnergy(y, x) + min;

        }else{
            this.costMatrix[y][x] = this.pixelEnergy(y, x);
        }

        this.backTrack[y][x] = origin;
    }

    private void minHorizontal(int y, int x) {
        int origin = 0;
        int cBehind = 255;
        int cOver = 255;
        int cUnder = 255;

        if (x > 0) {
            double tBehind = this.costMatrix[y][x - 1];
            double tOver = Double.MAX_VALUE / 2;
            double tUnder = Double.MAX_VALUE / 2;

            if (y > 0 && y < this.currHeight - 1) {
                cBehind = Math.abs(this.carved[y-1][x] - this.carved[y + 1][x]);
                cOver = cBehind;
                cUnder = cOver;
            }

            if (y > 0) {
                cOver += Math.abs(this.carved[y-1][x] - this.carved[y][x-1]);
                tOver = this.costMatrix[y - 1][x - 1];
            }

            if (y  < this.currHeight - 1) {
                cUnder += Math.abs(this.carved[y][x-1] - this.carved[y+1][x]);
                tUnder = this.costMatrix[y + 1][x - 1];
            }

            double costBehind = tBehind + cBehind;
            double costOver = tOver + cOver;
            double costUnder = tUnder + cUnder;

            double min = 0;

            if (costUnder < costBehind && costUnder < costOver  && y < this.currWidth - 1) {
                origin = 1;
                min = costUnder;
            }
            else if (costOver < costBehind && costOver < costUnder && y > 0) {
                origin = - 1;
                min = costOver;
            }else{
                min = costBehind;
                origin = 0;
            }

            this.costMatrix[y][x] = this.pixelEnergy(y, x) + min;

        }else{
            this.costMatrix[y][x] = this.pixelEnergy(y, x);
        }

        this.backTrack[y][x] = origin;
    }


    private double pixelEnergy(int y, int x) {
        int currentColor = carved[y][x];
        int verticalColor = -1;
        int horizontalColor = -1;

        if (y == this.currHeight - 1) {
            verticalColor = this.carved[y-1][x];
        } else {
            verticalColor = this.carved[y+1][x];
        }

        if (x == this.currWidth - 1) {
            horizontalColor = this.carved[y][x-1];
        } else {
            horizontalColor = this.carved[y][x+1];
        }

        double horizontal = Math.abs(currentColor - horizontalColor);
        double vertical = Math.abs(currentColor - verticalColor);

        double energy = Math.sqrt(Math.pow(vertical, 2) + Math.pow(horizontal, 2));

        return energy;
    }

    public BufferedImage carveImage(CarvingScheme carvingScheme) {
        int numVertical = Math.abs(this.outWidth - this.inWidth);
        int numHorizontal = Math.abs(this.outHeight - this.inHeight);
        this.removeSeams(numVertical,  numHorizontal, carvingScheme);
        return this.reconstructImage();
    }

    public BufferedImage showSeams(boolean showVerticalSeams, int seamColorRGB) {
        int numVertical = Math.abs(this.outWidth - this.inWidth);
        int numHorizontal = Math.abs(this.outHeight - this.inHeight);
        BufferedImage shownSeamImage;
        if (showVerticalSeams){
            shownSeamImage = this.showVerticalSeams(numVertical, seamColorRGB);
        }else{
            shownSeamImage = this.showHorizontalSeams(numHorizontal, seamColorRGB);
        }
        return shownSeamImage;

    }

    private BufferedImage showHorizontalSeams(int numOfHorizontalSeams, int seamColorRGB) {
        removeHorizontal(numOfHorizontalSeams);
        BufferedImage outputImage = this.duplicateWorkingImage();

        for (Coordinate[] seam : this.horizontalCoordinates){
            for(int i = 0; i < seam.length; i++){
                outputImage.setRGB(seam[i].X, seam[i].Y,seamColorRGB);
            }
        }

        return outputImage;
    }

    private BufferedImage showVerticalSeams(int numOfVerticalSeams, int seamColorRGB) {
        removeVertical(numOfVerticalSeams);
        BufferedImage outputImage = this.duplicateWorkingImage();

        for (Coordinate[] seam : this.verticalCoordinates){
            for(int i = 0; i < seam.length; i++){
                outputImage.setRGB(seam[i].X, seam[i].Y,seamColorRGB);
            }
        }
        return outputImage;
    }
}
