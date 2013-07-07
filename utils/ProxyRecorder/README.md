# ProxyRecorder

Record each node separately in a ProxySpace.

## Example

```javascript
p = ProxySpace.push(s.boot);

~sin = { SinOsc.ar(SinOsc.kr(12!2, 0, 800, 400)) };

~fx = { PitchShift.ar(~sin.ar, 0.5, LFNoise2.kr(1!2, 0.3, 1)) };

~sin.play;
~fx.play;

r = ProxyRecorder(p);

// add the nodes you wish to record
r.add([~sin, ~fx]);

r.record;
r.stop;
```