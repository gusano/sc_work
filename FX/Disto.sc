// from Batuhan on the SC list
Disto {

    *ar { | audio, amount = 0.99, freq = 3800 |
        var in, amCoef = 2 * amount / (1-amount);

        in = HPF.ar(audio, 400) * 0.5;

        ^MidEQ.ar(
            LPF.ar(
                (1+amCoef) * in/(1+(amCoef*in.abs)),
                freq + [0, 100]
            ) * 0.5,
            120,
            0.7,
            8
        );
    }
}
