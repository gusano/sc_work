+ Server {
    meterCorner { |alwaysOnTop = true|
        var bounds = Rect(1232, 518, 134, 230);

        ^this.meter.window.bounds_(bounds).alwaysOnTop_(alwaysOnTop);
    }
}