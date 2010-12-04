/*
	Notes:
	- NEEDS JITMIDIKtl quark !
	- suports only endless mode (preset 00101010)
	History:
	- 20101204: 
	  - better handling of params update (no more defer)
	  - added ascii view of params (because NdefGui is broken with 
Qt and I *don't* wanna go back to heavy-on-my-poor-cpu SwingOSC)
	- 20101105:
	  - fixed delta ccVal bug and added args to mapTo()
	  - now remove unused (yvan volochine)
	  - still could be *much better*
	- 20101104 - started rewrite (yvan volochine)
	- 20101101 - mapTo() totally rewritten in hurry (yvan volochine)
	TODO:
	- make it better
	- refactor
	- rewrite non-endless part (probably no time for that =)
	- remove already used before re-assigning
	- ability to map several nodes to a bank
*/

PocketDialKtl : MIDIKtl {
	classvar <>verbose = false; 

	var <>softWithin = 0.05, <lastVals;	// for normal mode
	var <>endless;

	var <orderedNames;
	var <proxyDict, <proxyParamsDict, <resetDict, <orderedParamsDict;
	var <>inform = true;
	
	var lastTime;
	
	*new { |srcID, ccDict, endless = true| 
		^super.newCopyArgs(srcID, ccDict).endless_(endless).init;
	}

	init { 
		super.init;
		orderedNames = defaults[this.class].keys.asArray.sort;
		proxyParamsDict = ();
		resetDict = ();
		orderedParamsDict = ();
		lastTime = Main.elapsedTime;
	}

	free { 
		ccresp.remove;
		ccDict.clear;
		proxyParamsDict.clear;
		resetDict.clear;
		orderedParamsDict.clear;
	}

	update { |proxy, bank=nil, offset=nil|
		var pairs = proxy.getKeysValues;
		var dict = proxyParamsDict[proxy] ?? ();

		// store ordered params
		orderedParamsDict.add(proxy -> pairs.flop[0]);

		if (bank.notNil, { /* 1st update */
			pairs.do{|p, i|
				var cckey = orderedNames[bank-1 * 16 + offset + i - 1];
				// remove old key if any
				if (dict[p[0]].notNil, {
					this.mapCC(dict[p[0]][\key], nil);
					dict[p[0]][\key] = nil;
				});
				dict.add( p[0] -> (\key: cckey, \val: p[1]) );
			};
			proxyParamsDict[proxy] = dict;
		}, {
			/* we just fetch the new values */
			pairs.do{|p|
				proxyParamsDict[proxy][p[0]][\val] = p[1]
			}
		});
		resetDict[proxy] = true;
	}

	mapTo { |proxy, bank=1, offset=1, params=nil, stepmin=0.05, stepmax=0.5, mapVol=true|
		var pparams, pairs;
		pairs = proxy.getKeysValues;
		pparams = params ?? pairs.flop[0];

		if (pparams.size > 16, {
			warn("Too many params!\nMaximum is 16.")
		}, {
			if (proxyParamsDict[proxy].isNil or: { resetDict[proxy] != true }, {
				this.update(proxy, bank, offset)
			});
			pparams.do{ |p, i|
				var cc = orderedNames[bank-1 * 16 + offset + i - 1];
				var func;
				if (p.asSpec.isNil, { 
					warn("% doesn't have a Spec !\n% not mapped.\n".format(p, p)) 
				}, {
					this.mapCC(cc, this.generateFunction(proxy, p, stepmin, stepmax));
					if (inform, { postf("mapping % on % - bank %\n", p, cc, bank) });
				});
			};
			if (mapVol == true, {
				this.mapVolume(proxy, 16, bank);
			});
		});
	}

	generateFunction { |proxy, param, stepmin, stepmax|
		var func = { |val|
			var delta, currentVal, newVal, time;
			time = Main.elapsedTime;
			delta = val - 64;
			delta = delta * delta.abs.linlin(1, 7, stepmin, stepmax);

			/* if no cc was moved for 1 second, update NodeProxy params in case they have been changed from the outside */
			if (time - lastTime > 1, {
				resetDict.add(proxy -> false);
			});

			// update params ?
			if (resetDict[proxy] != true, {
				this.update(proxy)
			});
			currentVal = proxyParamsDict[proxy][param][\val];
			currentVal = param.asSpec.unmap(currentVal);
			newVal = param.asSpec.map(currentVal + (delta / 127));
			
			try { proxy.set(param, newVal) };
			proxyParamsDict[proxy][param][\val] = newVal;

			/*if (inform, { 
				postf("%: % -> %\n", proxy.asCompileString, param, newVal.round(1e-4))
				});*/
			if (inform, { this.asciiParams(proxy) });

			lastTime = time;
		};
		^func;
	}

	mapVolume { |proxy, ccnr, bank=1|
		var cc = orderedNames[bank-1 * 16 + ccnr - 1 ];
		this.mapCC(cc, { |val|
			var delta, volume;
			delta = val - 64;
			delta = delta * delta.abs.linlin(1, 7, 0.05, 0.5); // bigger default step for vol
			volume = \amp.asSpec.unmap(proxy.vol);
			volume = \amp.asSpec.map(volume + (delta / 127));
			proxy.vol_(volume);
			if (inform, { postf("% vol -> %\n", proxy.asCompileString, volume.round(1e-4)) });
		});
	}

	asciiParams { |proxy|
		// returns keys-params in an ascii way (no gui)
		("[ " ++ proxy.asCompileString ++ " ]\n").post;

		orderedParamsDict[proxy].do{ |key|
			var val = proxyParamsDict[proxy][key]['val'];
			val = key.asSpec.unmap(val);
			key = key.asString;
			key = key ++ (" " ! (16 - (key.size+1)));
			key = key.replace("[", "").replace("]", "").replace(", ", "");
			val = (val * 20).round;
			val = ("#"!val);
			val = val ++ ("-"!(20 - (val.size-1)));
			val = val.asString.replace(" ", "").replace(",", "").replace("]", "").replace("[", "");
			(key ++ "[" ++ val ++ "]" ++ "\n").post;
			"";
		};
		"\n\n\n\n".post;
		"";
	}


	*makeDefaults { 
		/*	all midi chan 1, 
		bank 1:  0 - 15, bank 2: 16 - 31, bank 3: 32 - 47, bank 4: 48 - 63
		*/

		// just one bank of knobs
		defaults.put(this, (
			kn01: '1_0',  kn02: '1_1',  kn03: '1_2',  kn04: '1_3',  kn05: '1_4',  kn06: '1_5',  kn07: '1_6',  kn08: '1_7',
			kn09: '1_8',  kn10: '1_9',  kn11: '1_10', kn12: '1_11', kn13: '1_12', kn14: '1_13', kn15: '1_14', kn16: '1_15',
			kn17: '1_16', kn18: '1_17', kn19: '1_18', kn20: '1_19', kn21: '1_20', kn22: '1_21', kn23: '1_22', kn24: '1_23', 
			kn25: '1_24', kn26: '1_25', kn27: '1_26', kn28: '1_27', kn29: '1_28', kn30: '1_29', kn31: '1_30', kn32: '1_31',
			kn33: '1_32', kn34: '1_33', kn35: '1_34', kn36: '1_35', kn37: '1_36', kn38: '1_37', kn39: '1_38', kn40: '1_39', 
			kn41: '1_40', kn42: '1_41', kn43: '1_42', kn44: '1_43', kn45: '1_44', kn46: '1_45', kn47: '1_46', kn48: '1_47',
			kn49: '1_48', kn50: '1_49', kn51: '1_50', kn52: '1_51', kn53: '1_52', kn54: '1_53', kn55: '1_54', kn56: '1_55', 
			kn57: '1_56', kn58: '1_57', kn59: '1_58', kn60: '1_59', kn61: '1_60', kn62: '1_61', kn63: '1_62', kn64: '1_63'
		));
	}

}
