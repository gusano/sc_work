/**
 * @file    BCR.sc
 * @desc    Use Behringer BCR2000 with SuperCollider.
 * @author  Yvan Volochine <yvan.volochine@gmail.com>
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 * @version 0.2
 * @since   2011-09-19
 * @link    http://github.com/gusano/sc_work/tree/master/MIDI
 * @TODO    Add support for presets
 */

BCR : YVMidiController {

    var <ccDict;           // moved CCs
    var <nodeDict;         // store vol, on-off and selector keys
    var <>update = true;   // (en|dis)able updating BCR2000
    var <selector = "btB"; // selector for 'learn' buttons


    *new { |srcName="bcr", destName|
        ^super.new.init(srcName, destName);
    }

    // prepare MIDI in/out. If no name is given for the destination, we assume
    // it's the same as the one given for source.
    init { |srcName, destName|
        if (destName.isNil, { destName = srcName });
        this.findMidiIn(srcName);
        this.findMidiOut(destName);
        super.init();
        ccDict   = ccDict ?? ();
        nodeDict = nodeDict ?? ();
        this.makeResp();
    }

    free {
        super.free();
        ccDict.clear();
        nodeDict.clear();
    }

    // find the MIDIIn device via name pattern. If several sources contain
    // this name, only the first one is used.
    findMidiIn { |srcName|
        MIDIClient.sources.do{ |x|
            block { |break|
                if (x.device.containsi(srcName), {
                    srcID = x.uid;
                    ("\n\nBCR MIDIIn: %\n".format(x.device)).post;
                    break.();
                })
            }
        }
    }

    // find the MIDIOut device via name pattern. If several destinations contain
    // this name, only the first one is used.
    findMidiOut { |destName|
        block { |break|
            MIDIClient.destinations.do{ |x, i|
                if ( x.device.containsi(destName), {
                    // For some reasons, destID is the index and not uid;
                    destID = i;
                    this.connectJack();
                    ("BCR MIDIOut: %\n".format(x.device)).post;
                    break.();
                })
            }
        }
    }

    // main CCResponder lookup for actions to trigger
    makeResp {
        resp.remove();
        resp = CCResponder({ |src, chan, ccn, ccval|
            var lookie = this.makeCCKey(chan, ccn);
            if (this.class.verbose, { ['cc', src, chan, ccn, ccval].postcs });
            if (ctlDict[lookie].notNil) {
                try {
                    ctlDict[lookie].value(ccn, ccval);
                } { |e|
                    e.errorString.warn;
                };
                ccDict.put("%_%".format(chan, ccn).asSymbol, ccval);
            }
        }, srcID);
    }

    addAction{ |ctlKey, action|
        var newKey = defaults[this.class][ctlKey];
        ctlDict.add(newKey -> action);
    }

    removeAction{ |key|
        ctlDict.removeAt(key.asSymbol);
    }

    mapToNodeParams { |node ... pairs|
        pairs.do { |pair|
            var ctl, param, spec, func;
            #ctl, param = pair;
            this.checkParamSpec(param);
            func = { |ctl, val|
                var mappedVal;
                if (ccDict[nodeDict[node]['recall']] > 0, {
                    mappedVal = param.asSpec.map(val / 127);
                    node.set(param, mappedVal);
                    "%: % -> %\n".format(
                        node.cs, param, mappedVal.round(1e-3)
                    ).post;
                });
            };
            this.addAction(ctl, func)
        };
        this.sendFromProxy(node, pairs);
    }

    // update BCR with current node values
    sendFromProxy { |node, pairs|
        var ctlNames, params, currVals, midiVals;
        if (destID.notNil and: update == true, {
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

    // send a CCval to MIDIOut
    sendCtlValue { |ctlName, val|
        var chanCtl = this.ccKeyToChanCtl(defaults[this.class][ctlName]);
        midiOut.control(chanCtl[0], chanCtl[1], val);
    }

    // find out if the Node is a Synth or a NodeProxy.
    // we assume that user passes a String|Symbol in case of a Synth
    getNodeType { |node|
        if (node.isKindOf(NodeProxy),
            { ^node },
            { ^Synth.basicNew(node) }
        )
    }

    // declare a function that will recursively assign all node params
    // to CCs and which will be toggled by a given ccSelector.
    // TODO: refactor
    mapTo { |aNode, id, offset = 'knE1', preset|
        var node, nodeValues, nodeParams, ccKey, ccSelector, offsetChar,
            offsetNr, ctlKeyName, ccNewNames, pairs, func, newParams;

        if (id > 8, {
            BCRError("Cannot map more than 8 nodes.").throw
        });
        node       = this.getNodeType(aNode);
        nodeValues = this.getParamsValues(node);
        nodeParams = nodeValues.flop[0];
        ccKey      = (selector ++ id).asSymbol;
        ccSelector = this.getCCNumForKey(ccKey);
        offsetChar = offset.asString.drop(2).at(0).asString;
        offsetNr   = offset.asString.drop(3).asInteger;
        ctlKeyName = defaults[this.class][ccKey];
        ccNewNames = this.incrementCCNames(
            nodeParams.size, offsetNr, offsetChar
        );
        pairs = [ccNewNames, nodeParams].flop;
        func  = { |num, val|
            // TODO presets
            //var theKey;
            //theKey= defaults[this.class].findKeyForValue(("0_"++num).asSymbol);
            if (val > 0, {
                pairs.do{ |pair| this.mapToNodeParams(node, pair) };
                //this.managePreset(theKey, node, pairs);
            });
            //if (val == 0, { this.managePreset(nil) })
        };
        newParams = ccNewNames.collect{ |key|
            "0_%".format(this.getCCNumForKey(key)).asSymbol
        };
        //if (offsetChar.ascii > 69, {
        //    "Mapping offset is too low: %".format(offsetChar).warn
        //});
        ctlDict.put(ctlKeyName, func);
        // create nodeDict[node] and save params and recall
        nodeDict.put(node, ());
        nodeDict[node].add('params' -> newParams);
        nodeDict[node].add('recall' -> "0_%".format(ccSelector).asSymbol);
        this.assignVolume(node, id);
        this.assignToggle(node, id);
        this.assignReset(node, id, pairs, nodeValues);
        // FIXME: TODO
        //if ( preset.notNil, { presets.put(ccKey, preset) });
    }

    // remove node and associated volume, selector and function
    unmap { |node|
        try {
            nodeDict[node].keys.do{ |key|
                if (key != 'params', {
                    this.removeAction(nodeDict[node][key]);
                }, {
                    nodeDict[node]['params'].do(this.removeAction(_));
                })
            };
            nodeDict.removeAt(node);
        } { |e|
            e.errorString.warn;
        }
    }

    unmapAll {
        nodeDict.keys.do{ |key| this.unmap(key) }
    }

    // assign node volume to top knob
    assignVolume { |node, id|
        var volKnob = "kn%%".format(this.getGroupChar(id), id).asSymbol;
        var func;
        if (node.isKindOf(NodeProxy), {
            func = { |cc, val| node.vol_(\amp.asSpec.map(val / 127), 0.05) }
        }, {
            // assume the Synth has a 'vol' param...
            // TODO server message style with name
            func = { |cc, val| node.set(\vol, \amp.asSpec.map(val / 127)) }
        });
        this.addAction(volKnob, func);
        nodeDict[node].add(
            'volume' -> "0_%".format(this.getCCNumForKey(volKnob)).asSymbol
        );
    }

    // assign play/stop to 1st row button
    assignToggle { |node, id|
        var tglButton = "bt%%".format(this.getGroupChar(id), id).asSymbol;
        var func;
        if (node.isKindOf(NodeProxy), {
            func = { |cc, val|
            if (val > 0 and: {node.monitor.isPlaying.not},
                { node.play },
                { node.stop }
            )}
        }, {
            func = { "not implemented".postln };
            //s.sendBundle(s.latency, ["/s_new", "test", s.nextNodeID, 0, 1])
        });
        this.addAction(tglButton, func);
        nodeDict[node].add(
            'toggle' -> "0_%".format(this.getCCNumForKey(tglButton)).asSymbol
        );
    }

    // assign reset to top knob push-mode
    assignReset { |node, id, pairs, defaultparams|
        var rstButton = "tr%%".format(this.getGroupChar(id), id).asSymbol;
        var func;

        if (node.isKindOf(NodeProxy), {
            func = { |cc, val|
            if (val > 0, {
                // safer to use default NodeProxy params values than Spec ones
                defaultparams.do { |def|
                    node.set(def[0], def[1]);
                };
                this.sendFromProxy(node, pairs);
            }) }
        }, {
            "BCR::assignRest is not implemented for Synth".warn;
            ^nil
        });
        this.addAction(rstButton, func);
        nodeDict[node].add(
            'reset' -> "0_%".format(this.getCCNumForKey(rstButton)).asSymbol
        );
    }

    getParamsValues { |node|
        if (node.isKindOf(NodeProxy),
            { ^node.getKeysValues },
            { ^this.getSynthKeysValues(node) }
        )
    }

    getSynthKeysValues { |node|
        var name = node.defName.asSymbol;
        var metadata = SynthDescLib.global.at(name).metadata;

        if (metadata.notNil and: {metadata.defaults.notNil}, {
            // TODO
        })
    }

    // get currently mapped nodes
    mapped {
        nodeDict.keys.do{ |key|
            var nr = nodeDict[key]['volume'].asString.split($_).at(1);
            "% -> %\n".format(nr, key.cs).post;
        }
    }

    // connect SuperCollider MIDIOut to BCR MIDIIn (Linux only)
    // TODO: private use so refactor and move this to an extension
    connectJack {
        var os = thisProcess.platform.name;
        if (os == \linux,
            { "aconnect SuperCollider:5 BCR2000:0".unixCmd }, // FIXME hardcoded values
            { "BCR::connectJack is not supported on %".format(os).warn }
        )
    }

    managePreset { |key, node, pairs|
        "'managePreset' is not yet imlemented".warn
    }

    getCCNumForKey { |key|
        ^defaults[this.class][key].asString.drop(2).asInteger;
    }

    // for the 4 top encoder groups
    getGroupChar { |id|
        var chars = ["A", "B", "C", "D"];
        ^chars[((id - 1) / 8).asInt]
    }

    // check if the param has a valid ControlSpec.
    // if not, assign a default one to it [0, 127]
    checkParamSpec { |param|
        if (param.asSpec.isNil, {
            Spec.add(param, [0, 127]);
            ("BCR:\ndefault Spec for" + param).warn;
        })
    }

    // increment ccKeys (automatically change symbol if needed)
    // ex: \knE6, \knE7, \knE8, \knF1, \knF2, ...
    incrementCCNames { |size, offsetNr, offsetChar|
        ^size.collect { |i|
            var newKey, currentId;
            currentId = (offsetNr - 1 + i % 8) + 1;
            if (currentId == 1 and: { i > 0 }, {
                offsetChar = (offsetChar.ascii[0] + 1).asAscii
            });
            newKey = "kn%%".format(offsetChar.asString, currentId.asString).asSymbol
        }
    }

    *makeDefaults {
        defaults.put(this, BCR.getDefaults);
    }

    // store the CC numbers in 'defaults' Dictionary.
    *getDefaults {
        var dict     = ();
        var midi     = BCRSettings.midiChannel;
        var settings = BCRSettings.ccs;

        dict.putAll(settings);

        // append midi channel
        dict.keys.do{ |key|
            dict[key] = "%_%".format(midi, dict[key]).asSymbol
        };

        ^dict
    }
}


BCRError : Error {
    errorString {
        ^"BCR ERROR: " ++ what
    }
}
