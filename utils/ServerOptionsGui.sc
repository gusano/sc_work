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
            \numAudioBusChannels: (\type: NumberBox, \modified: false),
            \numControlBusChannels: (\type: NumberBox, \modified: false),
            \numInputBusChannels: (\type: NumberBox, \modified: false),
            \numOutputBusChannels: (\type: NumberBox, \modified: false),
            \maxNodes: (\type: NumberBox, \modified: false),
            \maxSynthDefs: (\type: NumberBox, \modified: false),
            \blockSize: (\type: NumberBox, \modified: false),
            \hardwareBufferSize: (\type: NumberBox, \modified: false),
            \memSize: (\type: NumberBox, \modified: false),
            \numWireBufs: (\type: NumberBox, \modified: false),
            \sampleRate: (\type: NumberBox, \modified: false),
            \inDevice: (\type: TextField, \modified: false),
            \outDevice: (\type: TextField, \modified: false)
        );

        advancedOptions = (
            \protocol: (\type: TextField, \modified: false),
            \numRGens: (\type: NumberBox, \modified: false),
            \loadDefs: (\type: CheckBox, \modified: false),
            \inputStreamsEnabled: (\type: TextField, \modified: false),
            \zeroConf: (\type: CheckBox, \modified: false),
            \restrictedPath: (\type: TextField, \modified: false),
            \initialNodeID: (\type: NumberBox, \modified: false),
            \remoteControlVolume: (\type: CheckBox, \modified: false),
            \memoryLocking: (\type: CheckBox, \modified: false)
        );
        if (Server.program.asString.endsWith("supernova")) {
            advancedOptions.put(\threads, (\type: NumberBox, \modified: false))
        };

        serverOptions = server.options;
        currentValues = ();
        this.drawGui();
    }

    /**
     * drawGui Draw the main GUI
     */
    drawGui {
        var topLayout, bottomLayout;
        var infoText, modeButton, applyButton, cancelButton;

        infoText = StaticText().string_("Options for" + server.name);

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

        topLayout = QHLayout(infoText, modeButton);
        bottomLayout = QHLayout(cancelButton, applyButton);

        w = Window.new("Server Options").front;
        w.bounds_(
            Rect(
                (Window.screenBounds.width / 2) - (width / 2),
                (Window.screenBounds.height / 2) - (height / 2),
                width,
                height
            )
        ).layout_(QVLayout(topLayout, simpleView, advancedView, bottomLayout));
    }

    /**
     * drawSettings Draws the settings and their corresponding values
     * @param Dictionary options
     * @param boolean    visible
     */
    drawSettings { |options, visible = true|
        var view = View().background_(Color.new(0.58, 0.69, 0.75));
        var grid = QGridLayout();

        view.layout_(grid);
        view.visible_(visible);

        options.keys.do{ |opt, i|
            var label, guiElement, val;

            label = StaticText().string_(opt)
                .stringColor_(Color.new(0.1, 0.1, 0.1));
            val = serverOptions.tryPerform(opt.asGetter);
            guiElement = options[opt][\type].new();
            guiElement.action_{ |el| options[opt][\modified] = true };

            grid.add(label, i, 0);
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
     * applyChanges Only apply modified settings and reboot the server
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
        server.reboot;
        w.close;
    }

}
