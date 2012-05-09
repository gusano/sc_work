TubeDist {
	// http://new-supercollider-mailing-lists-forums-use-these.2681727.n2.nabble.com/tube-simulation-in-sc-td5901036.html
	*ar { |input, tube = 15, limit = 0.9|
		var outSound;
		// e.g. try tube in range 1-50
		outSound = ((input*tube).exp - (input* tube * -1.2).exp);
		outSound = outSound / ((input*tube).exp + (input * tube * -1.0).exp);
		outSound = Limiter.ar(outSound / tube, limit, 0.01);
		^outSound
	}
}