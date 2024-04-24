package com.example.trabalhoavancada;

import java.util.concurrent.BlockingQueue;

public interface ConsultaCallback {
    void onRegionsLoaded(BlockingQueue<Region> regions);
    void onCancelled();
}
