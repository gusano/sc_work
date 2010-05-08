// =========
// sc workspace
// =========

p = ProxySpace.push(s.boot)
p.clear
// NOISE DAY TODAY !
~a = { (HPF.ar(LFNoise1.ar(120!2), ~a.ar + [10, 10.1]) * 120).tanh}
b = BCR.new()
//~a.play
b.mapTo(~a, 1)

~a = { (HPF.ar(FSinOsc.ar(18), LFNoise1.ar(4!2) * ~a.ar) * 120).tanh}
~a = { |freq=3000| (HPF.ar(FSinOsc.ar(180 * ~a.kr.abs + freq.lag(0.05)), LFNoise1.ar(4!2) * ~a.kr) * 2).tanh} 
~a = { |freq=300| (HPF.ar(FSinOsc.ar(180 * ~a.kr.abs + freq.lag(0.05)), LFNoise1.ar(4!2, freq) * ~a.kr) * 2).tanh} // trigger often ?
~a = { |freq=300| (HPF.ar(FSinOsc.ar(180 * ~a.kr.abs + freq.lag(0.1)), LFNoise1.ar(4!2, 22000 - freq) * ~a.kr) * 2).tanh} // audioactivity ??
p.reduce
// florian (2)
~a = { HPF.ar(LFNoise2.ar(999).range(55, 555)).tanh}
~del = { YVDel.ar(~a.ar)}
~del.play
~a = { HPF.ar(LFNoise2.ar(999!2).range(55, 555)).tanh + DelayL.ar(~a.ar, 1.0, LFNoise2.kr(LFNoise2.kr(1!2, 0.4, 3).abs, 5).range(0.65, 1).tanh)}
m = ProxyMixer.new(p)
// experiemnsst
~a = { HPF.ar(LFNoise2.ar(2399!2).range(10, 555)).tanh + AllpassL.ar(~a.ar, 0.3, LFNoise2.kr(1!2).range(0.065, 0.299))}


JSCTabbedPane

m = ProxyMixer.new(p)

// TIP !!!!!!!
p.reduce // ProxySpace
// this ends all not needed proxies (not playing or not used as source)
// THANKS Alberto :D