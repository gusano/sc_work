ServerOptionsGui {

    var server, serverOptions;
    var simpleOptions, simpleViewOptions, advancedViewOptions;
    var currentValues;
    var <parent, <>bounds, specialView, simpleView, advancedView;
    var width = 340, height = 520, standalone;
    var <>applyFunction, <>cancelFunction;
    var <>verbose = true;

    *new { |server, parent, bounds|
        ^super.new.init(server, parent, bounds)
    }

    init { |serverArg, parentArg, boundsArg|

        server = serverArg ?? Server.default;

        if (parentArg.isNil, {
            parent = Window.new("Server Options").front.bounds_(
                Rect(
                    (Window.screenBounds.width / 2) - (width / 2),
                    (Window.screenBounds.height / 2) - (height / 2),
                    width,
                    height
                )
            );
            standalone = true;
        }, {
            parent = parentArg
        });

        bounds = boundsArg ?? Rect(
            0, 0, parent.bounds.width, parent.bounds.height
        );

        applyFunction = {};
        cancelFunction = {};

        simpleViewOptions = (
            \numInputBusChannels:        (\type: NumberBox, \modified: nil, \pos: 2),
            \numOutputBusChannels:       (\type: NumberBox, \modified: nil, \pos: 3),
            \sampleRate:                 (\type: NumberBox, \modified: nil, \pos: 4),
            \blockSize:                  (\type: NumberBox, \modified: nil, \pos: 5),
            \memSize:                    (\type: NumberBox, \modified: nil, \pos: 6),
            \numPrivateAudioBusChannels: (\type: NumberBox, \modified: nil, \pos: 7),
            \numControlBusChannels:      (\type: NumberBox, \modified: nil, \pos: 8)
        );
        // TODO Win support
        Platform.case(
            \osx, {
                simpleViewOptions.add(
                    \inDevice -> (\type: PopUpMenu, \modified: nil, \pos: 0)
                );
                simpleViewOptions.add(
                    \outDevice -> (\type: PopUpMenu, \modified: nil, \pos: 1)
                );
            },
            \linux, {
                //simpleViewOptions.add(
                //    \device -> (\type: PopUpMenu, \modified: nil, \pos: 1)
                //);
                height = height - 30;
            }
        );

        advancedViewOptions = (
            \verbosity:            (\type: NumberBox, \modified: nil, \pos: 0),
            \maxNodes:             (\type: NumberBox, \modified: nil, \pos: 1),
            \maxSynthDefs:         (\type: NumberBox, \modified: nil, \pos: 2),
            \numWireBufs:          (\type: NumberBox, \modified: nil, \pos: 3),
            \hardwareBufferSize:   (\type: NumberBox, \modified: nil, \pos: 4),
            \protocol:             (\type: TextField, \modified: nil, \pos: 5),
            \loadDefs:             (\type: CheckBox,  \modified: nil, \pos: 6),
            \inputStreamsEnabled:  (\type: TextField, \modified: nil, \pos: 7),
            \outputStreamsEnabled: (\type: TextField, \modified: nil, \pos: 8),
            \numRGens:             (\type: NumberBox, \modified: nil, \pos: 9),
            \restrictedPath:       (\type: TextField, \modified: nil, \pos: 10),
            \initialNodeID:        (\type: NumberBox, \modified: nil, \pos: 11),
            \remoteControlVolume:  (\type: CheckBox,  \modified: nil, \pos: 12),
            \memoryLocking:        (\type: CheckBox,  \modified: nil, \pos: 13),
            \zeroConf:             (\type: CheckBox,  \modified: nil, \pos: 14)
        );

        simpleOptions = (
            \latency:         (\type: NumberBox, \modified: nil, \pos: 0),
            \recChannels:     (\type: NumberBox, \modified: nil, \pos: 1),
            \recHeaderFormat: (\type: PopUpMenu, \modified: nil, \pos: 2),
            \recSampleFormat: (\type: PopUpMenu, \modified: nil, \pos: 3)
        );


        if (Server.program.asString.endsWith("supernova")) {
            advancedViewOptions.put(
                \threads, (\type: NumberBox, \modified: false, \pos: 15)
            );
        };

        serverOptions = server.options;
        currentValues = ();
        this.drawGui();
    }

    drawGui {
        var mainView, topLayout, bottomLayout;
        var infoText, linkButton, modeButton, applyButton, cancelButton;
        var blue, red;

        blue = Color(0.58, 0.69, 0.75);
        red = Color(0.75, 0.58, 0.69);

        infoText = StaticText().string_("Options for" + server.name)
            .font_(Font(Font.defaultSansFace, 18, true))
            .align_(\center);

        linkButton = Button()
            .states_([["ServerOptions help"]])
            .action_{ HelpBrowser.openHelpFor("ServerOptions") };

        modeButton = Button().states_([
            ["Advanced settings"], ["Simple settings"]
        ]).action_({ |butt|
            this.swapView(butt.value)
        });

        specialView = this.getView(simpleOptions, blue);
        simpleView = this.getView(simpleViewOptions, red, true);
        advancedView = this.getView(advancedViewOptions, red, true, false);

        cancelButton = Button().states_([["Cancel"]])
            .action_({ this.cancelAction });
        applyButton = Button().states_([["Apply"]])
            .action_{ this.applyAction() };

        topLayout = QVLayout(
            infoText, QHLayout(linkButton, modeButton)
        );
        bottomLayout = QHLayout(cancelButton, applyButton);

        mainView = View(parent);
        mainView.bounds_(bounds).layout_(
            QVLayout(
                topLayout, specialView, simpleView, advancedView, bottomLayout
            )
        );
    }

    getRebootText {
        ^StaticText().string_(
            "These options will only be set after rebooting the server"
        ).background_(Color.red(0.7, 0.2))
    }

    getView { |options, color, hasTitle = false, visible = true|
        var view = View().background_(color);
        var grid = QGridLayout();
        var title, rowStart = 0;

        view.layout_(grid);
        view.visible_(visible);

        if (hasTitle, {
            title = this.getRebootText();
            rowStart = 2;
            grid.addSpanning(title, 0, 0, rowStart, 3)
        });

        options.keys.do{ |key|
            var option, label, guiElement, val, row;

            option = options[key];

            row = rowStart + option[\pos];

            label = StaticText().string_(key)
                .stringColor_(Color.new(0.1, 0.1, 0.1));

            val = serverOptions.tryPerform(key.asGetter);

            guiElement = option[\type].new()
                .action_{ option[\modified] = true };

            if (key == \latency, {
                guiElement.clipLo_(0).decimals_(2).scroll_step_(0.01)
            });

            if (key == \device or: {key == \inDevice} or: {key == \outDevice}, {
                grid.add(label, row, 0);
                grid.addSpanning(guiElement, row, 1, 1, 2);
                this.setAudioDevice(key, guiElement);
            }, {
                grid.addSpanning(label, row, 0, 1, 2);
                grid.add(guiElement, row, 2);
            });

            if (val.notNil, { guiElement.value_(val) });

            if (options == simpleOptions, {
                this.setSimpleOption(key, guiElement)
            });

            currentValues.add(key -> guiElement);
        };

        ^view
    }

    setAudioDevice { |key, guiElement|
        // TODO: use jack_lsp on linux and SC_JACK_SERVER...
        Platform.case(
            \osx, {
                var keys = (\inDevice: \inDevices, \outDevice: \outDevices);
                try {
                    this.setPopupItems(
                        guiElement,
                        ServerOptions.tryPerform(keys[key].asGetter),
                        serverOptions.tryPerform(key.asGetter)
                    )
                } { |e|
                    e.errorString.warn
                }
            }
        );
    }

    setSimpleOption { |option, element|
        option.switch(
            \latency, { element.value_(server.latency) },
            \recChannels, { element.value_(server.recChannels) },
            \recHeaderFormat, {
                this.setPopupItems(
                    element, this.getHeaderFormats, server.recHeaderFormat
                )
            },
            \recSampleFormat, {
                this.setPopupItems(
                    element, this.getSampleFormats, server.recSampleFormat
                )
            }
        )
    }

    setPopupItems { |popup, items, value|
        var array = items.collect(_.asSymbol);
        popup.items_(items);
        popup.value_(array.indexOf(value.asSymbol));
    }

    swapView { |buttonValue|
        var simple = [simpleView, specialView];

        buttonValue.switch(
            0, { simple.do(_.visible_(true));  advancedView.visible_(false) },
            1, { simple.do(_.visible_(false)); advancedView.visible_(true) }
        )
    }

    cancelAction {
        if (standalone.notNil, {
            parent.close
        }, {
            this.cancelFunction.value()
        })
    }

    applyAction {
        [simpleViewOptions, advancedViewOptions].do{ |options|
            options.keys.do{ |key|
                if (options[key][\modified].notNil, {
                    this.setServerOption(server.options, key, options[key][\type])
                });
            }
        };

        simpleOptions.keys.do{ |key|
            if (simpleOptions[key][\modified].notNil, {
                this.setServerOption(server, key, simpleOptions[key][\type]);
            });
        };

        if (standalone.notNil, {
            parent.close
        }, {
            applyFunction.value()
        })
    }

    setServerOption { |options, key, type|
        var value;
        switch (type,
            PopUpMenu, { value = currentValues[key].item.asString },
            TextField, { value = currentValues[key].value.asString },
            CheckBox,  { value = currentValues[key].value.asBoolean },
            NumberBox, {
                if (key == \latency, {
                    value = currentValues[key].value.asFloat
                }, {
                    value = currentValues[key].value.asInteger
                })
            }
        );
        options.tryPerform(key.asSetter, value);
        if (verbose, {
            " - changed: % -> %".format(key, value).postln; "";
        })
    }

    getSampleFormats {
        //^[\int8, \int16, \int24, \int32, \mulaw, \alaw, \float]
        ^[\int16, \int24, \int32, \float]
    }

    getHeaderFormats {
        //^[
        //    \aiff, \wav, \sun, \next, \sd2, \ircam, \raw, \mat4,
        //    \mat5, \paf, \svx, \nist, \voc, \w64, \pvf, \xi, \htk,
        //    \sds, \avr, \flac, \caf
        //]
        ^[\aiff, \wav]
    }
}
