/**
 * @file    PocketDialGui.sc
 * @desc    GUI to have access to On|Off and volume with Pocket Dial nodes.
 * @author  Yvan Volochine <yvan.volochine@gmail.com>
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 * @version 0.1
 * @since   2012-02-16
 * @link    http://github.com/gusano/sc_work/tree/master/MIDI
 * @todo    Use ProxyMixer once it's rewritten with Qt
 */

PocketDialGui {

    var <pocketDial, <win, <modeButton, <volumes, <plays, <assigns;

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
        assigns = ();
    }

    makeGui {
        if (win.notNil, { win.close });
        win = Window("PocketDialGui", Rect(100, 100, 400, 400)).front;
        win.layout_(QGridLayout());
        win.onClose_({ pocketDial.lock(false) });
        this.updateLayout();
    }

    update {
        win.view.removeAll();
        this.updateLayout();
    }

    updateLayout {
        //pocketDial.mapped.keys.do{ |id|
        16.do{ |id|
            var item, vol, play, assign, name;
            id = id + 1;
            try {
                item = pocketDial.mapped[id];
            };
            if (item.notNil, {
                vol = Slider()
                .orientation_(\horizontal)
                .action_{ |sl|
                    item[\node].vol_(sl.value)
                }
                .value_(item[\node].vol)
                .canFocus_(false);

                play = Button()
                .states_([
                    ["start", Color.black, Color.green],
                    ["stop", Color.black, Color.red]
                ])
                .action_{ |butt|
                    if (butt.value == 1, {
                        item[\node].play
                    }, {
                        item[\node].stop
                    })
                }
                .canFocus_(false);
                if (item[\node].monitor.isPlaying, {
                    play.value = 1
                }, {
                    play.value = 0
                });
                plays.add(id -> play);

                if (id > 4, { // only for virtual banks
                    assign = Button()
                    .states_([
                        ["assign", Color.black, Color.green],
                        ["assign", Color.black, Color.red]
                    ])
                    .action_{ |butt|
                        if (butt.value == 1, {
                            "assign".postln
                        }, {
                            "not assign".postln
                        })
                    }
                    .canFocus_(false);
                }, {
                    assign = nil;
                });
                assigns.add(id -> assign);

                name = StaticText().string_(item[\name]);

                volumes.add(id -> vol);

                win.layout.addSpanning(StaticText().string_(id.asString), id, 0);
            }, {
                vol = play = assign = name = StaticText().string_("");
            });
            win.layout.addSpanning(vol, id, 1);
            win.layout.addSpanning(play, id, 2);
            win.layout.addSpanning(assign, id, 3);
            win.layout.addSpanning(name, id, 4);
        };
        this.addModeButton();
        this.manageKeys();
    }

    addModeButton {
        modeButton = Button()
        .states_([
            ["volumes"],
            ["volumes", Color.black, Color.red]
        ])
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
        cc = (cc % 8) + 1; // works on all banks
        try {
            proxy = pocketDial.mapped[cc][\node];
            delta  = val - 64;
            // use a bigger default step for volume
            delta  = delta * delta.abs.linlin(1, 7, 0.05, 0.8);
            volume = \amp.asSpec.unmap(proxy.vol);
            volume = \amp.asSpec.map(volume + (delta / 127));
            proxy.vol_(volume);
            { volumes[cc].value_(volume) }.defer(0.1);
        }
    }

    manageKeys {
        var mode;
        win.view.keyDownAction_({ |view, char, modifiers, unicode, keycode|
            this.managePlay(unicode - 48);
            if (unicode == 32, {
                mode = modeButton.value;
                modeButton.valueAction_((1 - mode).abs);
            }, {
                // on my machine only...
                this.manageAssign(keycode - 16777263)
            })
        })
    }

    managePlay { |id|
        var node;
        try {
            node = pocketDial.mapped[id][\node];
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

    manageAssign { |id|
        if (id > 4 and: { id < 9 }, {
            ("managing " ++ id).postcs;
        })
    }

}
