package edu.dio.zanetti;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class Function {
    @FunctionName("heroes")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        
        if (request.getHttpMethod().equals(HttpMethod.POST)) {
            context.getLogger().info("Requisition POST recognized");
            return Optional.ofNullable(request.getBody().get())
                .map(body -> {
                    SuperheroRequest hero = createSuper(body).orElseThrow();
                    context.getLogger().info("Succefully mapped body to Java POJO");
                    BlobContainerClient containerClient = StorageConnector.getInstance();

                    context.getLogger().info("Succefully received Blob Container instance");
                    context.getLogger().info("Receiving base64 image from body and converting");
                    byte[] imageBytes = Base64.getDecoder().decode(hero.getImageBase64());
                    context.getLogger().info("Creating or replacing existent blobs");
                    String blobName = hero.getName().replaceAll("\s+", "_") + ".png";
                    BlobClient blobClient = containerClient.getBlobClient(blobName);
                    
                    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
                        BlobHttpHeaders headers = new BlobHttpHeaders().setContentType("image/jpeg");
                        blobClient.upload(inputStream, imageBytes.length, true);
                        blobClient.setHttpHeaders(headers);
                        context.getLogger().info("Image successfully uploaded to Blob Storage");
                    } catch (Exception e) {
                        context.getLogger().severe("Error uploading image: " + e.getMessage());
                        return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to upload image").build();
                    }

                    SuperheroResponse heroResponse = querySuper(hero, blobClient.getBlobUrl()).get();
                    CosmosAsyncClient cosmosClient = CosmosConnector.getInstance();
                    CosmosAsyncDatabase database = cosmosClient.getDatabase("ToDoList");
                    CosmosAsyncContainer container = database.getContainer("Items");
                    

                    container.createItem(heroResponse).subscribe(
                        response -> context.getLogger().info("Successfully stored hero in Cosmos DB"),
                        error -> context.getLogger().severe("Error storing hero in Cosmos DB: " + error.getMessage())
                    );

                    return request.createResponseBuilder(HttpStatus.OK).body(heroResponse).build();
                })
                .orElseGet(() -> request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Passe as características do herói a ser publicado: name, power, alter, imageUrl").build());
        }
        context.getLogger().info("It's not a POST request, going to GET pathway");
        final String queryId = request.getQueryParameters().get("id");

        if (queryId == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Passe o ID a ser buscado na consulta").build();
        }
        context.getLogger().info("Parameter ID recognized");
        context.getLogger().info("Connecting to Cosmos Client");
        CosmosAsyncClient cosmosClient = CosmosConnector.getInstance();
        CosmosAsyncDatabase database = cosmosClient.getDatabase("ToDoList");
        CosmosAsyncContainer container = database.getContainer("Items");
        context.getLogger().info("Connected to Cosmos Client");
        SuperheroResponse heroResponse;
        try {
            context.getLogger().info("Trying to read item from Cosmos DB");
            heroResponse = container.readItem(queryId, PartitionKey.NONE, SuperheroResponse.class)
                .map(CosmosItemResponse::getItem)
                .toFuture()
                .get();
            ObjectMapper mapper = new ObjectMapper();
            context.getLogger().info("Mapping Cosmos item found into Java POJO");
            String jsonResponse = mapper.writeValueAsString(heroResponse);
            return request.createResponseBuilder(HttpStatus.OK).header("Content-Type", "application/json").body(jsonResponse).build();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Fail to convert found hero into return object").build();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Fail to convert found hero into return object").build();
        }
    }

    private Optional<SuperheroRequest> createSuper(String hero) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return Optional.of(mapper.readValue(hero, SuperheroRequest.class));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Optional.empty();
        } 
    }

    private Optional<SuperheroResponse> querySuper(SuperheroRequest hero, String imgUrl) {
        SuperheroResponse heroResponse = new SuperheroResponse();
        heroResponse.setId(UUID.randomUUID().toString());
        heroResponse.setName(hero.getName());
        heroResponse.setPower(hero.getPower());
        heroResponse.setAlias(hero.getAlias());
        heroResponse.setImageUrl(imgUrl);
        return Optional.of(heroResponse);
    }
}
