+ UGen {

	// handy shortcuts
	pitchShift { |pitch=1, winsize=0.1|
		^PitchShift.perform(PitchShift.methodSelectorForRate(this.rate), this, winsize, pitch)
	}
	
	hpf { |freq|
		^HPF.perform(HPF.methodSelectorForRate(this.rate), this, freq)
	}
	
	lpf { |freq|
		^LPF.perform(LPF.methodSelectorForRate(this.rate), this, freq)
	}

}

+ Array {

	pitchShift { |pitch=1, winsize=0.1|
		^PitchShift.perform(PitchShift.methodSelectorForRate(this.rate), this, winsize, pitch)
	}
	
	hpf { |freq|
		^HPF.perform(HPF.methodSelectorForRate(this.rate), this, freq)
	}
	
	lpf { |freq|
		^LPF.perform(LPF.methodSelectorForRate(this.rate), this, freq)
	}

}