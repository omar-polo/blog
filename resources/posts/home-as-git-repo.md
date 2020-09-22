I finally decided to track my dotfiles on a git repository.  This
should make things simpler when migrating to another machine, as well
as having consistent configuration across various computers.  We'll
see how well it will go for me.

I've done a bit of research on the internet and I've found
[this](https://drewdevault.com/2019/12/30/dotfiles.html).  The author
of that post suggest to use your whole $HOME as a git repository, with
a one-byte `.gitignore`:

```gitignore
*
```

While the mine is actually two-bytes long due to a newline, this got
me started.  Git will ignore everything (music, documents, logs...)
except files that you add with `-f`.  So far so good.

---

**edit**: the part that follows is mostly wrong.  The problem I had
was due to this piece of my global git config:

```git
[core]
	excludesfile = ~/.gitignore
```

that was making git ignoring every file in my existings repos.

Why I had that thing in the first place?  Well, it's the result of
migrating to this machine.  I had a global gitignore file in my home
directory to ignore common files (like emacs backups, acme guide files
and so on), that I forgot to copy on the new machine.

I've kept the rest of the post because of the `.git/info/exclude` bit
that I didn't known about.

---

Except that this broke all my git repos.

I have several git repositories in subfolders inside my home, and
since git goes recursively when searching for `.gitignore`s it will
ignore EVERY file.

Maybe the author has his `~/build` or `~/src` mounted with NFS or
something else (git should stop at filesystem boundaries AFAIK), but
this isn't my case.

Fortunately there is a simple solution:
```sh
$ cd
$ mv .gitignore .git/info/exclude
```

This way, for your `~` repository, git will exclude files listed on
`~/.git/info/exclude` (that is, every file not manually added), while
behaving normally on every repository you have inside your home.
