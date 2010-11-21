+ UGen {

	// handy shortcuts
	pitchShift { |pitch=1, winsize=0.1|
		^PitchShift.multiNew(this.rate, this, winsize, pitch)
	}
	
	hpf { |freq|
		^HPF.multiNew(this.rate, this, freq)
	}
	
	lpf { |freq|
		^LPF.multiNew(this.rate, this, freq)
	}

}