These days, for various reason, I'm using USB tethering very often.
Enabling it it's not difficult at all, it as simple as

```sh
doas dhclient urndis0
```

But it's tedious.  Especially if you need to do it multiple times per
day.  I needed something to save me from filling my shell history of
`dhclient urndis0`.

Enter [`hotplugd(8)`](https://man.openbsd.org/hotplugd)!

`hotplugd` is a daemon that will listen for the attach/detach of
various devices and execute a script.  It's that simple, and at the
same time really useful.

Disclaimer: I don't like to write *howtos* because the man pages are
generally better, and the information in blog like this tends to rot
sooner or later.  I encourage you to go read the
[`hotplugd(8)`](https://man.openbsd.org/hotplugd) man page (it's
really short, simple and understandable -- it even has some examples!)
and consider this post as a "did you know?"-sort of thing.

With the disclaimer in place, let's continue.  The idea is that
`hotplugd` will execute `/etc/hotplug/attach` and `detach` script when
a device is attached or detached.  It doesn't need to be a shell
script, any executable file will do, but will stick with a plain old
sh script.

When you enable your phone USB tethering, a new device called
`urndisN` is created.  So, knowing this, we just need to execute
`dhclient` inside the `attach` script on the correct `urndis(4)`
devices.  Easy peasy:

```sh
#!/bin/sh
# /etc/hotplug/attach

DEVCLASS=$1
DEVNAME=$2

case $DEVCLASS in
# network devices
3)
	case $2 in
        # USB tethering
        urndis*) dhclient $2 ;;
    esac
esac
```

Remember to make the script executable and to enable `hotplugd(8)`:

```sh
# chmod +x /etc/hotplug/attach
# rcctl enable hotplugd
# rcctl start hotplugd
```

Every time you enable the tethering on your phone your computer will
automatically connect to it.  In theory the same principle can also be
used to automatically mount discs when plugged, but I haven't tried
yet.

NB: I'm not paranoid enough to worry about accidentally connect to a
stranger's phone.  You have been warned.
