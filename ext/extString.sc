+ String {

	just { |length, separator = " ", direction = 'left'|
		var fill;
		if (length.notNil, {
			fill = separator ! (length - this.size);
			fill = "".catArgs(*fill);

			switch(direction,
				'left',  { ^this ++ fill },
				'right', { ^fill ++ this }
			);
		}, {
			"length cannot be nil".warn;
			""; // avoid double warning
		})
	}

	ljust { |length, separator = " "|
		^this.just(length, separator, 'left')
	}

	rjust { |length, separator = " "|
		^this.just(length, separator, 'right')
	}

}

// usage:
/*
(
"I  find this neat !".split(Char.space).do{|x|
	x.rjust(10, ".").postcs;
}
)
*/