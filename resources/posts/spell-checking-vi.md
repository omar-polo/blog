UNIX is all about files and programs that do one thing and to it well,
right? `vi(1)` is one of my favorite text editors. However it lacks
some feature: but here's where the *composition* shines.

It's stupid and dead-simple actually, but I haven't thought
about it until some weeks ago. With a simple

```vi
map « :w^M:!aspell -c %^M:e!^M^M
```

in your `~/.nexrc` it's simple to do spell checking in `vi`.

**Friendly remainder**: the `^M` is literally the *enter* key inserted
with `C-v ENTER` or `C-v C-m`.

I've also the following binding in my `~/.nexrc` to spell check
Italian text:

```vi
map » :w^M:!aspell --lang=it -c %^M:e!^M^M
```

### What's that gibberish?

OK, it may be non-obvious what that that mapping does, so let's split
it into pieces:

 - `map «` starts a mapping on the `«` key
 - `:w^M` writes the current file (the return is necessary to *enter* the command)
 - `:!aspell -c %^M` run aspell over the file (`%` is replaced with the current file name)
 - `:e!^M` force vi to re-read the file
 - `^M` Tell vi to render the editor. After a command execution vi doesn't render its interface. Rather, it wait (a bit like `ex`) for a command. 

### Why the `«` and `»` characters?

Those keys aren't bind to anything and are simple to type with my keyboard layout.
