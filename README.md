<h1>API de Heróis</h1>
<hr>
<h2>Tecnologias Utilizadas:</h2>
<ul>
  <li>Java 17</li>
  <li>Azure Cosmos DB</li>
  <li>Azure Functions</li>
  <li>Azure Blob Storage</li>
  <li>Padrão REST (apenas GET e POST)</li>
</ul>
<hr>
<h2>Como utilizar:</h2>
<p>Para recuperar as informações: name, power, alias e url da imagem do super-herói</p>
<h4>GET http://dio-queryhero.azurewebsites.net/api/heroes?id=352d2ada-8431-4868-ac1a-ef4aaa1301b3</h4><br>
<p>Para criar um novo super-herói dentro do Azure Cosmos DB e também armazenar a imagem no Blob Storage. O valor passado da imagem deve ser em base64</p>
<h4>POST http://dio-queryhero.azurewebsites.net/api/heroes<br>Body:<br></h4>

```json
  {
    "name": "Spiderman",
    "power": "Spider Sense, Web, Super Strength",
    "alias": "Peter Parker",
    "imageBase64": "..."
  }
```
