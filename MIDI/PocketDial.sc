/**
 * @file    PocketDial.sc
 * @desc    Use Doepfer Pocket Dial with SuperCollider.
 * @author  Yvan Volochine <yvan.volochine@gmail.com>
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 * @version 0.1
 * @since   2011-11-09
 * @link    http://github.com/gusano/sc_work/tree/master/MIDI
 */

PocketDial : MIDIKtl {

    /**
     * @var bool verbose
     */
    classvar <>verbose = false;

    /**
     * @var Dictionary ccDict Store all moved CCs
     */
    var <ccDict;

    var <>softWithin = 0.05, <lastVals; // for normal mode
    var <>endless;

    var <orderedNames;
    var <proxyDict, <proxyParamsDict, <resetDict, <orderedParamsDict;
    var <>inform = true;

    var lastTime;

    *new { |srcID, ccDict, endless = true|
        ^super.newCopyArgs(srcID, ccDict).endless_(endless).init;
    }

    init {
        super.init;
        orderedNames = defaults[this.class].keys.asArray.sort;
        proxyParamsDict = ();
        resetDict = ();
        orderedParamsDict = ();
        lastTime = Main.elapsedTime;
    }

    free {
        ccDict.clear;
        proxyParamsDict.clear;
        resetDict.clear;
        orderedParamsDict.clear;
    }

    update { |proxy, bank=nil, offset=nil|
        var pairs = proxy.getKeysValues;
        var dict = proxyParamsDict[proxy] ?? ();

        // store ordered params
        orderedParamsDict.add(proxy -> pairs.flop[0]);

        if (bank.notNil, { /* 1st update */
            pairs.do{|p, i|
                var cckey = orderedNames[bank-1 * 16 + offset + i - 1];
                // remove old key if any
                if (dict[p[0]].notNil, {
                    this.mapCC(dict[p[0]][\key], nil);
                    dict[p[0]][\key] = nil;
                });
                dict.add( p[0] -> (\key: cckey, \val: p[1]) );
            };
            proxyParamsDict[proxy] = dict;
        }, {
            /* we just fetch the new values */
            pairs.do{|p|
                proxyParamsDict[proxy][p[0]][\val] = p[1]
            }
        });
        resetDict[proxy] = true;
    }

    mapTo { |proxy, bank=1, offset=1, params=nil, stepmin=0.05, stepmax=0.5, mapVol=true|
        var pparams, pairs;
        pairs = proxy.getKeysValues;
        pparams = params ?? pairs.flop[0];

        if (pparams.size > 16, {
            warn("Too many params!\nMaximum is 16.")
        }, {
            if (proxyParamsDict[proxy].isNil or: { resetDict[proxy] != true }, {
                this.update(proxy, bank, offset)
            });
            pparams.do{ |p, i|
                var cc = orderedNames[bank-1 * 16 + offset + i - 1];
                var knob = offset + i;
                if (p.asSpec.isNil, {
                    warn("% doesn't have a Spec !\n% not mapped.\n".format(p, p))
                }, {
                    this.mapCC(cc, this.generateFunction(proxy, p, stepmin, stepmax));
                    if (inform, { postf("mapping % on knob % - bank %\n", p, knob, bank) });
                });
            };
            if (mapVol == true, {
                this.mapVolume(proxy, 16, bank);
            });
        });
    }

    generateFunction { |proxy, param, stepmin, stepmax|
        var func = { |val|
            var delta, currentVal, newVal, time;
            time = Main.elapsedTime;
            delta = val - 64;
            delta = delta * delta.abs.linlin(1, 7, stepmin, stepmax);

            /* if no cc was moved for 1 second, update NodeProxy params in case they have been changed from the outside */
            if (time - lastTime > 1, {
                resetDict.add(proxy -> false);
            });

            // update params ?
            if (resetDict[proxy] != true, {
                this.update(proxy)
            });
            currentVal = proxyParamsDict[proxy][param][\val];
            currentVal = param.asSpec.unmap(currentVal);
            newVal = param.asSpec.map(currentVal + (delta / 127));

            try { proxy.set(param, newVal) };
            proxyParamsDict[proxy][param][\val] = newVal;

            /*if (inform, {
                postf("%: % -> %\n", proxy.asCompileString, param, newVal.round(1e-4))
            });*/
            if (inform, { this.asciiParams(proxy) });

            lastTime = time;
        };
        ^func;
    }

    mapVolume { |proxy, ccnr, bank=1|
        var cc = orderedNames[bank-1 * 16 + ccnr - 1 ];
        this.mapCC(cc, { |val|
            var delta, volume;
            delta = val - 64;
            delta = delta * delta.abs.linlin(1, 7, 0.05, 0.5); // bigger default step for vol
            volume = \amp.asSpec.unmap(proxy.vol);
            volume = \amp.asSpec.map(volume + (delta / 127));
            proxy.vol_(volume);
            if (inform, { postf("% vol -> %\n", proxy.asCompileString, volume.round(1e-4)) });
        });
    }

    asciiParams { |proxy|
        // returns keys-params in an ascii way (no gui)
        ("[ " ++ proxy.asCompileString ++ " ]\n").post;

        orderedParamsDict[proxy].do{ |key|
            var val = proxyParamsDict[proxy][key]['val'];
            val = key.asSpec.unmap(val);
            key = key.asString;
            key = key ++ (" " ! (16 - (key.size+1)));
            key = key.replace("[", "").replace("]", "").replace(", ", "");
            val = (val * 20).round;
            val = ("#"!val);
            val = val ++ ("-"!(20 - (val.size-1)));
            val = val.asString.replace(" ", "").replace(",", "").replace("]", "").replace("[", "");
            (key ++ "[" ++ val ++ "]" ++ "\n").post;
            "";
        };
        "\n\n\n\n".post;
        "";
    }


    /**
     * *makeDefaults Initialize PocketDial CC params
     */
    *makeDefaults {
        defaults.put('PocketDial', PocketDial.getDefaults);
    }

    /**
     * *getDefaults Stores the CC numbers in 'defaults' Dictionary.
     * @return Dictionary
     */
    *getDefaults {
        /**
         * @desc All on midi chan 0,
         *       CC numbers:  0-15, 16-31, 32-47, 48-63
         */
        var dict = ();
        var banks = ["A", "B", "C", "D"];

        4.do { |i|
            16.do { |ii|
                dict.put(
                    ("kn" ++ banks[i] ++ (ii + 1)).asSymbol,
                    ("0_" ++ (ii + (i * 16))).asSymbol
                )
            }
        };
        ^dict
    }

}