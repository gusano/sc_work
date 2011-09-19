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

    classvar <>verbose = false;

    /**
     * *new
     *
     * @param string srcName  Name pattern for MIDI source
     * @param string destName Name pattern for MIDI destination
     *
     * @return BCR
     */
    *new { |srcName, destName|
        ^super.new.init(srcName, destName);
    }

    /**
     * init
     * Prepare MIDI in/out.
     * If no name is given for the destination, we assume it's the same as
     * the one given for source. If 'nil' is given, don't use MIDIOut.
     *
     * @param string srcName  Name pattern for MIDI source
     * @param string destName Name pattern for MIDI destination
     *
     * @return BCR
     */
    init { |srcName, destName = ""|
        this.checkDependency();
        super.init();
        this.findMidiIn(srcName);
        if (destName == "", { destName = srcName });
        if (destName.notNil, { this.findMidiOut(destName) });
        // TODO rewrite this
        /*
        if( destID.notNil ) {
            midiOut = MIDIOut(destID);
            ktlNames.pairsDo{ |key|
                var chanCtl = this.keyToChanCtl(key);
                midiOut.control(chanCtl[0], chanCtl[1], 0)
            }
        }
        */
    }

    checkDependency {
        if ('Ktl'.asClass.isNil, {
            Error("Required 'Ktl' quark was not found.").throw
        })
    }

    /**
     * findMidiIn
     * Finds the MIDIIn device via name pattern. If several sources contains
     * this name, only the first one is used.
     *
     * @param string srcName  Name pattern for MIDI source
     *
     * @return void
     */
    findMidiIn { |srcName|
        MIDIClient.sources.do{ |x|
            if (x.device.contains(srcName), {
                srcID = x.uid;
                ("BCR MIDI input:" + x.device).postln; "";
            })
        };
    }

    /**
     * findMidiOut
     * Finds the MIDIOut device via name pattern. If several sources contains
     * this name, only the first one is used.
     *
     * @param string destName  Name pattern for MIDI destination
     *
     * @return void
     */
    findMidiOut { |destName|
        "'findMidiOut' is not yet implemented".postln;
    }

    /**
     * *getDefaults
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
        defaults.put(this, this.getDefaults);
    }
}
