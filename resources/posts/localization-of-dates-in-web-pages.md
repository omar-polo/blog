I'll surely not be the first one to acknowledge that localization is
way more complex than a mere translation, but recently I was almost
bitten by a details, and that's the reason for this entry.  There are
cases when it's easy to think that localization equals translation.

One common thing is printing dates: the date of when a comment was
posted, the publication date of a post or the creation date of an
entity, and so on.  The javascript `Date` object has an
`.toLocaleDateString()` method that prints a locale-aware
representation of the date using the user locale, or the provided one

	d = new Date()
	d.toLocaleDateString() // my locale is en-US
	// 8/3/2020

	d.toLocaleDateString('it-IT')
	// 3/8/2020

	d.toLocaleDateString('ja')
	// 2020/8/3

Now, let's say that you have an application that's available in
various languages.  Let's assume, without loss of generality, that
Italian isn't among the languages you support, what happens if an
Italian use you web application?  You may, as it's common, use English
as default fallback language, and the user is probably fine with that.

If you've used `.toLocaleDateString`, or similar functions, without
specifying the locale, now you're screwed.  Or, to say at least, you
have a confused user.  Your user will read phrases like:

> [...] posted on 3/8/2020.

that just can't be interpreted correctly.  The user will probably
assume that since the site is in English the date is probably in the
format `month/day/year`, while it may be according to its locale and
thus be interpreted as `day/month/year`.

The fundamental problem here is a lack of context between the
application and the user.  While the HTML `time` tag has the
`datetime` attribute to provide a machine-readable date, no browsers
as far as I know provide a way to inform the user how that date (or
date time) should be interpreted.  It's only useful to machines, not
to users.

To reach a conclusion, what should be done?  Honestly I don't think
there is a silver bullet.  In some occasions I decided to use the less
ambiguous `YYYY/MM/DD` format, but it may not always be applicable.
You could try to map languages with locales, but it also may not work
as there isn't a bijection between languages and locales

	d.toLocaleDateString('ja')
	// "2020/8/3"
	d.toLocaleDateString('ja-JP-u-ca-japanese')
	// "R2/8/3"

and bloating the settings with things like "How you prefer date to be
printed" may be a bit silly.

I'm starting to think that providing locale-aware APIs that don't take
an explicit locale as **required** argument is just broken, but I may
be wrong.

P.S. node, at least on my system -- OpenBSD -CURRENT as of a couple
of days ago, seems to completely ignore the given locale.

P.P.S. [this commit][commit] from mpv was just too funny to read, it
reinforces my thought on requiring the locale in locale-aware APIs.

[commit]: https://github.com/mpv-player/mpv/commit/1e70e82baa9193f6f027338b0fab0f5078971fbe
