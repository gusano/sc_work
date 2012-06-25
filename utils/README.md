Some [SuperCollider](http://github.com/supercollider/supercollider) utilities.

ServerOptionsGui
----------------
GUI utility for setting Server options.

#### Screenshot (both simple and advanced views):

![My image](http://yvanvolochine.com/media/images/ServerOptionsGui.gif)

#### Usage

    s = Server.local;
    g = ServerOptionsGui(s);
Note that some settings need the Server to be rebooted for the changes to apply:
    s.reboot;

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
