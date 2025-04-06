/* ControlSliceStateIamge.java: state of an image for a part of a Blinkenlight API control

   Copyright (c) 2012-2016, Joerg Hoppe
   j_hoppe@t-online.de, www.retrocmp.com

   Permission is hereby granted, free of charge, to any person obtaining a
   copy of this software and associated documentation files (the "Software"),
   to deal in the Software without restriction, including without limitation
   the rights to use, copy, modify, merge, publish, distribute, sublicense,
   and/or sell copies of the Software, and to permit persons to whom the
   Software is furnished to do so, subject to the following conditions:

   The above copyright notice and this permission notice shall be included in
   all copies or substantial portions of the Software.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
   JOERG HOPPE BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
   IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.


   27-Jul-2016	JH       - paintOrder: Z order from Photoshop stack
                        - variant: same state may displayed in differnt ways
                         (PDP-15 switch shadow, enlarged knob)
   17-May-2012  JH      created


   A special state of an image for a part of a Blinkenlight API control

   a "state" can be
   a  luminance of a LED
   a  digit 0..9,
   a color green/yellow/red
   ....
   Only here low level image loading is done.
*/

package blinkenbone.panelsim;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RescaleOp;
import java.awt.image.WritableRaster;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;

import javax.swing.table.TableModel;

import blinkenbone.CSVParser;

public class ControlSliceStateImage extends Object {

	public static ResourceManager resourceManager;
	// public static int scaledBackgroundWidth; // on demand width rescaled
	public static String resourceImageFilePathPrefix; // part of image filepath
														// for constructor
	public static String resourceImageFileNamePrefix; // part of image filepath

	// scaled position for each image filename (without path),
	// read from external CSV file.
	private static HashMap<String, Point> csvImageCoordinates;
	private static HashMap<String, Integer> csvImagePaintOrders;

	public int state; // state of the control (bit value, knob position). usally
						// 0 = OFF, 1 = ON (swotch, lamp)..
	public int variant; // diffent images for same state,
	// for lighting situations and display modes

	public String resourceFilepath; // full path

	public Point scaledPosition; // x/y of left top corner on screen
	public Rectangle scaledRectangle; // bounding box in image
	// Position of image in Photoshop layer stack
	public int paintOrder; // 0=background, higher numbers hide lower ones

	// only one stateimage of a ControlSlizeVisualisation is visible
	// this is marked as "clickable" by the paint() function,
	// only visible/clickable images are targets of mouse clicks, see getClickableStateImage()
	public boolean clickable ;

	public BufferedImage scaledStateImage; // calculated : rescale, fixup

	public Color getPixelAt(Point clickpoint) {
		int rgb = scaledStateImage.getRGB(clickpoint.x - scaledRectangle.x,
				clickpoint.y - scaledRectangle.y);
		return new Color(rgb, true); // preserve alpha
	}

	// public int scaled_left; // left x position, calculated from ref_width,
	// ref_center_x and img.getWidth()
	// public int scaled_top; // top y position, from ref_width, ref_center_y
	// and
	// img.getHeigth()

	// transparency of this image. Used before drawImage()
	// see
	// https://www.teialehrbuch.de/Kostenlose-Kurse/JAVA/6693-Transparenz-in-Java2D.html
	public AlphaComposite alphaComposite = null;

	/*
	 * load image for a state
	 *
	 * @param ;
	 *
	 * full file path for resourcemanger is given by
	 * imageFilepathPrefix
	 * ("blinkenbone/panel/sim/panel1140/images/pdp1140industrial_size=1000_")
	 * + suffix ("LED_A15_ON.png") ;
	 *
	 * state: code of state of this image
	 * variant: code for lighting situtation or display mode
	 *
	 * unscaledReferenceWidth: width, for which left/top are given
	 *
	 * scaledReferenceWidth: target size of total image (backgroudn) is used to
	 * load right image file
	 *
	 * unscaledLeft, unscaledTop: coordianten of state image in unscaled
	 * background image.
	 *
	 * ref_left/top is typically large, as picture taken from DSLR: 4300 pixels
	 */
	public ControlSliceStateImage(String imageFilepathSuffix, int state, int variant,
			float rescaleOpScale, float rescaleopOffset) {

		this.state = state;
		this.variant = variant;

		// generate filename from backgroundSize and filename template
		this.resourceFilepath = resourceImageFilePathPrefix + resourceImageFileNamePrefix
				+ imageFilepathSuffix;
		// System.out.printf("Loading state image %s ...",
		// this.resourceFilepath);
		scaledStateImage = resourceManager.createBufferedImage(this.resourceFilepath);

		// apply current brightness/contrast settings
		if (rescaleOpScale != 1 || rescaleopOffset != 0) {
			// RescaleOp rescaleOp = new RescaleOp(rescaleOpScale,
			// rescaleopOffset, null) ;
			RescaleOp rescaleOp = new RescaleOp(
					new float[] { rescaleOpScale, rescaleOpScale, rescaleOpScale, 1f },
					new float[] { rescaleopOffset, rescaleopOffset, rescaleopOffset, 0 }, null);
			rescaleOp.filter(scaledStateImage, scaledStateImage);
		}

		// System.out.printf("done.%n");
		String pureFilename = resourceImageFileNamePrefix + imageFilepathSuffix;
		// use external Photoshop coordinates
		Point p = csvImageCoordinates.get(pureFilename);
		assert (p != null);
		scaledPosition = p;
		scaledRectangle = new Rectangle(p.x, p.y, scaledStateImage.getWidth(),
				scaledStateImage.getHeight());
		paintOrder = csvImagePaintOrders.get(pureFilename);

		// System.out.printf("Position of state image %s = (%d,%d),
		// paintOrder=%d\n",
		// imageFilepathSuffix, p.x, p.y, paintOrder);
		// System.out.printf("%s;(%d,%d)\n",pureFilename, p.x, p.y);
	}

