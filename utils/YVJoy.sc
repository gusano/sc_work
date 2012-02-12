YVJoy {

    classvar <vendorId = 1103;

    var <dev, <actions, <keys;


    *new {
        ^super.new.init();
    }

    init {
        keys = (1: 307, 2: 306, 3: 304, 4: 305);
        this.buildDeviceList();
    }

    free {
        actions.clear;
        keys.clear;
        dev.close;
        GeneralHID.stopEventLoop;
    }

    buildDeviceList {
        var found;
        GeneralHID.buildDeviceList;
        found = GeneralHID.findBy(this.class.vendorId);
        dev = GeneralHID.open(found);
        GeneralHID.startEventLoop();
    }

    // TODO: toggle mode
    mapTo { |id, funcOn, funcOff = nil|
        dev.slots[1].at(keys[id]).action_{ |v|
            if (v.value.asInteger > 0, {
                funcOn.value
            }, {
                if (funcOff.notNil, {
                    funcOff.value
                })
            })
        }
    }

    unmap { |id|
        dev.slots[1].at(keys[id]).action_{}
    }
}
