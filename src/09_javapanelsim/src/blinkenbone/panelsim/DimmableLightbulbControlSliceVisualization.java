/* DimmableLightbulbControlSliceVisualization.java: a light blub, brightness coupled with color temperature.

   Copyright (c) 2016-2016, Joerg Hoppe
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


   09-Feb-2016  JH      created

   A Lightbulb loads 16x the same unscaled image from image file,
   then makes 16 states with decreasing brightness from it.
   Color temperatur changed with brightness.
   Brightness maybe realized with as much transparency as possible.
*/

package blinkenbone.panelsim;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.RescaleOp;

import blinkenbone.blinkenlight_api.Control;

public class DimmableLightbulbControlSliceVisualization extends ControlSliceVisualization {
	static final int brightnessLevels = 16;
	String imageFilename;
	boolean useTransparency;
	
	// after load a image from disk, these Rescaleop Params are applied
	// this allows to modify brightness & contrast.
	// Here static defaults for all new visualizations
	public static float defaultImageRescaleopScale = 1 ;
	public static float defaultImageRescaleopOffset = 0 ;


	public DimmableLightbulbControlSliceVisualization(String imageFilename, PanelControl panelControl,
			Control c, int bitpos, boolean useTransparency) {
		super(c.name + "." + bitpos, panelControl, c, bitpos);

		// take image parameters from class 
		imageRescaleopScale = defaultImageRescaleopScale ;
		imageRescaleopOffset = defaultImageRescaleopOffset ;
		
		this.imageFilename = imageFilename; // single file for all
											// brightness states
		this.useTransparency = useTransparency;
	}

	@Override
	public void loadStateImages() {
		// 8 states:
		// load a LED only once as state 0, then clone it 'brightnessLevels'
		// times
		this.addStateImage(imageFilename, 0);
		for (int state = 1; state < brightnessLevels; state++) {
			this.addStateImage(this.getStateImage(0), state);
		}
	}

	/*
	 * Redshift measured for a DEC PDP-10 panel light bulb.
	 * Measured where 16 power levels (voltage * current).
	 * Nominal driven with +15V = 16/16 = 100% brightness.
	 * 100% color temperature is 2350K.
         *
         * Compare to http://www.tannerhelland.com/4435/convert-temperature-rgb-algorithm-code/
	 */

	private static final Color[] redshiftTable = new Color[] {
			// (r,g,b)
			new Color(0, 0, 0), // level = 0/16
			new Color(1, 0, 0), // level = 1/16
			new Color(10, 1, 1), // level = 2/16
			new Color(42, 14, 6), // level = 3/16
			new Color(82, 32, 13), // level = 4/16
			new Color(117, 52, 23), // level = 5/16
			new Color(171, 82, 41), // level = 6/16
			new Color(207, 106, 56), // level = 7/16
			new Color(233, 126, 69), // level = 8/16
			new Color(249, 145, 82), // level = 9/16
			new Color(252, 166, 100), // level = 10/16
			new Color(254, 187, 122), // level = 11/16
			new Color(254, 200, 142), // level = 12/16
			new Color(254, 210, 161), // level = 13/16
			new Color(254, 218, 174), // level = 14/16
			new Color(254, 224, 185), // level = 15/16
			new Color(254, 231, 194) // level = 16/16
	};

	/*
	 * return the max of R|G|B for whole image
	 * Fast linear access .... see
	 * http://stackoverflow.com/questions/6524196/java-get-pixel-array-from-image
	 */
	private Color getBrightestColorComponent(BufferedImage image) {
		final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		final boolean hasAlphaChannel = image.getAlphaRaster() != null;
		int maxR, maxG, maxB;
		int r, g, b;

		maxR = maxG = maxB = 0;
		if (hasAlphaChannel) {
			final int pixelLength = 4;
			for (int pixel = 0; pixel < pixels.length; pixel += pixelLength) {
				// argb += (((int) pixels[pixel] & 0xff) << 24); // alpha
				b = (int) pixels[pixel + 1] & 0xff; // blue
				if (b > maxB)
					maxB = b;
				g = (int) pixels[pixel + 2] & 0xff; // green
				if (g > maxG)
					maxG = g;
				r = (int) pixels[pixel + 3] & 0xff; // red
				if (r > maxR)
					maxR = r;
			}
		} else {
			final int pixelLength = 3;
			for (int pixel = 0; pixel < pixels.length; pixel += pixelLength) {
				b = (int) pixels[pixel] & 0xff; // blue
				if (b > maxB)
					maxB = b;
				g = (int) pixels[pixel + 1] & 0xff; // green
				if (g > maxG)
					maxG = g;
				r = (int) pixels[pixel + 2] & 0xff; // red
				if (r > maxR)
					maxR = r;
			}
		}
		return new Color(maxR, maxG, maxB);
	}

