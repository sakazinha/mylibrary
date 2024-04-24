package com.example.trabalhoavancada;

import androidx.annotation.NonNull;

import com.example.mylibrary.CalculationRegion;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class Region {
    private String nome;
    private double latitude;
    private double longitude;
    private String usuario;
    private long timestamp;
    private static BlockingQueue<Region> regionQueue = new LinkedBlockingQueue<>();
    private static Semaphore semaphore = new Semaphore(1);
    private static DatabaseReference referencia = FirebaseDatabase.getInstance().getReference();

    // Construtor da classe Region
    public Region(String nome, double latitude, double longitude, String usuario, long timestamp) {
        this.nome = nome;
        this.latitude = latitude;
        this.longitude = longitude;
        this.usuario = usuario;
        this.timestamp = timestamp;
    }
    public Region() {

    }

    // Método para adicionar uma região à fila
    public void addRegionToQueue() {
        // Cria uma nova instância da região com os dados fornecidos
        Region region = new Region(nome, latitude, longitude, usuario, timestamp);

        // Imprime informações sobre a região
        System.out.println("Tentando adicionar região: " + region.getNome() + " por " + region.getUsuario() + " em " + new Date(region.getTimestamp()));

        //Verificar a região na fila
        RegionQueueManager regionQueueManager = new RegionQueueManager(regionQueue, semaphore, nome, latitude, longitude, usuario, timestamp);
        // Inicie a thread
        regionQueueManager.start();
    }

    // Método para adicionar as regiões da fila ao banco de dados
    public void addAllToDataBase() {
        try {
            // Crie uma lista para armazenar as regiões a serem adicionadas ao banco de dados
            List<Region> regionsList = new ArrayList<>();

            // Coleta todas as regiões da fila
            while (!regionQueue.isEmpty()) {
                Region region = regionQueue.take();
                regionsList.add(region);
            }

            // Inicia uma nova thread para adicionar todas as regiões ao banco de dados
            AddDataBase addDataBaseThread = new AddDataBase(regionsList, semaphore);
            addDataBaseThread.start();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    // Métodos getters para os atributos da classe Region
    public String getNome() {
        return nome;
    }

    public String getUsuario() {
        return usuario;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
