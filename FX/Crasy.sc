Crasy {

    *ar { |in|
        var snd;

        snd = in.reverse.distort.ring1(
            SinOsc.ar(MouseX.kr(1, 2e3, 1), [0, pi/2])
        ) * 0.8;
        snd = CombL.ar(snd, 0.5, MouseY.kr(0, 0.49) + LFNoise2.kr(1!2, 0.01).abs, 4, 1, snd);
        ^Limiter.ar(snd, 0.8)
    }
}

Crasy2 {

    *ar { |in, x = 200, y = 0.2|
        var snd;

        snd = in.reverse.distort.ring1(
            SinOsc.ar(x.linexp(0, 1, 1, 2e3) + MouseX.kr(0, 0), [0, pi/2])
        ) * 0.8;
        snd = CombL.ar(snd, 0.5, y.linlin(0, 1, 0, 0.49) + MouseY.kr(0, 0) + LFNoise2.kr(1!2, 0.01).abs, 4, 1, snd);
        ^Limiter.ar(snd, 0.8)
    }
}