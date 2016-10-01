#!/bin/bash
BR=192

set -e
set -o pipefail
trap 'echo ERROR; read a' ERR

cd "$(dirname "$0")"
cwd="$(pwd)"

for x in ???; do
	echo "--- $x"
	cd "$cwd"
	cd "$x"
	for mp3 in *.mp3; do
		if [ ! -f "$mp3" ]; then continue; fi
		normalize-mp3 -T .45 --bitrate "$BR" "$mp3"
	done
done
echo "DONE"
exit 0
