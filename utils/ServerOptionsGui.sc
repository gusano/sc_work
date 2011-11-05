/**
 * @file    ServerOptionsGui.sc
 * @desc    GUI utility to change or visualize default server options.
 * @author  Yvan Volochine <yvan.volochine@gmail.com>
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 * @version 0.1
 * @since   2011-11-04
 * @link    http://github.com/gusano/sc_work/tree/master/utils
 *
 * @todo add a server argument, alphabetically sort options (if possible),
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
     * @var Dictionary settings
     */
    var settings;

    /**
     * @var Dictionary currentValues
     */
    var currentValues;

    /**
     * @var Window w
     */
    var w;

    /**
     * @var View simpleView A view containing the basic settings
     */
    var simpleView;

    /**
     * @var View advancedView A view containing the advanced settings
     */
    var advancedView;

    /**
     * @var Integer width
     */
    var width = 300;

    /**
     * @var Integer height
     */
    var height = 410;


    /**
     * *new
     * @return super
     */
    *new { |server = nil|
        ^super.new.init(server)
    }

    /**
     * init Initialise default server options
     * @param Server aServer
     * @return self
     */
    init { |aServer|
        if (aServer.notNil, {
            server = aServer;
        }, {
            server = Server.default;
        });
        simpleOptions = (
            \numAudioBusChannels: (\type: NumberBox),
            \numControlBusChannels: (\type: NumberBox),
            \numInputBusChannels: (\type: NumberBox),
            \numOutputBusChannels: (\type: NumberBox),
            \maxNodes: (\type: NumberBox),
            \maxSynthDefs: (\type: NumberBox),
            \blockSize: (\type: NumberBox),
            \hardwareBufferSize: (\type: NumberBox),
            \memSize: (\type: NumberBox),
            \numWireBufs: (\type: NumberBox),
            \sampleRate: (\type: NumberBox),
            \inDevice: (\type: TextField),
            \outDevice: (\type: TextField)
        );

        advancedOptions = (
            \protocol: (\type: TextField),
            \numRGens: (\type: NumberBox),
            \loadDefs: (\type: CheckBox),
            \inputStreamsEnabled: (\type: TextField),
            \zeroConf: (\type: CheckBox),
            \restrictedPath: (\type: TextField),
            \initialNodeID: (\type: NumberBox),
            \remoteControlVolume: (\type: CheckBox),
            \memoryLocking: (\type: CheckBox),
            \threads: (\type: NumberBox)
        );

        serverOptions = server.options;
        settings = ();
        currentValues = ();
        this.prepareSettings();
        this.drawGui();
    }

    /**
     * drawGui Draw the main GUI
     */
    drawGui {
        var topLayout, bottomLayout;
        var infoText, modeButton, applyButton, cancelButton;

        infoText = "Options for" + server.name;

        modeButton = Button().states_([
            ["Advanced settings"], ["Simple settings"]
        ]).action_({ |butt|
            this.swapView(butt.value)
        });

        simpleView = this.drawSettings(simpleOptions);
        advancedView = this.drawSettings(advancedOptions, false);

        cancelButton = Button().states_([["Cancel"]]).action_({ w.close });
        applyButton = Button().states_([["Apply (reboot server)"]])
            .action_{ this.applyChanges() };

        topLayout = QHLayout(StaticText().string_(infoText), modeButton);
        bottomLayout = QHLayout(cancelButton, applyButton);

        w = Window.new("Server Options", Rect(100, 100, width, height)).front;
        w.layout_(QVLayout(topLayout, simpleView, advancedView, bottomLayout));
    }

    /**
     * drawSettings Draws the settings and their corresponding values
     * @param Dictionary options
     * @param boolean    visible
     */
    drawSettings { |options, visible = true|
        var view = View();
        var grid = QGridLayout();

        view.layout_(grid);
        view.visible_(visible);

        options.keys.do{ |opt, i|
            var guiElement, val;
            val = serverOptions.tryPerform(opt.asGetter);
            grid.add(StaticText().string_(opt), i, 0);
            guiElement = options[opt][\type].new();
            grid.add(guiElement, i, 1);
            if (val.notNil, { guiElement.value_(val) });
            currentValues.add(opt -> guiElement);
        };

        ^view
    }

    /**
     * swapView Swap between simple and advanced view
     * @param Integer buttonValue
     */
    swapView { |buttonValue|
        buttonValue.switch(
            0, { advancedView.visible_(false); simpleView.visible_(true) },
            1, { simpleView.visible_(false); advancedView.visible_(true) }
        )
    }

    /**
     * applyChanges
     */
    applyChanges {
        currentValues.keys.do{ |key|
            [key, currentValues[key].value].postcs
        }
    }

    /**
     * prepareSettings
     */
    prepareSettings {
        [simpleOptions, advancedOptions].do{ |options|
            options.keys.do{ |opt|
                settings.add(opt -> serverOptions.tryPerform(opt.asGetter))
            }
        }
    }
}
