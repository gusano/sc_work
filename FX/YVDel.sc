YVDel {

    *ar { |in, decayTime=2, delayScale=0.05, dev=0|
        var z, y;

//      decayTime = MouseX.kr(0,16);
//      delayScale = MouseY.kr(0.01, 1);

        z = Mix.new(in) * 0.02;
        // 8 comb delays in parallel :
        y = Mix.new(CombL.ar(in, 0.1, {0.04.rand2 + 0.05}.dup(8) * delayScale, decayTime));

        // chain of 5 allpass delays on each of two channels (10 total) :
        5.do({ y = AllpassN.ar(y, 0.050, {0.050.rand}.dup(4) + dev, 1) });

        // eliminate DC
        ^Limiter.ar(Splay.ar(LeakDC.ar(y) + (in * 0.7)), 0.9)
    }

    *test { |ouno, dos|
        "debugging".postln;
    }

    *testCapital { |ouno, dos|
        "debugging".postln;
    }


}