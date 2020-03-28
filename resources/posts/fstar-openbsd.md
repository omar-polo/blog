[F*](https://fstar-lang.org) is a *general purpose functional
programming language with effects aimed at program verification.*
Recently I've been playing a bit with it, it's nice, and here's a
quick guide on how to compile it on OpenBSD.

-----

*Edit*: This "guide" is partially incomplete. Doing some re-install
after this guide was published, I noticed that not everything is as I
wrote it here. Things change, I suppose. Some problems have been
fixed, but not everything. In particular `ocamlfind` will complain
(probably multiple times) during the build that it cannot find the
`XXX` package, and a simple `opam install XXX` will amend.

-----

We'll need both `git` and GNU `make` from ports, and also ocaml (to
build F* and run F* programs), opam (the ocaml package manager),
ocaml-camlp4 and python 3 (to build `z3`, a theorem prover).

	$ pkg_add git gmake ocaml ocaml-camlp4 opam python

*Note* I've installed camlp4 from the ports instead of through opam
because I was getting an error. I don't have much experience with
ocaml, but I've read *somewhere* that installing through the package
manager solved those problems.

## Building z3

z3 is the engine that powers the verification system of F*
(AFAIK). It's not available through ports, so we'll need to build from
source. Even though is a project that came from Microsoft, it seems to
run on OpenBSD just fine. Building it is also straightforward:

	$ git clone https://github.com/Z3Prover/z3
	$ cd z3
	$ CXX=clang++ python3 script/mk_make.py
	$ cd build
	$ make
	$ cp z3 $SOMEWHERE_IN_PATH # (maybe?)

*Note* by default `script/mk_make.py` will try to use base g++ and the
build will fail. The version of g++ is too ancient and doesn't support
C++11.

## Building F*

First of all, we need some ocaml dependencies, so make sure to
initialize opam (see `opam init`) and add the suggested stuff to your
shell init file.

	$ opam install ocamlfind batteries stdint zarith ppx_deriving ppx_deriving_yojson ocaml-migrate-parsetree process
	
We can now build the F* compiler from the ocaml output present in the
repo. You can also build the ocaml output by yourself, but I've skip
this step.

	$ gmake -j9 -C src/ocaml-output

The last step is to build the library. While the docs describes this
as an optional step, I wasn't able to compile F* programs without it.

	$ gmake -j9 -C ulib/ml

This was all. Let's try the hello world now!

	$ gmake -C examples/hello hello

It should take a decent amount of time to compile, output a *lot* of
text and, finally, at the end, a beautiful "Hello World".

Congrats, now you have a working F* installation!

