/**
 * @file    ServerOptionsGui.sc
 * @desc    GUI utility to change or visualize default server options.
 * @author  Yvan Volochine <yvan.volochine@gmail.com>
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 * @version 0.1
 * @since   2011-11-04
 * @link    http://github.com/gusano/sc_work/tree/master/utils
 *
 * @usage   g = ServerOptionsGui(s)
 */

ServerOptionsGui {

    /**
     * @var Server server
     */
    var server;

    /**
     * @var ServerOptions serverOptions
     */
    var serverOptions;

    /**
     * @var Dictionary simpleViewOptions
     */
    var simpleViewOptions;

    /**
     * @var Dictionary advancedViewOptions
     */
    var advancedViewOptions;

    /**
     * @var Dictionary simpleOptions Options that do not need Server to be
     *      rebooted to be set
     */
    var simpleOptions;

    /**
     * @var OrderedIdentitySet orderedKeys Sorted options
     */
    var orderedKeys;

    /**
     * @var OrderedIdentitySet orderedSimpleKeys Sorted simple options
     */
    var orderedSimpleKeys;

    /**
     * @var OrderedIdentitySet orderedAdvancedKeys Sorted advanced options
     */
    var orderedAdvancedKeys;

    /**
     * @var Dictionary currentValues
     */
    var currentValues;

    /**
     * @var mixed parent The parent GUI
     */
    var <parent;

    /**
     * @var Rect bounds Main View bounds
     */
    var <>bounds;

    /**
     * @var View simpleView A view containing the basic settings
     */
    var simpleView;

    /**
     * @var View advancedView A view containing the advanced settings
     */
    var advancedView;

    /**
     * @var View specialView A view containing the simple options
     */
    var specialView;

    /**
     * @var Integer width Main view width
     */
    var width = 300;

    /**
     * @var Integer height Main view height
     */
    var height = 520;

    /**
     * @var Boolean standalone If no parent is given, we run in standalone mode
     */
    var standalone;

    /**
     * @var Function applyFunction Function called when clicking Apply
     *      This is only called when not in standalone mode
     */
    var <>applyFunction;

    /**
     * @var Function cancelFunction Function called when clicking Cancel
     *      This is only called when not in standalone mode
     */
    var <>cancelFunction;


    /**
     * *new
     * @param Server server
     * @param mixed  parent The parent container
     * @param Rect   bounds
     * @return self
     */
    *new { |server, parent, bounds|
        ^super.new.init(server, parent, bounds)
    }

    /**
     * init Initialise default server options
     * @param Server serverArg
     * @param mixed  parentArg
     * @param Rect   boundsArg
     * @return self
     */
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
            \numAudioBusChannels:   (\type: NumberBox, \modified: nil),
            \numControlBusChannels: (\type: NumberBox, \modified: nil),
            \numInputBusChannels:   (\type: NumberBox, \modified: nil),
            \numOutputBusChannels:  (\type: NumberBox, \modified: nil),
            \blockSize:             (\type: NumberBox, \modified: nil),
            \memSize:               (\type: NumberBox, \modified: nil),
            \sampleRate:            (\type: NumberBox, \modified: nil),
            \inDevice:              (\type: PopUpMenu, \modified: nil),
            \outDevice:             (\type: PopUpMenu, \modified: nil)
        );

        advancedViewOptions = (
            \verbosity:            (\type: NumberBox, \modified: nil),
            \maxNodes:             (\type: NumberBox, \modified: nil),
            \maxSynthDefs:         (\type: NumberBox, \modified: nil),
            \numWireBufs:          (\type: NumberBox, \modified: nil),
            \hardwareBufferSize:   (\type: NumberBox, \modified: nil),
            \protocol:             (\type: TextField, \modified: nil),
            \numRGens:             (\type: NumberBox, \modified: nil),
            \loadDefs:             (\type: CheckBox,  \modified: nil),
            \inputStreamsEnabled:  (\type: TextField, \modified: nil),
            \outputStreamsEnabled: (\type: TextField, \modified: nil),
            \zeroConf:             (\type: CheckBox,  \modified: nil),
            \restrictedPath:       (\type: TextField, \modified: nil),
            \initialNodeID:        (\type: NumberBox, \modified: nil),
            \remoteControlVolume:  (\type: CheckBox,  \modified: nil),
            \memoryLocking:        (\type: CheckBox,  \modified: nil),
            \zeroConf:             (\type: CheckBox,  \modified: nil)
        );

        simpleOptions = (
            \latency:         (\type: NumberBox),
            \recChannels:     (\type: NumberBox),
            \recHeaderFormat: (\type: PopUpMenu),
            \recSampleFormat: (\type: PopUpMenu)
        );

        orderedKeys = OrderedIdentitySet[
            \latency, \recChannels, \recHeaderFormat, \recSampleFormat
        ];

        orderedSimpleKeys = OrderedIdentitySet[
            \inDevice, \outDevice, \numInputBusChannels, \numOutputBusChannels,
            \sampleRate, \blockSize, \memSize, \numAudioBusChannels,
            \numControlBusChannels
        ];

        orderedAdvancedKeys = OrderedIdentitySet[
            \verbosity, \maxNodes, \maxSynthDefs, \numWireBufs,
            \hardwareBufferSize, \protocol, \loadDefs, \inputStreamsEnabled,
            \outputStreamsEnabled, \numRGens, \restrictedPath, \initialNodeID,
            \remoteControlVolume, \memoryLocking, \zeroConf
        ];

        if (Server.program.asString.endsWith("supernova")) {
            advancedViewOptions.put(
                \threads, (\type: NumberBox, \modified: false)
            );
            orderedAdvancedKeys.add(\threads);
        };

        serverOptions = server.options;
        currentValues = ();
        this.drawGui();
    }

    /**
     * drawGui
     * Draw the main GUI: one QVLayout containing one QVLayout for the top
     * button, one QGridLayout for the options and one QHLayout for the
     * bottom buttons.
     */
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

        specialView = this.drawSettings(simpleOptions, blue);
        simpleView = this.drawSettings(simpleViewOptions, red);
        advancedView = this.drawSettings(advancedViewOptions, red, false);

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

    /**
     * getRebootText
     * @return StaticText
     */
    getRebootText {
        ^StaticText().string_(
            "These options will only be set after rebooting the server"
        ).background_(Color.red(0.7, 0.2))
    }

    /**
     * drawSettings Draws the settings and their corresponding values
     * @param Dictionary options
     * @param Color      color
     * @param boolean    visible
     */
    drawSettings { |options, color, visible = true|
        var view = View().background_(color);
        var grid = QGridLayout();
        var keys, title;

        view.layout_(grid);
        view.visible_(visible);

        options.switch(
            simpleOptions, { keys = orderedKeys },
            simpleViewOptions, {
                keys = orderedSimpleKeys;
                title = this.getRebootText();
            },
            advancedViewOptions, {
                keys = orderedAdvancedKeys;
                title = this.getRebootText();
            }
        );

        if (options != simpleOptions, {
            grid.addSpanning(title, 0, 0, 2, 2)
        });

        keys.do{ |opt, i|
            var label, guiElement, val, row;

            if (options != simpleOptions, {
                row = i + 2
            }, {
                row = i
            });

            label = StaticText().string_(opt)
                .stringColor_(Color.new(0.1, 0.1, 0.1));

            val = serverOptions.tryPerform(opt.asGetter);

            guiElement = options[opt][\type].new()
                .action_{ options[opt][\modified] = true };

            grid.add(label, row, 0);
            grid.add(guiElement, row, 1);

            if (val.notNil, { guiElement.value_(val) });

            if (options == simpleOptions, {
                this.setSimpleOption(opt, guiElement)
            }, {
                if (opt == \inDevice, {
                    guiElement.items_(ServerOptions.inDevices)
                });
                if (opt == \outDevice, {
                    guiElement.items_(ServerOptions.outDevices)
                });
            });

            currentValues.add(opt -> guiElement);
        };

        ^view
    }

    /**
     * setSimpleOption Special case for server simple settings
     * @param Symbol option
     * @param mixed  GUI element
     */
    setSimpleOption { |option, element|
        option.switch(
            \latency, { element.value_(server.latency) },
            \recChannels, { element.value_(server.recChannels) },
            \recHeaderFormat, {
                element.items_(this.getHeaderFormats());
                element.value_(
                    this.getHeaderFormats().indexOf(
                        server.recHeaderFormat.asSymbol
                    )
                )
            },
            \recSampleFormat, {
                element.items_(this.getSampleFormats());
                element.value_(
                    this.getSampleFormats().indexOf(
                        server.recSampleFormat.asSymbol
                    )
                )
            }
        )
    }

    /**
     * swapView Swap between simple and advanced view
     * @param Integer buttonValue
     */
    swapView { |buttonValue|
        var simple = [simpleView, specialView];

        buttonValue.switch(
            0, { simple.do(_.visible_(true));  advancedView.visible_(false) },
            1, { simple.do(_.visible_(false)); advancedView.visible_(true) }
        )
    }

    /**
     * cancelAction
     */
    cancelAction {
        if (standalone.notNil, {
            parent.close
        }, {
            this.cancelFunction.value()
        })
    }

    /**
     * applyAction
     */
    applyAction {
        [simpleViewOptions, advancedViewOptions].do{ |options|
            options.keys.do{ |key|
                if (options[key][\modified].notNil, {
                    server.options.tryPerform(
                        key.asSetter, currentValues[key].value
                    )
                });
            }
        };

        simpleOptions.keys.do{ |key|
            var value;
            if (key == \recHeaderFormat or: { key == \recSampleFormat }, {
                value = currentValues[key].item;
            }, {
                value = currentValues[key].value;
            });
            server.tryPerform(key.asSetter, value);
        };

        if (standalone.notNil, {
            parent.close
        }, {
            applyFunction.value()
        })
    }

    /**
     * getSampleFormats
     * @return Array
     */
    getSampleFormats {
        ^[\int8, \int16, \int24, \int32, \mulaw, \alaw, \float]
    }

    /**
     * getHeaderFormats
     * @return Array
     */
    getHeaderFormats {
        ^[
            \aiff, \wav, \sun, \next, \sd2, \ircam, \raw, \mat4,
            \mat5, \paf, \svx, \nist, \voc, \w64, \pvf, \xi, \htk,
            \sds, \avr, \flac, \caf
        ]
    }
}
