The HTTP protocol provides a definition for various headers.  Among
the various header defined, there is one that interest this rant
today: `Accept-Language`.

In layman's terms, the `Accept-Language` is used to tell the server
what language(s) the user understands.  For instance, when I visit a
website, firefox sends something like this:

```http
Accept-Language: en-US,en;q=0.5
```

This means that I want English pages.  Now, as always, in reality the
server can do what it wants, and that's right.  As an example, when
you visit Wikipedia you can read an entry in other languages, or maybe
you're using a web application that lets you choose the language.

There are also moments when you have to ignore this headers.  No
matter what `Accept-Language` you send when reading this entry, the
server will always reply with an English text.  This is also fine, as
not every content is available in every language.

However, if you have content in different languages then ignoring the
`Accept-Language` header and use the IP address to determine the
language to use is just plain dumb.  This is so stupid that I
shouldn't even write about it, but one of the biggest sites out there
does this, and today was the straw that breaks the camel's back.

Even if I'm Italian and live in Italy, I use the `en_US.UTF-8` locale
and browse the internet in English.  Over 90% of my queries on DDG or
google are in English (the percentage is completely arbitrary, but
shouldn't be too wrong).  I want the majority of websites to use the
English language when they serve me pages, and most of them respects
this decision.  Not google.

If I search something on google, I usually do it in a "private"
window, so the preferences aren't saved and every single time I have
to click to "English" in the homepage.  Or skim through a page of
results only to realize that I have to click "Change to English".

(Regarding the "private" window: I try to avoid google in general, so
I came up with this rule to use it only in private window.  It's a
inconvenience to reduce the number of times I use it.  Privacy isn't
really a concern here, given my `User-Agent` and a bit of GeoIP
machinery it should be über easy to track me online.  How many OpenBSD
users live in northern Italy?  I only know one guy except me)

There was a time when if you visited `google.it` it should use the
Italian language for the search results, and `google.com` used to
deliver English/international-ish results.  That was just perfect
(still broken, but at least workable).  Now google just see an Italian
IP and use Italian for the search results.

I wonder if I'll go to, say, Japan in vacation and do a search on
google, will I see Japanese results?

Even if this is just another example of google completely ignoring
even the most basic standards just to cook up an half-broken NIH
solution, it surprises me every time I think about it.  (Talking 'bout
NIH, believe me, I have another rant waiting to be written about
building a library made by them)

Is it really too difficult for google to just parse a goddamn header?

---

Addendum: the reason I prefer to browse the web in English is that, at
least for what concerns CS and tech in general, there seems to be
better results in English pages rather than in Italian pages.

