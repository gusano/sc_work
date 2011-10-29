// found formula on MusicDSP.org (or similar)

Distorsion {
    *ar { |in, amount = 1|
        ^atan(in * amount) * (1.0 / atan(amount))
    }
}