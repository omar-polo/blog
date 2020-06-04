Clojure was the first lisp I've learned.  Well, technically speaking,
my first one was scheme (racket actually), because was the language
chosen as an introduction to programming in my university.  Back in
the days, I was way more stupid than I am now (or at least I like to
think that) and I didn't really liked the language at all.

Fortunately enough, a couple of months later, I don't remember why, I
started learning clojure.  I found the idea of code-as-data, the sexp
thing, the syntax was... interesting.

(don't hate on me: recently I given scheme a second chance and I heavily respect it.  It's really a clean, nice and elegant language)

Fast forward several years: (as a complete elisp noob) I'm hacking a
piece of my Emacs configuration.  I have a line like this:

```elisp
(member (nth 2 (org-heading-components)) org-todo-keywords-1)
```

(now that I think of it, this is not a great example to show of how
cool is the `->` macro, but still...)

In a hypothetical elisp-clojure mix that line can be written as:
```clojure
(->> (org-heading-components)
  (nth 2)
  (-> (member org-todo-keywords-1)))
```

Sometimes some lispy form tends to be written *backwards* due to the
syntax of the language.  The threading macro lets you *invert* the
flow of the expression, and sometimes this makes the whole more
readable.

If you don't know what the `->` macro in clojure does, it does this:
```clojure
(-> x
  (* 5)
  (/ 2)
  inc)

;; gets rewritten as

(inc (/ (* x 5) 2))
```

That is, the first parameter is passed as first argument on the next
form, and so on.  The `->>` is similar, but adds the parameter as
*last* argument.

If you haven't seen anything about how macros are written, well, I
don't think I'm the most qualified to show how they works, but the
idea is that a macro gets called at compile time, when the compiler
has just finished reading the whole expression.  The macro is
therefore a function executed at compile time that returns code.

(these kind of macros are not the only possible.  Some lisp dialects
allows *reader macro*s, but that's another story)

What follows is an amateurish implementation of the macro in elisp.
It is definitely not something hard to write (quite the opposite in
fact) but can be useful as some sort of introduction to macros in
elisp, to show how *malleable* lisps are and to highlight how clean
the code is thanks to a little recursion.

```elisp
(defmacro -> (x &rest xs)
  "Should work like clojure `->'."
  (declare (indent defun))
  (cond
   ((eq xs nil)
    x)
   
   ((symbolp (car xs))
    `(-> (,(car xs) ,x)
       ,@(cdr xs)))

   (t
    `(-> (,(caar xs) ,x ,@(cdar xs))
       ,@(cdr xs)))))
```

First we have our base case: if `xs` is nil (i.e. the case of `(->
x)`) we can rewrite it as `x`.  Easy, right?

Then we have the two recursive cases:

 - `(-> x fn ...)` gets rewritten as `(-> (fn x) ...)` and
 - `(-> x (fn ...) ...)` gets rewritten as `(-> (fn x ...) ...)`.

The revelation writing this macro was that when I first learn of
`caar`, `cadr`, `cdar`, `caadr`, ... I thought of them being a joke
while now I can see that they can lead to cleaner code (sort of).

The counterpart, `->>`, is almost identical in its implementation:

```elisp
(defmacro ->> (x &rest xs)
  "Should work like clojure' `->>'"
  (declare (indent defun))
  (cond
   ((eq xs nil)
    x)

   ((symbolp (car xs))
    `(->> (,(car xs) ,x)
       ,@(cdr xs)))

   (t
    `(->> (,@(car xs) ,x)
       ,@(cdr xs)))))
```
