/* Panelsim1170_app.java: visual application framework for PDP-11/70 sim

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

   20-Apr-2016  JH      added POWER/LOCK keyswitch
   28-Feb-2016  JH v1.01 bugfix: all controls were reset on image size change.
                         Visualization load logic separated from image load.
   20-Sep-2015  JH      created
*/

package blinkenbone.panelsim.panelsim1170;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.acplt.oncrpc.apps.jportmap.OncRpcEmbeddedPortmap;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;

import blinkenbone.blinkenlight_api.PanelList;
import blinkenbone.blinkenlight_api.RpcServer;
import blinkenbone.panelsim.ResourceManager;
import blinkenbone.rpcgen.rpc_blinkenlight_api;

import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;

import java.awt.event.MouseMotionAdapter;

public class Panelsim1170_app {
	public static String version = "v1.01";
	public static String programName = "Panelsim1170_app " + version;

	private final ResourceManager resourceManager = new ResourceManager(this.getClass());

	private JFrame frmPdp1170_OEM_panel;
	// declare some controls here, not in initialize().
	// necessary, if they are accessed outside initialize().
	private Panel1170 panel1170;
	private JPanel toolBar;

	static private String[] args; // raw
	static JSAPResult commandlineParameters;// parsed with JSAP

	// Published BlinkenLight Api Panel
	// used by visualisation in "panel1170" and by rpcserver
	PanelList blinkenlightApiPanels;

	RpcServer rpcsrv;
	OncRpcEmbeddedPortmap oncRpcEmbeddedPortmap;

	// frequency of display updates, see DimmableLEDVisualization
	// faster update: flicker on RUN.
	// slower update: RSX11 idle pattern gets blurry
	private int repaintInterval = 50; // 50ms -> 20 cycles/sec
	// count down display cycles for LED in ON state
	private int activityLedTotalRepaintCycles = 250 / repaintInterval; // 250msec
	private int activityLedRemainingRepaintCycles;
	ImageIcon activityLedONIcon;
	ImageIcon activityLedOFFIcon;

