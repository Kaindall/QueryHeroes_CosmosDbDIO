package edu.dio.zanetti;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;

public class StorageConnector {
    private static BlobContainerClient instance;

    private StorageConnector(){};

    public static BlobContainerClient getInstance() {
        System.out.println("Criando acesso ao Blob Storage");

        String credential = System.getenv("STORAGE_KEY");
    
        if (instance == null) {
            instance = new BlobContainerClientBuilder()
                .endpoint("https://diocompleteapib6fe.blob.core.windows.net")
                .sasToken(credential)
                .containerName("dio-superheroes-image")
                .buildClient();
        }
        return instance;
    }
}
