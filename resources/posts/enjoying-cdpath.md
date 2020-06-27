If you use the shell a lot, you may find this advice useful, in case
you didn't already knew it.  There's an environment variable called
CDPATH, used by most shell I know and others cli tools, whose *raison
d'être* is to simplify `cd`-ing around.

As various environmental variables in UNIX, its value is a list of
directories separated by colons, just like PATH.  For instance, this
is what I currently have in my `~/.kshrc`:

```sh
export CDPATH=.:$HOME/w:/usr/ports
```

With that in place, no matter where my current working directory is, I
can `cd games/godot` to jump to `/usr/ports/games/godot`!

A note of warning: `.` (the dot aka your current working directory)
should be present in your `$CDPATH`, otherwise you won't be able to
`cd` into directories not found in your `$CDPATH` (you can use `cd
./$somedir`, but isn't probably what you want).

## Programs that I know respect `$CDPATH`

Since the entry would be too short otherwise, here's some programs
that I know respect `$CDPATH`, and how they behave.

### ksh (OpenBSD pdksh)

Just as I showed you up there.  When you `cd` into a directory inside
your `$CDPATH` it will print your new current working directory:

	$ cd games/godot
	/usr/ports/games/godot

It will not, however, autocomplete.

### bash

It will behave just like ksh.

### zsh

`zsh` respects `$CDPATH`.  It does not seem to do completions tho :(

### rc

9ports rc does not seem to inherit `$CDPATH`, but you can set it
(unsurprisingly) with

	cdpath=(. /usr/ports)

in your `~/lib/profile`. Other versions of `rc` (I'm talking about the
one you get with the `rc` package on FreeBSD) do inherit it, so double
check!

Additionally, `rc` prints the `pwd` only if you're `cd`-ing into
something that's not within `.` (the current directory).  So:

	% pwd
	/home/op
	% echo $cdpath
	. /usr/ports
	% cd bin  # won't print /home/op/bin
	% cd games
	/usr/ports/games
	%

### csh & tcsh

	set cdpath = (. /usr/ports)

for the rest, behaves exactly like `rc`.  I don't really use csh, nor
tcsh, so I can't make further comments.

### fish

I've installed fish just for this post.  It does respect `$CDPATH`
and, unlike other shells, is also able to do proper autocompletion
out-of-the-box.

### vi (nvi)

`vi` will inherit your `$CDPATH` (but make sure you're `export`ing
it!).  You can also `:set cdpath=…` if you wish.  You cannot edit a
file like `:e games/godot/Makefile` and assume vi will open
`/usr/ports/games/godot/Makefile` though, you need first to `:cd
games/godot` and then `:e Makefile`!

### bonus: emacs

Emacs vanilla `M-x cd` respects your `$CDPATH`, you just have to
delete the default text in the minibuffer.  It also does proper
autocompletion!  Additionally, `eshell` respects `$CDPATH` too! Not
all emacs packages will, unfortunately.  For instance, `ivy` doesn't
seem to care about it.

On the other hand, with emacs you have other ways to quickly jump
around.  Other than bookmarks (that I don't use), you have packages
like `projectile` that lets you jump between "projects" easily.
