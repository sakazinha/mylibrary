package com.example.trabalhoavancada;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.mylibrary.CalculationRegion;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class DataBaseQueueManager extends Thread{
    private DatabaseReference referencia = FirebaseDatabase.getInstance().getReference();
    private BlockingQueue<Region> regions;
    private Semaphore semaphore;
    private String nome;
    private double latitude;
    private double longitude;
    private String usuario;
    private long timestamp;

    public DataBaseQueueManager(BlockingQueue<Region> regions, Semaphore semaphore, String locationName, double latitude, double longitude, String usuario, long timestamp) {
        this.regions = regions;
        this.semaphore = semaphore;
        this.nome = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.usuario = usuario;
        this.timestamp = timestamp;
    }

    public void run(){
        try{
            // Adquira a permissão do semáforo antes de acessar a lista
            semaphore.acquire();

            queryDataBase(new ConsultaCallback() {
                @Override
                public void onRegionsLoaded(BlockingQueue<Region> regionsFromDataBase) {
                    Log.d("DEBUG", "Entrou no método onRegionsLoaded");

                    if (regionsFromDataBase == null || nome == null) {
                        Log.d("DEBUG", "Lista de regiões do banco de dados ou nome é nulo.");
                        semaphore.release();
                        return;
                    }

                    boolean regionExists = false;
                    for (Region region : regionsFromDataBase) {
                        Log.d("Consulta Banco de Dados", "Região do Banco de Dados - Nome: " + region.getNome());
                        if (region.getNome() != null && region.getNome().equals(nome)) {
                            regionExists = true;
                            break;
                        }
                    }

                    if (!regionExists) {
                        // Verificar se a nova região está a menos de 30 metros de distância de outras regiões na lista local e do banco de dados
                        boolean tooClose = checkRegionProximity(latitude, longitude, regionsFromDataBase);
                        if (!tooClose) {
                            // Adicionar o objeto Region à lista de regiões local
                            AddRegion addRegionThread = new AddRegion(regions, semaphore, nome, latitude, longitude, timestamp, usuario);
                            addRegionThread.start();
                            semaphore.release();

                        } else {
                            // Se a nova região estiver muito próxima de outra região, registrar uma mensagem no log
                            Log.d("Consulta Banco de Dados ", "A nova região está muito próxima de outra região do Banco");
                        }
                    } else {
                        // Se a região já existir, registrar uma mensagem no log
                        Log.d("Consulta Banco de Dados", "Esta região já está na lista do Banco de Dados");
                    }

                    //Libere a permissão do semáforo após acessar a lista
                    semaphore.release();
                }

                @Override
                public void onCancelled() {
                    // Tratar o cancelamento da consulta
                    Log.d("Consulta Banco de Dados", "Consulta cancelada");
                    // Libere a permissão do semáforo após acessar a lista
                    semaphore.release();
                    Log.d("Consulta Banco de Dados", "Semáforo do Banco liberado");

                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Log.d("Consulta Banco de Dados", "Thread Finalizada");
    }

    private void queryDataBase(final ConsultaCallback callback){
        DatabaseReference regiao = referencia.child("regiao"); // Obtém uma referência para o nó "regioes" no banco de dados
        BlockingQueue<Region> list = new LinkedBlockingQueue<>(); // Cria uma lista para armazenar as regiões obtidas do banco de dados
        regiao.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                BlockingQueue<Region> lista = new LinkedBlockingQueue<>(); // Cria uma lista temporária para armazenar as regiões obtidas do banco de dados
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    // Extrai os dados (nome, latitude, longitude, timestamp, usuário) de cada região no banco de dados
                    String name = childSnapshot.child("nome").getValue(String.class);
                    double latitude = childSnapshot.child("latitude").getValue(Double.class);
                    double longitude = childSnapshot.child("longitude").getValue(Double.class);
                    Long timestamp = childSnapshot.child("timestamp").getValue(Long.class);
                    String user = childSnapshot.child("user").getValue(String.class);

                    // Cria um objeto Region com os dados extraídos
                    Region region = new Region(name, latitude, longitude, user, timestamp);
                    lista.add(region); // Adiciona o objeto Region à lista temporária
                }
                // Notificar o callback com a lista de regiões após a conclusão da consulta
                callback.onRegionsLoaded(lista);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Em caso de erro na leitura do banco de dados, registra uma mensagem de log
                Log.i("Consulta Banco de Dados", "Erro na leitura do Banco de Dados" + error);
                // Notificar o callback sobre o cancelamento da consulta
                callback.onCancelled();
            }
        });
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
