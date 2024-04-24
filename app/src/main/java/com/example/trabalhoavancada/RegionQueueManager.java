package com.example.trabalhoavancada;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.mylibrary.CalculationRegion;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class RegionQueueManager extends Thread{
    private BlockingQueue<Region> regionQueue;
    private Semaphore semaphore;
    private String locationName;
    private double latitude;
    private double longitude;
    private String usuario;
    private long timestamp;

    public RegionQueueManager(BlockingQueue<Region> regions, Semaphore semaphore, String locationName, double latitude, double longitude, String usuario, long timestamp) {
        this.regionQueue = regions;
        this.semaphore = semaphore;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.usuario = usuario;
        this.timestamp = timestamp;
    }

    public void run(){
        try {
            // Adquira a permissão do semáforo antes de acessar a lista
            semaphore.acquire();

            boolean regionExists = false;

            // Verifica se a fila está vazia
            if (regionQueue.isEmpty()) {
                Log.d("Consulta Lista de Regiões", "A lista está vazia, verificando banco de dados.");

                //Verificar a região no banco de dados
                DataBaseQueueManager dataBaseQueueManager = new DataBaseQueueManager(regionQueue, semaphore, locationName, latitude, longitude, usuario, timestamp);
                // Inicie a thread
                dataBaseQueueManager.start();
                //Libere a permissão do semáforo após iniciar a thread
                semaphore.release();

            } else {
                // Verifica se a região já existe na fila
                regionExists = regionQueue.contains(new Region(locationName, latitude, longitude, usuario, timestamp));

                if (!regionExists) {
                    // Verificar se a nova região está a menos de 30 metros de distância de outras regiões na lista local e do banco de dados
                    boolean tooClose = checkRegionProximity(latitude, longitude, regionQueue);
                    if (!tooClose) {
                        //Verificar a região no banco de dados
                        DataBaseQueueManager dataBaseQueueManager = new DataBaseQueueManager(regionQueue, semaphore, locationName, latitude, longitude, usuario, timestamp);
                        // Inicie a thread
                        dataBaseQueueManager.start();

                    } else {
                        // Se a nova região estiver muito próxima de outra região, registrar uma mensagem no log
                        Log.d("Consulta Lista de Regiões", "A nova região está muito próxima de outra região da lista de regiões");
                    }
                } else {
                    // Se a região já existir, registrar uma mensagem no log
                    Log.d("Consulta Lista de Regiões", "Esta região já está na lista de regiões");
                }

                //Libere a permissão do semáforo após concluir as verificações
                semaphore.release();
            }

            // Libere a permissão do semáforo após acessar a lista
            semaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Log.d("Consulta Lista de Regiões", "Thread Finalizada");
    }

    private boolean checkRegionProximity(double latitude, double longitude, BlockingQueue<Region> regions) {
        CalculationRegion distancia = new CalculationRegion(); // Utiliza um objeto GeoCalculator para calcular a distância
        for (Region region : regions) { // Percorre todas as regiões na lista
            double distance = distancia.calculateDistance(region.getLatitude(), region.getLongitude(), latitude, longitude); // Calcula a distância entre a nova região e a região atual na lista
            if (distance < 30) { // Se a distância for menor que 30 metros, retorna verdadeiro
                return true;
            }
        }
        return false; // Caso contrário, retorna falso
    }
}

