/**
 * @file    BCR.sc
 * @desc    Use Behringer BCR2000 with SuperCollider.
 * @author  Yvan Volochine <yvan.volochine@gmail.com>
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 * @version 0.1
 * @since   2011-09-19
 * @link    http://github.com/gusano/sc_work/tree/master/MIDI
 */

BCR : MIDIKtl {

    /**
     * @var bool verbose
     */
    classvar <>verbose = false;

    /**
     * @var Dictionary ccStoreDict Store all moved CCs
     */
    var <ccStoreDict;

    /**
     * @var Dictionary toogleDict Store toogle buttons
     */
    var <toogleDict;

    /**
     * @var String selector The button used for toggling recall mode for a node
     */
    var <selector = "btB";

    /**
     * *new
     *
     * @param string srcName  Name pattern for MIDI source
     * @param string destName Name pattern for MIDI destination
     * @return BCR
     */
    *new { |srcName, destName|
        ^super.new.init(srcName, destName);
    }

    /**
     * init
     * Prepare MIDI in/out.
     * If no name is given for the destination, we assume it's
     * the same as the one given for source.
     *
     * @param string srcName  Name pattern for MIDI source
     * @param string destName Name pattern for MIDI destination
     * @return BCR
     */
    init { |srcName, destName|
        if (destName.isNil, { destName = srcName });
        this.checkDependencies();
        this.findMidiIn(srcName);
        this.findMidiOut(destName);
        super.init();
        ccStoreDict = ccStoreDict ?? ();
        toogleDict = toogleDict ?? ();
    }

    /**
     * checkDependencies
     *
     * @return void
     * @throws Error if dependencies are not installed
     */
    checkDependencies {
        if ('Ktl'.asClass.isNil, {
            Error("Required 'Ktl' quark is not installed.").throw
        })
    }

    /**
     * findMidiIn
     * Finds the MIDIIn device via name pattern. If several
     * sources contain this name, only the first one is used.
     *
     * @param string srcName  Name pattern for MIDI source
     * @return void
     */
    findMidiIn { |srcName|
        MIDIClient.sources.do{ |x|
            block { |break|
                if (x.device.containsi(srcName), {
                    srcID = x.uid;
                    ("\n\nBCR MIDIIn:" + x.device).postln; "";
                    break.();
                })
            }
        }
    }

    /**
     * findMidiOut
     * Finds the MIDIOut device via name pattern. If several
     * destinations contain this name, only the first one is used.
     *
     * @param string destName  Name pattern for MIDI destination
     * @return void
     */
    findMidiOut { |destName|
        block { |break|
            MIDIClient.destinations.do{ |x, i|
                if ( x.device.containsi(destName), {
                    // For some reasons, destID is the index and not uid;
                    destID = i;
                    ("BCR MIDIOut:" + x.device).postln; "";
                    break.();
                })
            }
        }
    }

    /**
     * makeResp
     * CCs look for possible actions to trigger
     *
     * @return void
     */
    makeResp {
        this.removeResp();
        resp = CCResponder({ |src, chan, ccn, ccval|
            var lookie = this.makeCCKey(chan, ccn);
            if (this.class.verbose, { ['cc', src, chan, ccn, ccval].postcs });
            if (ktlDict[lookie].notNil) {
                ktlDict[lookie].value(ccn, ccval);
                ccStoreDict.put(ccn, ccval);
            }
        }, srcID);
    }

    /**
     * addAction
     * Add a function to a specific CC
     *
     * @param Symbol   ctlKey 'knE1'
     * @param Function action The function to be executed
     * @return void
     */
    addAction{ |ctlKey, action|
        var newKey = defaults['BCR'][ctlKey];
        ktlDict.add( newKey -> action );
    }

    /**
     * removeAction
     * Remove a function for a CC
     *
     * @param Symbol ctlKey '0_33'
     * @return void
     */
    removeAction{ |ctlKey|
        try
        { ktlDict.removeAt( ctlKey ) }
        { |e| (e.errorString).throw }
    }

    /**
     * mapToNodeParams
     *
     * @param mixed node
     * @param array pairs The name|params of the node
     * @return void
     */
    mapToNodeParams { |node ... pairs|
        pairs.do { |pair|
            var ctl, param, spec, func;
            #ctl, param = pair;
            this.checkParamSpec(param);
            func = { |ctl, val|
                if (ccStoreDict[toogleDict[node]] > 0, {
                    node.set(param, param.asSpec.map(val / 127));
                });
            };
            this.addAction(ctl, func)
        };
        if (destID.notNil) {
            this.sendFromProxy(node, pairs);
        };
    }

    /**
     * sendFromProxy Update BCR with current node values
     *
     * @param mixed node
     * @param array pairs The name|params of the node
     * @return void
     */
    sendFromProxy { |node, pairs|
        var ctlNames, params, currVals, midiVals;
        if (destID.notNil, {
            #ctlNames, params = pairs.flop;
            currVals = node.getKeysValues(params).flop[1];
            midiVals = currVals.collect { |val, i|
                (params[i].asSpec.unmap(val) * 127).round.asInteger;
            };
            [ctlNames, midiVals].flop.do { |pair|
                this.sendCtlValue(*pair);
            }
        })
    }

    /**
     * sendCtlValue Send a CCval to MIDIOut
     *
     * @param Symbol ctlName
     * @param int    val
     * @return void
     */
    sendCtlValue { | ctlName, val |
        var chanCtl = this.ccKeyToChanCtl(defaults['BCR'][ctlName]);
        midiOut.control(chanCtl[0], chanCtl[1], val);
    }

    /**
     * autoMapNode
     * Declare a function that will recursively assign all node params to CCs
     * and which will be toggled by a given ccSelector.
     *
     * @param mixed  node The node being controlled (SynthDef, NodeProxy, ...)
     * @param int    id   The "column" which controls the node (1 to 32) (8 x 4 groups)
     * @param Symbol offset
     * @param Preset preset TODO
     * @return void
     * @TODO   refactor
     */
    autoMapNode { |node, id, offset = 'knE1', preset |
        var nodeValues = node.getKeysValues;
        var nodeParams = nodeValues.flop[0];
        var ccKey      = (selector ++ id).asSymbol;
        var ccSelector = this.getCCNumForKey(ccKey);
        var offsetChar = offset.asString.drop(2).at(0).asString;
        var offsetNr   = offset.asString.drop(3).asInteger;
        var ctlKeyName = defaults['BCR'][ccKey];
        var ccNewNames = this.incrementCCNames(
            nodeParams.size, offsetNr, offsetChar
        );
        var pairs = [ccNewNames, nodeParams].flop;
        var func  = { |num, val|
            //var theKey;
            //theKey= defaults['BCR'].findKeyForValue(("0_"++num).asSymbol); // presets
            if (val > 0, {
                pairs.do{ |pair| this.mapToNodeParams(node, pair) };
                //this.managePreset(theKey, node, pairs);
            });
            //if (val == 0, { this.managePreset(nil) })
        };
        ktlDict.put(ctlKeyName, func);
        toogleDict.put(node, ccSelector);
        this.assignVolume(node, id);
        this.assignToggle(node, id);
        this.assignReset(node, id, pairs, nodeValues);
        // FIXME: TODO
        //if ( preset.notNil, { presets.put(ccKey, preset) });
    }

    /**
     * assignVolume on top knobs
     *
     * @param mixed node The node to control
     * @param int   id   The "column" number
     * @return void
     */
    assignVolume { |proxy, id|
        var volKnob = "kn%%".format(this.getGroupChar(id), id).asSymbol;
        this.addAction(volKnob, { |cc, val|
            proxy.vol_(\amp.asSpec.map(val / 127), 0.05)
        })
    }

    /**
     * assignToggle Play/stop on 1st row buttons
     *
     * @param mixed node The node to control
     * @param int   id   The "column" number
     * @return void
     */
    assignToggle { |proxy, id|
        var tglButton = "bt%%".format(this.getGroupChar(id), id).asSymbol;
        this.addAction(tglButton, { |cc, val|
            if (val > 0 and: {proxy.monitor.isPlaying.not}, {
                proxy.play
            }, {
                proxy.stop
            })
        })
    }

    /**
     * assignReset Reset node params with top knobs push-mode
     *
     * @param mixed node The node to control
     * @param int   id   The "column" number
     * @return void
     */
    assignReset { |proxy, id, pairs, defaultparams|
        var rstButton = "tr%%".format(this.getGroupChar(id), id).asSymbol;
        this.addAction(rstButton, { |cc, val|
            if (val > 0, {
                // safer to use default NodeProxy params values than Spec ones
                defaultparams.do { |def|
                    proxy.set(def[0], def[1]);
                };
                this.sendFromProxy(proxy, pairs);
            })
        })
    }

    /**
     * managePreset
     *
     * @return
     */
    managePreset { |key, node, pairs|
        "'managePreset' is not yet imlemented".warn
    }

    /**
     * getCCNumForKey
     *
     * @param symbol key
     * @return int
    */
    getCCNumForKey { |key|
        ^defaults['BCR'][key].asString.drop(2).asInteger;
    }

    /**
     * getGroupChar For the 4 top encoder groups
     *
     * @param int id
     * @return String
     */
    getGroupChar { |id|
        var chars = ["A", "B", "C", "D"];
        ^chars[((id - 1) / 8).asInt]
    }

    /**
     * checkParamSpec
     * Check if the param has a valid ControlSpec.
     * If not, assign a default one to it [0, 127]
     *
     * @param Symbol param
     * @return void
     */
    checkParamSpec { |param|
        if (param.asSpec.isNil, {
            Spec.add(param, [0, 127]);
            ("BCR:\ndefault Spec for" + param).warn;
        })
    }

    /**
     * incrementCCNames
     * Increment ccKeys and automatically change lettre when needed:
     * ex: \knB6, \knB7, \knB8, \knC1, ...
     *
     * @param int size Number of times to increment
     * @param int offsetNr
     * @param int offsetChar
     * @return Array
     */
    incrementCCNames { | size, offsetNr, offsetChar |
        ^size.collect { | i |
            var newKey, currentId;
            currentId = (offsetNr - 1 + i % 8) + 1;
            if (currentId == 1 and: { i > 0 }, {
                offsetChar = (offsetChar.ascii[0] + 1).asAscii
            });
            newKey = ("kn" ++ offsetChar.asString ++ currentId.asString).asSymbol
        }
    }

    /**
     * *getDefaults
     * Stores the CC numbers in 'defaults' Dictionary.
     *
     * @return Dictionary
     */
    *getDefaults {
        var dict = Dictionary.new;

        8.do{ |i|
            //4 encoder groups
            4.do{ |j|
                // top knob push mode
                dict.put(
                    ( "tr" ++ ["A", "B", "C", "D"][j] ++ (i + 1)).asSymbol,
                    ("0_" ++ (57 + (8 * j) + i)).asSymbol
                );
                // knobs (top row)
                dict.put(
                    ( "kn" ++ ["A", "B", "C", "D"][j] ++ (i + 1)).asSymbol,
                    ("0_" ++ (1 + (8 * j) + i)).asSymbol
                );
            };

            // buttons 1st row
            dict.put(("btA" ++ (i + 1)).asSymbol, ("0_" ++ (89 + i)).asSymbol);
            // buttons 2nd row
            dict.put(("btB" ++ (i + 1)).asSymbol, ("0_" ++ (97 + i)).asSymbol);
            // knobs (lower 3 rows)
            dict.put(("knE" ++ (i + 1)).asSymbol, ("0_" ++ (33 + i)).asSymbol);
            dict.put(("knF" ++ (i + 1)).asSymbol, ("0_" ++ (41 + i)).asSymbol);
            dict.put(("knG" ++ (i + 1)).asSymbol, ("0_" ++ (49 + i)).asSymbol);
        };

        // buttons (4 bottom right ones)
        dict.putAll(
            (
                prA1: '0_105',
                prA2: '0_106',
                prB1: '0_107',
                prB2: '0_108'
            )
        );
        ^dict
    }

    *makeDefaults {
        defaults.put('BCR', BCR.getDefaults);
    }
}
