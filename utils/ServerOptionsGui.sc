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
     * @var Dictionary simpleOptions
     */
    var simpleOptions;

    /**
     * @var Dictionary advancedOptions
     */
    var advancedOptions;

    /**
     * @var Dictionary recordingOptions Options that are set when server is booted
     */
    var recordingOptions;

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
     * @var View specialView A view containing the special recording settings
     */
    var specialView;

    /**
     * @var Integer width Main view width
     */
    var width = 300;

    /**
     * @var Integer height Main view height
     */
    var height = 420;

    /**
     * @var Boolean standalone If no parent is given, we run in standalone mode
     */
    var standalone;

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

        cancelFunction = {};

        simpleOptions = (
            \numAudioBusChannels:   (\type: NumberBox, \modified: false),
            \numControlBusChannels: (\type: NumberBox, \modified: false),
            \numInputBusChannels:   (\type: NumberBox, \modified: false),
            \numOutputBusChannels:  (\type: NumberBox, \modified: false),
            \blockSize:             (\type: NumberBox, \modified: false),
            \memSize:               (\type: NumberBox, \modified: false),
            \sampleRate:            (\type: NumberBox, \modified: false),
            \inDevice:              (\type: TextField, \modified: false),
            \outDevice:             (\type: TextField, \modified: false)
        );

        advancedOptions = (
            \verbosity:            (\type: NumberBox, \modified: false),
            \maxNodes:             (\type: NumberBox, \modified: false),
            \maxSynthDefs:         (\type: NumberBox, \modified: false),
            \numWireBufs:          (\type: NumberBox, \modified: false),
            \hardwareBufferSize:   (\type: NumberBox, \modified: false),
            \protocol:             (\type: TextField, \modified: false),
            \numRGens:             (\type: NumberBox, \modified: false),
            \loadDefs:             (\type: CheckBox,  \modified: false),
            \inputStreamsEnabled:  (\type: TextField, \modified: false),
            \outputStreamsEnabled: (\type: TextField, \modified: false),
            \zeroConf:             (\type: CheckBox,  \modified: false),
            \restrictedPath:       (\type: TextField, \modified: false),
            \initialNodeID:        (\type: NumberBox, \modified: false),
            \remoteControlVolume:  (\type: CheckBox,  \modified: false),
            \memoryLocking:        (\type: CheckBox,  \modified: false)
        );

        recordingOptions = (
            \recChannels:     (\type: NumberBox, \modified: false),
            \recHeaderFormat: (\type: PopUpMenu, \modified: false),
            \recSampleFormat: (\type: PopUpMenu, \modified: false)
        );

        orderedSimpleKeys = OrderedIdentitySet[
            \inDevice, \outDevice, \numInputBusChannels, \numOutputBusChannels,
            \sampleRate, \blockSize, \memSize, \numAudioBusChannels,
            \numControlBusChannels
        ];

        orderedAdvancedKeys = OrderedIdentitySet[
            \verbosity, \maxNodes, \maxSynthDefs, \numWireBufs,
            \hardwareBufferSize, \protocol, \loadDefs, \inputStreamsEnabled,
            \outputStreamsEnabled, \numRGens, \restrictedPath, \initialNodeID,
            \remoteControlVolume, \memoryLocking
        ];

        if (Server.program.asString.endsWith("supernova")) {
            advancedOptions.put(\threads, (\type: NumberBox, \modified: false));
            orderedAdvancedKeys.add(\threads);
        };

        serverOptions = server.options;
        currentValues = ();
        this.drawGui();
    }

    /**
     * drawGui
     * Draw the main GUI: one QVLayout containing one QHLayout for the top button,
     * one QGridLayout for the options and one QHLayout for the bottom buttons.
     */
    drawGui {
        var mainView, topLayout, bottomLayout;
        var infoText, modeButton, applyButton, cancelButton;

        infoText = StaticText().string_("Options for" + server.name);

        modeButton = Button().states_([
            ["Advanced settings"], ["Simple settings"]
        ]).action_({ |butt|
            this.swapView(butt.value)
        });

        simpleView = this.drawSettings(simpleOptions);
        specialView = this.drawSettings(recordingOptions);
        advancedView = this.drawSettings(advancedOptions, false);

        cancelButton = Button().states_([["Cancel"]])
            .action_({ this.cancelAction });
        applyButton = Button().states_([["Apply (reboot server)"]])
            .action_{ this.applyChanges() };

        topLayout = QHLayout(infoText, modeButton);
        bottomLayout = QHLayout(cancelButton, applyButton);

        mainView = View(parent);
        mainView.bounds_(bounds).layout_(
            QVLayout(
                topLayout, specialView, simpleView, advancedView, bottomLayout
            )
        );
    }

    /**
     * drawSettings Draws the settings and their corresponding values
     * @param Dictionary options
     * @param boolean    visible
     */
    drawSettings { |options, visible = true|
        var view = View().background_(Color.new(0.58, 0.69, 0.75));
        var grid = QGridLayout();
        var keys;

        view.layout_(grid);
        view.visible_(visible);

        options.switch(
            simpleOptions,    { keys = orderedSimpleKeys },
            advancedOptions,  { keys = orderedAdvancedKeys },
            recordingOptions, { keys = recordingOptions.keys }
        );

        keys.do{ |opt, i|
            var label, guiElement, val;

            label = StaticText().string_(opt)
                .stringColor_(Color.new(0.1, 0.1, 0.1));

            val = serverOptions.tryPerform(opt.asGetter);

            guiElement = options[opt][\type].new();
            guiElement.action_{ options[opt][\modified] = true };

            grid.add(label, i, 0);
            grid.add(guiElement, i, 1);

            if (val.notNil, { guiElement.value_(val) });

            if (options == recordingOptions, {
                this.setRecordingOption(opt, guiElement)
            });

            currentValues.add(opt -> guiElement);
        };

        ^view
    }

    /**
     * setRecordingOption Special case for server recording settings
     * @param Symbol option
     * @param mixed  GUI element
     */
    setRecordingOption { |option, element|
        option.switch(
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
     * applyChanges
     * Only apply modified settings for all options (except recording options
     * which are set anyway) and reboot the server
     */
    applyChanges {
        [simpleOptions, advancedOptions].do{ |options|
            options.keys.do{ |key|
                if (options[key][\modified], {
                    server.options.tryPerform(
                        key.asSetter, currentValues[key].value
                    );
                })
            }
        };

        recordingOptions.keys.do{ |key|
            var value;
            if (key == \recHeaderFormat or: { key == \recSampleFormat }, {
                value = currentValues[key].item;
            }, {
                value = currentValues[key].value;
            });
            server.tryPerform(key.asSetter, value);
        };

        server.reboot;
        //parent.close;
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
