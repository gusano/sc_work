YVIPhone {
    var <>netAddr,
        <nodes,
        <proxySpace,
        <task,
        <>responders,
        <>waitTime = 0.2,
        <emptyEnvir,
        <trackedNodes,
        <sources,
        <mixBusses;

    *new { |aProxySpace, aNetAddr|
        ^super.new.init(aProxySpace, aNetAddr)
    }

    init { |aProxySpace, aNetAddr|
        proxySpace   = aProxySpace;
        netAddr      = aNetAddr;
        nodes        = ();
        emptyEnvir   = false;
        responders   = List();
        trackedNodes = Array.newClear(6);
        sources      = this.prepareSources();
        mixBusses    = this.prepareBusses();

        this.clearPhone();
        this.addResponders();
        this.startTask();
        CmdPeriod.add({
            this.stopTask();
            this.clearPhone();
            this.freeResponders();
            this.free();
        });
    }

    free {
        nodes = responders = trackedNodes = sources = mixBusses = nil;
    }

    startTask {
        task = Task({
            inf.do{
                this.watchEnvir();
                waitTime.wait;
            }
        }).play(SystemClock)
    }

    stopTask {
        task.stop;
    }

    // add created nodes
    watchEnvir {
        var envir = proxySpace.envir;

        if (envir.size == 0 and: { emptyEnvir.not }, {
            this.clearPhone();
            emptyEnvir = true;
            ^nil;
        });
        emptyEnvir = false;
        envir.keys.do{ |key|
            var name = key.cs,
                node = envir.at(key);

            this.checkNode(name, node);
        }
    }

    checkNode { |name, node|
        var envir = proxySpace.envir,
            index = envir.size,
            found = false;

        if (trackedNodes.indexOf(this.fixName(name)).isNil, {
            this.addNode(name, node, index);
        })
    }

    addNode { |name, node, index|
        var path = "/label%".format(index);

        if (index > 6, {
            ^nil;
        });

        nodes.add(index -> (\name: name, \node: node));
        trackedNodes[index - 1] = this.fixName(name);
        netAddr.sendMsg(path, name.asString.replace("'", ""));
    }

    clearPhone {
        (1..6).do{ |n|
            var path = "/label%".format(n),
                name = "#%".format(n);
            netAddr.sendMsg(path, name);
        };
        this.resetMultiToggle();
    }

    /**
     * Create 36 Responders and 6 Ndef which will act as mix
     * so we can route several sources to the same fx in bus
     */
    addResponders {
        (1..6).do{ |x|
            (1..6).do{ |y|
                var path = "/1/multitoggle/%/%".format(x, y);

                responders.add(this.makeResponder(path, x, y));
            }
        }
    }

    freeResponders {
        responders.do(_.free);
    }

    makeResponder { |path, x, y|
        var func = { |m|
            var val = m[1].asInteger,
                mixSrc;

            try {
                switch(val,
                    0, { mixSrc = this.removeSrc(y, 7 - x) },
                    1, { mixSrc = this.addSrc(y, 7 - x) }
                );

                this.applyMix(y, mixSrc);
            } { |e|
                e.errorString.warn;
            }
        };

        ^OSCFunc({ |m| func.(m) }, path);
    }

    applyMix { |col, source|
        var bus = mixBusses[col];

        bus.source = source.reject{ |src| src.isNil }
            .mean * (1 / (source.size * 0.5));

        this.getNode(col) <<>.in bus;
    }

    addSrc { |col, row|
        sources[col][row - 1] = this.getNode(row);
        ^sources[col];
    }

    removeSrc { |col, row|
        sources[col][row - 1] = Ndef(\empty);
        ^sources[col];
    }

    resetMultiToggle {
        (1..6).do{ |x|
            (1..6).do{ |y|
                var path = "/1/multitoggle/%/%".format(x, y);

                netAddr.sendMsg(path, 0);
            }
        }
    }

    prepareSources {
        var dict = ();

        (1..6).do{ |x|
            dict[x] = Array.newClear(6);
        };

        ^dict;
    }

    prepareBusses {
        var dict = ();

        (1..6).do{ |x|
            var name = "mix%".format(x).asSymbol;

            dict[x] = Ndef(name).fadeTime_(0.2);
        }

        ^dict;
    }

    getNode { |index|
        ^proxySpace.at(this.fixName(nodes[index]['name']));
    }

    fixName { |name|
        ^name.asString.replace("'", "").asSymbol;
    }
}