	/*
	 * Launch the application.
	 */
	public static void main(final String[] args) {

		EventQueue.invokeLater(new Runnable() {

			void parseArgs() throws Exception {
				Panelsim1170_app.args = args;
				SimpleJSAP jsap = new SimpleJSAP(programName,
						"Shows a simulated PDP-11/70 console panel with Blinkenlight API interface",
						new Parameter[] {
								new FlaggedOption("width", JSAP.INTEGER_PARSER, "1000",
										JSAP.NOT_REQUIRED, 'w', "width",
										"Display panel in this width, must be one of the defined."),
								new FlaggedOption("addr_select", JSAP.INTEGER_PARSER, "0",
										JSAP.NOT_REQUIRED, 'a', "addr_select",
										"Set start position of ADDR_SELECT knob."),
								new FlaggedOption("data_select", JSAP.INTEGER_PARSER, "0",
										JSAP.NOT_REQUIRED, 'd', "data_select",
										"Set start position of DATA_SELECT knob."),
								 new FlaggedOption("keyswitch", JSAP.INTEGER_PARSER, "1",
										JSAP.NOT_REQUIRED, 'k', "keyswitch",
										"Set start position of POWER/LOCK keyswitch.") });
				commandlineParameters = jsap.parse(args);
				if (jsap.messagePrinted())
					System.exit(1);
			}

			public void run() {
				try {
					parseArgs();
					// the application
					Panelsim1170_app window = new Panelsim1170_app();
					window.frmPdp1170_OEM_panel.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

	/**
	 * Create the application.
	 *
	 * And start the Blinkenlight RPC server in a separate thread
	 */
	public Panelsim1170_app() {

		// the RPC portmapper
		try {
			oncRpcEmbeddedPortmap = new OncRpcEmbeddedPortmap();
		} catch (Exception e) {
			e.printStackTrace();
		}

		blinkenlightApiPanels = new PanelList();

		initialize();

		try {
			/*
			 * now panel1170 has one Panel constructed and added to
			 * blinkenlightApiPanels.
			 *
			 * Start the Blinkenlight RPC server in a separate thread. It
			 * publishes the Panel struct blinkenlightApiPanels
			 */
			rpcsrv = new RpcServer(blinkenlightApiPanels, this.getClass().getName(), args);
			rpcsrv.program_info = programName;
			// init activity LED
			rpcsrv.clientHeartbeat = 0;
			activityLedRemainingRepaintCycles = 0;

			Thread t = new Thread(rpcsrv, "BlinkenlightApi RpcServer Thread");
			t.setDaemon(true); // should end, when application ends
			t.start(); // excutes rpcsrv.run(); // never ends
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmPdp1170_OEM_panel = new JFrame();
		frmPdp1170_OEM_panel.setResizable(false);
		frmPdp1170_OEM_panel.setTitle("<set at runtime by panel.getApplicationTitle()>");
		frmPdp1170_OEM_panel.setBounds(100, 100, 711, 624);
		frmPdp1170_OEM_panel.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmPdp1170_OEM_panel.getContentPane().setLayout(null);

		/*
		 * toolbar with buttons and combobox
		 */
		toolBar = new JPanel();
		// toolBar.setFloatable(false);
		toolBar.setBounds(0, 0, 1000, 27);
		frmPdp1170_OEM_panel.getContentPane().add(toolBar);

		/*
		 * JPanel with picture stack on it
		 */
		panel1170 = new Panel1170(resourceManager);
		// create control visualizations
		panel1170.init(commandlineParameters);

		// this line is without function at run time.
		// but gives the panel dimension at design time
		panel1170.setBounds(23, 37, 470, 322);

		// publish with RPC server
		blinkenlightApiPanels.addPanel(panel1170.blinkenlightApiPanel);

		// on click: show/hide control image
		panel1170.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent arg0) {
				panel1170.mouseDown(arg0.getPoint(), arg0.getButton());
				panel1170.repaint();
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				panel1170.mouseUp(arg0.getPoint(), arg0.getButton());
				panel1170.repaint();
			}
		});

		panel1170.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent arg0) {
				// mouse is moved with pressed button
				panel1170.mouseDragged(arg0.getPoint(), arg0.getButton());
			}
		});

		panel1170.setForeground(Color.GREEN);
		// panel1170.setBackground(Color.PINK);
		panel1170.setBackground(Color.PINK); // fills hole in PDP-1170 panel
												// background image ?
		frmPdp1170_OEM_panel.getContentPane().add(panel1170);
		frmPdp1170_OEM_panel.setTitle(panel1170.getApplicationTitle());

		JButton btnLampSwitch = new JButton("test");
		btnLampSwitch.setBounds(109, 3, 63, 21);
		btnLampSwitch.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				panel1170.setSelftest(rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_ALLTEST);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				panel1170.setSelftest(rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_NORMAL);
			}
		});
		toolBar.setLayout(null);
		toolBar.add(btnLampSwitch);

		final JComboBox comboboxWidth = new JComboBox();
		comboboxWidth.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int newWidth = Integer.parseInt(comboboxWidth.getSelectedItem().toString());
				// load new scaled images
				panel1170.loadControlVisualizationImages(newWidth);
				layoutPanel(newWidth);
			}
		});

		// fill combobox with supported widths
		comboboxWidth.setModel(new DefaultComboBoxModel(panel1170.getSupportedWidths()));

		comboboxWidth.setBounds(251, 3, 68, 21);
		toolBar.add(comboboxWidth);

		JLabel lblWidth = new JLabel("width:");
		lblWidth.setBounds(194, 6, 53, 15);
		toolBar.add(lblWidth);

		JLabel lblActive = new JLabel("active:");
		lblActive.setBounds(345, 6, 53, 15);
		toolBar.add(lblActive);

		activityLedONIcon = new ImageIcon(Panelsim1170_app.class
				.getResource("/blinkenbone/panelsim/images/red_led_ON_23px.png"));
		activityLedOFFIcon = new ImageIcon(Panelsim1170_app.class
				.getResource("/blinkenbone/panelsim/images/red_led_OFF_23px.png"));
		final JLabel imgActiveLED = new JLabel("");
		// labelActiveLED.setIcon(new
		// ImageIcon(Panelsim1170_app.class.getResource("/blinkenbone/panelsim/resources/red_led_ON_23px.png")));
		imgActiveLED.setIcon(activityLedOFFIcon);
		imgActiveLED.setBounds(401, 3, 28, 23);
		toolBar.add(imgActiveLED);

		final JLabel lblCopyright = new JLabel(
				"(C) 2012-2016 by Joerg Hoppe (j_hoppe@t-online.de, www.retrocmp.com)");
		lblCopyright.setVisible(false);
		lblCopyright.setBounds(438, 6, 600, 15);
		toolBar.add(lblCopyright);

		JButton buttonClear = new JButton("reset");
		buttonClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				panel1170.clearUserinput();

			}
		});
		buttonClear.setBounds(12, 3, 75, 21);
		toolBar.add(buttonClear);

		/*
		 * timer: update panel1170 periodically. Needed to visualize values of
		 * Blinkenlight API panel
		 */
		// rpc server updates panel1170 on change of output controls (LEDs)

		// paint surface much slower
		javax.swing.Timer repaintTimer = new javax.swing.Timer(repaintInterval,
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						imgActiveLED.setIcon(activityLedONIcon);

						// acitivity LED ON period refresh, if RPC client access
						if (rpcsrv.clientHeartbeat > 0) {
							activityLedRemainingRepaintCycles = activityLedTotalRepaintCycles;
							rpcsrv.clientHeartbeat = 0;
						}

						// full selftest active?
						if (panel1170
								.getSelftest() == rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_MODE_ALLTEST) {
							imgActiveLED.setIcon(activityLedONIcon);
							// show copyright only on full selftest
							lblCopyright.setVisible(true);
						} else {
							if (activityLedRemainingRepaintCycles > 0) {
								imgActiveLED.setIcon(activityLedONIcon);
								activityLedRemainingRepaintCycles--;
							} else
								imgActiveLED.setIcon(activityLedOFFIcon);
							lblCopyright.setVisible(false);
						}
						panel1170.repaint();
					}
				});

		// get initial display width from commandline or smallest
		// then get index of width selection comboBox
		Integer initialWidth = commandlineParameters.getInt("width");
		// initialWidth may be null. find the list entry for the combobox
		int comboboxSelectedIdx = -1;
		for (int idx = 0; idx < comboboxWidth.getModel().getSize(); idx++) {
			Integer item = (Integer) comboboxWidth.getModel().getElementAt(idx);
			if (item.intValue() == initialWidth)
				comboboxSelectedIdx = idx;
		}
		if (comboboxSelectedIdx == -1) { // not found: use smallest width
			comboboxSelectedIdx = 0;
			initialWidth = panel1170.getSupportedWidths()[0];
		}
		comboboxWidth.setSelectedIndex(comboboxSelectedIdx);
		// this calls the scaled images in loadControlVisualizationImages() ;

		panel1170.syncKnobsFeedbackLeds(); // later it's done in mousedown(knob)

		repaintTimer.start(); // as last action

	}

	void layoutPanel(int width) {

		// inside of frame so gross wie panel
		// Insets insets = frame.getInsets();
		// int insetwidth = insets.left + insets.right;
		// int insetheight = insets.top + insets.bottom;
		// frame.setSize((int)panel1170.getWidth() + insetwidth,
		// (int)panel1170.getHeight() + insetheight);

		// eigenes fixes layout
		// no images loaded? empty default dimensions

		// toolbar as width as the panel image
		toolBar.setSize(panel1170.getWidth(), toolBar.getHeight());

		// set size of content pane, and adapt outer JFrame
		frmPdp1170_OEM_panel.getContentPane().setPreferredSize(new Dimension(
				panel1170.getWidth(), panel1170.getHeight() + toolBar.getHeight()));
		frmPdp1170_OEM_panel.pack();
		frmPdp1170_OEM_panel.setVisible(true);

		// shift Panel down by top panel heigth
		Rectangle b = panel1170.getBounds();
		b.x = 0;
		b.y = toolBar.getHeight();
		panel1170.setBounds(b);

	}
}
