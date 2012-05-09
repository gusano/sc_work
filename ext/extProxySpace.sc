/*
 * NEEDS extNodeProxy.sc !!!
 *
 */

// yvan volochine 2009

+ ProxySpace {

	getFxs {
		var fxs = ();
		
		this.envir.keys.do{ |k|
			if( this.envir[k].isFX.notNil,
				{ fxs.add(k -> this.envir[k].isFX) }
			);
		};
		if ( fxs.size > 0,
			{ ^fxs },
			{ ^nil }
		)
	}

	getSources {
		var sources = List.new;
		
		this.envir.keys.do{ |k|
			if( this.envir[k].isFX.isNil,
				{ sources.add(k) }
			);
		};
		if ( sources.size > 0,
			{ ^sources },
			{ ^nil }
		)
	}
	/*
	getSources {
		var sources;
		
		sources = this.envir.keys.collect{ |k|
			if( this.envir[k].isFX.isNil,
				{ k }
			);
		};
		
		^sources
	}
	*/
}
