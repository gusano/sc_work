// Glisson synthesis
// Curtis Roads, Microsound, p121
// first experiments, being curious because of this post on the sc list:
// http://bit.ly/eyMgyL

(
s.waitForBoot{
	var t, w, labels, ranges=(), actions, specs, inits, playButton;

	SynthDef(\glisson, { |out=0, freq_s=222, freq_e=222, amp=0.82, dur=0.1, pan=0|
		var env, freq, snd;
		// try other enveloppes
		env  = EnvGen.kr(Env.sine(dur), doneAction: 2);
		freq = Line.kr(freq_s, freq_e, dur * 0.98);
		snd  = SinOsc.ar(freq, 0, amp);
		snd  = Pan2.ar(snd * env, pan);
		Out.ar(out, snd * 0.8);
	}).add;

	// GUI code below...

	labels = ['wait', 'duration', 'freq_start', 'freq_end', 'pan'];

	// EZRanger actions
	actions = (
		'wait'      : { |v| ~wait_min = v[0]; ~wait_max = v[1] },
		'duration'  : { |v| ~dur_min = v[0]; ~dur_max = v[1] },
		'freq_start': { |v| ~freq_s_min = v[0]; ~freq_s_max =  v[1] },
		'freq_end'  : { |v| ~freq_e_min = v[0]; ~freq_e_max = v[1] },
		'pan'       : { |v| ~pan_min = v[0]; ~pan_max = v[1] }
	);
	specs = (
		'wait'      : [0.001, 0.5, \lin, 0.001],
		'duration'  : [0.001, 0.5, \lin, 0.001],
		'freq_start': [20, 2e4, \exp, 1],
		'freq_end'  : [20, 2e4, \exp, 1],
		'pan'       : 'pan'
	);
	inits = (
		'wait'      : [0.05, 0.06],
		'duration'  : [0.05, 0.06],
		'freq_start': [200, 300],
		'freq_end'  : [250, 350],
		'pan'       : [-1, 1]
	);

	w = Window.new("Glisson synthesis", Rect(100, 100, 410, 150))
		.background_(Color.new(0.95, 0.96, 0.23))
		.onClose_{ t.stop; t.reset; }
		.front;
	w.addFlowLayout;

	playButton = Button(w, 120@20)
		.states_([
			['start', Color.white, Color.new(0, 0.6, 0)],
			['stop', Color.white, Color.red]
		]).action_{ |b|
			if (b.value == 1, {
				t.play(SystemClock)
			}, {
				t.stop; t.reset;
			})
		}.canFocus_(false);

	// rangeSliders
	labels.do{ |txt|
		ranges[txt] = EZRanger(w, 400@20, txt.asString, specs[txt], {|sl| actions[txt].(sl.value)}, inits[txt], true, numberWidth: 50);
		// this one is not is default sc
		if (EZSlider.findMethod('canFocus_').isNil.not, {
			ranges[txt].canFocus_(false);
		})
	};

	// task
	t = Task({
		inf.do{
			Synth(\glisson, [
				\freq_s, rrand(~freq_s_min, ~freq_s_max),
				\freq_e, rrand(~freq_e_min, ~freq_e_max),
				\dur,    rrand(~dur_min, ~dur_max),
				\pan,    rrand(~pan_min, ~pan_max)
			]);
			rrand(~wait_min, ~wait_max).max(0.001).wait;
		}
	});
}
);

s.meter
