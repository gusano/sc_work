GamePad {
	classvar <metaMap, <q, <>hidMaps, <space, <cookieMaps, <normMaps, <wingsAndRooms, <modStates;
	classvar <gamepad, <vendor; // added (yvan)
	classvar <>verbose=false, <>defaultAction, <modNames = #[\lHat, \rHat, \midL, \midR];
	
	*initClass {
		hidMaps = hidMaps ?? { () };
		cookieMaps = cookieMaps ?? { () };
		normMaps = normMaps ?? { () };
		wingsAndRooms = wingsAndRooms ?? { () };
		modStates = modStates ?? { () };
		
		defaultAction = { |productID, vendorID, locID, cookie, val|
			[productID, vendorID, locID, cookie, val].postln;
		};

		q = q ?? { () };
		// space is where proxies, their ctLoops etc are kept together.
		space = space ?? { () };

		Spec.specs.addAll([
			\loopTempo -> ControlSpec(0.1, 10, \exp)
		]);

		// fix this last.
		q[\usedRatios] = (); // per buffer -> BufBank

		metaMap = (
			\shiftMidR: true,
			midL: { |val, loop| },
			midR: { |val, loop| var flag;
				if (val==0, {
					flag = metaMap[\shiftMidR].not; metaMap[\shiftMidR] = flag;
					("meta" + #["loop start/length", "value range/shift"][flag.binaryValue]).postln;
				})
			},
			lHat: { |val, loop| if (val==0, { "flipInv loop".postln; loop.flipInv })  },
			rHat: { |val, loop| if (val==0, { "flip loop".postln; loop.flip }) },
			joyLX: { |val, loop|
				if (metaMap[\shiftMidR], { loop.scaler_(val * 2); }, { loop.start_(val); })
			},
			joyLY: { |val, loop|
				if (metaMap[\shiftMidR]) { loop.shift_(val - 0.5); } { loop.length_(val.squared * 4); }
			},
			joyRY: { |val, loop| loop.tempo_(\loopTempo.asSpec.map(val)); },
			joyRX: { |val, loop| loop.jitter_(val - 0.5) }
		);
	}

	*init {
		gamepad = ();
		GeneralHID.buildDeviceList;
		"HID product, USB plug locID:".postcln;

		// if we have vendorID, open only 1 device (yvan)
		if ( vendor.isNil, {
			"vendor is missing, opening all devices".warn;
			// open all devices
			GeneralHID.deviceList.do{|dev, i|
				gamepad.add(i -> GeneralHID.open(dev))
			}
		}, {
			// only one here
			gamepad.add(0 -> GeneralHID.open( GeneralHID.findBy( vendor ) ))
		});


		gamepad.do{|dev|
			var vendorID = vendor ? dev.info.vendor;
			var locID = dev.info.physical;

			cookieMaps[locID] = cookieMaps[vendorID];
			normMaps[locID] = normMaps[vendorID];
			modStates[locID] = (midL: false, midR: false, lHat: false, rHat: false);
			
			dev.action_({ |type, cookie, val|
				var cookieMap;

				cookieMap = cookieMaps[locID];
				if (cookieMap.notNil, {
					this.hidAction(locID, cookie, val);
				}, {
					"GamePad defaultAction:".postln;
					defaultAction.value(vendorID, type, locID, cookie, val);
				})
			})
		}
	}

	*startHID { |vendorarg|
		vendor = vendorarg;
		this.init;
		GeneralHID.startEventLoop;
	}

	*stop {
		GeneralHID.stopEventLoop;
		gamepad.do(_.close); // close devices (yvan)
	}

	*putProxy { |pos, name, map|
		// only works if currentEnvironment is a proxyspace.
		// may be made more general later.
		var ctLoop;

		ctLoop = CtLoop(pos, map).rescaled_(true)
			.dontRescale([\midL, \midR, \lHat, \rHat]);

		space.put(pos, (
			name: name,
			proxy: currentEnvironment[name],
			ctLoop: ctLoop,
			map: map,
			meta: false,
			recNames: [\joyRY, \joyLX, \joyRX, \joyLY, \midL, \midR, \lHat, \rHat]
		));
	}

	*hidAction { |locID, cookie, rawVal| // args changed with GeneralHID (yvan)

		var cookieMap, cookieName, mySpace, wingAndRoom, val;
		var curLoop, curPx, curMap, curMeta, padOffsets;
		var recTime, normMap, modState;

		// we know we have a cookieMap!
		cookieMap = cookieMaps[locID];
		cookieName = cookieMap.getID(cookie);

		// normalize ranges:
		normMap = normMaps[locID];

		// if no normMap was found, this fails. it should.
		val = normMap[cookieName].value(rawVal) ? rawVal;
		
		if (verbose == true, {
			[this.name, locID, cookieName, cookie, rawVal, val.round(1e-4)].postln
		});

		wingAndRoom = wingsAndRooms[locID] ?? { wingsAndRooms.put(locID, [0, 0]); [0, 0] };
		mySpace = space[wingAndRoom.sum];
		modState = modStates[locID];

		if (mySpace.isNil, {
			// "no mans land... jump to switching spaces.".postln;
		}, {
			curPx = mySpace[\proxy];
			curMap = mySpace[\map];
			curLoop = mySpace[\ctLoop];
			curMeta = mySpace[\meta];

			if (mySpace[\recNames].includes(cookieName) and: curMap.notNil, {
				
				if (curMeta,
					{ metaMap[cookieName].value(val, curLoop); },
					{ curMap[cookieName].value(val, modState); }
				);

				if (curLoop.notNil, { curLoop.recordEvent(cookieName, val); });
					// write modifier states!
				if (modNames.includes(cookieName), {
					[cookieName, val, val == 0];
					(modState.put(cookieName, val == 0));
				});

				^this
			});
			// turn this into a case.switch thing?
			if (cookieName == \rfTop, {
				// mute button toggles: if paused, play, else pause.
				if (val == 0 and: curPx.notNil, {
					if (curPx.paused, { curPx.resume.wakeUp.play; }, { curPx.pause })
				});
				^this
			});

			if (cookieName == \lfTop, {
				mySpace.meta = false;
				[ { curLoop.startRec; }, { curLoop.stopRec; } ][val].value;
				^this
			});
			if (cookieName == \lfBot and: { val == 0 }, {
				if (curLoop.isPlaying) { mySpace.meta = false };
				curLoop.togglePlay;
				^this
			});
			if (cookieName == \rfBot and: { val == 0 }, {
				curMeta = curMeta.not;
				mySpace.meta = curMeta;
			// curLoop.rescaled_(curMeta); ?? really trun it off?
				("MetaCtl for" + mySpace.name + curMeta).postln;
				^this
			})
		});
		// switchOffsets ( specific for locID ;-)
		if (cookieName.asString.contains("compass") and: { val >= 0 }, {

			////Joker special:: switch sample folders from gamepad/////////

			if ( modState[\midR], {
				val.switch(
					0, { BufBank.loadFiles(true); },
					2, { BufBank.loadFiles },
					3, { BufBank.stepFolder },
					1, { BufBank.stepFolder(-1) }
				);
			}, {
				"room: ".postc; wingAndRoom.put(1, val.postln)
			});
			^this
		});

		if ([\bt1, \bt2, \bt3, \bt4].includes(cookieName), {
			if( val == 0, {
				if( modState[\midL], {
					// Zweitnutzung zum Audio Aufnehmen
					// ~record.set(\bufnum, BufBank.bufIDs[cookie - 3], \amp, 1);
					// q[\recStartTime]=thisThread.seconds;
					// ~record= q[\recordFunc];
					[ "__recording: " ++ (cookieName) ].postln;
				}, {
					"wing: ".postc;
					wingAndRoom.put(0, ([\bt1, \bt2, \bt3, \bt4].indexOf(cookieName) * 4).postln);
				});
			}, {
					// val 1;
					if( modState[\midL], {
// ~record.set(\amp, 0);
// recTime = thisThread.seconds- q[\recStartTime];
// q[\usedRatios].put(cookie - 3, recTime/ BufBank.jamSize ? 10);
						["_record ended" +(cookie-2), recTime ].postln;
					});
			});
		});
	}
}

Impact : GamePad {
	classvar <vendorID = 1973;
	
	*initClass {
		Class.initClass(GamePad);

		cookieMaps[vendorID] = TwoWayIdentityDictionary[
			
			\bt1 -> 3, // 4 buttons, righthand; up is 1, pressed is 0.
			\bt2-> 4,
			\bt3-> 5,
			\bt4 -> 6,

			\lfTop -> 7, // 4 fire buttons, up 1, down 0
			\lfBot -> 8,
			\rfTop -> 9,
			\rfBot -> 10,

			\midL -> 11, // middle shift buttons
			\midR -> 12,
			\lHat -> 13, // hat switches on joysticks
			\rHat -> 14,
			// newer Impact is docd here,
			// older is 0 - 255.
			\joyLX -> 15, // joystick left x-axis (horizontal) left is 127, right is -128!
			\joyLY -> 16, // joy left y-axis, up is 127.
			\joyRX -> 18, // joy right x-axis, left is 127, right is -128!
			\joyRY -> 17,

			\compass -> 19 // west is 2, south is 4, north is 8, east is 6
		];

		hidMaps[vendorID] = { arg locID, cookie, val; this.hidAction(locID, cookie, val); };

		// Impact new norm map
		normMaps[vendorID] = (
			joyLX: { |val| 255 - val / 255 },
			joyLY: { |val| val /  255 },
			joyRX: { |val| 255 - val / 255 },
			joyRY: { |val| val /  255 },
			compass: { |val| (2: 0, 4: 1, 6: 2, 8: 3, 9: -1)[val] }
		);
	}
}

Betop : GamePad {
	classvar <rangeMap, <vendorID = 3727;

	*initClass {
		Class.initClass(GamePad);

		cookieMaps[vendorID] = TwoWayIdentityDictionary[

			\bt1 -> 4, // 4 buttons, righthand; up is 1, pressed is 0.
			\bt2 -> 5,
			\bt3 -> 6,
			\bt4 -> 7,

			\lfTop -> 8, // 4 fire buttons, up 1, down 0
			\lfBot -> 10,
			\rfTop -> 9,
			\rfBot -> 11,

			\midL -> 12, // middle shift buttons
			\midR -> 13,
			\lHat -> 14, // hat switches on joysticks
			\rHat -> 15,

			\joyLX -> 19, // joystick left x-axis (horizontal) left is 255, right 0!
			\joyLY -> 20, // joy left y-axis, up is 255.
			\joyRX -> 18, // joystick right x-axis (horizontal) left is 255, right 0!
			\joyRY -> 17, // joy right x-axis, left is 127, right is -128!
			
			\compass -> 22 // west is 1, south is 3, north is 7, east is 5
		];
		
		hidMaps[vendorID] = { arg locID, cookie, val; this.hidAction(locID, cookie, val); };

		normMaps[vendorID] = (
			joyLX: { |val| 255 - val / 255 },
			joyLY: { |val| val /  255 },
			joyRX: { |val| 255 - val / 255 },
			joyRY: { |val| val /  255 },
			compass: { |val| (1: 0,3: 1, 5: 2, 7: 3, -8: -1)[val] }
		);
	}
}


FireStorm : GamePad { // Thrustmaster FireStorm Dual Analog 3
	classvar <rangeMap, <vendorID = 1103;

	*initClass {
		Class.initClass(GamePad);

		Platform.case(
			\osx, {
				cookieMaps[vendorID] = TwoWayIdentityDictionary[

					\bt1 -> 1, // 4 righthand buttons. up is 0, pressed is 1
					\bt2 -> 2,
					\bt3 -> 4,
					\bt4 -> 3,

					\lfTop -> 5, // 4 fire buttons, up 0, down 1
					\lfBot -> 6,
					\rfTop -> 7,
					\rfBot -> 8,

					\midL -> 11, // this one does *not* exist here !
					\midR -> 12, // this one does *not* exist here !
					\lHat -> 9,
					\rHat -> 10,

					\joyLX -> 48, // joystick left x-axis, left is 127, right is -128
					\joyLY -> 49, // joy left y-axis, up is 127, down is -128
					\joyRX -> 53, // joystick right x-axis, left is 127, right is -128
					\joyRY -> 54, // joy right y-axis, up is 255, down is 0

					\compass -> 57 // west is 1, north is 7, east is 5, south is 3, center is -8
				];

				// changed, but can't see where this is needed (yvan)
				hidMaps[vendorID] = { |locID, cookie, val| this.hidAction(cookie, val) };

				normMaps[vendorID] = (
						\bt1: { |val| val * 100 },
						\bt2: { |val| 1 - val },
						\bt3: { |val| 1 - val },
						\bt4: { |val| 1 - val },
						\lfTop: { |val| 1 - val },
						\lfBot: { |val| 1 - val },
						\rfTop: { |val| 1 - val },
						\rfBot: { |val| 1 - val },
						\joyLX: { |val| val + 128 / 255 },
						\joyLY: { |val| val + 128 / 255 },
						\joyRX: { |val| 1.0 - (val + 128 / 255) },
						\joyRY: { |val| val / 255 },
						\compass: { |val| (1: 0, 7: 1, 5: 2, 3: 3, -8: -1)[val] }
				)

			},
			\linux, {
				cookieMaps[vendorID] = TwoWayIdentityDictionary[

					\bt1 -> 306, // 4 righthand buttons. up is 0, pressed is 1
					\bt2 -> 304,
					\bt3 -> 305,
					\bt4 -> 307,

					\lfTop -> 308, // 4 fire buttons, up 0, down 1
					\lfBot -> 309,
					\rfTop -> 310,
					\rfBot -> 311,

					\midL -> 11, // this one does *not* exist here !
					\midR -> 12, // this one does *not* exist here !
					\lHat -> 312,
					\rHat -> 313,

					\joyLX -> 0, // joystick left x-axis, left is 0, right is 1
					\joyLY -> 1, // joy left y-axis, up is 0, down is 1
					\joyRX -> 5, // joystick right x-axis, left is 0, right is 1
					\joyRY -> 6, // joy right y-axis, up is 0, down is 1

					\compassX -> 16, // west is 0, east is 1, center is 0.5
					\compassY -> 17 // north is 0, south is 1, center is 0.5
				];

				//hidMaps[vendorID] = { arg locID, cookie, val; this.hidAction(locID, cookie, val); };
				// changed, but can't see where this is needed (yvan)
				hidMaps[vendorID] = { |locID, cookie, val| this.hidAction(cookie, val) };

				normMaps[vendorID] = (
					\bt1: { |val| 1 - val },
					\bt2: { |val| 1 - val },
					\bt3: { |val| 1 - val },
					\bt4: { |val| 1 - val },
					\lfTop: { |val| 1 - val },
					\lfBot: { |val| 1 - val },
					\rfTop: { |val| 1 - val },
					\rfBot: { |val| 1 - val },
					\joyLX: { |val| val + 128 / 255 },
					\joyLY: { |val| 1 - (val + 128 / 255) },
					\joyRX: { |val| val + 128 / 255 },
					\joyRY: { |val| 1 - (val / 255) },
					\compassX: { |val| (1: 0, 1: 2, 0.5: -1)[val] },
					\compassY: { |val| (1: 1, 0: 3, 0.5: -1)[val] }
				)
			},
			\windows, { "no GeneralHID support on Windows".error }
		)
	}
}
