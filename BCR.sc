/*
	with help from adc
	20101101 - just for VC (Yvan Volochine)
	needs a big REWRITE ! but works fine =)
	TODO:
	- write help file
	USAGE:
	// FIRST replace with your cc numbers in BCR::makeDefaults (at the end)
	p = ProxySpace.push(s.boot)
	b = BCR.new
	~sin = {SinOsc.ar(\freq.kr(222), 0, \amp.kr(0.1))}
	~sin.play
	b.mapTo(~sin, 1)
	// now trigger ~sin.play/stop with #1 top left square button on the BCR
	// ~sin volume is the #1 top left round knob
	// #1 square trigger on the second row is "map mode".. turn it on and
	// ~sin params are assigned by default on first main controller knobs
	// offset, preset mapping, etc..
*/


YVMIDIController { 
	var <deviceName, <srcID, <ccDict, <noteOnDict, <noteOffDict, <ccresp, <ccStoreDict;
	classvar <autoMapDict; // Store bt if in automap mode (gusano)

	*new { |deviceName, ccDict| 
		^super.newCopyArgs(deviceName, ccDict).init(deviceName, ccDict);
	}
	
	free { 
		ccresp.remove;
		
		ccDict.clear;
		autoMapDict.clear;
		noteOnDict.clear;
		noteOffDict.clear;
		// redraw pxmix and pxedit with clear colors...
	}

	init { 
		ccDict = ccDict ?? ();
		noteOnDict = noteOnDict ?? ();
		noteOffDict = noteOffDict ?? ();
		ccStoreDict = ccStoreDict ?? (); // gusano
		
		ccresp.remove; 
		ccresp = CCResponder({ |src, chan, ccn, val| 
			var lookie = this.makeCCKey(chan, ccn);
			if (this.class.verbose, { ['cc', src, chan, ccn, val].postcs });
			ccStoreDict.put(ccn, val); // store all moved ccs (gusano)
			ccDict[lookie].value(chan, ccn, val);
		}, srcID);
	}

	makeCCKey { |chan, cc| ^(chan.asString ++ "_" ++ cc).asSymbol }
	
	ccKeyToChanCtl { |ccKey| ^ccKey.asString.split($_).asInteger }

	makeNoteKey { |chan, note| 
		var key = chan.asString;
		if (note.notNil) { key = key ++ "_" ++ note };
		^key.asSymbol 
	}
}



