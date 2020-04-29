Recently, a friend of mine introduced me to the wonderful world of
game development with Godot.

During the initial (and still ongoing btw) enemy AI implementation, I
searched a bit on how to implement a state machine in gdscript.  Now,
a state machine isn't something difficult to do, and neither
error-prone, but I wanted to know if there was some idiomatic way of
doing things.  Oh boy, what I found is rather nice.

Let's start with a simple introduction: a state machine (for what
matters to us in games) is a way to manage the state of an actor.  It
can be the player, or an enemy, or even some other entity (such as an
object that can receive inputs).  A state machine is a collection of
states, only one of which can be enabled and receive inputs.  We can,
of course, change to another state whenever we want.

For instance, let's say that we have a player that can run and dash,
if we model the player with a state machine we can define two state,
`Normal` and `Dash`, with `Normal` being the default, and switch to
`Dash` when a key is pressed, and then from the `Dash` state return to
the `Normal` state when a certain amount of time has passed.

Another example can be the enemies: they're in a `Wandering` state by
default but if they see a player they start following it, and thus
they change their state to `Chase`.  Then, if the player dies, or it's
too far away, they can switch back to `Wandering`.

There may be other ways to manage states, but I do really like the
state machine abstraction (not only in games), and the code is pretty
clean and simple to maintain, so...

---

The simplest way to implement such machine in gdscript is something
along the lines of:

```gdscript
enum states { WANDERING, CHASING, FLEEING }

var state = WANDERING


func _process(delta):
  if state == WANDERING:
    do_wandering_process(delta)
  elif state == CHASING:
    do_chasing_process(delta)
  else:
    do_fleeing_process(delta)


func do_wandering_process(delta):
  if can_see_player():
    state = CHASING
  else:
    move_randomly()

# ...
```

and this is fine.  Rather verbose, if I you ask me, but it's fine, and
it works.

It turns out we can do better, like [this article on
gdscript.com](https://gdscript.com/godot-state-machine) shows.  Their
proposed approach involves a node, called `StateMachine`, which
manages the states, and some sub-nodes where you implement the logic
for that state.  There are some bits that I think can be done better,
so here's a (in my opinion) slightly better state machine (using typed
gdscript this time):

```gdscript
# StateMachine.gd
extends Node
class_name StateMachine

const DEBUG := false

var state
var state_name := "default state"

func _ready() -> void:
  state = get_child(0)
  _enter_state()


func change_to(s : String) -> void:
  state_name = s
  state = get_node(s)
  _enter_state()


func _enter_state() -> void:
  if DEBUG:
    print("entering state ", state_name)
  state.state_machine = self
  state.enter()


# proxy game loop function to the current state

func _process(delta : float) -> void:
  state.process(delta)


func _physics_process(delta : float) -> void:
  state.physics_process(delta)


func _input(event : InputEvent) -> void;
  state.input(event)
```

(if you're wondering why I've type-annotated everything but the `var
state`, that is due a current limitation of the type annotations in
Godot: if class A has annotation of class B, then class B cannot have
annotations of A.  This limitation is known and is worked on, future
version of Godot won't have this problem.)

The state machine proposed is very simple: it holds a current state
and provides a `change_to` function to change the state.  That's it.

Where my implementation differs from the one linked before, is in the
implementation of the states.  All my states inherits from the `State`
node, so all that machinery with `has_method` isn't needed.  The
implementation of the superclass `State` is as follow:

```
# State.gd
extends Node
class_name State

var state_machine : StateMachine

func enter() -> void:
  pass

func process(_delta : float) -> void:
  pass

func physics_process(_delta : float) -> void:
  pass

func input(_event : InputEvent) -> void:
  pass
```

One additional features this approach offers that the previous don't
is the `enter` function.  Sometimes you want to perform action upon
entering a state (like changing the animation and stuff), and it's
cleaner to have an explicit `enter` function that gets called.

Then, we can define states as:

```gdscript
# WanderingState.gd
extends State

# myself
onready var enemy : Actor = get_node("../..")

func process(delta : float) -> void:
  if can_see_player():
    state_machine.change_to("ChasingState")
  else:
    move_randomly()
```

and as

```gdscript
# ChasingState
extends State

# myself
onready var enemy = get_node("../..")

func process(delta : float) -> void:
  if player_is_dead():
    state_machine.change_to("WanderingState")
  elif enemy.life < FLEE_THRESHOLD:
    state_machine.change_to("FleeingState")
  else:
    ememy.velocity = move_and_slide(...)
```

with a scene tree as follows:
```
Enemy (KinematicBody2D in my case)
 |- ...
 \- StateMachine
     |- WanderingState
     |- ChasingState
     \- FleeingState
```

(the `get_node("../..")` is used to get the `Enemy` from the states,
so they can change parameters.)

This way, each state gets its own script with its own
`process`/`physics_process`, as well as with their private variables
and functions.  I really like the approach.

A final note: I didn't take benchmarks on any of the proposed
approaches, so I don't know what is the fastest.

## Addendum: Pushdown Automata

One additional thing that sometimes you want your state machine is
some sort of history.  The state machine as defined in the previous
example doesn't have a way to _go back_ to a previous state.  The
linked article solves this by using a stack (really an array) where
upon changing state the old state get pushed.

I honestly don't like the approach.  Let's say that you have three
states `A`, `B` and `C`, and that only `C` can, sometimes, go back in
history, but `A` and `B` never do so.  With a simple stack you can get
a situation where you have

```gdscript
oldstates = [A, B, A, B, A, B, A, B, A, B, ...]
```

that is wasting memory for no good.

One way to have record history but save some memory can be along the lines
of what follows:

```gdscript
# StateMachine
extends Node
class_name StateMachine

var state
var last_state
var history = []

func change_to(s : String) -> void:
  last_state = state
  state = get_node(s)
  _enter_state()

func prepare_backtrack() -> void:
  history.push_front(last_state)

func pop() -> void:
  history.pop_front()

func backtrack() -> void:
  state = history.pop_front()
  if state.count() > 0:
    last_state = state[0]
  _enter_state()

# the rest is not changed
```

and then call `state_machine.prepare_backtrack()` on `C` `enter`
function.  Then, if `C` wants to backtrack it can simply call
`backtrack()`, otherwise if it wants to change to another state, say
`B`, it can do so with:

```
state_machine.pop()
state_machine.change_to("B")
```

This approach isn't as idiot-proof as the one in the linked article,
but if used with a little bit of care can provide a pushdown automata
without memory wasting for states that don't backtrack.
