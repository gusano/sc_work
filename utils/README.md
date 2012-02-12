These are utilities of all kind.

GamePad
=======
Cross platform port of Alberto De Campo's ```GamePad``` quark.

YVJoy
=====
This is an emergency class written in a hurry to be able to use
a joystick (Thrustmaster Firestorm) for my next gigs.

Experimental !

Usage
-----
    p = ProxySpace.push(s.boot)

    ~noise = {WhiteNoise.ar(0.3)}

    j = YVJoy.new();

    // top button plays|stops NodeProxy
    j.mapTo(1, { ~noise.play }, { ~noise.stop })


ServerOptionsGui
================
GUI utility for Server options (work in progress).
