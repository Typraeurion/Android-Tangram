# Simple makefile for the documentation

.PHONY: all pdf html clean

all: pdf html

pdf: Building_Tangram.pdf Tangram.pdf

html: Building_Tangram/Building_Tangram.html Tangram/Tangram.html

Building_Tangram.pdf: Building_Tangram.texinfo
	texi2pdf Building_Tangram.texinfo

Building_Tangram:
	mkdir -p Building_Tangram

Building_Tangram/Building_Tangram.html: Building_Tangram.texinfo | Building_Tangram
	texi2html --split node Building_Tangram.texinfo
	touch $@

Tangram.pdf: Tangram.texinfo
	texi2pdf Tangram.texinfo

Tangram:
	mkdir -p Tangram

Tangram/Tangram.html: Tangram.texinfo | Tangram
	texi2html --split node Tangram.texinfo
	touch $@
