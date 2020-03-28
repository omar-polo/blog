CSS sprites are a nice idea, stolen from the game developers I think,
that consist to merge all the images you need in a single one and
download only that. I will not prove that it's best than downloading
*n* images, but it should clear that downloading a file, albeit a bit
bigger, requires less TCP segments going back and forth.

The other day I needed a CSS sprite. Being the lazy person I am, right
after The GIMP was open I thought that I didn't want to do the math.

After a bit of searching, I found a post where the author wrote a
script to generate the sprites (and the relative css file) with image
magick. I have lost that link, and the history of my browser doesn't
help. I remember that the domain name contains "php", but nothing
more. However, I rewritten the script, so it better fits my needs.

The idea is to join all the image using `convert(1)` and then generate
a CSS file with the offsets using `identify(1)`. Both programs should
come along with imagemagick.

Here's the script

	#!/bin/sh

	convert sprites/* -append img/sprites.png

	for i in sprites/*; do
		identify -format '%W %H %f\n' $i
	done | awk '
	function fname(path) {
		sub (".png", "", path)
		return path
	}

	BEGIN {
		y = 0

		print "%sprite-base {"
		print "	background-image: url(/img/sprites.png);"
		print "	background-repeat: no-repeat;"
		print "}"
		print ""
	}

	{
		width = $1
		height = $2
		class = fname($3)

		print "%sprite-" class " {"
		print "	@extend %sprite-base;"
		if (y == 0)
			print "	background-position: 0 0;"
		else
			print "	background-position: 0 -" y "px;"
		print "}"
		print ""

		y += height;
	}
	' > scss/_sprites.scss

Assuming that the images are within `sprites/` and all of them are png
files, this script will generate the sprite in `img/sprites.png` and a
SASS file in `sass/_sprits.scss` with one placeholder for every image
(the naming scheme is `%sprite-$NAMEFILE` with the `$NAMEFILE` being
the file name without extension).

It should be trivial to edit to handle pure CSS output, and eventually
other image formats.
