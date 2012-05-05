/**
 * @file    YVMidiController.sc
 * @desc    Abstract class for using MIDI controllers with SuperCollider.
 * @author  Yvan Volochine <yvan.volochine@gmail.com>
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 * @version 0.1
 * @since   2012-05-05
 * @link    http://github.com/gusano/sc_work/tree/master/MIDI
 */

YVMidiController {

    classvar <defaults;
    classvar <>verbose = false;


    var <srcID;   // index of MIDIIN device used
    var <destID;  // index of MIDIOUT device used
    var <midiOut; // MIDIOut
    var <ctlDict; // dictionnary which contains all mapped controls/functions
    var <resp;    // main CCResponder


    *initClass {
        defaults = ();
        this.allSubclasses.do(_.makeDefaults);
    }

    *makeDefaults {
        // subclasses have to override this method.
        // they put their controller keys and chan/ccnum combinations into
        // defaults[class]
        defaults.put(this.class, ());
    }

    init {
        ctlDict = ctlDict ?? ();

        if( destID.notNil ) {
            midiOut = MIDIOut(destID);
            defaults[this.class].pairsDo{ |key|
                var chanCtl = this.keyToChanCtl(key);
                midiOut.control(chanCtl[0], chanCtl[1], 0)
            };
        }
    }

    // format MIDI chan|cc
    makeCCKey { |chan, cc| ^(chan.asString ++ "_" ++ cc).asSymbol }

}
