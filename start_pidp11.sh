#!/bin/bash

# Make a temp directory in the /run ram disk (if it's not already created)
mkdir /dev/shm/pidp11 2>/dev/null

#echo "*** Start portmapper for RPC service, OK to fail if already running"
rpcbind & 
sleep 2

echo "*** booting $1 ***"
# create a bootscript for simh in the /run ramdisk:
(echo cd ./pdp11_operating_systems/$1;
echo do boot.ini
) >/dev/shm/pidp11/tmpsimhcommand.txt
echo "*** Start SIMH ***"
./src/02.3_simh/4.x+realcons/bin/pdp11_realcons /dev/shm/pidp11/tmpsimhcommand.txt
	
# Delete tmp simh command file
rm /dev/shm/pidp11/*.txt
