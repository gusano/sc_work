// historizing NodeProxy
// from adc

+ NodeProxy { 

	histSet { arg ... args; // pairs of keys or indices and value
		var str; 
		if (History.started) { 
			str  = this.asCompileString ++ ".set(*" + args.asCompileString + ");";
			thisProcess.interpreter.cmdLine_(str)
				.interpretPrintCmdLine;
		} { 
			this.set(*args);
		};
	}
	
	histPlay {
		var str;
		if (History.started) {
			str = this.asCompileString ++ ".play";
			thisProcess.interpreter.cmdLine_(str).interpretPrintCmdLine
		} {
			this.play
		}
	}

	histStop {
		var str;
		if (History.started) {
			str = this.asCompileString ++ ".stop";
			thisProcess.interpreter.cmdLine_(str).interpretPrintCmdLine
		} {
			this.stop
		}		
	}

	histVol_ { |val|
		var str;
		if (History.started) {
			str = this.asCompileString ++ ".vol_(" ++ val.asString ++ ");";
			thisProcess.interpreter.cmdLine_(str).interpretPrintCmdLine
		} { 
			this.initMonitor(val)
		}
	}

//	
//	unset {
//	
//	}
//	
//	rebuild { 
//	
//	}
//	
//	send { 
//	
//	}
//	
//	map { 
//	
//	}
//	
//	unmap { 
//	
//	}
//	
//	pause { 
//	
//	}
//	
//	resume { 
//	
//	}

}
