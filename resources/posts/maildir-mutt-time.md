Mutt is, by far, my favourite mail client. It's small, configurable,
fast and it does not get in your way. It's a great tool.

My setup involves various maildir: I have a decent amount of filters
that I use to order the mail in different directories.

One thing that I've found annoying is switching maildir: opening ones
with ~3k mail takes too much time, at least on my machine. Now I think
that using the `threads` sorting was the culprit, but it's only a
guess.

Anyway, while I was searching the manpage for a totally different
issue, I've found the `header_cache` settings. Quoting the manpage:

> This variable points to the header cache database. [...] By default
> it is *unset* so no header caching will be used.
>
> Header caching can greatly improves speed when opening POP, IMAP HM
> or Maildir folders, see "caching" for details.

There are also other relating settings, like `header_cache_compress`
that may be interestig.
