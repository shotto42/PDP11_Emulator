#!/bin/bash

sudo apt update
#Install SDL2, optionally used for PDP-11 graphics terminal emulation
sudo apt install -y libsdl2-dev
#Install pcap, optionally used when PDP-11 networking is enabled
sudo apt install -y libpcap-dev
#Install readline, used for command-line editing in simh
sudo apt install -y libreadline-dev
# Install screen
sudo apt install -y screen
# Install newer RPC system
sudo apt install -y libtirpc-dev
# Install rpcbind
sudo apt install -y rpcbind

# Install JRE 1.7 for the panel simulation
sudo apt install -y default-jre
# Ant build tool to build the JAVA panels
sudo apt install -y ant

# Enable rpcbind
sudo systemctl enable rpcbind
sudo systemctl start rpcbind

# Allow user to start rpcbind without sudo
sudo setcap CAP_NET_BIND_SERVICE=+eip /usr/sbin/rpcbind