	// traditionally form, without "variant code" (== 0)
	public ControlSliceStateImage(String imageFilepathSuffix, int state, float rescaleOpScale,
			float rescaleopOffset) {
		this(imageFilepathSuffix, state, 0, rescaleOpScale, rescaleopOffset);
	}

	/*
	 * make a deep copy of a ControlSliceStateImage,
	 * without loading it from disk again.
	 * (do not use clone()!)
	 */
	public ControlSliceStateImage(ControlSliceStateImage original, int newstate) {
		this.state = newstate;
		this.variant = original.variant  ;
		this.resourceFilepath = original.resourceFilepath;
		this.scaledStateImage = deepImageCopy(original.scaledStateImage);
		this.scaledPosition = new Point(original.scaledPosition);
		this.scaledRectangle = new Rectangle(original.scaledRectangle);
		this.paintOrder = original.paintOrder;
	}

	static BufferedImage deepImageCopy(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	/*
	 * public ControlSliceStateImage clone() {
	 * ControlSliceStateImage result;
	 * result = new ControlSliceStateImage()
	 * result.scaledPosition = (Point) this.scaledPosition.clone();
	 * result.scaledRectangle = (Rectangle) this.scaledRectangle.clone();
	 * result.scaledStateImage = deepCopy(this.scaledStateImage);
	 * return result;
	 * }
	 */

	/*
	 * read the HashMap of all image positions
	 * csvFilepathSuffix is "pdp10ki10_size=1200_controls.csv"
	 *
	 * File has the format
	 * filename;left;top
	 * pdp10ki10_size=1200_Hintergrund.png;0; 0
	 * pdp10ki10_size=1200_black.png;33; 16
	 * pdp10ki10_size=1200_frame.png;32; 15
	 * pdp10ki10_size=1200_background.png;0; 0
	 * pdp10ki10_size=1200_org_picture.png;0; 0
	 *
	 * HashMap assoziates each filename with a Point(left,top)
	 */
	public static void loadImageInfos(String csvFilepathSuffix) {
		csvImageCoordinates = new HashMap<String, Point>();
		csvImagePaintOrders = new HashMap<String, Integer>();
		String resourceFilePath = resourceImageFilePathPrefix + resourceImageFileNamePrefix
				+ csvFilepathSuffix;
		TableModel t = null;
		// System.out.printf("path = %s\n", resourceFilePath);
		java.lang.ClassLoader cl = Thread.currentThread().getContextClassLoader();
		/*
		 * Show search path list
		 * URL[] urls = ((URLClassLoader) cl).getURLs();
		 * for (URL url: urls) {
		 * System.out.println(url.getFile());
		 * }
		 */

		InputStream is = cl.getResourceAsStream(resourceFilePath);
		t = CSVParser.parse(is, ";");

		for (int r = 0; r < t.getRowCount(); r++) {
			String filename = (String) t.getValueAt(r, 0);
			String sLeft = (String) (t.getValueAt(r, 1));
			String sTop = (String) (t.getValueAt(r, 2));
			int left = Integer.parseInt(sLeft.trim());
			int top = Integer.parseInt(sTop.trim());

			csvImageCoordinates.put(filename, new Point(left, top));
			// the order in the CSV file is the Photoshop paint order
			csvImagePaintOrders.put(filename, new Integer(r));
		}
	}

}
