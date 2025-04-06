/* TwoStateSituativeControlSliceVisualization.java: a ControlSlice visualization with
    		state 0 = "inactive" and state 1 = "active", but display depends on
            neighbar images.

   Copyright (c) 2016, Joerg Hoppe
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


   28-Jul-2016  JH  cloned from TwoStateControlVisualization


   Used for PDP-15 switch images: a switch puts a shadow on its neighbor.
   Each "ON" or "OFF" image comes in two variants, depending on the state of
   a neighbor.
 */

package blinkenbone.panelsim;

import blinkenbone.blinkenlight_api.Control;

public class TwoStateSituativeControlSliceVisualization extends ControlSliceVisualization {


    // the state of this ControlSliceVisualization determines the
    // "situation" of "this".
    public ControlSliceVisualization neighborControlSliceVisualization ;

    // imageFilename[neigborstate][state]:
    // image of the switch depending on its state and the neighbor state
	String[][] imageFilename;

    // simple constructor, does not attached images.
	public TwoStateSituativeControlSliceVisualization(PanelControl panelControl, Control c, int bitpos,
			ControlSliceVisualization neighborControlSliceVisualization) {
		// name: aus control, wenn da
		super((c == null) ? "" : c.name + "." + bitpos, panelControl, c, bitpos);
		this.imageFilename = new String[2][2];
        this.neighborControlSliceVisualization = neighborControlSliceVisualization ;
	}

	// define a image filename for a state and a state of the neighbor.
	// later this filename is compeleted with width-prefix and loaded
	// see "TwoStateControlVisualization",
	public void addStateImageFilename(String filename, int state, int neighborState) {
        this.imageFilename[neighborState][state] = filename ;
		if (state > maxState)
			maxState = state;
	}

	@Override
	public void loadStateImages() {
		// add all state(neighbor state combinations
		if (imageFilename[0][0] != null)
			this.addStateImage(imageFilename[0][0], 0, 0);
		if (imageFilename[0][1] != null)
			this.addStateImage(imageFilename[0][1], 1, 0);
		if (imageFilename[1][0] != null)
			this.addStateImage(imageFilename[1][0], 0, 1);
		if (imageFilename[1][1] != null)
			this.addStateImage(imageFilename[1][1], 1, 1);
	}

	@Override
	public void fixupStateImages() {

	}
	
	
	@Override
	// get "variant" as state from neighbor 
	public ControlSliceStateImage getStateImage(int state) {
		int neighborState = 0 ;
		if (neighborControlSliceVisualization != null)
			neighborState = neighborControlSliceVisualization.getState() ;
		return super.getStateImage(state, neighborState) ;
	}

}
