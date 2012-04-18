/**
 * @file    PocketDial.sc
 * @desc    Use Doepfer Pocket Dial with SuperCollider.
 * @author  Yvan Volochine <yvan.volochine@gmail.com>
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 * @version 0.1
 * @since   2011-11-09
 * @link    http://github.com/gusano/sc_work/tree/master/MIDI
 * @TODO:   Implement non-endless mode
 * @TODO:   Remove Ktl dependency ?
 */

PocketDial : MIDIKtl {

    /**
     * @var Boolean verbose
     */
    classvar <>verbose = false;

    /**
     * @var Dictionary nodeDict Store mapped params for each NodeProxy
     */
    var <nodeDict;

    /**
     * @var Dictionary mapped Store  each mapped NodeProxy
     */
    var <mapped;

    /**
     * @var Dictionary proxyParamsDict Store updated NodeProxy keys/values
     */
    var <proxyParamsDict;

    /**
     * @var Dictionary resetDict Store proxies that need to have params updated
     */
    var <resetDict;

    /**
     * @var Boolean endless Endless mode (TODO)
     */
    var <>endless;

    /**
     * @var Boolean inform
     */
    var <>inform = true;

    /**
     * @var Float lastTime Last time a param was changed (see updateProxyParams)
     */
    var lastTime;


    /**
     * *new
     * @param String  src     Name pattern for MIDI source/destination
     * @param Boolean endless Endless mode (non-endless mode TODO)
     * @return PocketDial
     */
    *new { |src, endless = true|
        ^super.new.init(src).endless_(endless).init;
    }

    /**
     * init Prepare MIDI in/out.
     *      If no name is given for the destination,
     *      assume it's the same as the one given for source.
     * @param String src Name pattern for MIDI source/destination
     */
    init { |src|
        this.findMidiIn(src);
        super.init();
        proxyParamsDict = ();
        resetDict       = ();
        nodeDict        = ();
        mapped          = (); // for PocketDialGui
        lastTime        = Main.elapsedTime;
    }

    /**
     * free
     */
    free {
        this.unmapAll();
        ktlDict.clear;
        proxyParamsDict.clear;
        resetDict.clear;
        nodeDict.clear;
        mapped.clear();
        super.free;
    }

    /**
     * findMidiIn Finds the MIDIIn device via name pattern. If several
     *            sources contain this name, only the first one is used.
     * @param String srcName Name pattern for MIDI source
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
     * @param Symbol   ctlKey 'knE1'
     * @param Function action The function to be executed
     */
    addAction{ |ctlKey, action|
        ktlDict.add(ctlKey -> action);
        "added action for %".format(ctlKey).postcs;
    }

    /**
     * mapToNodeParams
     * @param mixed node
     * @param Array pairs The name|params of the node
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
     * updateProxyParams Get new values for proxy params and store them.
     *                   This turns out to be much faster than using nudgeSet
     *                   which calls get() on a param for every new CC value.
     * @param NodeProxy proxy
     * @param Integer   bank
     * @param Integer   offset
     */
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

    /**
     * mapTo
     * Declare a function that will recursively assign all node params to CCs
     * @param NodeProxy proxy   The node being controlled
     * @param Integer   bank    One of the 4 banks
     * @param Integer   offset
     * @param Array     params
     * @param Float     stepmin
     * @param Float     stepmax
     * @param Boolean   mapVol  Automatically assign a CC to node volume
     */
    mapTo {
        arg proxy, bank=1, offset=1, params=nil,
            stepmin=0.05, stepmax=0.5, mapVol=true;

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

    /**
     * generateFunction Generate the function triggered by the CCResponder
     * @param NodeProxy proxy
     * @param Symbol    param
     * @param Float     stepmin
     * @param Float     stepmax
     * @return Function
     * @TODO refactor
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

    /**
     * mapVolume Map a knob (default cc 16) to proxy volume
     * @param NodeProxy proxy
     * @param Integer   cc
     */
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

    /**
     * unmap Unmap a given proxy
     * @param NodeProxy
     */
    unmap { |proxy|
        if (proxy.class != Symbol) {
            proxy = this.fixName(proxy)
        };
        nodeDict[proxy][\params].do{ |key|
            ktlDict.removeAt(key);
        };
        mapped.removeAt(nodeDict[proxy][\id]);
        nodeDict.removeAt(proxy);
    }

    /**
     * unmapAll Unmap all currently mapped proxies
     */
    unmapAll {
        nodeDict.keys.do(this.unmap(_))
    }

    /**
     * fix name so we can use both ~foo or 'foo' for accessing proxies
     */
    fixName { |name|
        ^name.cs.replace("~", "").asSymbol;
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
        var banks = ["A", "B", "C", "D", "E", "F","G","H"];
        var key = "kn%%%".format(banks[bank - 1], offset + nr).asSymbol;
        ^defaults[this.class][key]
    }

    /**
     * checkParamsSize Warn if the proxy has too many params
     * @param Integer size
     */
    checkParamsSize { |size|
        var max = 15;
        if (size > max, {
            "Too many params!\nMaximum is %.".format(max).warn
        });
    }

    /**
     * asciiParams Print param value in emacs post buffer
     * @param NodeProxy proxy
     * @param Symbol    param
     * @param Integer   val
     */
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
