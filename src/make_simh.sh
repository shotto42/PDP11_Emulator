
#!/bin/bash

# Comment by Oscar:
# this calls a much-simplified version of the impenetrable simh makefile
# however - it's simplified by me... you can still use the regular makefile from simh/BlinkenBone

cd ./02.3_simh/4.x+realcons/src
sudo make -f quickmake

cd ../
sudo setcap -v cap_net_raw,cap_net_admin=eip ./bin/pdp11_realcons