	// scale every color channel differently
	private void scaleColorComponents(BufferedImage image, float scaleR, float scaleG,
			float scaleB) {
		final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		final boolean hasAlphaChannel = image.getAlphaRaster() != null;
		int r, g, b;

		if (hasAlphaChannel) {
			final int pixelLength = 4;
			for (int pixel = 0; pixel < pixels.length; pixel += pixelLength) {
				// argb += (((int) pixels[pixel] & 0xff) << 24); // alpha

				b = (int) pixels[pixel + 1] & 0xff; // blue
				b = Math.round(b * scaleB);
				if (b > 255)
					b = 255;
				pixels[pixel + 1] = (byte) b;
				g = (int) pixels[pixel + 2] & 0xff; // green
				g = Math.round(g * scaleG);
				if (g > 255)
					g = 255;
				pixels[pixel + 2] = (byte) g;
				r = (int) pixels[pixel + 3] & 0xff; // red
				r = Math.round(r * scaleR);
				if (r > 255)
					r = 255;
				pixels[pixel + 3] = (byte) r;
			}
		} else {
			final int pixelLength = 3;
			for (int pixel = 0; pixel < pixels.length; pixel += pixelLength) {
				b = (int) pixels[pixel] & 0xff; // blue
				b = Math.round(b * scaleB);
				if (b > 255)
					b = 255;
				pixels[pixel] = (byte) b;
				g = (int) pixels[pixel + 1] & 0xff; // green
				g = Math.round(g * scaleG);
				if (g > 255)
					g = 255;
				pixels[pixel + 1] = (byte) g;
				r = (int) pixels[pixel + 2] & 0xff; // red
				r = Math.round(r * scaleR);
				if (r > 255)
					r = 255;
				pixels[pixel + 2] = (byte) r;
			}
		}
	}

	@Override
	/*
	 * give each state image a different brightness and a different red shift.
	 *
	 * If "transparency" is used, brightness is implemented with alpha
	 * transparency as much as possible (opact = bright light, transparent =
	 * dark light).
	 *
	 * (non-Javadoc)
	 *
	 * @see blinkenbone.panelsim.ControlSliceVisualization#fixupStateImages()
	 */
	//
	// state 0 = OFF
	// Alternative: use image transparency
	// see
	public void fixupStateImages() {
		for (int state = 0; state < brightnessLevels; state++) {
			ControlSliceStateImage cssi = this.getStateImage(state);

			/* encode levels as brightness */
			// state 0 => 0; state "brightness_levels-1" -> 1.0
			// float brightness = state * (1f / (brightnessLevels - 1));

			// 1): adjust image pixel colors and brightness according to
			// red-shift table.
			// brigntesslevelRGB := table[brigntesslevel]/ table[100%]RGB *
			// unscaledRGB ;
			float brightnessR = (float) redshiftTable[state].getRed()
					/ redshiftTable[16].getRed();
			float brightnessG = (float) redshiftTable[state].getGreen()
					/ redshiftTable[16].getGreen();
			float brightnessB = (float) redshiftTable[state].getBlue()
					/ redshiftTable[16].getBlue();

			if (useTransparency) {
				/*
				 * Produce reduced brightness with as much transparency as
				 * possible. This makes the "lamp windows" on the acryl visible if lamp is
				 * dim.
				 * Alpha: 0 = invisible, 1.0 = opaque.
				 */

				// 1. determine max color brightness of dimmed image
				// 2. enhance base brightness to max color value == 255.
				// (normalized to max(R|G|B = 255)
				// and set transparency, so transparency(255) = original
				// brightness.
				// 3.after Display, image is as dark as before.
				Color maxRGB = getBrightestColorComponent(cssi.scaledStateImage);
				float maxDimmedChannelVal = Math.max(brightnessR * maxRGB.getRed(), brightnessG * maxRGB.getGreen());
				maxDimmedChannelVal = Math.max(maxDimmedChannelVal, brightnessB * maxRGB.getBlue());
				float alpha = maxDimmedChannelVal / 255;
				// alpha < 1 makes intensity 255 visible as brightness
				// 'maxChannelVal'
				if (alpha > 0) { // lamp image not totally black:
					// adjust brightness of base image, so
					// alpha * 255 == max color component brightness
					brightnessR /= alpha;
					brightnessG /= alpha;
					brightnessB /= alpha;
					scaleColorComponents(cssi.scaledStateImage, brightnessR, brightnessG,
							brightnessB);
				}
				// float alpha = 0.1f + state * (0.9f / (brightnessLevels - 1));
				// https://www.teialehrbuch.de/Kostenlose-Kurse/JAVA/6693-Transparenz-in-Java2D.html
				// "AlphaComposite"
				// alpha = 0.5f ;
				// alpha = (float) state / (brightness_levels - 1);
				cssi.alphaComposite = AlphaComposite
						.getInstance(AlphaComposite.SRC_OVER, alpha);
			} else {
				// no transparency: just use the dim value from the table
				scaleColorComponents(cssi.scaledStateImage, brightnessR, brightnessG,
						brightnessB);
			}
		}
	}
}
