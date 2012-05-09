// yvan volochine 2009

+ NodeProxy {

	// handy small popup window with NodeProxy source
	src {
		var w, txt, content, button;

		content = this.asCompileString ++ " = " ++ this.source.asCompileString;
		w = Window.new("...", Rect(200, 400, 300, 180)).front;
		w.addFlowLayout;
		txt = TextView(w, 300@150).string_(content);
		button = Button(w, 100@24).states_([["eval"]]).action_({ |butt| if ( butt.value == 0, { txt.string.interpret })})
	}

	random { |rand=0.25, except, verbose=false|
		var randKeysVals, set, randRange; 

		set = this.getKeysValues(except: except);

		//randKeysVals = set.collect { |pair| 
		set.do { |pair|
			var key, val, normVal, randVal, spec; 
			#key, val = pair;
			spec = key.asSpec; 
			if (spec.notNil, {
				normVal =  spec.unmap(val); 
				randVal = rrand( 
					(normVal - rand).max(0), 
					(normVal + rand).min(1)
				); 
				//[key, val, normVal].postcs;
				//[key, spec.map(randVal)]
				this.set(key, spec.map(randVal));
				if (verbose, { [key, spec.map(randVal)].postcs })
			}, {
				Error("no spec found for" ++ spec.asString).throw
			});
		};
		// for some reasons this was killing sclang... (yvan)
		//this.set(*randKeysVals)
	}

	isFX {
		var str, result;
		
		str = this.source.asCompileString;
		str = str.split($,).at(0); // keep 1st part before any comma
		result = str.findRegexp("~[a-zA-Z0-9]*+(.ar)"); // look for "~xxxxxx.ar"
		if ( result.size > 0, 
			{ ^result[0][1] },
			{ ^nil }
		)
	}
}
