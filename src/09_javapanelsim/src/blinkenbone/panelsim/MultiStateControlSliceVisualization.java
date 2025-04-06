/* MultiStateControlSliceVisualization.java: a "Slice" visualization, where every image identifies
  						a different control values.

   Copyright (c) 2015-2016, Joerg Hoppe
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

   28-Feb-2016  JH		image definition logic separated from image load.
   23-Sep-2015  JH      created


   A "Slice" visualization, where every image identifies a different control values.
   "bitpos" is not used
   States are added with "addStateImage()"
 */

package blinkenbone.panelsim;

import java.util.Hashtable;
import blinkenbone.blinkenlight_api.Control;

public class MultiStateControlSliceVisualization extends ControlSliceVisualization {
	static final int brightness_levels = 16;
	Hashtable<Integer, String> imageFilenames; // filename[i] is for state[i]
	int maxState; // 0..maxState are defined

	public MultiStateControlSliceVisualization(PanelControl panelControl, Control c) {
		super(c.name, panelControl, c, 0);
		imageFilenames = new Hashtable<Integer, String>();
		maxState = -1;
	}

	// if no API control is assigned
	public MultiStateControlSliceVisualization(PanelControl panelControl, String name) {
		super(name, panelControl, null, 0);
		imageFilenames = new Hashtable<Integer, String>();
		maxState = -1;
	}

	
	// define a image filename for a state
	// later this filename is completed with width-prefix and loaded
	// see "TwoStateControlVisualization", but variable list of states
	public void addStateImageFilename(String filename, int state) {
		imageFilenames.put((Integer) state, filename);
		if (state > maxState)
			maxState = state;
	}

	@Override
	public void loadStateImages() {
		int state;
		String filename;
		for (state = 0; state <= maxState; state++) {
			filename = imageFilenames.get(state);
			if (filename != null) // all states must be defined, but who knows
				this.addStateImage(filename, state);
		}
	}

	@Override
	public void fixupStateImages() {
	}
}
