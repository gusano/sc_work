ProxyRecorder {

	var <proxyspace, <nodes;
	var <>folder;
	var <>headerFormat = "aiff", <>sampleFormat = "float";

	*new { |proxyspace|
		^super.newCopyArgs(proxyspace).init()
	}

	init {
		nodes  = ();
		folder = Platform.userAppSupportDir +/+ "Recordings";
	}

	free {
		nodes.do(_.clear);
		nodes = nil;
	}

	add { |proxies|
		this.prepareNodes(proxies);
		{ this.open(proxies) }.defer(0.5);
	}

	prepareNodes { |proxies|
		proxies.do{ |proxy, i|
			nodes.add(
				i -> RecNodeProxy.newFrom(proxy, 2)
			);
		}
	}

	open { |proxies|
		proxies.do{ |proxy, i|
			var dateTime  = Date.getDate.format("%Y%m%d-%Hh%m");
			var fileName  = ("%/%-%.%").format(
				folder, dateTime, proxy.asCompileString, headerFormat
			);

			nodes[i].open(fileName, headerFormat, sampleFormat);
		}
	}

	record { |paused=false|
		nodes.do(_.record(paused))
	}

	stop {
		this.close
	}

	close {
		nodes.do(_.close)
	}

	pause {
		nodes.do(_.pause)
	}

	unpause {
		nodes.do(_.unpause)
	}

	closeOne { |node|

	}
}

// for backwards compatibility
MultiRec : ProxyRecorder {
}