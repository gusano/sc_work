+ Server {
    meterCorner { |alwaysOnTop=true, width=134, height=230|
        var bounds, left, top, screen = Window.screenBounds;

        left   = screen.width - width;
        top    = screen.height - height - 20;
        bounds = Rect(left, top, width, height);

        ^this.meter.window.bounds_(bounds).alwaysOnTop_(alwaysOnTop);
    }
}