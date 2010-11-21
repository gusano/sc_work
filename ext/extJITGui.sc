// 20100510 - custom colors (yvan volochine)

+ JITGui {

	*initClass {
		Class.initClassTree(GUI);
		
		GUI.skins.put(\jit, 
			(
				fontSpecs: 	  ["monospace", 10],
				fontColor:    Color.black,
				background:   Color(0.8, 0.85, 0.7, 0.5),
				foreground:   Color.grey(0.95),
				onColor:      Color(0.5, 1, 0.5),
				//onColor:      Color.new(1, 0.5, 0), // orange
				offColor:     Color.clear,
				hiliteColor:  Color.green(1.0, 0.5),
				//hiliteColor:  Color.new(1.0, 0.5),
				gap:          0 @ 0,
				margin:       2@2,
				buttonHeight: 18,
				headHeight:   24
			)
		);
	}
}
