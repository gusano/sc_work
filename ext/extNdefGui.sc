// allow ProxyMixer and NdefGui to work with Qt without DragBoth

+ NdefGui {

	makeNameView { |nameWid, height|
		nameView = TextView(zone, Rect(0,0, nameWid, height))
		    .font_(font)
		    //.align_(0)
		    //.string_("xxx")
	}

}


+ NdefParamGui {

	makeViews {
		var sinkWidth = 40; 
		var height = skin.buttonHeight;

		specs = ();  
		replaceKeys = ();
		prevState = ( settings: [], overflow: 0, keysRotation: 0, editKeys: []);
		
		labelWidth = zone.bounds.width * 0.15; 
		
		#drags, valFields = { |i|
			var drag, field;
			drag = nil;
			/*
			drag = DragBoth(zone, Rect(0, 0, sinkWidth, skin.buttonHeight))
				.string_("-").align_(\center)
				.visible_(false)
				.font_(font);
			drag.action_(this.dragAction(i));
			*/
			field = CompositeView(zone, Rect(0, 0, bounds.width - sinkWidth - 20, height))
			.resize_(2);
			//.background_(skin.background);
			
			[drag, field]; 
			
		}.dup(numItems).flop;
		
		widgets = nil.dup(numItems); // keep EZGui types here

		zone.decorator.reset.shift(zone.bounds.width - 16, 0);

		scroller = EZScroller(zone,
			Rect(0, 0, 12, numItems * height),
			numItems, numItems,
			{ |sc| 
				keysRotation = sc.value.asInteger.max(0);
				prevState.put(\dummy, \dummy);
			}
		).visible_(false);
		scroller.slider.resize_(3);
	}

	setField { |index, key, value, sameKey = false|
		var area = valFields[index];
		var widget = widgets[index];
				
		if (replaceKeys[key].notNil) { 
			area.background_(skin.hiliteColor);
		} { 
			area.background_(skin.background);
		};
		
		/*
		if (value.isKindOf(NodeProxy)) { 
			drags[index].object_(value).string_("->" ++ value.key);
		} {
			drags[index].object_(nil).string_("-");
		};
				// dodgy - defer should go away eventually.
				// needed for defer in setToSlider... 
		{ drags[index].visible_(true) }.defer(0.05);
		*/
		if (value.isKindOf(SimpleNumber) ) {
			this.setToSlider(index, key, value, sameKey);
			^this
		};

		this.setToText(index, key, value, sameKey);
	}

}
