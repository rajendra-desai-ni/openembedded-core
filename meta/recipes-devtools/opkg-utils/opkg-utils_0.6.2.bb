SUMMARY = "Additional utilities for the opkg package manager"
SUMMARY:opkg-utils-shell-tools = "Additional utilities for the opkg package manager (shell scripts)"
SUMMARY:opkg-utils-python-tools = "Additional utilities for the opkg package manager (python scripts)"
SUMMARY:update-alternatives-opkg = "Utility for managing the alternatives system"
SECTION = "base"
HOMEPAGE = "http://git.yoctoproject.org/cgit/cgit.cgi/opkg-utils"
LICENSE = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://COPYING;md5=94d55d512a9ba36caa9b7df079bae19f \
                    file://opkg.py;beginline=2;endline=18;md5=ffa11ff3c15eb31c6a7ceaa00cc9f986"
PROVIDES += "${@bb.utils.contains('PACKAGECONFIG', 'update-alternatives', 'virtual/update-alternatives', '', d)}"

SRC_URI = "git://git.yoctoproject.org/opkg-utils;protocol=https;branch=master \
           file://0001-update-alternatives-correctly-match-priority.patch \
           "
SRCREV = "67994e62dc598282830385da75ba9b1abbbda941"

S = "${WORKDIR}/git"

TARGET_CC_ARCH += "${LDFLAGS}"

SUBPACKAGES = "opkg-utils-shell-tools ${@bb.utils.contains('PACKAGECONFIG', 'python', 'opkg-utils-python-tools', '', d)}"

PACKAGES =+ "${SUBPACKAGES}"

# main package is a metapackage that depends on all the subpackages
ALLOW_EMPTY:${PN} = "1"
RDEPENDS:${PN} += "${SUBPACKAGES}"

inherit perlnative

# For native builds we use the host Python
PYTHONRDEPS = "python3 python3-shell python3-io python3-math python3-crypt python3-logging python3-fcntl python3-pickle python3-compression python3-stringold"
PYTHONRDEPS:class-native = ""

PACKAGECONFIG = "python update-alternatives"
PACKAGECONFIG[python] = ",,,${PYTHONRDEPS}"
PACKAGECONFIG[update-alternatives] = ",,,"

FILES:opkg-utils-shell-tools = "\
    ${bindir}/opkg-build \
    ${mandir}/man1/opkg-build.1 \
    ${bindir}/opkg-buildpackage \
    ${bindir}/opkg-diff \
    ${bindir}/opkg-extract-file \
    ${bindir}/opkg-feed \
    ${bindir}/opkg-unbuild \
"

FILES:opkg-utils-python-tools = "\
    ${bindir}/opkg-compare-indexes \
    ${bindir}/opkg-graph-deps \
    ${bindir}/opkg-list-fields \
    ${bindir}/opkg-make-index \
    ${bindir}/opkg-show-deps \
    ${bindir}/opkg-update-index \
    ${bindir}/arfile.py \
    ${bindir}/opkg.py \
"

RDEPENDS:opkg-utils-shell-tools += "bash"

# python tools depend on shell tools because opkg-compare-indexes needs opkg-diff
RDEPENDS:opkg-utils-python-tools += "${PYTHONRDEPS} opkg-utils-shell-tools"

RRECOMMENDS:${PN}:class-native = ""
RRECOMMENDS:${PN}:class-nativesdk = ""
RDEPENDS:${PN}:class-native = ""
RDEPENDS:${PN}:class-nativesdk = ""

do_install() {
	oe_runmake PREFIX=${prefix} DESTDIR=${D} install
	if ! ${@bb.utils.contains('PACKAGECONFIG', 'update-alternatives', 'true', 'false', d)}; then
		rm -f "${D}${bindir}/update-alternatives"
	fi
}

do_install:append:class-target() {
	if ! ${@bb.utils.contains('PACKAGECONFIG', 'python', 'true', 'false', d)}; then
		grep -lZ "/usr/bin/env.*python" ${D}${bindir}/* | xargs -0 rm
	fi

	if [ -e "${D}${bindir}/update-alternatives" ]; then
		sed -i ${D}${bindir}/update-alternatives -e 's,/usr/bin,${bindir},g; s,/usr/lib,${nonarch_libdir},g'
	fi
}

# These are empty and will pull python3-dev into images where it wouldn't
# have been otherwise, so don't generate them.
PACKAGES:remove = "${PN}-dbg ${PN}-dev ${PN}-staticdev"

PACKAGES =+ "update-alternatives-opkg"
FILES:update-alternatives-opkg = "${bindir}/update-alternatives"
RPROVIDES:update-alternatives-opkg = "update-alternatives update-alternatives-cworth"
RREPLACES:update-alternatives-opkg = "update-alternatives-cworth"
RCONFLICTS:update-alternatives-opkg = "update-alternatives-cworth"

pkg_postrm:update-alternatives-opkg() {
	rm -rf $D${nonarch_libdir}/opkg/alternatives
	rmdir $D${nonarch_libdir}/opkg || true
}

BBCLASSEXTEND = "native nativesdk"

CLEANBROKEN = "1"
