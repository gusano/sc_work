// TODO:
// - add a decay to the vu-s (Amplitude.kr's release param?) -> thx Blackrain
// - separate GUI from this code

YVBusMeter {
	var <responder, <meterSynth, <meterGui, s, w;
	var <name, <port, <coord;

	*new { |win, busNr, coordinates|
		if (win.notNil and: {busNr.notNil}, {
			^super.new.init(win, busNr, coordinates)
		}, {
			Error(" args").throw
		})
	}

	free {
		try{
			responder.remove;
			meterSynth.free;
		}
	}

	init { |win, busNr, coordinates|
		s     = Server.default;
		coord = coordinates ?? [10, 10, 160, 10];
		w     = win;
		name  = ("vu_" ++ Date.localtime.stamp).asSymbol;
		port  = busNr;
		this.addSynth;
		this.drawMeter;
		this.addOscResponder;
		CmdPeriod.doOnce{ this.free };
	}

	addSynth {
		s.waitForBoot {
			SynthDef(\forMeter, {
				var imp, delimp, snd, id;
				snd    = In.ar(port, 1);
				imp    = Impulse.kr(10);
				delimp = Delay1.kr(imp);
				// measure rms and Peak
				SendReply.kr(
					imp,
					name,
					[Amplitude.kr(snd), K2A.ar(Peak.ar(snd, delimp).lag(0, 3))]
				)
			}).send(s);
			s.sync;
			meterSynth = Synth.new(\forMeter, nil, s, \addToTail);
		}
	}

	drawMeter {
		if( GUI.id == \swing, {
			meterGui = JSCPeakMeter(w, Rect(*coord))
		}, {
			meterGui = LevelIndicator(w, Rect(*coord))
		});
	}

	addOscResponder {
		responder = OSCresponder(s.addr, name, { |time, resp, msg|
			{
				if(meterGui.notNil, {
					meterGui.value_(msg[3].ampdb.linlin(-40, 0, 0, 1))
					    .peakLevel_(msg[4].ampdb.linlin(-40, 0, 0, 1))
					    .warning_(0.75)
					    .critical_(0.95)
					    //.drawsPeak_(true)
				})
			}.defer; // FIXME
		}).add;
		w.onClose_({ this.free })
	}
}