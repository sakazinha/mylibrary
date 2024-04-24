package com.example.trabalhoavancada;

import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

public class AddRegion extends Thread {
    private BlockingQueue<Region> regionQueue;
    private Semaphore semaphore;
    private String locationName;
    private double latitude;
    private double longitude;
    private long timestamp;
    private String user;

    public AddRegion(BlockingQueue<Region> regionQueue, Semaphore semaphore, String locationName, double latitude, double longitude,long timestamp, String user) {
        this.regionQueue = regionQueue;
        this.semaphore = semaphore;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.user = user;
    }

    @Override
    public void run() {
        try {
            // Adquire a permissão do semáforo antes de adicionar a região à fila
            semaphore.acquire();

            // Cria a região com os dados fornecidos
            Region region = new Region(locationName, latitude, longitude, user, System.currentTimeMillis());

            // Adicione a região à fila
            regionQueue.put(region);

            // Libere a permissão do semáforo após adicionar a região à fila
            semaphore.release();

            // Imprime uma mensagem informando que a região foi adicionada à fila
            System.out.println("Região adicionada à fila: " + region.getNome() + " por " + region.getUsuario() + " em " + new Date(region.getTimestamp()));
            System.out.println("Tamanho da fila após adicionar região: " + regionQueue.size());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
