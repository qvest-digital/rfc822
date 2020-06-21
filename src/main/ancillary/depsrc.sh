#!/usr/bin/env mksh
# -*- mode: sh -*-
#-
# Copyright © 2018, 2019, 2020
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
# Build tarball containing source JARs of dependencies. Assume those
# without suitable source in Maven Central have distfiles copied and
# placed under $depsrcpath/ as courtesy copy.
#
# Scope is to provide sources of the artefacts we ship, for some li‐
# cences; we ship what we can, but Maven only makes source JARs pro‐
# vided by upstreams available, which aren’t CCS, but we cannot help
# it. Scope is not to provide everything needed to rebuild; we don’t
# ship scope “provided” things like Lombok (compiler plugin), Tomcat
# servlet API, PostgreSQL driver (JDBC), or even Tomcat or the JDK.

# initialisation
export LC_ALL=C
unset LANGUAGE
set -e
set -o pipefail
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
[[ -n $DEPSRC_RUN_FROM_MAVEN ]] || die \
    'do not call me directly, I am only used by Maven' \
    "$(export -p)"
# initialisation
cd "$(dirname "$0")"
ancillarypath=$PWD
. ./cksrc.sh
cd "$parentpompath"

# look at depsrc_add() in cksrc.sh to see the files in that directory
if [[ $require_depsrcpath_present != 0 ]]; then
	[[ -d $depsrcpath/. ]] || die "add $depsrcpath/ from deps-src.zip"
fi

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
rm -rf target/dep-srcs*
mkdir -p target/dep-srcs-parent
sed 's!<packaging>[^<>]*</packaging>!<packaging>pom</packaging>!g' \
    <pom.xml >target/dep-srcs-parent/pom.xml

npoms=0
function dopom {
	has=' '
	exec >target/pom-srcs-$npoms.xml
	cat <<-EOF
	<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
		<modelVersion>4.0.0</modelVersion>
		<parent>
			<groupId>$pgID</groupId>
			<artifactId>$paID</artifactId>
			<version>$pVSN</version>
			<relativePath>dep-srcs-parent/</relativePath>
		</parent>
		<artifactId>release-sources-$npoms</artifactId>
		<packaging>jar</packaging>
		<dependencyManagement>
			<dependencies>
	EOF
	while read -r g a v; do
		# check for multiple versions of same artefact
		if [[ $has = *" $g:$a "* ]]; then
			print -ru5 -- $g $a $v
			continue
		fi
		has+="$g:$a "

		# <dependencyManagement>
		#	<dependencies>
		cat <<-EOF
				<dependency>
					<groupId>$g</groupId>
					<artifactId>$a</artifactId>
					<version>$v</version>
		EOF
		depsrc_exclusions
		cat <<-EOF
				</dependency>
		EOF
		#	</dependencies>
		# </dependencyManagement>

		if depsrc_nosrc; then
			# see cksrc.sh for where to get sources
			continue
		fi

		# <dependencies>
		cat >&4 <<-EOF
			<dependency>
				<groupId>$g</groupId>
				<artifactId>$a</artifactId>
				<version>$v</version>
			</dependency>
		EOF
		# </dependencies>
	done <target/pom-srcs.in 4>target/pom-srcs.tmp 5>target/pom-srcs.out
	cat <<-EOF
			</dependencies>
		</dependencyManagement>
		<dependencies>
	EOF
	cat target/pom-srcs.tmp - <<-EOF
		</dependencies>
	</project>
	EOF
	exec >&8
	let ++npoms
	mv target/pom-srcs.out target/pom-srcs.in
}
cat "$ancillarypath"/ckdep.mvn >target/pom-srcs.in
while [[ -s target/pom-srcs.in ]]; do
	dopom
done

mkdir target/dep-srcs
for pom in target/pom-srcs-*.xml; do
	[[ -e $pom ]] || break # no dependencies case
	print -ru8 -- "downloading dependencies #${pom//[!0-9]}"
	mvn -B -f $pom -Dwithout-implicit-dependencies \
	    -DexcludeTransitive=true -DoutputDirectory="$PWD/target/dep-srcs" \
	    -Dclassifier=sources -Dmdep.useRepositoryLayout=true \
	    dependency:copy-dependencies
done

function doit {
	local f=$depsrcpath/$1 g=${2//./'/'} a=$3 v=$4

	[[ ! -d target/dep-srcs/$g/$a ]] || die \
	    'missing dependency sources unexpectedly found'
	mkdir -p target/dep-srcs/$g/$a
	mkdir target/dep-srcs/$g/$a/$v
	cp $f target/dep-srcs/$g/$a/$v/
}

depsrc_add

set_e_grep() (
	set +e
	grep "$@"
	rv=$?
	(( rv == 1 )) && rv=0  # no match ≠ error
	exit $rv
)

find target/dep-srcs/ -type f | \
    set_e_grep -F -v -e _remote.repositories -e maven-metadata-local.xml | \
    while IFS= read -r x; do
		x=${x#target/dep-srcs/}
		x=${x%/*}
		v=${x##*/}
		x=${x%/*}
		p=${x##*/}
		x=${x%/*}
		print -r -- ${x//'/'/.} $p $v
done | sort | set_e_grep -v "${depsrc_grep_exclusions[@]}" \
    >target/dep-srcs.actual
set_e_grep -v "${depsrc_grep_inclusions[@]}" <"$ancillarypath"/ckdep.mvn \
    >target/dep-srcs.expected
diff -u target/dep-srcs.actual target/dep-srcs.expected >&9
print -ru8 -- "depsrc.sh finished successfully"
# leave the rest to the maven-assembly-plugin
