BCR.sc | PocketDial.sc
----------------------

Utility classes to use 'Doepfer Pocket Dial' and 'Behringer BCR2000' within SuperCollider.

These are to be used with Ndef or ProxySpace.

### Install

Copy `BCR.sc`, `PocketDial.sc`, `YVMidiController.sc`, `BCRSettings.sc` (and their helpfiles) inside your user extensions folder:
`Platform.userExtensionDir`

### BCR

#### Usage

 - edit `BCRSettings.sc` and change midiChannel and CCs numbers according to your BCR settings

 - recompile SuperCollider library

NodeProxy are mapped to BCR top columns, which contain a knob and 2 buttons: [see picture](http://yvanvolochine.com/media/images/bcr_top.gif)
 - top knob is proxy volume
 - top knob *push* resets proxy params to their default values
 - first button below toggles on-off (play-stop)
 - second button below toggles editing proxy parameters
 - all params are then assigned to the main 24 knobs (starting at the first one if no offset is specified)

BCR will visually update its values when toggling edit button.
You can assign up to 8 proxies.

Video demo coming soon...

#### Example code

    p = ProxySpace.push(s.boot)

    ~sin = { |freq=440, amp=0.2| SinOsc.ar(freq, [0, 2pi], amp) }

    b = BCR("bcr") // name pattern for MIDI device

    b.mapTo(~sin, 2)

    // now the 2 first knobs from the main knobs area ae assigned to
    // ~sin freq and amp
    // alternatively, you can specify an offset to use different knobs:
    b.mapTo(~sin, 2, \knF1) // use first 2 knobs on the 2nd line
    // or
    b.mapTo(~sin, 2, \knE7) // use the last 2 knobs on 1st line

    // unmap a node
    b.unmap(~sin)

    // unmap all nodes
    b.unmapAll

    // cleanup
    b.free

### PocketDial

#### Usage

For endless mode only !
This assumes that your PocketDial uses CCs from 1 up to 63 (4 banks).
You can map up to 4 nodes, one on each banks.
By default, params are assigned starting from the first knob, unless you specify an offset.
The last knob (8th) controls the node volume.

#### Example code

    p = ProxySpace.push(s.boot)

    ~sin = { |freq=440, amp=0.2| SinOsc.ar(freq, [0, 2pi], amp) }

    d = PocketDial("usb") // name pattern for MIDI device

    // map ~sin parameters to 1st bank
    d.mapTo(~sin, 1)

    // now the first 2 knobs are assigned to ~sin freq and amp
    // alternatively you can specify an offset:
    d.mapTo(~sin, 1, 3)
    // knob 3 is freq, knob 4 is amp

    // you can change the knob velocity with stepmin and stepmax
    d.mapTo(~sin, 1, stepmax: 1.5)
    // try moving the knobs faster..

### TODO

 - help files
 - video demo for BCR
