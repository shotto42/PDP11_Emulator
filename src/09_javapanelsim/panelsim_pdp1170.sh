#!/bin/bash

# Select a start width, rotate knobs to CONS PHY and DATA PATH
# So user can watch Idle pattern, and operate LOAD ADR, EXAM, etc.
java -classpath panelsim_all.jar blinkenbone.panelsim.panelsim1170.Panelsim1170_app --width 1000  --addr_select 1 --data_select 1


