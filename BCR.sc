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
        super.init();
        this.findMidiIn(srcName);
        this.findMidiOut(destName)
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
            MIDIClient.destinations.do{ |x|
                if ( x.device.containsi(destName), {
                    destID = x.uid;
                    ("BCR MIDIOut:" + x.device).postln; "";
                    break.();
                })
            }
        }
    }

    /**
     * addAction
     * Add a function to a specific CC
     *
     * @param Symbol   ctlKey '0_33'
     * @param Function action The function to be executed
     * @return void
     */
    addAction{ |ctlKey, action|
        ktlDict.add( ctlKey -> action );
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
     * @return void
     */
    mapToNodeParams { |node ... pairs|
        pairs.do { |pair|
            var ctlName, paramName, specName;
            #ctlName, paramName = pair;
            this.checkParamSpec;
            this.addAction(
                ctlName,
                { |ch, cc, midival|
                    node.set(paramName, paramName.asSpec.map(midival / 127));
                }
            )
        };
        if (midiOut.notNil) {
            this.sendFromProxy(node, pairs);
        };
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
        var ccKey      = (selector ++ id).asSymbol.postln;
        var ccSelector = this.getCCNumForKey(ccKey);
        var offsetNr   = offset.asString.drop(3).asInteger;
        var offsetChar = offset.asString.drop(2).at(0).asString;
        var ccNewNames = this.incrementCCNames(nodeParams.size, offsetNr, offsetChar);
        var ctlKeyName = defaults[this][ccKey].postln;
        var pairs      = [ccNewNames, nodeParams].flop;
        var func       = { |chan, num, val|
            var theKey = defaults[this].findKeyForValue(("0_"++num).asSymbol); // presets
            if (num == ccSelector and: { val > 0 }, {
                pairs.do{ |pair| this.mapToNodeParams(node, pair) };
                this.managePreset(theKey, node, pairs);
            });
            if ( num == ccSelector and: { val == 0 }, {
                this.managePreset(nil);
            })
        };
        ktlDict.put(ctlKeyName, func);
        // FIXME: TODO
        //if ( preset.notNil, { presets.put(ccKey, preset) });
        //this.assignVolume(node, ccId);
        //this.assignToogle(node, ccId);
        //this.assignReset(node, ccId, pairs, defaultparams);
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
        ^defaults[this][key].asString.drop(2).asInteger;
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
        defaults.put(this, BCR.getDefaults);
    }
}
