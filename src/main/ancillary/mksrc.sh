#!/usr/bin/env mksh
# -*- mode: sh -*-
#-
# Copyright © 2016, 2017, 2018, 2019, 2020
#	mirabilos <t.glaser@tarent.de>
#
# Provided that these terms and disclaimer and all copyright notices
# are retained or reproduced in an accompanying document, permission
# is granted to deal in this work without restriction, including un‐
# limited rights to use, publicly perform, distribute, sell, modify,
# merge, give away, or sublicence.
#
# This work is provided “AS IS” and WITHOUT WARRANTY of any kind, to
# the utmost extent permitted by applicable law, neither express nor
# implied; without malicious intent or gross negligence. In no event
# may a licensor, author or contributor be held liable for indirect,
# direct, other damage, loss, or other issues arising in any way out
# of dealing in the work, even if advised of the possibility of such
# damage or existence of a defect, except proven that it results out
# of said person’s immediate fault when using the work as intended.
#-
# Build-specific tool to create a tarball of the entire source code.

# initialisation
export LC_ALL=C
unset LANGUAGE
set -e
set -o pipefail
unset GZIP
exec 8>&1 9>&2

function errmsg {
	set -o noglob
	local x

	print -ru9 -- "$1"
	shift
	IFS=$'\n'
	set -- $*
	IFS=$' \t\n'
	for x in "$@"; do
		print -ru9 -- "| $x"
	done
}
function die {
	errmsg "$@"
	exit 1
}

# check that we’re really run from mvn
[[ -n $MKSRC_RUN_FROM_MAVEN ]] || die \
    'do not call me directly, I am only used by Maven' \
    "$(export -p)"
# initialisation
cd "$(dirname "$0")"
ancillarypath=$PWD
. ./cksrc.sh
cd "$parentpompath"

[[ ! -e failed ]] || die \
    'do not build from incomplete/dirty tree' \
    'a previous mksrc failed and you used its result'

# get project metadata
<pom.xml xmlstarlet sel \
    -N pom=http://maven.apache.org/POM/4.0.0 -T -t \
    -c /pom:project/pom:groupId -n \
    -c /pom:project/pom:artifactId -n \
    -c /pom:project/pom:version -n \
    |&
e=0
IFS= read -pr pgID || e=1
IFS= read -pr paID || e=1
IFS= read -pr pVSN || e=1
[[ $e = 0 && -n $pgID && -n $paID && -n $pVSN ]] || die \
    'could not get project metadata' \
    "pgID=$pgID" "paID=$paID" "pVSN=$pVSN"
# create base directory
tbname=target/mksrc
tzname=$paID-$pVSN-source
tgname=$tbname/$tzname
rm -rf "$tgname"
mkdir -p "$tgname"

# performing a release?
if [[ $IS_M2RELEASEBUILD = true ]]; then
	# if the dependency list for licence review still has a to-do or
	# fail item, fail the build (we can handle the current list sin‐
	# ce ./ckdep.sh already fails the build for not up-to-date list)
	if grep -e ' TO''DO$' -e ' FA''IL$' "$ancillarypath"/ckdep.lst; then
		die 'licence review incomplete'
	fi
fi

# check for source cleanliness
x=$(git status --porcelain)
if [[ -n $x ]]; then
	errmsg 'source tree not clean; “git status” output follows:' "$x"
	if [[ $IS_M2RELEASEBUILD = true ]]; then
		:>"$tgname"/failed
		:>"$tbname"/failed
		print -ru8 -- "[WARNING] maven-release-plugin prepare, continuing anyway"
		cd "$tbname"
		paxtar -M dist -cf - "$tzname"/f* | gzip -n9 >"../$tzname.tgz"
		rm -f src.tgz
		ln "../$tzname.tgz" src.tgz
		exit 0
	fi
	exit 1
fi

# copy git HEAD state
print -ru8 -- "copying source tree"
git ls-tree -r --name-only -z HEAD | sort -z | paxcpio -p0du "$tgname/"
print -ru8 -- "trimming source tree"

# omit what will anyway end up in depsrcs
if [[ $drop_depsrc_from_mksrc != 0 ]]; then
	rm -rf "$tgname/$depsrcpath"
fi

# create source tarball
ts=$(TZ=UTC git show --no-patch --pretty=format:%ad \
    --date=format-local:%Y%m%d%H%M.%S)
cd "$tbname"
find "$tzname" -print0 | TZ=UTC xargs -0r touch -h -t "$ts" --
print -ru8 -- "archiving source"
find "$tzname" \( -type f -o -type l \) -print0 | sort -z | \
    paxcpio -oC512 -0 -Hustar -Mdist | gzip -n9 >"../$tzname.tgz"
print -ru8 -- "moving source tarballs into place"
rm -rf "$tzname"  # to save space
rm -f src.tgz
ln "../$tzname.tgz" src.tgz

# shove dependencies’ sources into place
rm -f deps-src.zip
cd ..
found=0
for x in *-sources-of-dependencies.zip; do
	[[ -e $x && -f $x && ! -h $x && -s $x ]] || continue
	if (( found++ )); then
		errmsg 'multiple depsrcs archives found:' \
		    "$(ls -l *-sources-of-dependencies.zip)"
		break
	fi
	fn=$x
done
(( found == 1 )) || die 'could not link dependency sources'
ln "$fn" mksrc/deps-src.zip
print -ru8 -- "mksrc.sh finished successfully"
