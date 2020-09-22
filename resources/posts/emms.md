Without a specific motivation, I wanted to try some Emacs music
players.  I briefly tried BONGO, to finally set on EMMS, the Emacs
Multi Media System.

One thing that I really appreciate about EMMS is how minimal it is:
there is only one interface, the playlist buffer, and nothing else.

As Emacs itself, EMMS is also very flexible, and thanks to this I
believe I've found/build the best music player ever with it.  If you
use Emacs I really encourage you to try EMMS out.  The rest of this
post is just bits of my configuration and some screenshots.

## Setup

I'm not doing some fancy stuff to set it up.  I'm just doing what
suggested in the documentation:

```elisp
(require 'emms-setup)
(emms-all)
(emms-default-players)

;; fancy
(setq emms-mode-line-format "「%s」")
```

## Select a song

While you can easily select a song off a playlist in the `*EMMS
Playlist*` buffer, I prefer to use selectrum to do it.  The following
code is what I ended up with.

```elisp
(defun my/selectrum-emms ()
  "Select and play a song from the current EMMS playlist."
  (interactive)
  (with-current-emms-playlist
    (emms-playlist-mode-center-current)
    (let* ((selectrum-should-sort-p nil)
           (current-line-number (line-number-at-pos (point)))
           (lines (cl-loop
                   with min-line-number = (line-number-at-pos (point-min))
                   with buffer-text-lines = (split-string (buffer-string) "\n")
                   with lines = nil
                   for l in buffer-text-lines
                   for n = min-line-number then (1+ n)
                   do (push (propertize l' line-num n)
                            lines)
                   finally return (nreverse lines)))
           (selected-line (selectrum-read "Song: " lines
                                          :default-candidate (nth (1- current-line-number) lines)
                                          :require-match t
                                          :no-move-default-candidate t)))
      (when selected-line
        (let ((line (get-text-property 0 'line-num selected-line)))
          (goto-line line)
          (emms-playlist-mode-play-smart)
          (emms-playlist-mode-center-current))))))
```

![emms selectrum](/img/emms-selectrum.png "Selecting a song from the current playlist with selectrum")

Should be pretty easy to adapt it to use the standard
`completing-read`.

## Hydra

I usually listen to music while doing various things, and I don't the
context switch from/to what I'm working on and the playlist buffer.

An hydra seems the best approach: you get "bindings that stick around"
with just the little UI you need to manage your player.  It's
basically a mini-popup-ui for music controls.  (in addition, I've
recently discovered the hydra package, and I couldn't find an excuse
not to build a hydra for EMMS).

![emms hydra](/img/emms-hydra.png "An Hydra for EMMS")

I'm still pretty new to hydra, and I'm not 100% happy about how I'm
doing the interpolation, but anyway, here's the code.

First, some helper functions

```elisp
(defun my/active-p (x)
  "Return a string representation for the (assumed boolean) X."
  (if x 'yup 'nop))

(defun my/emms-player-status ()
  "Return the state of the EMMS player: `not-active', `playing', `paused' or `dunno'.

Modeled after `emms-player-pause'."
  (cond ((not emms-player-playing-p)
         ;; here we should return 'not-active.  The fact is that
         ;; when i change song, there is a short amount of time
         ;; where we are ``not active'', and the hydra is rendered
         ;; always during that short amount of time.  So we cheat a
         ;; little.
         'playing)
        
        (emms-player-paused-p
         (let ((resume (emms-player-get emms-player-playing-p 'resume))
               (pause (emms-player-get emms-player-playing-p 'pause)))
           (cond (resume 'paused)
                 (pause  'playing)
                 (t      'dunno))))
        (t (let ((pause (emms-player-get emms-player-playing-p 'pause)))
             (if pause 'playing 'dunno)))))

(defun my/emms-toggle-time-display ()
  "Toggle the display of time information in the modeline"
  (interactive "")
  (if emms-playing-time-display-p
      (emms-playing-time-disable-display)
    (emms-playing-time-enable-display)))
```

and then the full hydra

```elisp
(defhydra hydra-emms (:hint nil)
  "
%(my/emms-player-status) %(emms-track-description (emms-playlist-current-selected-track))

^Volume^     ^Controls^       ^Playback^              ^Misc^
^^^^^^^^----------------------------------------------------------------
_+_: inc     _n_: next        _r_: repeat one ;%(my/active-p emms-repeat-track)    _t_oggle modeline
_-_: dec     _p_: prev        _R_: repeat all ;%(my/active-p emms-repeat-playlist)    _T_oggle only time
^ ^          _<_: seek bw     _#_: shuffle            _s_elect
^ ^          _>_: seek fw     _%_: sort
^ ^        _SPC_: play/pause
^ ^        _DEL_: restart
  "
  ("+" emms-volume-raise)
  ("-" emms-volume-lower)
  ("n" emms-next)
  ("p" emms-previous)
  ("<" emms-seek-backward)
  (">" emms-seek-forward)
  ("SPC" emms-pause)
  ("DEL" (emms-player-seek-to 0))
  ("<backspace>" (emms-player-seek-to 0))
  ("r" emms-toggle-repeat-track)
  ("R" emms-toggle-repeat-playlist)
  ("#" emms-shuffle)
  ("%" emms-sort)
  ("t" (progn (my/emms-toggle-time-display)
              (emms-mode-line-toggle)))
  ("T" my/emms-toggle-time-display)
  ("s" my/selectrum-emms)

  ("q" nil :exit t))
```

## tag support

I compiled the companion program `emms-print-metadata` to have tag
support.  Tags are read from a cache or using the helper program, so
don't get frustrated if your tags aren't updated: clear the cache and
re-import the music.

To use the helper program one needs to

```elisp
(require 'emms-info)
(require 'emms-info-libtag)
(setq emms-info-functions '(emms-info-libtag))
```

## OpenBSD-specific

EMMS should use `mixerctl(1)` to adjust the volume on OpenBSD.  While
it should work out of the box, `mixerctl` is considered too low level
for normal user, and should be available only for root from 6.8
onwards.

So, here's a `emms-volume-sndioctl.el` that works.  It's basic, and
should be probably polished a little bit more.

```elisp
;;; emms-volume-sndioctl.el --- a mode for changing volume using sndioctl

;; This file is NOT part of EMMS.

;;;###autoload
(defun emms-volume-sndioctl-change (amount)
  "Change sndioctl output.level by AMOUNT."
  (message "Playback channels: %s"
           (with-temp-buffer
             (when (zerop
                    (call-process "sndioctl" nil (current-buffer) nil
                                  "-n"
                                  (format "output.level=%s%f"
                                          (if (> amount 0) "+" "")
                                          (/ (float amount) 100))))
               (replace-regexp-in-string "\n" "" (buffer-string))))))

(provide 'emms-volume-sndioctl)
```

Leave it somewhere in your `load-path`, `~/.emacs.d/lisp/` is a good
place, and then add to your configuration

```elisp
(require 'emms-volume-sndioctl)
(setq-default emms-volume-change-function 'emms-volume-sndioctl-change)
```
