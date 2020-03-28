The first version of this website had a theme switcher. It was
implemented with CSS variables (and a bit of javascript). Then the
javascript switcher was eventually removed, and the theme forced to be
dark, but I kept the CSS variables *just in case* (read: I'm lazy.)

*Edit*: this is no longer the case. The current version of the website
is *yet another one*, with a 100% rewritten (and pure) CSS.

The real reason I left the CSS variables was that I didn't wanted to
use a CSS preprocessor (such as `less` or `sass`) to manage such a
simple file (306 line, with blanks and comments). But, at the same
time, I didn't want to copy-paste the colors everywhere.

## Introducing the C preprocessor

The C preprocessor is a simple and well-known beast (sort of, at
least), and it's included in the base system installation of most
(pratically all, I presume) OSes.

If you have never used it, here's a quick howto.

You can define constants with
```
#define PI 3.141592653589793238462643383279502884197169
```
and use them whenever you like, for instance

```css
double p = PI / 4;
```

The preprocessor is more powerful, it supports `#include`s and
function-like macro (even variadic). But `#define`s are enough to
manage a couple of CSS variables.

Now, let's see how this applies to CSS. Given a file with the
following content

	#define BASE1 #221635
	
	body {
	    background-color: BASE1;
	}

we can *compile* it with

	$ cpp -P file.css > a.css

and obtain a valid CSS file `a.css`.

## Conclusions

It's weird. It's weird to invoke `cpp` to *build* a CSS files.

But it's also *satisfying*, in some sense.

As a conclusion, I would like to note that another option is to use
m4, a general purpose macro language that should be present on every
POSIX system. Unfortunately, I don't know the language very well, so I
opted to `cpp`.
