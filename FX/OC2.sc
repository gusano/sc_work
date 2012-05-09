OC2 {
	// an emulation of the Boss OC-2 pedal
	// Synth by Sean Costello - Class by Yvan Volochine
	
	*ar { |input, freq = 400, oct1 = 1.0, oct2 = 0.0, direct = 1.0|
		var in, a, b, c;
		
		in = LeakDC.ar(input, 0.999);
		a = LPF.ar(in, 400); // lowpass filter input - experiment with cutoff freq
		b = ToggleFF.ar(a); // use flip-flop to generate square wave
		                               //an octave below input
		c = ToggleFF.ar(b); // square wave two octaves below input
		
		// the lowpass filtered signal is multiplied by the suboctave
		// square wave, which results in a signal an octave (or two octaves)
		// below the input signal, but with the dynamics of the input signal,
		// and a nice, semi-sinusoidal tone
		// oct1, oct2 and direct control the mix of the various suboctaves and
		// input signal
		^(a * b * oct1) + (a * c * oct2) + (in * direct) 
	}
}

//	If you want to emulate the old-school octave up, use abs(in). Most of the
//	octave-up pedals, like the Octavia, Foxx Tone Machine, Univox Superfuzz, and
//	their clones, used a full-wave rectifier on the input signal to produce a
//	signal with predominantly even harmonics of the input frequencies. Full-wave
//	recification in analog is well approximated by the absolute value function
//	in digital. From what I have read of the OT-10, the "Effect" knob would be
//	the equivalent of the a*b*oct1 signal in the above patch, while "Edge" would
//	simply be abs(in).
//	
//	Hope this is useful.
//	
//	Sean Costello
