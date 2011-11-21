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
    /**
     * @var Doctionary nodeDict Used to store mapped params for a node
     */
    var <nodeDict;

    var <>softWithin = 0.05, <lastVals; // for normal mode
    var <>endless;

    var <orderedNames;
    var <proxyDict, <proxyParamsDict, <resetDict, <orderedParamsDict;
    var <>inform = true;

    var lastTime;


    *new { |srcName, endless = true|
        ^super.new.init(srcName).endless_(endless).init;
    }

    /**
     * init
     * Prepare MIDI in/out.
     * If no name is given for the destination, we assume it's
     * the same as the one given for source.
     *
     * @param String srcName Name pattern for MIDI source
     */
    init { |srcName|
        this.checkDependencies();
        this.findMidiIn(srcName);
        super.init();
        ccDict = ccDict ?? ();
        proxyParamsDict = ();
        resetDict = ();
        orderedParamsDict = ();
        nodeDict = ();
        lastTime = Main.elapsedTime;
    }

    /**
     * checkDependencies
     * @throws PocketDialError if dependencies are not installed
     */
    checkDependencies {
        if ('Ktl'.asClass.isNil, {
            PocketDialError(
                "Required 'Ktl' quark is not installed."
            ).throw
        })
    }

    free {
        this.unmapAll();
        ktlDict.clear;
        ccDict.clear;
        proxyParamsDict.clear;
        resetDict.clear;
        orderedParamsDict.clear;
        nodeDict.clear;
        super.free;
    }

    unmapAll {

    }

    /**
     * findMidiIn
     * Finds the MIDIIn device via name pattern. If several
     * sources contain this name, only the first one is used.
     *
     * @param String srcName Name pattern for MIDI source
     * @return self
     */
    findMidiIn { |srcName|
        MIDIClient.sources.do{ |x|
            block { |break|
                if (x.device.containsi(srcName), {
                    srcID = x.uid;
                    ("\n\PocketDial MIDIIn: %\n".format(x.device)).post;
                    break.();
                })
            }
        }
    }

    /**
     * addAction Add a function to a specific CC
     *
     * @param Symbol   ctlKey 'knE1'
     * @param Function action The function to be executed
     * @return self
     */
    addAction{ |ctlKey, action|
        ktlDict.add(ctlKey -> action);
        "added action for %".format(ctlKey).postcs;
    }

    /**
     * mapToNodeParams
     *
     * @param mixed node
     * @param Array pairs The name|params of the node
     * @return self
     */
    mapToNodeParams { |node ... pairs|
        pairs.do { |pair|
            var ctl, param, spec, func;
            #ctl, param = pair;
            this.checkParamSpec(param);
            func = { |ctl, val|
                var mappedVal;
                mappedVal = param.asSpec.map(val / 127);
                node.set(param, mappedVal);
                "%: % -> %\n".format(
                    node.cs, param, mappedVal.round(1e-3)
                ).post;
            };
            this.addAction(ctl, func)
        };
    }

    /**
     * update TODO: document
     * @param NodeProxy proxy
     * @param Integer   bank
     * @param Integer   offset
     */
    update { |proxy, bank=nil, offset=nil|
        var pairs = proxy.getKeysValues;
        var dict = proxyParamsDict[proxy] ?? ();

        try {
        // store ordered params
        orderedParamsDict.add(proxy -> pairs.flop[0]);

        if (bank.notNil, { /* 1st update */
            pairs.do{|p, i|
                var ccKey = this.getCCKey(i, bank, offset);
                // remove old key if any
                if (dict[p[0]].notNil, {
                    this.mapCC(dict[p[0]][\key], nil);
                    dict[p[0]][\key] = nil;
                });
                dict.add( p[0] -> (\key: ccKey, \val: p[1]) );
            };
            proxyParamsDict[proxy] = dict;
        }, {
            /* we just fetch the new values */
            pairs.do{|p|
                proxyParamsDict[proxy][p[0]][\val] = p[1]
            }
        });
        resetDict[proxy] = true;
        } {|e| e.errorString.warn }
    }

    /**
     * mapTo
     * Declare a function that will recursively assign all node params to CCs
     *
     * @param NodeProxy proxy   The node being controlled
     * @param Integer   bank    One of the 4 banks
     * @param Integer   offset
     * @param Array     params
     * @param Float     stepmin
     * @param Float     stepmax
     * @param Boolean   mapVol  Automatically assign CC16 to node volume
     */
    mapTo {
        arg proxy, bank=1, offset=1, params=nil,
            stepmin=0.05, stepmax=0.5, mapVol=true;

        var pparams, pairs, maxNrOfCCs;

        pairs = proxy.getKeysValues;
        pparams = params ?? pairs.flop[0];
        if (mapVol, { maxNrOfCCs = 15 }, { maxNrOfCCs = 16 });

        this.checkParamsSize(pparams.size);

        if (proxyParamsDict[proxy].isNil or: { resetDict[proxy] != true }, {
            this.update(proxy, bank, offset)
        });

        nodeDict[proxy] = ();
        nodeDict[proxy][\params] = List.new();

        pparams.do{ |p, i|
            var cc = this.getCCKey(i, bank, offset);
            var action;

            if (i < maxNrOfCCs, {
                if (p.asSpec.isNil, {
                    Spec.add(p.asSymbol, [0, 127]);
                    "% doesn't have a Spec !\nusing default\n".format(p).warn
                });
                this.addAction(
                    cc, this.generateFunction(proxy, p, stepmin, stepmax)
                );
                nodeDict[proxy][\params].add(cc);
                if (inform, {
                    postf("mapping % -> %_%\n", p, bank, i + offset)
                });
            });
        };
        if (mapVol == true, { this.mapVolume(proxy, 16, bank) });
    }

    /**
     * TODO: rewrite this mess
     */
    generateFunction { |proxy, param, stepmin, stepmax|
        var func = { |val|
            var delta, currentVal, newVal, time;
            time = Main.elapsedTime;
            delta = val - 64;
            delta = delta * delta.abs.linlin(1, 7, stepmin, stepmax);

            // if no cc was moved for 1 second, update NodeProxy params in case
            // they have been changed from the outside
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

            if (inform, {
                this.asciiParams(proxy, param, param.asSpec.unmap(newVal))
            });

            lastTime = time;
        };
        ^func;
    }

    mapVolume { |proxy, ccnr, bank=1|
        var cc = this.getCCKey(ccnr, bank, 0);

        this.addAction(cc, { |val|
            var delta, volume;
            delta  = val - 64;
            // use a bigger default step for volume
            delta  = delta * delta.abs.linlin(1, 7, 0.05, 0.5);
            volume = \amp.asSpec.unmap(proxy.vol);
            volume = \amp.asSpec.map(volume + (delta / 127));
            proxy.vol_(volume);
            if (inform, {
                this.asciiParams(proxy, 'vol', \amp.asSpec.unmap(volume))
            });
        });
        nodeDict[proxy][\params].add(cc);
    }

    /**
     * unmap
     * @param NodeProxy
     */
    unmap { |proxy|
        nodeDict[proxy][\params].do { |key|
            ktlDict.removeAt(key);
        };
        nodeDict[proxy][\params].clear;
    }

    /**
     * getCCKey Get CCkey corresponding to bank and offset
     * Given a knob number, a bank and offset, find the corresponding '0_13'
     * @param Integer nr
     * @param Integer bank
     * @param Integer offset
     * @return Symbol
     * @see makeResponder()
     */
    getCCKey { |nr, bank, offset=1|
        var banks = ["A", "B", "C", "D"];
        var key = "kn%%%".format(banks[bank - 1], offset + nr).asSymbol;
        ^defaults[this.class][key]
    }

    checkParamsSize { |size|
        var max = 15;
        if (size > max, {
            "Too many params!\nMaximum is %.".format(max).warn
        });
    }

    asciiParams { |proxy, param, val|
        var size = 13, pos, str;
        pos = (val * size).round.asInteger;
        str = "";
        size.do { |i|
            if (i < pos, { str = str ++ "|" }, { str = str ++ "." });
        };
        (str + param + proxy.cs ++ "\n").asSymbol.post;
    }

    /**
     * *makeDefaults Initialize PocketDial CC params
     */
    *makeDefaults {
        defaults.put(this, PocketDial.getDefaults);
    }

    /**
     * *getDefaults Stores the CC numbers in 'defaults' Dictionary
     * @return Dictionary
     */
    *getDefaults {
        // All on midi chan 0,
        // CC numbers are 0-15, 16-31, 32-47, 48-63
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


PocketDialError : Error {
    errorString {
        ^"PocketDial ERROR: " ++ what
    }
}
