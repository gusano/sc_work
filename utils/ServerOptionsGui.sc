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
     * @var Window w
     */
    var w;

    /**
     * @var View view The composite view containing the settings
     */
    var view;

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
     */
    init {
        simpleOptions = (
            \numAudioBusChannels: \static,
            \numControlBusChannels: \static,
            \numInputBusChannels: \static,
            \numOutputBusChannels: \static,
            \maxNodes: \static,
            \maxSynthDefs: \static,
            \blockSize: \static,
            \hardwareBufferSize: \static,
            \memSize: \static,
            \numWireBufs: \static,
            \sampleRate: \static,
            \inDevice: \static,
            \outDevice: \static
        );

        advancedOptions = (
            \protocol: \static,
            \numRGens: \static,
            \loadDefs: \static,
            \inputStreamsEnabled: \static,
            \blockAllocClass: \static,
            \zeroConf: \static,
            \restrictedPath: \static,
            \initialNodeID: \static,
            \remoteControlVolume: \static,
            \memoryLocking: \static
        );

        server = Server.default;
        serverOptions = server.options;
        this.drawGui();
    }

    /**
     * drawGui Draw the main GUI
     */
    drawGui {
        var button, blueColor;
        blueColor = Color.new(0.25, 0.55, 0.83);
        w = Window.new("Server Options", Rect(100, 100, 400, 400)).front;
        button = Button(w, Rect(240, 10, 150, 30)).states_(
            [
                ["Advanced settings", Color.white, blueColor],
                ["Simple settings", Color.white, blueColor]
            ]
        ).action_({ |butt|
            this.swapView(butt.value)
        });
        this.drawSettings(simpleOptions);
    }

    /**
     * drawSettings Draws the settings and their corresponding values
     * @param Dictionary options
     */
    drawSettings { |options|
        if (view.notNil, { view.remove });
        view = CompositeView(w, Rect(0, 50, 400, 350));
        view.addFlowLayout;
        options.keys.do{ |opt|
            var val = serverOptions.tryPerform(opt.asGetter).asString;
            StaticText(view, 200@20).string_(opt);
            TextField(view, 180@20).string_(val);
        }
    }

    /**
     * swapView Swap between simple and advanced view
     * @param Integer buttonValue
     */
    swapView { |buttonValue|
        buttonValue.switch(
            0, { this.drawSettings(simpleOptions) },
            1, { this.drawSettings(advancedOptions) }
        )
    }
}
