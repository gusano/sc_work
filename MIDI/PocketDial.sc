/**
 * @file    PocketDial.sc
 * @desc    Use Doepfer Pocket Dial with SuperCollider.
 * @author  Yvan Volochine <yvan.volochine@gmail.com>
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 * @version 0.2
 * @since   2011-11-09
 * @link    http://github.com/gusano/sc_work/tree/master/MIDI
 * @TODO:   Implement non-endless mode
 */

PocketDial : YVMidiController {

    var <nodeDict;        // mapped params for each NodeProxy
    var <mapped;          // mapped NodeProxy
    var <proxyParamsDict; // updated NodeProxy keys/values
    var <resetDict;       // NodeProxies that need to have params updated
    var <>endless;        // endless mode (TODO)
    var lastTime;         // last time a param was changed
    var <>locked = false; // for PocketDialGui
    var <>gui;            // for PocketDialGui
    var <>inform = true;  // post params values in post window when moving CCs

    // src: name pattern for MIDI source/destination
    *new { |src, endless = true|
        ^super.new.init(src).endless_(endless);
    }


    // prepare MIDI in/out.
    // if no name is given for the destination, we assume it's the same as the
    // one given for source.
    init { |src|
        this.findMidiIn(src);
        this.makeResp();
        super.init();
        proxyParamsDict = ();
        resetDict       = ();
        nodeDict        = ();
        mapped          = (); // for PocketDialGui
        lastTime        = Main.elapsedTime;
    }

    free {
        this.unmapAll();
        ctlDict.clear;
        proxyParamsDict.clear;
        resetDict.clear;
        nodeDict.clear;
        mapped.clear();
        super.free;
    }

    // find the MIDIIn device via name pattern. If several sources contain this
    // name, only use the first one.
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

    makeResp {
        resp.remove();
        resp = CCResponder({ |src, chan, ccn, ccval|
            var lookie = this.makeCCKey(chan, ccn);

            if (this.class.verbose, { ['cc', src, chan, ccn, ccval].postcs });

            if (ctlDict[lookie].notNil and: { locked.not }, {
                //ctlDict[lookie].valueAll(ccval);
                ctlDict[lookie].value(ccval)
            });
            if (locked, {
                // volume
                gui.manageVol(ccn, ccval);
            });
        }, srcID);
    }

    lock { |bool|
        locked = bool;
    }

    addAction{ |ctlKey, action|
        ctlDict.add(ctlKey -> action);
        "added action for %".format(ctlKey).postcs;
    }

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

    // Get new values for proxy params and store them. This turns out to be much
    // faster than calling get() on a param for every new CC value (nudgeSet).
    updateProxyParams { |proxy, bank=nil, offset=nil|
        var pairs = proxy.getKeysValues;
        var dict  = proxyParamsDict[proxy] ?? ();

        try {
            if (bank.notNil, { // 1st update
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
                // just fetch the new values
                pairs.do{|p|
                    proxyParamsDict[proxy][p[0]][\val] = p[1]
                }
            });
            resetDict[proxy] = true;
        } {|e| e.errorString.warn }
    }

    // declare a function that will recursively assign all node params to CCs
    mapTo {
        arg proxy, bank=1, offset=1, params=nil,
            stepmin=0.05, stepmax=1.0, mapVol=true;

        var proxyName, pparams, pairs, maxNrOfCCs, i = 0;
        var ccVol = this.getCCKey(16, bank, 0);

        pairs = proxy.getKeysValues;
        pparams = params ?? pairs.flop[0];
        if (mapVol, { maxNrOfCCs = 15 }, { maxNrOfCCs = 16 });

        this.checkParamsSize(pparams.size);

        if (proxyParamsDict[proxy].isNil or: { resetDict[proxy] != true }, {
            this.updateProxyParams(proxy, bank, offset)
        });

        // use name as symbol String
        proxyName = this.fixName(proxy);

        nodeDict[proxyName] = ();
        nodeDict[proxyName][\node] = proxy;
        nodeDict[proxyName][\params] = List.new();
        // for PocketDialGui
        nodeDict[proxyName][\id] = bank;
        mapped.add(bank -> (\node: proxy, \name: proxyName));

        pparams.do{ |p|
            var cc = this.getCCKey(i, bank, offset);
            var action;

            if (p.asSymbol != \in,{
                if (i < maxNrOfCCs, {
                    if (p.asSpec.isNil, {
                        Spec.add(p.asSymbol, [0, 127]);
                        "% doesn't have a Spec !\nusing default\n".format(p).warn
                    });
                    this.addAction(
                        cc, this.generateFunction(proxy, p, stepmin, stepmax)
                    );
                    nodeDict[proxyName][\params].add(cc);
                    if (inform, {
                        postf("mapping % -> %_%\n", p, bank, i + offset)
                    });
                    i = i + 1;
                })
            })
        };
        if (mapVol == true, {
            nodeDict[proxyName][\params].add(ccVol);
            this.mapVolume(proxy, ccVol);
        });
    }

    // generateFunction Generate the function triggered by the CCResponder
    // TODO refactor
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
                this.updateProxyParams(proxy)
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

    // map a knob (default cc 16) to NodeProxy volume
    mapVolume { |proxy, cc|
        this.addAction(cc, { |val|
            var delta, volume;
            delta  = val - 64;
            // use a bigger default step for volume
            delta  = delta * delta.abs.linlin(1, 7, 0.05, 0.5);
            volume = \amp.asSpec.unmap(proxy.vol);
            volume = \amp.asSpec.map(volume + (delta / 127));
            proxy.vol_(volume);
            if (inform, {
                this.asciiParams(proxy, \vol, \amp.asSpec.unmap(volume))
            });
        });
    }

    unmap { |proxy|
        if (proxy.class != Symbol) {
            proxy = this.fixName(proxy)
        };
        nodeDict[proxy][\params].do{ |key|
            ctlDict.removeAt(key);
        };
        mapped.removeAt(nodeDict[proxy][\id]);
        nodeDict.removeAt(proxy);
    }

    unmapAll {
        nodeDict.keys.do(this.unmap(_))
    }

    // fix NodeProxy name so we can use both ~foo or 'foo' for accessing proxies
    fixName { |name|
        ^name.cs.replace("~", "").asSymbol;
    }

    getCCKey { |nr, bank, offset=1|
        var banks = ["A", "B", "C", "D", "E", "F","G","H"];
        var key = "kn%%%".format(banks[bank - 1], offset + nr).asSymbol;
        ^defaults[this.class][key]
    }

    checkParamsSize { |size|
        var max = 15;
        if (size > max, {
            "Too many params!\nMaximum is %.".format(max).warn
        });
    }

    // print param value in emacs post buffer
    asciiParams { |proxy, param, val|
        var size = 13, pos, str;
        pos = (val * size).round.asInteger;
        str = "";
        size.do { |i|
            if (i < pos, { str = str ++ "|" }, { str = str ++ "." });
        };
        (str + param + proxy.cs ++ "\n").asSymbol.post;
    }

    *makeDefaults {
        defaults.put(this, PocketDial.getDefaults);
    }

    // store CC numbers in 'defaults' Dictionary
    *getDefaults {
        // All on midi chan 0,
        // CC numbers are 0-15, 16-31, 32-47, 48-63
        // banks E, F, G, H are virtual
        var dict = ();
        var banks = ["A", "B", "C", "D", "E", "F","G", "H"];

        banks.size.do { |i|
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
