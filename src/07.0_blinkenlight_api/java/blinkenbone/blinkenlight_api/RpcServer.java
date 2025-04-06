/* RpcServer.java: Blinkenlight API RPC server

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


   01-May-2012  JH      created
*/


package blinkenbone.blinkenlight_api;

import java.io.IOException;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.acplt.oncrpc.OncRpcException;

import blinkenbone.blinkenlight_api.Control.ControlType;
import blinkenbone.rpcgen.rpc_blinkenlight_apiServerStub;
import blinkenbone.rpcgen.rpc_blinkenlight_api;
import blinkenbone.rpcgen.rpc_blinkenlight_api_control_struct;
import blinkenbone.rpcgen.rpc_blinkenlight_api_control_type_t;
import blinkenbone.rpcgen.rpc_blinkenlight_api_controlvalues_struct;
import blinkenbone.rpcgen.rpc_blinkenlight_api_getcontrolinfo_res;
import blinkenbone.rpcgen.rpc_blinkenlight_api_getinfo_res;
import blinkenbone.rpcgen.rpc_blinkenlight_api_getpanelinfo_res;
import blinkenbone.rpcgen.rpc_blinkenlight_api_infostringtype;
import blinkenbone.rpcgen.rpc_blinkenlight_api_nametype;
import blinkenbone.rpcgen.rpc_blinkenlight_api_panel_struct;
import blinkenbone.rpcgen.rpc_blinkenlight_api_setpanel_controlvalues_res;
import blinkenbone.rpcgen.rpc_param_cmd_get_struct;
import blinkenbone.rpcgen.rpc_param_cmd_set_struct;
import blinkenbone.rpcgen.rpc_param_result_struct;
import blinkenbone.rpcgen.rpc_test_cmdstatus_struct;
import blinkenbone.rpcgen.rpc_test_data_struct;

