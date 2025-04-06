/* PanelList.java: Blinkenlight API data struct - list of panels

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


   04-May-2012  JH      created
*/


package blinkenbone.blinkenlight_api;

import java.util.ArrayList;

/**
 * @author joerg
 *
 */
public class PanelList {
	public ArrayList<Panel> panels;

	public PanelList() {
		panels = new ArrayList<Panel>();
	}
	/*
	 * delete all panels
	 */
	void clear() {
		panels.clear();
	}

	/*
	 * new panel for list
	 */
	public Panel addPanel(Panel panel) {
		panels.add(panel);
		panel.index = panels.indexOf(panel);
		return panel;
	}

	/*
	 * search a panel by name
	 */
	public Panel getPanel(String panelname) {
		for (Panel p : panels) {
			if (p.name.equalsIgnoreCase(panelname))
				return p; // found
		}
		return null; // not found
	}
	/*
	 * Dump of panelsand controls
	 */
	public void diagprint() {
		for (Panel p : panels) {
			System.out.printf("Panel[%d]:%n", p.index);
			System.out.printf("  name ................. = \"%s\"%n", p.name);
			System.out.printf("  input controls ....... = %d%n", p.controls_inputs_count);
			System.out.printf("  input byte stream len  = %d%n", p.controls_inputs_values_bytecount);
			System.out.printf("  output controls ...... = %d%n", p.controls_outputs_count);
			System.out.printf("  output byte stream len = %d%n", p.controls_outputs_values_bytecount);
			for (Control blc : p.controls) {
				System.out.printf("  control[%d]:%n", blc.index);
				System.out.printf("    name ... = \"%s\"%n", blc.name);
				System.out.printf("    type ... = %s (%s)%n", blc.type.text, blc.is_input?"input":"output");
				System.out.printf("    radix .. = %d%n", blc.radix);
				System.out.printf("    bit len  = %d%n", blc.value_bitlen);
				System.out.printf("    byte len = %d%n", blc.value_bytelen);
			}
		}
	}
}
