/**
 * @file    YVSlider.sc
 * @desc    Very basic implementation of EZSlider compatible with Qt
 * @author  Yvan Volochine <yvan.volochine@gmail.com>
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 * @version 0.1
 * @since   2012-04-18
 * @link    http://github.com/gusano/sc_work/tree/master/utils
 *
 * @usage   l = YVSlider("hola", \freq.asSpec, { |x| x.postcs })
 * @todo    orientation, fancy colors, ...
 */

YVSlider {

    var <layout, <label, <slider, <numbox, <spec;

    *new { |label, spec, action|
        ^super.new.init(label, spec, action)
    }

    // returns a QLayout
    init { |aLabel, aSpec, action|
        spec   = aSpec;
        label  = this.prGetText(aLabel);
        slider = this.prGetSlider();
        numbox = this.prGetNumbox();
        layout = this.prGetLayout();

        (spec.step == 0).if { numbox.scroll_step_(0.01) };
        this.prSetGuiAction(action);

        ^layout
    }

    prGetText { |string|
        ^StaticText()
            .string_(string)
            .minWidth_(40)
            .maxWidth_(50);
    }

    prGetSlider {
        ^Slider()
            .orientation_(\horizontal)
            .value_(spec.unmap(spec.default));
    }

    prGetNumbox {
        ^NumberBox()
            .maxWidth_(80)
            .value_(spec.default)
            .clipLo_(spec.clipLo)
            .clipHi_(spec.clipHi);
    }

    prGetLayout {
        ^HLayout(label, [slider, stretch: 1], numbox);
    }

    prSetGuiAction { |action|
        slider.action_{ |sl|
            action.(spec.map(sl.value));
            numbox.value_(spec.map(sl.value));
        };
        numbox.action_{ |num|
            action.(num.value);
            slider.value_(spec.unmap(num.value))
        }
    }

}
