/* TwoStateControlVisualization.java: a normal ControlSlice visualization, which only has
    		state 0 = "inactive" and state 1 = "active"

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


   The "inactive" state image may be null, if its painted onto the background.
   Used for background and switches.
 */

package blinkenbone.panelsim;

import blinkenbone.blinkenlight_api.Control;

public class TwoStateControlSliceVisualization extends ControlSliceVisualization {
	String[] imageFilename;

	// constructor with image for inactive and active state
	public TwoStateControlSliceVisualization(String imageFilenameInactive,
			String imageFilenameActive, PanelControl panelControl, Control c, int bitpos) {
		// name: aus control, wenn da
		super((c == null) ? "" : c.name + "." + bitpos, panelControl, c, bitpos);
		this.imageFilename = new String[2];
		this.imageFilename[0] = imageFilenameInactive;
		this.imageFilename[1] = imageFilenameActive;
	}

	// constructor with single image for active state.
	public TwoStateControlSliceVisualization(String imageFilenameActive, PanelControl panelControl,
			Control c, int bitpos) {
		super((c == null) ? "" : c.name + "." + bitpos, panelControl, c, bitpos);
		this.imageFilename = new String[2];
		this.imageFilename[0] = null;
		this.imageFilename[1] = imageFilenameActive;
	}

	@Override
	public void loadStateImages() {
		if (imageFilename[0] != null)
			this.addStateImage(imageFilename[0], 0);

		// only state 1
		this.addStateImage(imageFilename[1], 1);
	}

	@Override
	public void fixupStateImages() {

	}
}
