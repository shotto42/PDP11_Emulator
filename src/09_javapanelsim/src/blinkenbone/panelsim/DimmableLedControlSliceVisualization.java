/* DimmableLedControlSliceVisualization.java: a lamp with different brightness states

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


   17-May-2012  JH      created


   A LED loads 16x the same unscaled image from image file, then makes 16
   states with decreasing brightness from it.
   Alternative: use increasing transparences to become darker.
*/

package blinkenbone.panelsim;

import java.awt.AlphaComposite;
import java.awt.image.RescaleOp;

import blinkenbone.blinkenlight_api.Control;


public class DimmableLedControlSliceVisualization extends ControlSliceVisualization {
	static final int brightnessLevels = 16;
	String imageFilename;
	boolean useTransparency;
	
	// after load a image from disk, these Rescaleop Params are applied
	// this allows to modify brightness & contrast.
	// Here static defaults for all new visualizations
	public static float defaultImageRescaleopScale = 1 ;
	public static float defaultImageRescaleopOffset = 0 ;


	public DimmableLedControlSliceVisualization(String imageFilename, PanelControl panelControl, Control c,
			int bitpos, boolean useTransparency) {
		super(c.name + "." + bitpos, panelControl, c, bitpos);

		// take image parameters from class 
		imageRescaleopScale = defaultImageRescaleopScale ;
		imageRescaleopOffset = defaultImageRescaleopOffset ;


		this.imageFilename = imageFilename; // single file for all
											// brightness states
		this.useTransparency = useTransparency ;
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

	@Override
	// give each state image a different brightness
	// state 0 = OFF = very dim (so LED is visible a bit)
	// Alternative: use image transparency
	// see
	public void fixupStateImages() {
		for (int state = 0; state < brightnessLevels; state++) {
			ControlSliceStateImage cssi = this.getStateImage(state);
			if (useTransparency) {
				/* encode levels as alpha transparency. 0 = invisible, 1.0 = opaque */
				float alpha = 0.1f + state * (0.9f / (brightnessLevels - 1));
				// https://www.teialehrbuch.de/Kostenlose-Kurse/JAVA/6693-Transparenz-in-Java2D.html
				// "AlphaComposite"
				// alpha = 0.5f ;
				// alpha = (float) state / (brightness_levels - 1);
				cssi.alphaComposite = AlphaComposite
						.getInstance(AlphaComposite.SRC_OVER, alpha);
			} else {
				/* encode levels as brightness */
				// state 0 => 0; state "brightness_levels-1" -> 1.0
				// float brightness = 0 + state * (1f / (brightness_levels -
				// 1));
				// state 0 => 0.1; state "brightness_levels-1" -> 1.0
				float brightness = 0.1f + state * (0.9f / (brightnessLevels - 1));
				// Howto: Adjust brightness and contrast of BufferedImage in
				// Java
				// http://stackoverflow.com/questions/3433275/adjust-brightness-and-contrast-of-bufferedimage-in-java
				// http://docs.oracle.com/javase/6/docs/api/java/awt/image/RescaleOp.html
				RescaleOp rescaleOp = new RescaleOp(brightness, 0, null);
				rescaleOp.filter(cssi.scaledStateImage, cssi.scaledStateImage);
				// Source and destination are the same.
			}
		}
	}
}
