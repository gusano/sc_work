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
    var width = 340;

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
            \inDevice:              (\type: PopUpMenu, \modified: nil, \pos: 0),
            \outDevice:             (\type: PopUpMenu, \modified: nil, \pos: 1),
            \numInputBusChannels:   (\type: NumberBox, \modified: nil, \pos: 2),
            \numOutputBusChannels:  (\type: NumberBox, \modified: nil, \pos: 3),
            \sampleRate:            (\type: NumberBox, \modified: nil, \pos: 4),
            \blockSize:             (\type: NumberBox, \modified: nil, \pos: 5),
            \memSize:               (\type: NumberBox, \modified: nil, \pos: 6),
            \numAudioBusChannels:   (\type: NumberBox, \modified: nil, \pos: 7),
            \numControlBusChannels: (\type: NumberBox, \modified: nil, \pos: 8)
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
            \latency:         (\type: NumberBox, \pos: 0),
            \recChannels:     (\type: NumberBox, \pos: 1),
            \recHeaderFormat: (\type: PopUpMenu, \pos: 2),
            \recSampleFormat: (\type: PopUpMenu, \pos: 3)
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
     * getView Assign different options to a View
     * @param Dictionary options
     * @param Color      color
     * @param boolean    hasTitle
     * @param boolean    visible
     */
    getView { |options, color, hasTitle = false, visible = true|
        var view = View().background_(color);
        var grid = QGridLayout();
        var title, rowStart = 0;

        view.layout_(grid);
        view.visible_(visible);

        if (hasTitle, {
            title = this.getRebootText();
            rowStart = 2;
            grid.addSpanning(title, 0, 0, 2, 2)
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

            grid.add(label, row, 0);
            grid.add(guiElement, row, 1);

            if (val.notNil, { guiElement.value_(val) });

            if (options == simpleOptions, {
                this.setSimpleOption(key, guiElement)
            }, {
                if (key == \inDevice, {
                    try { this.setPopupItems(
                        guiElement, ServerOptions.inDevices, serverOptions.inDevice
                        )}
                });
                if (key == \outDevice, {
                    try { this.setPopupItems(
                        guiElement, ServerOptions.outDevices, serverOptions.outDevice
                    )}
                });
            });

            currentValues.add(key -> guiElement);
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

    /**
     * setPopupItems
     * @param PopUpMenu popup
     * @param Array     items
     * @param mixed     value
     */
    setPopupItems { |popup, items, value|
        var array = items.collect(_.asSymbol);
        popup.items_(items);
        popup.value_(array.indexOf(value.asSymbol));
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
                    this.setServerOption(server.options, key, options[key][\type])
                });
            }
        };

        simpleOptions.keys.do{ |key|
            this.setServerOption(server, key, simpleOptions[key][\type]);
        };

        if (standalone.notNil, {
            parent.close
        }, {
            applyFunction.value()
        })
    }

    /**
     * setServerOption
     * @param mixed  options Server or Server.options
     * @param Symbol key
     * @param mixed  type
     */
    setServerOption { |options, key, type|
        if (type == PopUpMenu, {
            options.tryPerform(
                key.asSetter, currentValues[key].item.asString
            )
        }, {
            options.tryPerform(
                key.asSetter, currentValues[key].value
            )
        });
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
