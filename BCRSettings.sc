/**
 * Edit this file and change 'midiChannel' and all ccs numbers according to
 * your BCR2000 preset. Then recompile SuperCollider library and you're done
 */

BCRSettings {

    // MIDI channel
    classvar <midiChannel = 0;

    *ccs {
        ^(
            // top knobs (push mode) for resetting params
            \trA1: 57,
            \trA2: 58,
            \trA3: 59,
            \trA4: 60,
            \trA5: 61,
            \trA6: 62,
            \trA7: 63,
            \trA8: 64,

            // top knobs (rotary mode) for volumes
            \knA1: 1,
            \knA2: 2,
            \knA3: 3,
            \knA4: 4,
            \knA5: 5,
            \knA6: 6,
            \knA7: 7,
            \knA8: 8,

            // top row buttons for on-off
            \btA1: 89,
            \btA2: 90,
            \btA3: 91,
            \btA4: 92,
            \btA5: 93,
            \btA6: 94,
            \btA7: 95,
            \btA8: 96,

            // bottom row buttons for edit mode
            \btB1: 97,
            \btB2: 98,
            \btB3: 99,
            \btB4: 100,
            \btB5: 101,
            \btB6: 102,
            \btB7: 103,
            \btB8: 104,

            // main buttons for parameters
            \knE1: 33,
            \knE2: 34,
            \knE3: 35,
            \knE4: 36,
            \knE5: 37,
            \knE6: 38,
            \knE7: 39,
            \knE8: 40,
            \knF1: 41,
            \knF2: 42,
            \knF3: 43,
            \knF4: 44,
            \knF5: 45,
            \knF6: 46,
            \knF7: 47,
            \knF8: 48,
            \knG1: 49,
            \knG2: 50,
            \knG3: 51,
            \knG4: 52,
            \knG5: 53,
            \knG6: 54,
            \knG7: 55,
            \knG8: 56,

            // buttons below preset buttons
            \prA1: 105,
            \prA2: 106,
            \prB1: 107,
            \prB2: 108
        )
    }
}
