/**
 * @file    PocketDialGui.sc
 * @desc    GUI to have access to On|Off and volume with Pocket Dial nodes.
 * @author  Yvan Volochine <yvan.volochine@gmail.com>
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 * @version 0.1
 * @since   2012-02-16
 * @link    http://github.com/gusano/sc_work/tree/master/MIDI
 */

PocketDialGui {

    var <pocketDial, <win, <modeButton, <volumes, <plays;

    *new { |pocketDial=nil|
        if (pocketDial.isNil, {
            Error("missing pocketDial argument".throw)
        });
        ^super.new.init(pocketDial);
    }

    init { |aPocketDial|
        pocketDial = aPocketDial;
        win = nil;
        volumes = ();
        plays = ();
    }

    makeGui {
        if (win.notNil, { win.close });
        win = Window("PocketDialGui").front;
        win.layout_(QGridLayout());
        this.updateLayout();
    }

    updateLayout {
        //pocketDial.ids.keys.do{ |id|
        16.do{ |id|
            var item, vol, play, name;
            try {
                item = pocketDial.ids[id];

                vol = Slider()
                .orientation_(\horizontal)
                .action_{ |sl|
                    item[\node].vol_(sl.value)
                }
                .value_(item[\node].vol);

                play = Button()
                .states_([["start", Color.green], ["stop", Color.red]])
                .action_{ |butt|
                    if (butt.value == 1, {
                        item[\node].play
                    }, {
                        item[\node].stop
                    })
                };
                if (item[\node].monitor.isPlaying, {
                    play.value = 1
                }, {
                    play.value = 0
                });
                plays.add(id -> play);

                name = StaticText().string_(item[\name]);

                volumes.add(id -> vol);
            } {
                vol = play = name = StaticText().string_("");
            };
            win.layout.addSpanning(id, id, 0);
            win.layout.addSpanning(vol, id, 1);
            win.layout.addSpanning(play, id, 2);
            win.layout.addSpanning(name, id, 3);
        };
        this.addModeButton();
        this.manageKeys();
    }

    addModeButton {
        modeButton = Button()
        .states_([["volumes"], ["volumes", Color.red]])
        .action_{ |butt| this.manageVolMode(butt.value) }
        .canFocus_(false);
        win.layout.addSpanning(modeButton, 16, 1, 4);
    }

    manageVolMode { |mode|
        if (mode == 1, {
            pocketDial.lock(true);
            pocketDial.gui = this;
        }, {
            pocketDial.lock(false);
        });
    }

    manageVol { |cc, val|
        var delta, volume, proxy;
        cc = cc % 8; // works on all banks
        proxy = pocketDial.ids[cc][\node];
        delta  = val - 64;
        // use a bigger default step for volume
        delta  = delta * delta.abs.linlin(1, 7, 0.05, 0.8);
        volume = \amp.asSpec.unmap(proxy.vol);
        volume = \amp.asSpec.map(volume + (delta / 127));
        proxy.vol_(volume);
        { volumes[cc].value_(volume) }.defer(0.1);
    }

    manageKeys {
        var mode;
        win.view.keyDownAction_({ |view, char, modifiers, unicode, keycode|
            this.managePlay(unicode - 49);
            if (unicode == 32, {
                mode = modeButton.value;
                modeButton.valueAction_((1 - mode).abs);
            })
        })
    }

    managePlay { |id|
        var node;
        try {
            node = pocketDial.ids[id][\node];
        };
        if (node.notNil, {
            if (node.monitor.isPlaying, {
                node.stop;
                { plays[id].value_(0) }.defer(0.1)
            }, {
                node.play;
                { plays[id].value_(1) }.defer(0.1)
            });
        })
    }

}
