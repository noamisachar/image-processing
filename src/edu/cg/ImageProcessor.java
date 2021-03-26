package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.rmi.UnexpectedException;

public class ImageProcessor extends FunctioalForEachLoops {
	
	//MARK: Fields
	public final Logger logger;
	public final BufferedImage workingImage;
	public final RGBWeights rgbWeights;
	public final int inWidth;
	public final int inHeight;
	public final int workingImageType;
	public final int outWidth;
	public final int outHeight;
	
	//MARK: Constructors
	public ImageProcessor(Logger logger, BufferedImage workingImage,
			RGBWeights rgbWeights, int outWidth, int outHeight) {
		super(); //Initializing for each loops...
		
		this.logger = logger;
		this.workingImage = workingImage;
		this.rgbWeights = rgbWeights;
		inWidth = workingImage.getWidth();
		inHeight = workingImage.getHeight();
		workingImageType = workingImage.getType();
		this.outWidth = outWidth;
		this.outHeight = outHeight;
		setForEachInputParameters();
	}
	
	public ImageProcessor(Logger logger,
			BufferedImage workingImage,
			RGBWeights rgbWeights) {
		this(logger, workingImage, rgbWeights,
				workingImage.getWidth(), workingImage.getHeight());
	}
	
	//MARK: Change picture hue - example
	public BufferedImage changeHue() {
		logger.log("Preparing for hue changing...");
		
		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int max = rgbWeights.maxWeight;
		
		BufferedImage ans = newEmptyInputSizedImage();
		
		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = r*c.getRed() / max;
			int green = g*c.getGreen() / max;
			int blue = b*c.getBlue() / max;
			Color color = new Color(red, green, blue);
			ans.setRGB(x, y, color.getRGB());
		});
		
		logger.log("Changing hue done!");
		
		return ans;
	}
	
	//MARK: Nearest neighbor - example
	public BufferedImage nearestNeighbor() {
		logger.log("Applying nearest neighbor interpolation...");
		BufferedImage ans = newEmptyOutputSizedImage();
		
		pushForEachParameters();
		setForEachOutputParameters();
		
		forEach((y, x) -> {
			int imgX = (int)Math.round((x*inWidth) / ((float)outWidth));
			int imgY = (int)Math.round((y*inHeight) / ((float)outHeight));
			imgX = Math.min(imgX,  inWidth-1);
			imgY = Math.min(imgY, inHeight-1);
			ans.setRGB(x, y, workingImage.getRGB(imgX, imgY));
		});
		
		popForEachParameters();
		
		return ans;
	}
	
	//MARK: Unimplemented methods
	public BufferedImage greyscale() {
		logger.log("Preparing greyscale...");
		
		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int weightsSum = rgbWeights.weightsSum;
		
		BufferedImage ans = newEmptyInputSizedImage();
		
		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = r*c.getRed();
			int green = g*c.getGreen();
			int blue = b*c.getBlue();
			int greyScaled = (red + green + blue) / weightsSum;
			Color color = new Color(greyScaled, greyScaled, greyScaled);
			ans.setRGB(x, y, color.getRGB());
		});
		
		logger.log("Greyscale done!");
		
		return ans;
	}

	public BufferedImage gradientMagnitude() {
		logger.log("Preparing gradient magnitude...");
		
		BufferedImage greyscaled = greyscale();
		int width = inWidth;
		int height = inHeight;
		if (height < 2 || width < 2)
		{
			try {
				throw new UnexpectedException("Image is too small");
			} catch (UnexpectedException e) {
				e.printStackTrace();
			}
		}

		BufferedImage ans = newEmptyInputSizedImage();

		forEach((y, x) -> {
			Color cCurr = new Color(greyscaled.getRGB(x, y));
			Color cPrevH;
			Color cPrevW;
			if (y == height-1)
			{
				cPrevH = new Color(greyscaled.getRGB(x, y-1));
			}
			else
			{
				cPrevH = new Color(greyscaled.getRGB(x, y+1));
			}
			if (x == width-1)
			{
				cPrevW = new Color(greyscaled.getRGB(x-1, y));
			}
			else
			{
				cPrevW = new Color(greyscaled.getRGB(x+1, y));
			}
			double dx = Math.abs(cCurr.getRed() - cPrevW.getRed());
			double dy = Math.abs(cCurr.getRed() - cPrevH.getRed());
			int magnitude = (int) Math.sqrt((dx * dx + dy * dy) / 2);
			Color color = new Color(magnitude, magnitude, magnitude);
			ans.setRGB(x, y, color.getRGB());
		});
		
		logger.log("Gradient magnitude ready!");
		
		return ans;
	}

	public BufferedImage bilinear() {
		logger.log("Preparing for bilinear interpolation...");
		BufferedImage ans = newEmptyOutputSizedImage();

		// calculating new positions
		double newX = inWidth / (outWidth + 1.0);
		double newY = inHeight / (outHeight + 1.0);
		double tempY = newY;
		double tempX = newX;
		int topLeftX, topRightX, bottomRightX, bottomLeftX;
		int topLeftY, topRightY, bottomRightY, bottomLeftY;

		for (int i = 0; i < outWidth; i++) {
			for (int j = 0; j < outHeight; j++) {

				bottomLeftX = bottomRightX = topLeftX = topRightX = (int)Math.floor(tempX);
				bottomLeftY = bottomRightY = topLeftY = topRightY = (int)Math.floor(tempY);

				// checking width boundries
				if (topRightX == inWidth - 1)
					topRightX = inWidth - 1;
				if (bottomRightX == inWidth - 1)
					bottomRightX = inWidth - 1;
				if (topLeftX == 0)
					topLeftX = 0;
				if (bottomLeftX == 0)
					bottomLeftX = 0;

				// checking height boundries
				if (bottomLeftY == 0)
					bottomLeftY = 0;
				if (bottomRightY == 0)
					bottomRightY = 0;
				if (topLeftY == inHeight - 1)
					topLeftY = inHeight - 1;
				if (topRightY == inHeight - 1)
					topRightY = inHeight - 1;

				// calculating u and v vectors
				double u = Math.abs(bottomLeftX - tempX);
				double v = Math.abs(bottomRightY - tempY);

				// creating neighboring colors
				Color topLeftColor = new Color(workingImage.getRGB(topLeftX, topLeftY));
				Color topRightColor = new Color(workingImage.getRGB(topRightX, topRightY));
				Color bottomLeftColor = new Color(workingImage.getRGB(bottomLeftX, bottomLeftY));
				Color bottomRightColor = new Color(workingImage.getRGB(bottomRightX, bottomRightY));

				int newRed = (int) (((int) ((bottomLeftColor.getRed() * u) + (bottomRightColor.getRed() * (1 - u))) * v) +
						(int) ((topLeftColor.getRed() * u) + (topRightColor.getRed() * (1 - u))) * (1 - v));

				int newGreen = (int) ((((bottomLeftColor.getGreen() * u) + (bottomRightColor.getGreen() * (1 - u))) * v) +
						((int) ((topLeftColor.getGreen() * u) + (topRightColor.getGreen() * (1 - u))) * (1 - v)));

				int newBlue = (int) (((int) ((bottomRightColor.getBlue() * u) + (bottomLeftColor.getBlue() * (1 - u))) * v) +
						((int) ((topLeftColor.getBlue() * u) + (topRightColor.getBlue() * (1 - u))) * (1 - v)));

				if (newBlue > 255)
					newBlue = 255;
				if (newBlue < 0)
					newBlue = 0;
				if (newRed > 255)
					newRed = 255;
				if (newRed < 0)
					newRed = 0;
				if (newGreen > 255)
					newGreen = 255;
				if (newGreen < 0)
					newGreen = 0;

				Color color = new Color(newRed, newGreen, newBlue);

				ans.setRGB(i, j, color.getRGB());
				tempY += newY;
			}
			tempX += newX;
			tempY = 0;
		}
		logger.log("Bilinear interpolation done!");
		return ans;
	}
	
	//MARK: Utilities
	public final void setForEachInputParameters() {
		setForEachParameters(inWidth, inHeight);
	}
	
	public final void setForEachOutputParameters() {
		setForEachParameters(outWidth, outHeight);
	}
	
	public final BufferedImage newEmptyInputSizedImage() {
		return newEmptyImage(inWidth, inHeight);
	}
	
	public final BufferedImage newEmptyOutputSizedImage() {
		return newEmptyImage(outWidth, outHeight);
	}
	
	public final BufferedImage newEmptyImage(int width, int height) {
		return new BufferedImage(width, height, workingImageType);
	}
	
	public final BufferedImage duplicateWorkingImage() {
		BufferedImage output = newEmptyInputSizedImage();
		
		forEach((y, x) -> 
			output.setRGB(x, y, workingImage.getRGB(x, y))
		);
		
		return output;
	}
}