public class RpcServer extends rpc_blinkenlight_apiServerStub implements
		Runnable {

	public int clientHeartbeat; // is inced on every client request
	private Logger logger;

	public String program_info; // to be set by caller
	private String program_name;
	private String program_args[];
	private PanelList panel_list;

	/*
	 * construct with simple hostname
	 */
	public RpcServer(PanelList pl, String program_name, String program_args[])
			throws OncRpcException, IOException {
		logger = Logger.getLogger("RpcServer");
		logger.setLevel(Level.FINEST); // log everything
		logger.log(Level.INFO, "RpcServer logging started");

		this.program_name = program_name;
		this.program_args = program_args;

		this.panel_list = pl;
		clientHeartbeat = 0;
	}

	/*
	 * info() return a multi line string with info about - server commandline -
	 * compile date
	 */
	@Override
	public rpc_blinkenlight_api_getinfo_res RPC_BLINKENLIGHT_API_GETINFO_1() {
		synchronized (this) {
			clientHeartbeat++;
		}
		rpc_blinkenlight_api_getinfo_res result = new rpc_blinkenlight_api_getinfo_res();
		StringBuffer buf = new StringBuffer();
		if (program_args != null && program_args.length > 0) {
			buf.append(program_args[0]);
			for (int i = 1; i < program_args.length; i++) {
				buf.append(" ");
				buf.append(program_args[i]);
			}
		}

		result.info = new rpc_blinkenlight_api_infostringtype(String.format(
				"Server info................: %s%n"
						+ "Server program name........: %s%n"
						+ "Server command line options: %s%n",
				// +"Server compile time .......: " __DATE__ " " __TIME__ "\n",
				// not in java
				program_info, program_name, buf.toString()));

		result.error_code = 0;
		return result;

	}

	/*
	 * getpanelinfo() return info over panel with handle "i_panel" i_panel is
	 * just the index in blinkenlight_panel_list.panels[] if invalid index:
	 * result.errno=1, else errno=0
	 */
	@Override
	public rpc_blinkenlight_api_getpanelinfo_res RPC_BLINKENLIGHT_API_GETPANELINFO_1(
			int i_panel) {
		synchronized (this) {
			clientHeartbeat++;
		}
		rpc_blinkenlight_api_getpanelinfo_res result = new rpc_blinkenlight_api_getpanelinfo_res();

		logger.finest(String.format(
				"blinkenlight_api_getpanelinfo(i_panel=%d)", i_panel));
		if (i_panel >= panel_list.panels.size()) {
			logger.finest(String.format("  i_panel > panels_count"));
			result.error_code = 1;
		} else {
			Panel p = panel_list.panels.get(i_panel);

			// allocate new result
			result.panel = new rpc_blinkenlight_api_panel_struct();
			result.panel.controls_outputs_count = p.controls_outputs_count;
			result.panel.controls_inputs_count = p.controls_inputs_count;
			result.panel.controls_inputs_values_bytecount = p.controls_inputs_values_bytecount;
			result.panel.controls_outputs_values_bytecount = p.controls_outputs_values_bytecount;
			result.panel.name = new rpc_blinkenlight_api_nametype(p.name);
			// result.panel.info = new
			// rpc_blinkenlight_api_infostringtype(p.info);

			// return panel from definition to result. Its ust the name for the
			// moment
			// strncpy(result.blinkenlight_api_getpanelinfo_res_u.panel.name,
			// blp->name,
			// sizeof(result.blinkenlight_api_getpanelinfo_res_u.panel.name));
			result.error_code = 0;
			logger.finest(String.format("  result.name=%s, ...",
					result.panel.name.value));
		}
		if (result.error_code != 0) {
			// fully construct result objects also in error case
			result.panel = new rpc_blinkenlight_api_panel_struct();
			result.panel.name = new rpc_blinkenlight_api_nametype("dummy");
		}
		return result;
	}

	/*
	 * getcontrolinfo() return info over control handle "i_control" in panel
	 * "i_panel" i_control is the index in
	 * blinkenlight_panel_list.panels[i_panel].controls[] if invalid index:
	 * result.errno=1, else errno=0
	 */
	@Override
	public rpc_blinkenlight_api_getcontrolinfo_res RPC_BLINKENLIGHT_API_GETCONTROLINFO_1(
			int i_panel, int i_control) {
		synchronized (this) {
			clientHeartbeat++;
		}
		rpc_blinkenlight_api_getcontrolinfo_res result = new rpc_blinkenlight_api_getcontrolinfo_res();
		Panel p;
		Control c;

		logger.finest(String.format(
				"blinkenlight_api_getpanelinfo(i_panel=%d, i_control=%d)%n",
				i_panel, i_control));
		if (i_panel >= panel_list.panels.size()) {
			logger.finest(String.format("  i_panel > panels_count%n"));
			result.error_code = 1; // invalid panel
		} else {
			p = panel_list.panels.get(i_panel);
			if (i_control >= p.controls.size()) {
				logger.finest(String.format("  i_control > controls_count%n"));
				result.error_code = 1; // invalid control
			} else {
				c = p.controls.get(i_control);

				// allocate new result
				result.control = new rpc_blinkenlight_api_control_struct();
				result.control.name = new rpc_blinkenlight_api_nametype(c.name);
				result.control.is_input = c.is_input ? (byte) 1 : (byte) 0;
				result.control.type = new rpc_blinkenlight_api_control_type_t(
						c.type.value);
				result.control.radix = c.radix;
				result.control.value_bitlen = c.value_bitlen;
				result.control.value_bytelen = c.value_bytelen;

				// strncpy(result.blinkenlight_api_getcontrolinfo_res_u.control.name,
				// blc->name,
				// sizeof(result.blinkenlight_api_getcontrolinfo_res_u.control.name));
				result.error_code = 0;
				logger.finest(String.format("  result.name=%s, ...",
						result.control.name.value));
			}
		}
		if (result.error_code != 0) {
			// fully construct result objects also in error case
			result.control = new rpc_blinkenlight_api_control_struct();
			result.control.name = new rpc_blinkenlight_api_nametype("dummy");
			result.control.type = new rpc_blinkenlight_api_control_type_t(
					ControlType.none.value);
		}

		return result;
	}

	/*
	 * setpanel_controlvalues().
	 *
	 * Set all output controls of a panel. Argument is a list of bytes encoding
	 * the list of values for all output controls the value for each output
	 * control is build by combining the next "bytelen" bytes The order in the
	 * value list is the order of controls in the panels. (but indexes are not
	 * the same, output controls are mixed with input controls!)
	 *
	 * changes are notified to observers (GUI update of panel simualtors)
	 */

	@Override
	public rpc_blinkenlight_api_setpanel_controlvalues_res RPC_BLINKENLIGHT_API_SETPANEL_CONTROLVALUES_1(
			int i_panel, rpc_blinkenlight_api_controlvalues_struct valuelist) {
		synchronized (this) {
			clientHeartbeat++;
		}

		rpc_blinkenlight_api_setpanel_controlvalues_res result = new rpc_blinkenlight_api_setpanel_controlvalues_res();

		logger.finest(String.format(
				"blinkenlight_api_setpanel_controlvalues(i_panel=%d)%n",
				i_panel));

		if (i_panel >= panel_list.panels.size()) {
			logger.severe("i_panel > panels_count");
			result.error_code = 1; // invalid panel
		} else {
			int value_byte_idx; // index in received value byte stream
			Panel p = panel_list.panels.get(i_panel);
			// check: exact amount of values provided?
			// "Sum of bytes" must be "sum(all controls) of value_bytelen

			if (p.controls_outputs_values_bytecount != valuelist.value_bytes.length) {
				logger.severe(String
						.format("Error in blinkenlight_api_setpanel_controlvalues():"));
				logger.severe(String
						.format("Sum (Panel[%s].outputcontrols.value_bytelen) is %d, but %d values were transmitted.",
								p.name, p.controls_outputs_values_bytecount,
								valuelist.value_bytes.length));
				System.exit(1);
			}

			/*
			 * Go through all output controls, assign value to each. Decode
			 * control value from the right amount of bytes
			 */
			value_byte_idx = 0; // start of byte array
			for (Control c : p.controls)
				if (!c.is_input)
					synchronized (c) {
						long value;
						// application may not change control values in parallel

						assert (value_byte_idx < valuelist.value_bytes.length);
						value = Bitcalc.decode_long_from_bytes(
								valuelist.value_bytes, value_byte_idx,
								c.value_bytelen);
						c.value = value;

						logger.finest(String.format(
								"   control[%d].value = 0x%x (%d bytes)",
								c.index, c.value, c.value_bytelen));
						value_byte_idx += c.value_bytelen;

						/*
						 *
						 * /// write values of panel controls to BLINKENBUS.
						 * optimization: only changed if (mode_panelsim)
						 * panelsim_write_panel_output_controls(blp, 0); else
						 * blinkenbus_write_panel_output_controls(blp, 0);
						 */
						assert (value_byte_idx == valuelist.value_bytes.length); // all
																					// bytes
																					// processed
					}
			/*
			 * notify application: client has set outputs, refresh display (if
			 * any changes to output)
			 */
			p.setChanged(); // always notify obser, even if all controls stay
							// the same.
			// observer may need "display clock"
			p.notifyObservers(); // without info object
			result.error_code = 0;
		}
		return result;
	}

	/*
	 * getpanel_controlvalues() Query all input controls of a panel. Result is a
	 * list of bytes encoding the list of values for all input controls. The
	 * bytes for each input control is are the "bytelen" lsb for each value The
	 * order in the value list is the order of controls in the panels. (but
	 * indexes are not the same, output controls are mixed with input controls!)
	 */
	@Override
	public rpc_blinkenlight_api_controlvalues_struct RPC_BLINKENLIGHT_API_GETPANEL_CONTROLVALUES_1(
			int i_panel) {
		synchronized (this) {
			clientHeartbeat++;
		}

		rpc_blinkenlight_api_controlvalues_struct result = new rpc_blinkenlight_api_controlvalues_struct();

		logger.finest(String.format(
				"blinkenlight_api_getpanel_controlvalues(i_panel=%d)", i_panel));

		if (i_panel >= panel_list.panels.size())
			result.error_code = 1; // invalid panel
		else {
			int value_byte_idx; // index in result value byte stream
			Panel p = panel_list.panels.get(i_panel);

			// read values from BLINKENBUS into input controls values
			// if (mode_panelsim)
			// panelsim_read_panel_input_controls(blp);
			// else
			// blinkenbus_read_panel_input_controls(blp);

			result.value_bytes = new byte[p.controls_inputs_values_bytecount];

			/*
			 * go through all input controls, assign value from each into result
			 * stream each input control puts "value_bytelen" bytes into char
			 * stream, lsb first
			 */
			value_byte_idx = 0; // buffer start
			for (Control c : p.controls)
				if (c.is_input)
					synchronized (c) {
						// application may not change control values in parallel

						assert (value_byte_idx < result.value_bytes.length);

						Bitcalc.encode_long_to_bytes(result.value_bytes,
								c.value, value_byte_idx, c.value_bytelen);
						logger.finest(String
								.format("  result.values[] += control[%d].value = 0x%x (%d bytes)",
										c.index, c.value, c.value_bytelen));
						value_byte_idx += c.value_bytelen; // next pos in buffer
					}
			assert (value_byte_idx == result.value_bytes.length); // all bytes
																	// processed
			result.error_code = 0;
		}
		if (result.error_code > 0) {
			// fully construct result objects also in error case
			result.value_bytes = new byte[1];
		}

		return result;
	}

	/*
	 * make it Runnable (non-Javadoc)
	 *
	 * @see org.acplt.oncrpc.server.OncRpcServerStub#run()
	 */
	@Override
	public void run() {
		/*
		 * org.acplt.oncrpc.server.OncRpcServerStub#run() throws IOException and
		 * OncRpcException. This isn't comaptible with Runnable, so handle them
		 * here.
		 */
		try {
			super.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * blinkenbone.rpcgen.rpc_blinkenlight_apiServerStub#RPC_PARAM_GET_1(blinkenbone
	 * .rpcgen.rpc_param_cmd_get_struct)
	 *
	 * This panel simulation has no BlinkenBoards attached, so report state
	 * always as "active
	 */
	public rpc_param_result_struct RPC_PARAM_GET_1(
			rpc_param_cmd_get_struct cmd_get) {
		rpc_param_result_struct result = new rpc_param_result_struct();

		result.error_code = rpc_blinkenlight_api.RPC_ERR_PARAM_ILL_CLASS; // default:
																			// error
		result.object_class = cmd_get.object_class;
		result.object_handle = cmd_get.object_handle;
		result.param_handle = cmd_get.param_handle;
		result.param_value = 0;
		switch (cmd_get.object_class) {
		// case rpc_blinkenlight_api.RPC_PARAM_CLASS_BUS: break;
		case rpc_blinkenlight_api.RPC_PARAM_CLASS_PANEL:
			result.error_code = rpc_blinkenlight_api.RPC_ERR_PARAM_ILL_OBJECT;
			if (cmd_get.object_handle < panel_list.panels.size()) {
				Panel p = panel_list.panels.get(cmd_get.object_handle);
				result.error_code = rpc_blinkenlight_api.RPC_ERR_PARAM_ILL_PARAM;
				switch (cmd_get.param_handle) {
				case rpc_blinkenlight_api.RPC_PARAM_HANDLE_PANEL_BLINKENBOARDS_STATE:
					result.param_value = rpc_blinkenlight_api.RPC_PARAM_VALUE_PANEL_BLINKENBOARDS_STATE_ACTIVE;
					result.error_code = rpc_blinkenlight_api.RPC_ERR_OK;

					break;
				case rpc_blinkenlight_api.RPC_PARAM_HANDLE_PANEL_MODE:
					result.param_value = p.mode;
					result.error_code = rpc_blinkenlight_api.RPC_ERR_OK;
					break;
				}

			}
			break;
		// rpc_blinkenlight_api.case RPC_PARAM_CLASS_CONTROL: break;
		}
		// error code only 0 if class, object and param ok

		return result;
	}

	@Override
	public rpc_param_result_struct RPC_PARAM_SET_1(
			rpc_param_cmd_set_struct cmd_set) {
		rpc_param_result_struct result = new rpc_param_result_struct();

		// nothing implemented, but RPC_PARAM_HANDLE_PANEL_BLINKENBOARDS_STATE
		// is accepted. So result valid is given back
		result.error_code = rpc_blinkenlight_api.RPC_ERR_PARAM_ILL_CLASS; // default:
																			// error
		switch (cmd_set.object_class) {
		// case rpc_blinkenlight_api.RPC_PARAM_CLASS_BUS: break;
		case rpc_blinkenlight_api.RPC_PARAM_CLASS_PANEL:
			result.error_code = rpc_blinkenlight_api.RPC_ERR_PARAM_ILL_OBJECT;
			if (cmd_set.object_handle < panel_list.panels.size()) {
				Panel p = panel_list.panels.get(cmd_set.object_handle);
				result.error_code = rpc_blinkenlight_api.RPC_ERR_PARAM_ILL_PARAM;
				switch (cmd_set.param_handle) {
				case rpc_blinkenlight_api.RPC_PARAM_HANDLE_PANEL_BLINKENBOARDS_STATE:
					// just accept
					result.error_code = rpc_blinkenlight_api.RPC_ERR_OK;
					break;
				case rpc_blinkenlight_api.RPC_PARAM_HANDLE_PANEL_MODE:
					synchronized (p) {
						p.setMode(cmd_set.param_value) ;
					}
					p.setChanged(); // notify observer
					// observer may need "display clock"
					p.notifyObservers(); // without info object
					break;
				}
			}
			break;
		// case rpc_blinkenlight_api.RPC_PARAM_CLASS_CONTROL: break;
		}
		// error code only 0 if class, object and param ok

		if (result.error_code != rpc_blinkenlight_api.RPC_ERR_OK)
			return result;
		else {
			rpc_param_cmd_get_struct cmd_get = new rpc_param_cmd_get_struct();
			// result: query the same parameter
			cmd_get.object_class = cmd_set.object_class;
			cmd_get.object_handle = cmd_set.object_handle;
			cmd_get.param_handle = cmd_set.param_handle;
			result = RPC_PARAM_GET_1(cmd_get);

			return result;
		}
	}

	@Override
	public rpc_test_cmdstatus_struct RPC_TEST_DATA_TO_SERVER_1(
			rpc_test_data_struct data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public rpc_test_data_struct RPC_TEST_DATA_FROM_SERVER_1(
			rpc_test_cmdstatus_struct data) {
		// TODO Auto-generated method stub
		return null;
	}
}
