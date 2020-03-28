In my [previous article](/2019-10-16-poor-mans-unbound-dashboard.html)
I used a bash(1) construct to send the same data to two different places.

The fact that I was using a bash-specific (more or less) construct really
bothered me this time.  So I thought if there is a way in sh(1) to take
a piece of data from a pipeline and send it to two different places.

The canonical way to do this is `tee(1)`, and it's what I've used it the linked article:

	something | tee >(cmd) | something-else

(the `>(cmd)` construct is bash, and zsh I think, specific: it executes
`cmd` in a subshell and execute `tee` with something like `/dev/fd/63`
-- at least on linux and OpenBSD.)

Now I'll try to describe three different approaches to emulate the same
behavior of `>(...)` in pure sh(1), trying to outline both the advantages
and the cons.

## temporary files

That is:

	something | tee a-file | something-else
	cmd < a-file
	
	# and then, probably
	rm a-file

(or even better with `mktemp(1)`)

This is the most simple approach I can think of.  The obvious disadvantage
is the need to create and delete files quickly.  This shouldn't be a huge
problem in most cases, but for script do this constantly and (possibly)
quickly it can be.  Plus, at least for me, it doesn't seem very elegant,
but it's subjective.

## fifo to the rescue

An almost similar approach is to use fifo.  A fifo, aka a named pipe,
is a special file that you can write to and read from, in a FIFO
fashion.

	mkfifo a-fifo
	something | tee a-fifo | something-else
	while read line < a-fifo; do echo $line; done | cmd

This is especially useful if you need to have a long-running script that
needs to duplicate a stream from one source.  You create the fifos at the
startup and then you're done, instead of constantly creating temporary
files over and over again.  An annoying thing is that you need to wrap
the read from the fifo in some sort of loop: AFAIK to read the next item
in a fifo you need to close and re-open it.

## An unexpected sed

Let's add another requisite: other than dumbly copy the stream, suppose
that you want also to apply some sort of transformation to one branch
of the duplication, like filter out something.

Well, you can adapt the last two examples by adding a `| sed '...'`.
Or you can use the `w` command of sed.

I'll be a bit more specific: let's say that you need to *dispatch*
something taken from a single stream and feed *n* subprocess.  It's
exactly what I've tried to achieve in my last post: I've used `>(cmd)`
to dispatch the unbound statistics to different fifo, and then I had
different processes pulling out those stats from the fifo to draw graphs.

The code was like

	unbound-control stats			\
	| tee >(grep something > a-fifo)	\
	| tee >(grep something-else > b-fifo)	\
	| ...

Well, that can be rewritten as

	unbound-control stats			\
	| sed 's/something/&/w a-fifo'		\
	| sed 's/something-else/&/w b-fifo	\
	| ...

by leveraging some sed behaviours:

1. every line read is printed to stdout (eventually transformed)
2. after the `s` command you can use `w` to write the transformed lines
   to a local file

I've got this *illumination* by reading [Sed - An introduction and
Tutorial by Bruce Barnett](http://www.grymoire.com/Unix/Sed.html).

I tried to use `sed 'g/something/w a-fifo'` but `sed` complained
about "extra characters after command", so I assume that the no-op
`s/something/&/` is needed.

-----

Edit: I tried with `g/...` because I thought that `sed` had a `g` command
that behaves like the `vi(1)` or `sam(1)` `g` command.  `sed` has a `g`
command, but does something different.  The correct form would be `sed
'/something/w a-fifo'` because `sed`, like `awk(1)`, has the ability to
perform commands only on lines that match a regular expression.

-----


I find this last technique pretty, even prettier than the `>(grep ... >)`
idiom I used previously since it avoids the subshell to do the filtering
and because it does not make assumption on the shell used -- it only
requires a POSIX shell.

I hope you found this interesting. At least to me it really is.
