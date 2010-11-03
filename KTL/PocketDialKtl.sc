/*
	from PDKtl.sc
	20101101 - mapTo() totally rewritten in hurry (yvan volochine)
	TODO:
	- make it better
	- refactor
	- rewrite non-endless part (probably no time for that =)
	- remove unused
	- ability to map several nodes to a scene
	BUGS:
	- bad step calculation (delta ..)
*/



PocketDialKtl : MIDIKtl {
	classvar <>verbose = false; 

	var <>softWithin = 0.05, <lastVals;	// for normal mode
	var <>step, <>endless;				// for endless mode
	
	var <proxyDict, <proxyParamsDict;
	var <>inform = true;
	
	*new { |srcID, ccDict, endless = true, step = 0.1| 
		^super.newCopyArgs(srcID, ccDict).endless_(endless).step_(step).init;
	}
	
	init { 
		super.init;
		proxyDict = ();
		proxyParamsDict = ();
	}

	free { 
		ccresp.remove;
		ccDict.clear;
		proxyDict.clear;
		proxyParamsDict.clear;
	}

	mapTo { |proxy, scene, offset=1|
		var params, pairs, tmpParams = ();

		pairs = proxy.getKeysValues;

		if (proxyDict[scene].notNil, {
			warn("Removing previous node from %".format(scene));
		});
		proxyDict.add(scene -> proxy);
		postf("Adding % at scene %\n", proxy, scene);

		pairs.do{ |v, i|
			tmpParams.add(i+1 -> v)
		};
		proxyParamsDict.add(proxy -> tmpParams);

		params = pairs.flop[0];
		params.do{ |p, i|
			var cc = ("kn0" ++ (offset+i).asString).asSymbol;
			postf("mapping param % at %\n", p, "kn0" ++ (i+1).asString);

			if (i < 16, {
				this.mapCCS(scene, cc, { |val|
					var delta, theProxy, ccName, ccNr, currentParam, currentVal, newVal;
					delta = val - 64;
					delta = delta * delta.abs.linlin(1, 7, 0.05, 0.2); /* new step */
					theProxy = proxyDict[scene];
					ccName = cc.asString;
					ccNr = ccName[ccName.size-1].asString.asInteger;
					#currentParam, currentVal = proxyParamsDict[theProxy][ccNr];
					/* get current because it changed maybe */
					/* REWRITE THIS PLEASE*/
					pairs.do{|arr|
						if (arr[0] == p, { currentVal = arr[1]})
					};
					if (currentParam.asSpec.notNil, {
						currentVal = currentParam.asSpec.unmap(currentVal);
						newVal = currentParam.asSpec.map(currentVal + (delta / 127));
						theProxy.set(currentParam, newVal);
						proxyParamsDict[theProxy][ccNr][1] = newVal;
						if (inform, {
							postf("% -> %\n", currentParam, newVal);
						});
					}, {
						warn("% doesn't have a Spec\n".format(currentParam));
					});
				});
			}, {
				warn("Too many params!\nCannot assign % at %\n".format(p, i+1))
			});
		};
		if (params.size < 16, {
			this.mapCCS(scene, 'kn16', { |val|
				var delta, theProxy, volume;
				delta = val - 64;
				delta = delta * delta.abs.linlin(1, 7, 0.05, 0.2); /* new step */
				theProxy = proxyDict[scene];
				volume = \amp.asSpec.unmap(theProxy.vol);
				volume = \amp.asSpec.map(volume + (delta / 127));
				theProxy.vol_(volume);
				if (inform, { postf("% vol -> %\n", theProxy, volume) });
			});
			postf("Mapping kn16 to % volume.\n", proxy);
		});
	}

	*makeDefaults { 
		/*	all midi chan 0, 
		scene 1: 0 - 15 
		scene2: 	16 - 31 
		scene 3: 32 - 47
		scene4: 48 - 63
		*/

		// just one bank of knobs
		defaults.put(this, (
			1: 	(	
				kn01: '1_0', kn02: '1_1', kn03: '1_2', kn04: '1_3', kn05: '1_4', kn06: '1_5', kn07: '1_6', kn08: '1_7',
				kn09: '1_8', kn10: '1_9', kn11: '1_10', kn12: '1_11', kn13: '1_12', kn14: '1_13', kn15: '1_14',kn16: '1_15'
			),
			2: 	(	
				kn01: '1_16', kn02: '1_17', kn03: '1_18', kn04: '1_19', kn05: '1_20', kn06: '1_21', kn07: '1_22', kn08: '1_23', 
				kn09: '1_24', kn10: '1_25', kn11: '1_26', kn12: '1_27', kn13: '1_28', kn14: '1_29', kn15: '1_30',kn16: '1_31'
			),
			3: 	(
				kn01: '1_32', kn02: '1_33', kn03: '1_34', kn04: '1_35', kn05: '1_36', kn06: '1_37', kn07: '1_38', kn08: '1_39', 
				kn09: '1_40', kn10: '1_41', kn11: '1_42', kn12: '1_43', kn13: '1_44', kn14: '1_45', kn15: '1_46',kn16: '1_47'
			),
			4: 	(
				kn01: '1_48', kn02: '1_49', kn03: '1_50', kn04: '1_51', kn05: '1_52', kn06: '1_53', kn07: '1_54', kn08: '1_55', 
				kn09: '1_56', kn10: '1_57', kn11: '1_58', kn12: '1_59', kn13: '1_60', kn14: '1_61', kn15: '1_62',kn16: '1_63'
			)
		));
	}



	/*
		// map to 
	mapToPxEdit { |editor, scene = 1, indices, lastIsVol = true| 
		var elementKeys, lastKey; 
		indices = indices ? (1..8); 
		
		elementKeys = ctlNames[scene].keys.asArray.sort[indices - 1]; 

		
		if (endless.not) { 
			
			if (lastIsVol) { 
				lastKey = elementKeys.pop;
				
					// use last slider for proxy volume
				this.mapCCS(scene, lastKey, { |ch, cc, val| 
					var lastVal = lastVals[lastKey];
					var mappedVol = \amp.asSpec.map(val / 127);
					var proxy = editor.proxy;
					if (proxy.notNil) { proxy.softVol_(mappedVol, softWithin, lastVal: lastVal) };
					lastVals[lastKey] = mappedVol;
				});
			};
			
			elementKeys.do { |key, i|  	
				this.mapCCS(scene, key, 
					{ |ccval| 
						var proxy = editor.proxy;
						var parKey =  editor.editKeys[i];
						var normVal = ccval / 127;
						var lastVal = lastVals[key];
						if (parKey.notNil and: proxy.notNil) { 
							proxy.softSet(parKey, normVal, softWithin, lastVal: lastVal) 
						};
						lastVals.put(key, normVal);
					}
				)
			};
			
		} { 
				// endless
			if (lastIsVol) { 
				lastKey = elementKeys.pop;
				
					// use last knob for proxy volume
				this.mapCCS(scene, lastKey, { |ccval| 
					var proxy = editor.proxy;
					if (proxy.notNil) { proxy.nudgeVol(ccval - 64 * step) };
				});
			};
			
			elementKeys.do { |key, i|  	
				this.mapCCS(scene, key, 
					{ |ccval| 
						var proxy = editor.proxy;
						var parKey =  editor.editKeys[i];
						if (parKey.notNil and: proxy.notNil) { 
							proxy.nudgeSet(parKey, ccval - 64 * step) 
						};
					}
				)
			}
		}
	}
	
	mapToPxMix { |mixer, scene = 1, splitIndex = 8, lastEdIsVol = true, lastIsMaster = true| 
 	
		var server = mixer.proxyspace.server;
		var elementKeys = ctlNames[scene].keys.asArray.sort; 
		var lastKey; 
		
		if (endless.not) { 
					// add master volume on slider 16
			if (lastIsMaster) { 
				lastKey = elementKeys.pop; 
				Spec.add(\mastaVol, [server.volume.min, server.volume.max, \db]);
				this.mapCCS(scene, lastKey, { |ccval| server.volume.volume_(\mastaVol.asSpec.map(ccval/127)) });
			};			
	
				// map first n sliders to volumes
			elementKeys.keep(splitIndex).do { |key, i| 
				this.mapCCS(scene, key, 
					{ |ccval| 
						var proxy = mixer.pxMons[i].proxy; 
						var lastVal, mappedVal, lastVol;
						if (proxy.notNil) { 
							lastVal = lastVals[key]; 
							mappedVal = \amp.asSpec.map(ccval / 127); 
							lastVol = if (lastVal.notNil) { \amp.asSpec.map(lastVal) }; 
							proxy.softVol_( \amp.asSpec.map(mappedVal), softWithin, true, lastVol ); 
						};
						lastVals[key] =  mappedVal;
					};
				)
			};
			
		} { 			// endless mode:
					// add master volume on knob 16
					// nudging master vol not working yet
//			if (lastIsMaster) { 
//				lastKey = elementKeys.pop; 
//				Spec.add(\mastaVol, [server.volume.min, server.volume.max, \db]);
//				this.mapCCS(scene, lastKey, { |ccval| server.volume.volume_(\mastaVol.asSpec.map(ccval/127)) });
//			};			
	
				// map first n knobs to volumes
			elementKeys.keep(splitIndex).do { |key, i| 
				
				this.mapCCS(scene, key, 
					{ |ccval| 
						var proxy = mixer.pxMons[i].proxy; 
						if (proxy.notNil) { 
							proxy.nudgeVol(ccval - 64 * step); 
						};
					};
				)
			};
		
		};
		
		this.mapToPxEdit(mixer.editor, scene, (splitIndex + 1 .. elementKeys.size));
		}*/
}
