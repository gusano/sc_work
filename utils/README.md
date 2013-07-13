Some [SuperCollider](http://github.com/supercollider/supercollider) utilities.

ServerOptionsGui
----------------
ServerOptionsGui moved to its own repository:
http://github.com/gusano/ServerOptionsGui

GamePad
-------
Cross platform port of Alberto De Campo's ```GamePad``` quark.

YVJoy
-----
This is an emergency class written in a hurry to be able to use
a joystick (Thrustmaster Firestorm) for my next gigs.

Experimental !

    p = ProxySpace.push(s.boot);

    ~noise = { WhiteNoise.ar(0.3) };

    j = YVJoy.new();

    // top button plays|stops NodeProxy
    j.mapTo(1, { ~noise.play }, { ~noise.stop });

    j.free;
