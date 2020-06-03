For a long time I've been procrastinating on this.  Even though there
is no compelling reason (from what I've understood -- I'm not a
cryptographer) to upgrade from my current 4096-bit RSA key to a shiny
new ed25519 key, today I've just made the first step towards the
removal of the "old" RSA key.

(A note must be made: not every ssh implementation supports ed25519
keys.  Even some keyring may not support it yet.)

The thing is that my current ssh public key is in a lot of places:
home devices, friends machines, remote servers, and even some web
sites... more than I can count and be *absolutely sure* I'm not
forgetting something, so a complete switch is not possible.

It's possible, however, to start using a new key without deleting the
first: as you may already know, in your ssh config you can use
`IdentityFile` (see `ssh_config(5)`).  The thing that I didn't knew
was that, by default, ssh checks multiple files, not only `id_rsa`
when logging.  This seems cool, and in fact it lets you save lines in
the configuration.

So, to keep it short, what I did was as simple as

	ssh-keygen -t ed25519 -f ~/.ssh/id_ed25519

and then adding the new public key to some servers.  You can check
with `ssh -v foo@bar` that ssh attempt to use various keys

    ; ssh -v foo@bar
    OpenSSH_8.3, LibreSSL 3.2.0
	debug1: Reading configuration data /home/op/.ssh/config
	[...]
	debug1: Offering public key: /home/op/.ssh/id_rsa RSA SHA256:...
	debug1: Authentications that can continue: ...
	debug1: Trying private key: /home/op/.ssh/id_dsa
	debug1: Trying private key: /home/op/.ssh/id_ecdsa
	debug1: Trying private key: /home/op/.ssh/id_ecdsa_sk
	debug1: Offering public key: /home/op/.ssh/id_ed25519 ED25519 SHA256:...
	debug1: Server accepts key: /home/op/.ssh/id_ed25519 ED25519 SHA256:...
	debug1: Authentication succeeded (publickey).
	[...]

(lines elided to keep them short and readable)

In this example, you can see that the ssh client first tries the
`id_rsa` key, but it didn't succeeded, then tries the (non existant in
my machine) `id_{dsa,ecdsa,ecdsa_sk}` and, finally, the `id_ed5519`
key.

In the end, the real reason that was keeping me from updating the key
was just the assumption that I needed to either replace the key in
every place or use the appropriate `IdentityFile` per-host.  Turns
out, this assumption is wrong and now I'm quite happy, with the
majority of the devices I connect to frequently already switched to
use my new ed25519 key, all without touching my local configuration :)
