Let's say you're debugging something on linux. Let's say that thing
crashed. What do you do? I will open gdb, load the executable and the
core file and try to figure it out why the thing crashed.

Well, it's exactly what happened. But there was no core file for
me. Poor me.

The defaults on OpenBSD, if I recall correctly, are to generate a core
file named `$prgname.core` in the directory where the program was
running before the crash. Also, but mind that I might be wrong, the
default ulimit settings is `unlimited` for the core files.

So, here's a quick description on *how to get that cores* on linux,
primarly aimed at myself:

#### ulimit

A `ulimit -c unlimited` is needed since the default may be 0.  This can
cause big core dumps in the `$HOME` if something like firefox or chrome
chrashes, but I prefer to have it in `.profile` than to have to remember
to execute the command.

#### pattern

On systems with systemd there should be a systemd-*somethingsomething*
daemon and *somethingsomething*-ctl to manage 'em.  I prefer have those
cores in the directory I'm in.  Bonus points if they are called like
I'm used.

```echo %e.core | sudo tee /proc/sys/kernel/core_pattern```

`%e` is replaced with the program name.

However, on my machine, the core files are called `$prgname.core.$pid`. To disable the `.pid` at the end

```echo 0 | sudo tee /proc/sys/kernel/core_uses_pid```

And that's all!