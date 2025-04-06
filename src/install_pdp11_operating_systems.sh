#!/bin/bash

#  List of PDP11 Operating Systems maintained by Oscar Vermeulen (https://obsolescence.dev/pdp11)

cd ..
wget -O ./pdp11_operating_systems.tar.gz http://pidp.net/pidp11/systems24.tar.gz
echo "Decompressing ... that takes a couple of seconds ..."
gzip -d pdp11_operating_systems.tar.gz
tar -xvf pdp11_operating_systems.tar
mv systems pdp11_operating_systems
rm pdp11_operating_systems.tar

# 20241126 Add Chase Covello's updated 2.11BSD straight from his github
# =============================================================================
echo
echo "2024 update: Add Chase Covello's updated 2.11BSD..." 
# Directory path
dir="./pdp11_operating_systems/211bsd+"
echo
echo "Checking if xz-utils is installed for decompression:"
sudo apt install xz-utils

# Check if the directory for Chase Covello's 211BSD already exists
if [ -d "$dir" ]; then
   echo
   echo "You already have the 211BSD+ directory!"
   echo "boot.ini and the disk image in $dir will be updated."
else
   echo
   echo "Creating $dir..."
   mkdir "$dir"
   echo
fi
echo
echo "Downloading from github.com/chasecovello/211bsd-pidp11"
echo "please visit that page for more information"
echo
# --no-check-certificate because of unclear Encryption errors from github
curl -L -o "${dir}/boot.ini" \
      "https://raw.githubusercontent.com/chasecovello/211bsd-pidp11/refs/heads/master/boot.ini" 
curl -L -o "${dir}/2.11BSD_rq.dsk.xz" \
      "https://github.com/chasecovello/211bsd-pidp11/raw/refs/heads/master/2.11BSD_rq.dsk.xz"
echo
echo Decompressing...
#cd "${dir}"
unxz -f ./${dir}/2.11BSD_rq.dsk.xz
echo
echo Modifying boot.ini by commenting out the icr device for bmp280 i2c
sed -i 's/^attach icr icr.txt$/;attach icr icr.txt/' "${dir}/boot.ini"
echo Modifying boot.ini by enabling the line set realcons connected:
sed -i 's/^;set realcons connected$/set realcons connected/' "${dir}/boot.ini"
echo
echo ...Done.
echo
echo Do not forget to visit github.com/chasecovello/211bsd-pidp11 
echo to find out about all the good stuff on this update!
echo



# 20241126 Add Johnny Billquist's latest RSX-11MPlus with BQTC/IP
# =============================================================================
# Directory path
dir="./pdp11_operating_systems/rsx11bq"
# Check if the directory for Johnny Billquists RSX-11 already exists
if [ -d "$dir" ]; then
  echo "You already have the Billquist RSX11BQ directory!"
  echo "Only the disk image in $dir will be updated."
else
  echo
  echo "Creating $dir..."
  mkdir "$dir"
  echo
  echo "Copying boot.ini from install/boot.ini.bilquist directory..."
  cp ./src/boot.ini.bilquist "${dir}/boot.ini"
fi

echo
echo "Getting files from ftp://ftp.dfupdate.se/pub/pdp11/rsx/pidp/"
echo
ftp_url="ftp://ftp.dfupdate.se/pub/pdp11/rsx/pidp"
files=("pidp.dsk.gz" "pidp.tap.gz")
cd ${dir}
for file in "${files[@]}"; do
  echo
  echo "Downloading $file..."
  wget --user="anonymous" --password="$email" -O ${file} "${ftp_url}/${file}"
  echo
  echo Decompressing...
  echo
  gunzip -f "${file}" 
done

echo    Do not forget to visit http://mim.stupi.net/pidp.htm 
echo    to find out about all the good stuff in this update!
