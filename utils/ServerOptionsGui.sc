/**
 * @file    ServerOptionsGui.sc
 * @desc    GUI utility to change or visualize default server options.
 * @author  Yvan Volochine <yvan.volochine@gmail.com>
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 * @version 0.1
 * @since   2011-11-04
 * @link    http://github.com/gusano/sc_work/tree/master/utils
 *
 * @todo alphabetically sort options (if possible),
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
     * @var View simpleView A composite view containing the basic settings
     */
    var simpleView;

    /**
     * @var View advancedView A composite view containing the advanced settings
     */
    var advancedView;

    /**
     * *new
     * @return super
     */
    *new {
        ^super.new.init()
    }

    /**
     * init Initialise default server options
     * @return self
     * @TODO: add special mode (sampleRate, devices, ...)
     */
    init {
        simpleOptions = (
            \numAudioBusChannels: (\type: Integer, \mode: "static"),
            \numControlBusChannels: (\type: Integer, \mode: "static"),
            \numInputBusChannels: (\type: Integer, \mode: "static"),
            \numOutputBusChannels: (\type: Integer, \mode: "static"),
            \maxNodes: (\type: Integer, \mode: "static"),
            \maxSynthDefs: (\type: Integer, \mode: "static"),
            \blockSize: (\type: Integer, \mode: "static"),
            \hardwareBufferSize: (\type: Integer, \mode: "static"),
            \memSize: (\type: Integer, \mode: "static"),
            \numWireBufs: (\type: Integer, \mode: "static"),
            \sampleRate: (\type: Integer, \mode: "static"),
            \inDevice: (\type: String, \mode: "static"),
            \outDevice: (\type: String, \mode: "static")
        );

        advancedOptions = (
            \protocol: (\type: String, \mode: "static"),
            \numRGens: (\type: Integer, \mode: "static"),
            \loadDefs: (\type: Boolean, \mode: "static"),
            \inputStreamsEnabled: (\type: Boolean, \mode: "static"),
            \blockAllocClass: (\type: Boolean, \mode: "static"),
            // FIXME
            //\zeroConf: (\type: "static"),
            //\restrictedPath: (\type: "static"),
            // FIXME special
            //\initialNodeID: (\type: "static"),
            \remoteControlVolume: (\type: Boolean, \mode: "static"),
            \memoryLocking: (\type: Boolean, \mode: "static")//,
            //\threads: (\type: "static")
        );

        server = Server.default;
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
        var modeButton, applyButton, cancelButton, blueColor;
        blueColor = Color.new(0.25, 0.55, 0.83);
        w = Window.new("Server Options", Rect(100, 100, 400, 410)).front;
        modeButton = Button(w, Rect(240, 10, 150, 30)).states_(
            [
                ["Advanced settings", Color.white, blueColor],
                ["Simple settings", Color.white, blueColor]
            ]
        ).action_({ |butt|
            this.swapView(butt.value)
        });
        simpleView = this.drawSettings(simpleOptions);
        advancedView = this.drawSettings(advancedOptions, false);
        cancelButton = Button(w, Rect(30, 380, 150, 20))
            .states_([["Cancel"]])
            .action_({ w.close });
        applyButton = Button(w, Rect(220, 380, 150, 20))
            .states_([["Apply (reboot server)"]]).action_{ this.applyChanges() };
    }

    /**
     * drawSettings Draws the settings and their corresponding values
     * @param Dictionary options
     * @param boolean    visible
     */
    drawSettings { |options, visible = true|
        var view = View(w, Rect(0, 50, 400, 330));
        view.addFlowLayout;
        options.keys.do{ |opt|
            var textField, val;
            val= serverOptions.tryPerform(opt.asGetter).asString;
            StaticText(view, 200@20).string_(opt);
            textField = TextField(view, 180@20).string_(val);
            currentValues.add(opt -> textField);
        };
        if (visible.not, { view.visible_(visible) });
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
        // TODO
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
