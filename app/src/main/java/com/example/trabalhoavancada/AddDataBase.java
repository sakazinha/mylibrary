package com.example.trabalhoavancada;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

public class AddDataBase extends Thread{

    private DatabaseReference referencia = FirebaseDatabase.getInstance().getReference();
    private List<Region> regions;;
    private Semaphore semaphore;

    public AddDataBase(List<Region> regions, Semaphore semaphore){
        this.regions = regions;
        this.semaphore = semaphore;
    }

    public void run(){
        try {
            // Aguarda a permissão para edição da lista de regiões
            semaphore.acquire();

            for (Region region : regions) {
                // Converte a região para um mapa (HashMap) para salvar no Firebase
                DatabaseReference regionRef = referencia.child("regiao").push(); // cria uma nova chave única
                regionRef.child("nome").setValue(region.getNome());
                regionRef.child("latitude").setValue(region.getLatitude());
                regionRef.child("longitude").setValue(region.getLongitude());
                regionRef.child("usuario").setValue(region.getUsuario());
                regionRef.child("timestamp").setValue(region.getTimestamp());

                // Imprime informações sobre a região
                System.out.println("Região adicionada ao banco de dados: " + region.getNome() + " por " + region.getUsuario() + " em " + new Date(region.getTimestamp()));
            }

            // Limpa a lista de regiões após salvar no banco de dados
            regions.clear();

            // Libera a permissão após concluir a edição da lista de regiões
            semaphore.release();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
