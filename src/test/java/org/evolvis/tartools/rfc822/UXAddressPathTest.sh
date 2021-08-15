#!/bin/mksh

# initialisation
export LC_ALL=C
unset LANGUAGE
set -e
set -o pipefail

cd "$(dirname "$0")"
rm -f *~

set +e
x=$(sed --posix 's/u\+/x/g' <<<'fubar fuu' 2>&1) && alias 'sed=sed --posix'
x=$(sed -e 's/u\+/x/g' -e 's/u/X/' <<<'fubar fuu' 2>&1)
case $?:$x {
(0:fXbar\ fuu) ;;
(*) print -u2 'E: your sed is not POSIX compliant'; exit 255 ;;
}
set -e

sed \
    -e 's/Path/UXAddress/g' \
    -e 's/UXAddressTest/UXAddressPathTest/g' \
    -e 's/Test ..link UXAddress. class/& ({@link Path} part)/' \
    -e 's/UXAddress\.AddressList/Path.AddressList/g' \
    -e 's/UXAddress\.AddrSpecSIDE/Path.AddrSpecSIDE/g' \
    -e '/UXAddress-trailing-dot-test/s/RN/WO/' \
    -e '/UXAddress-trailing-dot-test/s/SN/s0/g' \
    <PathTest.java >UXAddressPathTest.java~
mv UXAddressPathTest.java~ UXAddressPathTest.java
