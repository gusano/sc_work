YVDelay {
    *ar { |in|
        9.do {
            in = AllpassL.ar(in, 0.3, { 0.2.rand + 0.1 } ! 2, 5)
        };

        ^in.tanh
    }
}
