I have a tiny i686 at home with OpenBSD where I run, amongst some other
things, an instance of unbound.

Last night I decided that I wanted a dashboard to collect some statistics
about it.

My first thought was the ELK stack.  The only problem is the ram.
The little i686 has 1GB of ram, I don't know if it's enough to run
logstash, let alone the whole ELK.

A simple solution would be to collect the logs elsewhere, but I'm not
going to do this for various reason (lazyness being the first, followed
by the fact that having statistics about my dns queries isn't that
useful in my opinion, even if it's nice-to-have.)

Instead, my solution involves a bit of bash (don't hate me on this),
some fifos, tmux and ttyplot.

The primarly source of inspiration is [this
post](https://dataswamp.org/~solene/2019-07-29-ttyplot.html) that I red
some time ago: it's about plotting various system statistics with ttyplot.

The result is this

![unbound dashboard screenshot](/img/unbound-dashboard.png)

(note that I usually disable colors in xterm)

## The flow

	                    .-
	                   /     various   ------->     multiple
	unbound stats  -------    fifos    ------->      ttyplot
	                   \               ------->   per tmux pane
	                    `-

The idea is to run `unbound-control stats` every once in a while,
multiplexing its output and draw each (interesting) stats with ttyplot
in a tmux pane.

Why the fifos?  Well, if I'm not wrong, every time you call
`unbound-control stats` it will clear the statistics, so you can't run
it *n* times to collect *n* different stats.  And since the whole setup
requires only a couple of script, the easiest way was to use some fifos.

The whole setup requires three script:

 - `gen-dashboard.sh`
 - `dashboard.sh`
 - `mystatd.sh`

### `gen-dashboard.sh`

This is the startup script.  I run it on my crontab as `@reboot
/path/to/gen-dashboard.sh`.  It will create the required fifos, then
spawn a tmux session and create two windows and some panes.

```sh
#!/bin/sh

# create the fifos
for f in netstat queries hit miss time; do
        mkfifo /tmp/my-$f
done

session=dashboard

tmux new-session -d -s $session

# start mystatd.sh
tmux new-window -t $session:1 -n 'logs'
tmux send-keys "/path/to/mystatd.sh" C-m

# create the dashboard
tmux select-window -t $session:0

# setup the layout of the panes
tmux split-window -h
tmux select-pane -L
tmux split-window -v
tmux select-pane -R
tmux split-window -v -p 66
tmux split-window -v -p 50

# load the correct ttyplot in the panes
tmux select-pane -t 0
tmux send-keys "/path/to/dashboard.sh netstat" C-m

tmux select-pane -t 1
tmux send-keys "/path/to/dashboard.sh queries" C-m

tmux select-pane -t 2
tmux send-keys "/path/to/dashboard.sh hit" C-m

tmux select-pane -t 3
tmux send-keys "/path/to/dashboard.sh miss" C-m

tmux select-pane -t 4
tmux send-keys "/path/to/dashboard.sh time" C-m
```

(A possible improvement may be to tell tmux which command to run when
creating a pane instead of sending the keys to the shell, but it works
neverthless.)

There's nothing special about this script, so let's move to the next.

### `dashboard.sh`

This script also isn't interesting, all it does is pull the data out of
the correct fifo and start ttyplot with the correct labels and units.

```sh
#!/bin/sh

if [ -z "$1" ]; then
        echo "missing dashboard type"
        echo "usage: $0 <dashboard-name>"
        exit 0
fi

case "$1" in
        netstat)
                (while :; do
                        cat /tmp/my-netstat
                done) | ttyplot -t "IN Bandwidth in KB/s" \
                                -u "KB/s" -c "#"
                ;;

        queries)
                (while :; do
                        cat /tmp/my-queries
                done) | ttyplot -t "DNS Queries/5s" \
                                -u "q/5s" -c "#"
                ;;

        hit)
                (while :; do
                        cat /tmp/my-hit
                done) | ttyplot -t "DNS cache hit/5s" \
                                -u "ch/5s" -c "#"
                ;;

        miss)
                (while :; do
                        cat /tmp/my-miss
                done) | ttyplot -t "DNS cache miss/5s" \
                                -u "cm/5s" -c "#"
                ;;

        time)
                (while :; do
                        cat /tmp/my-time
                done) | ttyplot -t "DNS query time avg/5s" \
                                -c "#"
                ;;

        *)
                printf "%s\n" "$1 is not a valid dashboard"
                exit 1
                ;;
esac
```

### `mystatd.sh`

This is the (only?) interesting script.  It's also the only one that
requires bash, because I'm lazy, it was already installed as dependecy of
something, and because of the `>(cmd)` construct.  Rewriting the script
using only pure sh(1) constructs is left as an exercise to the reader
(hint: you need some extra fifo.)

```sh
#!/usr/bin/env bash

filter() {
        grep "$1" | awk -F= '{print $2}' > /tmp/my-$2
}

# unbound stats
( while :; do
        unbound-control stats                           \
        | grep -v thread0                               \
        | tee >(filter queries= queries)                \
        | tee >(filter hit hit)                         \
        | tee >(filter miss miss)                       \
        | filter time.avg time

        sleep 5
done ) &

# netstat - ty solene@ for the awk
(
        (while :; do
                netstat -ibn
                sleep 1
        done) | awk '
        BEGIN {
                old=-1
        }
        /^em0/ { 
                if(!index($4,":") && old>=0) {
                        print ($5-old)/1024
                        fflush
                        old = $5
                }
                if(old==-1) {
                        old = $5
                }
        }' | tee -a /tmp/my-netstat
) &

wait
```

The first piece collects the stat from unbound.  Let's break it in pieces.

 - `unbound-control stats` outputs the stats.  Keep in mind that this
 requires some privileges.  I've solved this by creating a script
 in /usr/local/bin that executes the command and allowed my user to
 launch that script via `doas(1)`.  Or you can run `mystatd.sh` as root.
 Do as you please.
 - `grep -v thread0` removes the per-thread stats (since my unbound
 uses only one thread).  A more solid approach like `egrep -v ^thread`
 is probably better.
 - `tee >(filter queries= queries) |` duplicates the stream: one copy
 goes to the subshell with `filter` and another copy goes on the pipes.
 - `filter` is just a small function to grep the desired entry and send
 it to `/tmp/my-$something`

The netstat bit filters the output of netstat (the awk is copied-pasted
from the previously linked post by solene@).  You may want to change the
`^em0` to match your network device.

And that's all!

## Possible improvements

 - if you `SIGINT` `mystatd.sh` some of its subprocess still run.  Maybe a
 `trap` is needed.  Since it is the only bash running on that system,
 `pkill bash` is, albeit a bit aggressive, a working solution.
 - replace bash.  It's not difficult, but requires more fifos.
 - ...

## Final considerations

This was fun.  Now I have a tmux session I remotely attach with cool
graphs.  This doesn't cover the archiviation of the statistics tho.
I think it should be trivial to add (just one more `|tee -a` to a local
file, maybe a cronjob to do rotation, ...) but for the moment I'm happy
with this result.
