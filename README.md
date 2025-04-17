# PDP11/70 Emulator
https://hotto.de/software-hardware/pdp11-70-emulator/

<img src="https://hotto.de/wp-content/uploads/2025/04/PDP1170_FrontPanel.jpg" width="500" />  


## Why another PDP11/70 project to run an emulated replica?
Just because the PDP11 series of computers built by DEC (Digital Equipment Cooperation) in the 1970s had a significant impact due to their role in the development of UNIX and the programming language C.  
As a 16-bit minicomputer it laid the groundwork for todays CPU designs.  
The PDP11/70 variant has been launched in 1975 running a 5 MHz CPU including an MMU able to address 8 MB of memory.  

**This project is simply combining the great work of the following developers:**
- Jörg Hoppe who developed the SIMH Realcons extensions and the JAVA panels [3][4]  
- Oscar Vermeulen who offers a real hardware replica of the PDP11/70 panel [5][6] and curated a number of operating systems [14][15][16][17]  
- The SIMH team who developed emulators for important computer systems like the PDP [7][8]  

**Supported target platforms to run the emulation:**
- Linux (e.g. Ubuntu 24)
- Raspberry PI (tested on a model 2 running a 32Bit Rasbian)  


## Download source code from Github and installation
```
$ git clone "https://github.com/shotto42/PDP11_Emulator"
$ cd pidp11/src					# Go into the src directory
$ ./install_dependencies.sh			# Install all required dependencies
$ ./install_pdp11_operating_systems.sh		# Install the operating systems curated by Oscar Vermeulen
$ ./make_simh.sh				# Compile the SIMH including REALCONS extensions
$ ./make_panel.sh				# Compile the JAVA based PDP panels
```


## Run the PDP11/70 Panel and the SIMH Emulator
```
$ cd pidp11					# Go into the main directory
$ ./start_panel.sh				# The PDP11/70 panel needs to be startet before the emulator
$ ./start_pdp11.sh <operating system>		# Start the emulator (e.g. using the 211bsd operating system)
```

## Raspberry PI configuration to establish a remote connection
```
$ sudo raspi-config
Interface Options -> SSH			# Activate SSH when the system is used without keyboard and HDMI monitor
Interface Options -> VNC			# Activate VNC when the Real VNC Viewer [22] is used to access the Raspberry PI desktop
Advanced Options -> A1 Expand Filesystem	# Make sure that the whole SD card is used for the Rasbian OS
Advanced Options -> A6 Wayland -> W1 X11	# Activate X11 to enable the app GUI forwarding via SSH (e.g. MobaXterm [21])
```

## Examples

**PDP11/70 emulator and cool-retro-term [23] running 2.11BSD UNIX on a LINUX Ubuntu system**

<img src="https://hotto.de/wp-content/uploads/2025/04/PDP1170_211BSD.jpg" width="500"/>


**Remote Raspberry PI access via MobaXterm [21] on Windows 11 using SSH and X11 forwarding**

<img src="https://hotto.de/wp-content/uploads/2025/04/PDP1170_211BSD_MobaXterm.jpg" width="500"/>


## Operating Systems curated by Oscar Vermeulen

**Standard [14]**

| OS						| Description                                           |
| --------------------------|-------------------------------------------------------|
| DOS-11 					| First OS for the PDP11 by DEC 						|
| RT-11 					| Small single-user real-time computing system by DEC 	|
| RSX-11MP 					| Multi-user real-time operating system by DEC 			|
| RSTS/E Version 7 			| Multi-user, time-sharing operating system by DEC 		|
| 2.11BSD 					| UNIX by Berkeley Software Distribution 				|
| UNIX 5, 6 and 7 			| UNIX variants by Bell Labs 							|
| System III and System V 	| UNIX variants by AT&T Unix Support Group USG 			|  


**Add-On [15, 16, 17]**
- Paul Nankervis Collection of Operating Systems (needs to be loaded separately) [15]
- 2.11BSD Chase Covello's updated version (installed via ./install_pdp11_operating_systems.sh) [16]
- RSX-11M-PLUS V4.6 (Johnny Billquist's latest version with BQTC/IP (installed via ./install_pdp11_operating_systems.sh) [17]


## Documentation, Tutorials and Software
- bitsaver.org archieved interesting PDP11/70 documents from manuals to detailed schematics [9][10]
- learningpdp11.com provides a lot of interesting blog posts to learn more about the operations of the PDP11 [12]
- dave.cheney.net has an interesting article covering the PDP11 inventions and architecture [13]
- PDP11 software is archieved by bitsaver.org [11]
- Another PDP11/70 emulator (web based) by Paul Nankervis can be found here [18]


## SIMH Commands
For the full SIMH documentation go to [19]  
PDP11 specific SIMH configuration documentation can be found here [20]

A couple of simple SIMH commands to manage the PDP11/70 emulator on its internal command line:

| OS						| Description                                           |
|---------------------------|-------------------------------------------------------|
| CTRL-E					| Enter the SIMH command line                           |
| CONTINUE                  | Continue the emulation of the machine                 |
| EXIT                      | Leave the emulator                                    |


## Fix in case of RPCBIND issues
```
$ sudo systemctl enable rpcbind
$ sudo systemctl start rpcbind
$ sudo setcap CAP_NET_BIND_SERVICE=+eip /usr/sbin/rpcbind
```

## References
[1]  https://hotto.de/software-hardware/pdp11-70-emulator/  
[2]  https://github.com/shotto42/PDP11_Emulator  
[3]  https://retrocmp.com/projects/blinkenbone/simulated-panels  
[4]  https://github.com/j-hoppe/BlinkenBone  
[5]  https://obsolescence.dev/index.html  
[6]  https://github.com/obsolescence/pidp11  
[7]  https://github.com/open-simh  
[8]  https://github.com/simh  
[9]  https://bitsavers.org/pdf/dec/pdp11/1170/   
[10] https://bitsavers.org/pdf/dec/pdp11/  
[11] https://bitsavers.org/bits/DEC/pdp11/  
[12] https://www.learningpdp11.com/  
[13] https://dave.cheney.net/2017/12/04/what-have-we-learned-from-the-pdp-11  
[14] http://pidp.net/pidp11/systems24.tar.gz  
[15] http://pidp.net/pidp11/nankervis.tar.gz  
[16] https://github.com/chasecovello/211bsd-pidp11  
[17] http://mim.stupi.net/pidp.htm  
[18] https://skn.noip.me/pdp11/pdp11.html  
[19] https://github.com/simh/simh/blob/master/doc/simh_doc.doc  
[20] https://github.com/simh/simh/blob/master/doc/pdp11_doc.doc  
[21] https://mobaxterm.mobatek.net/download-home-edition.html  
[22] https://www.realvnc.com/en/connect/download/viewer/  
[23] https://github.com/Swordfish90/cool-retro-term  