/*
@TODO: 
- merge to BCRKtl
- unmap, erase all
- check free()
- use PxPreset instead of Preset
@LIST OF CHANGES:
- fixed bug when auto-incrementing CC key - 20100521
- fixed offset (autoMapToPx) - 20100425
- fixed default params values (for reset) - 20100415
- added morph between 2 consecutive presets - 20100102
- fixed update from BCR when changing/recalling presets - 20091231
- added preset management (needs adc PxPreset + my Preset.sc) - 20091230
- added proxy.send for volume button in trigger mode (resetMode) - 20091203
- cleaned - 20091130
- added auto ControlSpec for non spec params - 20090716
- added MIDIId as class argument - 20090717
*/
BCR : YVMIDIController { 
	classvar <>verbose = false, <defaultCtlNames; 
	classvar <>midiOut;
	var <deviceName;
	var autoMapDict;
	var <>autoMapMode = false;
	var <>dumpMode = true;
	var <>soloMode = true;
	var <>presets; // for Preset.sc

	*initClass { 
		this.makeDefaults;
	}
	
	findMIDIIn {
		MIDIClient.sources.do{ |x|
			if ( x.device.contains(deviceName), {
				srcID = x.uid;
				("***\nMIDIIN uses ->" + x.device).postln;
				^srcID
			})
		}
	}
	
	connectJack {
		// find a better way (more "general", i.e. 4 is maybe not always 4... or maybe it is?)
		"aconnect SuperCollider:4 BCR2000:0".unixCmd;
	}
	
	initMIDIOut {
		var index, uid;
		MIDIClient.destinations.do{ |x, i|
			if ( x.device.contains(deviceName), {
				("MIDIOUT uses ->" + x.device + "\n***").inform;
				index = i;
				uid = x.uid;
			});
		};
		if ( uid.notNil, { 
			midiOut = MIDIOut(index, uid) 
		}, { "no midi out".warn });
	}
	
	init { | aDeviceName, ccDict | // see MIDIController.new(...)
		deviceName = aDeviceName ? "BCR2000";
		("looking for" + deviceName + "...").postln;
		super.init;
		// ccresp.function = { .. }; // idea from adc, not implemented yet
		srcID = this.findMIDIIn;
		this.initMIDIOut;
		// linux midi
		Platform.case(
			\linux, { this.connectJack }
		);
		autoMapDict = ();
		presets = ();
		Spec.add(\default, [0, 127]); // for non ControlSpec params
		^this
	}

	mapCC { | ctl= \sl1, action |
		var ccDictKey = defaultCtlNames[ctl]; // '0_42'
		ccDict.put(ccDictKey, action);
	}
		
	mapToPxPars { | proxy ... pairs |
		pairs.do { |pair| 
			var ctlName, paramName, specName;
			#ctlName, paramName = pair;
			if (Spec.specs[paramName].isMemberOf(ControlSpec).not) {
				Spec.add(paramName, [0, 127]);
				("default Spec for ->" + paramName).warn;
			};
			this.mapCC(ctlName, 
				{  |ch, cc, midival| 
					if (ccStoreDict[autoMapDict[proxy]] > 0) {
						proxy.set(paramName, paramName.asSpec.map(midival / 127));
						//proxy.histSet(paramName, paramName.asSpec.map(midival / 127));
						if (dumpMode == true, {
							[proxy, paramName, paramName.asSpec.map(midival / 127)].postcs
						})
					}
				}
			)
		};
		if (midiOut.notNil) { 
			this.sendFromProxy(proxy, pairs);
		};
	}

	sendFromProxy { | proxy, pairs |
		var ctlNames, paramNames, currVals, midivals;
		#ctlNames, paramNames = pairs.flop;
		currVals = proxy.getKeysValues(paramNames).flop[1];
		midivals = currVals.collect { |currval, i|
			(paramNames[i].asSpec.unmap(currval) * 127).round.asInteger;
		};
		[ctlNames, midivals].flop.do { |pair|
			this.sendCtlValue(*pair);
		}	
	}
	
	sendCtlValue { | ctlName, midival | 
		var chanCtl = this.ccKeyToChanCtl(defaultCtlNames[ctlName]);
		midiOut.control(chanCtl[0], chanCtl[1], midival);
	}
	
	// map volume to a top knob	
	assignVolume { | proxy, id |

		var thisKnob = ("knA"++id).asSymbol;
		("changing volume on " ++ proxy.asCompileString ++ " with " ++ thisKnob).asString.postln;
		this.mapCC(thisKnob, 
			{ |ch, cc, val| 
				try {
					proxy.vol_( \amp.asSpec.map(val / 127), 0.05)
					//proxy.histVol_( \amp.asSpec.map(val / 127), 0.05)
				}
			}
		)
	}
	
	assignToogle { | proxy, id |
		var thisToogle = ("btA"++id).asSymbol;
		// upper buttons: toggle play/stop 
		this.mapCC(thisToogle, 
			{ |ch, cc, val| defer { 
				if (val > 0) { 
					try {
						if (proxy.monitor.isPlaying.not) { 
							proxy.play
							//proxy.histPlay
						}
					}
				} {
					try {
						if (proxy.monitor.isPlaying) {
							proxy.stop
							//proxy.histStop
						}
					}
				}
			}
		})
	}
	
	assignReset { | proxy, id, pairs, defaultparams |
		var thisToggle = ("tr" ++ id).asSymbol; // volume button in toggle mode

		this.mapCC(thisToggle, { |ch, cc, val|
			if (val > 0, {
				try { 
					// in case there are no default values in the Spec, we use default NodeProxy params values
					defaultparams.do { |def|
						proxy.set(def[0], def[1]);
						//proxy.histSet(x, y)
					};
					this.sendFromProxy(proxy, pairs) // actualize BCR2000
				}
			})
		})
	}

	// activate presets
	managePreset { | ctlkeyname, proxy, pairs |
		var prst, resetBCR;

		if ( proxy.notNil, {
			resetBCR = { this.sendFromProxy(proxy, pairs) }
		});
		// ccStoreDict ??
		prst = presets[ctlkeyname];
		if ( prst.notNil, {
			this.mapCC(\prA1, { |chan, num, val| if ( val > 0, { prst.recall(prst.currentid); resetBCR.value; })});
			this.mapCC(\prB1, { |chan, num, val| if ( val > 0, { prst.previous; resetBCR.value; })});
			this.mapCC(\prB2, { |chan, num, val| if ( val > 0, { prst.next; resetBCR.value; })});
			this.mapCC(\prA2, { |chan, num, val|
				var name1, name2;
				var currentkey, nextid, ordered;
				
				ordered = prst.presets.keys.asArray.sort;
				currentkey = ordered.indexOf(prst.currentid);
				nextid = ordered.wrapAt(currentkey + 1);

				name1 = prst.presets[prst.currentid];
				name2 = prst.presets[nextid];
				if ( val > 0, {
					this.mapCC(\knD8, { |chan, num, val| prst.morph(val / 127, name1, name2); defer { resetBCR.value }});
					
				}, {
					this.mapCC(\knD8, { |chan, num, val| })
				})
			});
		}, {
			[\prA1, \prA2, \prB1, \prB2].do{ |cc| this.mapCC(cc, { |chan, num, val| if ( val > 0, { "no presets here".postln })})};
		});
	}

	// shortcut
	mapTo { | proxy, ccId, offset, preset |
		this.autoMapToPx(proxy, ccId, offset, preset)
	}
	
	// increment and change letter. ex: \knB7, \knB8, \knC1, etc...
	prAutoIncrementCCNames { | params, offsetNr, offsetChar |
		^params.size.collect { | i |
			var newKey, currentId;
			currentId = (offsetNr - 1 + i % 8) + 1; // from 1 to 8
			if (currentId == 1 and: { i > 0 }, { // we reach the beginning of the new row, increment letter
				//newKey = (offsetChar.ascii[0] + 1).asAscii
				offsetChar = (offsetChar.ascii[0] + 1).asAscii
			}/*, { 
				newKey = offsetChar
			}*/);
			//newKey = ("kn" ++ newKey.asString ++ currentId.asString).asSymbol
			newKey = ("kn" ++ offsetChar.asString ++ currentId.asString).asSymbol
		}
	}

	autoMapToPx { | proxy, ccId, offset, preset |
		var defaultparams = proxy.getKeysValues; // if autoMapToPx is called after initialisation, defaults have changed... 
		var params = defaultparams.flop[0];
		var ccSelector = defaultCtlNames[("btB"++ccId).asSymbol].asString.drop(2).asInteger;
		var ccStartName = offset ? \knB1;
		var offsetNr = ccStartName.asString.drop(3).asInteger; // ex: \knB4 -> 4
		var offsetChar = ccStartName.asString.drop(2).at(0).asString; // ex: \knB4 -> B
		var ccNewNames = this.prAutoIncrementCCNames(params, offsetNr, offsetChar);
		var pairs = [ccNewNames, params].flop.postln;
		var func;
		var ctlKeyName = defaultCtlNames[("btB" ++ ccId).asSymbol];
		// store preset if any
		if ( preset.notNil, {
			presets.put(("btB"++ccId).asSymbol, preset);
		});
		autoMapMode = true;
		//autoMapDict = autoMapDict ?? (); // can't remember right now what this is for...
		autoMapDict.put(proxy, ccSelector);

		func = { |chan, num, val|
			if (num == ccSelector and: { val > 0 }, {
				pairs.do{ |pair|
					this.mapToPxPars(proxy, pair)
				};
				this.managePreset(defaultCtlNames.findKeyForValue(("0_"++num).asSymbol), proxy, pairs);
			});
			if ( num == ccSelector and: { val == 0 }, {
				this.managePreset(nil);
			})
		};
		ccDict.put(ctlKeyName, func);

		this.assignVolume(proxy, ccId);
		this.assignToogle(proxy, ccId);
		this.assignReset(proxy, ccId, pairs, defaultparams);
		//autoMapMode = false; // is that necessary ?
	}

	unmap { | proxy |
		// todo...
	}
	
	free {
		autoMapDict = nil;
		presets = nil;
		super.free;
	}
	
	*makeDefaults { 
		// lookup for all scenes and ctlNames, \sl1, \kn1, \bu1, \bd1, 
		defaultCtlNames = (
			// knobs
			knA1: '0_1', 
			knA2: '0_2', 
			knA3: '0_3', 
			knA4: '0_4', 
			knA5: '0_5', 
			knA6: '0_6', 
			knA7: '0_7', 
			knA8: '0_8', 

			knB1: '0_33', 
			knB2: '0_34', 
			knB3: '0_35', 
			knB4: '0_36', 
			knB5: '0_37', 
			knB6: '0_38', 
			knB7: '0_39', 
			knB8: '0_40',
			
			knC1: '0_41', 
			knC2: '0_42', 
			knC3: '0_43', 
			knC4: '0_44', 
			knC5: '0_45', 
			knC6: '0_46', 
			knC7: '0_47', 
			knC8: '0_48', 
			
			knD1: '0_49', 
			knD2: '0_50', 
			knD3: '0_51', 
			knD4: '0_52', 
			knD5: '0_53', 
			knD6: '0_54', 
			knD7: '0_55', 
			knD8: '0_56',
			// top knob push mode
			tr1: '0_57',
			tr2: '0_58',
			tr3: '0_59',
			tr4: '0_60',
			tr5: '0_61',
			tr6: '0_62',
			tr7: '0_63',
			tr8: '0_64',
			// buttons 1st row
			btA1: '0_89',
			btA2: '0_90',
			btA3: '0_91',
			btA4: '0_92',
			btA5: '0_93',
			btA6: '0_94',
			btA7: '0_95',
			btA8: '0_96',
			// buttons 2nd row
			btB1: '0_97',
			btB2: '0_98',
			btB3: '0_99',
			btB4: '0_100',
			btB5: '0_101',
			btB6: '0_102',
			btB7: '0_103',
			btB8: '0_104',
			// presets buttons (4 bottom right ones here)
			prA1: '0_105',
			prA2: '0_106',
			prB1: '0_107',
			prB2: '0_108'
		);
	}
}
