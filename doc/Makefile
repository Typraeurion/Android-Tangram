# Simple makefile for the documentation

.PHONY: all pdf html clean

all: pdf html

pdf: Building_Tangram.pdf # Tangram.pdf

html: Building_Tangram_html/index.html # Tangram_html/index.html

Building_Tangram.pdf: Building_Tangram.texinfo
	texi2pdf Building_Tangram.texinfo

Building_Tangram_html:
	mkdir -p Building_Tangram_html
	ln -s ../Tan_placement_graph_example.png Building_Tangram_html/

Building_Tangram_html/index.html: Building_Tangram.texinfo | Building_Tangram_html
	texi2any --html -c HTML_MATH=mathjax --split node Building_Tangram.texinfo && touch $@

#Tangram.pdf: Tangram.texinfo
#	texi2pdf Tangram.texinfo

#Tangram_html:
#	mkdir -p Tangram_html

#Tangram_html/index.html: Tangram.texinfo | Tangram_html
#	texi2any --html --split node Tangram.texinfo && touch $@
