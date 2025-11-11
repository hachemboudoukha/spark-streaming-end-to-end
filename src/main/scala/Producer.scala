import java.util.Properties
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import java.sql.{Connection, DriverManager, ResultSet}

object Producer {
  def main(args: Array[String]): Unit = {
    // Configuration Kafka
    val kafkaProps = new Properties()
    kafkaProps.put("bootstrap.servers", "localhost:9092")
    kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    kafkaProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    
    // Configuration PostgreSQL
    val dbUrl = "jdbc:postgresql://localhost:5432/spark_streaming"
    val dbUser = "postgres"
    val dbPassword = "postgres"
    
    val producer = new KafkaProducer[String, String](kafkaProps)
    val topic = "spark-streaming-topic"
    
    println("=== PRODUCER DÉMARRÉ ===")
    println(s"Base de données: $dbUrl")
    println(s"Topic Kafka: $topic")
    
    var connection: Connection = null
    
    try {
      // Connexion à PostgreSQL
      connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
      println("✅ Connecté à PostgreSQL")
      
      // Lire les données de la table
      val statement = connection.createStatement()
      val resultSet = statement.executeQuery("SELECT * FROM teen_phone_addiction ORDER BY ID")
      
      println("Lecture des données...")
      
      // Récupérer les noms de colonnes
      val metaData = resultSet.getMetaData
      val columnCount = metaData.getColumnCount
      val columns = (1 to columnCount).map(metaData.getColumnName).toArray
      
      var count = 0
      
      // Lire chaque ligne et envoyer à Kafka
      while (resultSet.next()) {
        // Construire la ligne CSV
        val values = (1 to columnCount).map { i =>
          val value = resultSet.getString(i)
          if (value == null) "" else value
        }
        val csvLine = values.mkString(",")
        
        // Envoyer à Kafka
        val record = new ProducerRecord[String, String](topic, csvLine)
        producer.send(record)
        count += 1
        
        // Afficher le progrès
        if (count % 100 == 0) {
          println(s"Envoyé: $count lignes")
        }
        
        // Pause pour simuler le streaming
        Thread.sleep(50)
      }
      
      println(s"✅ Total envoyé: $count lignes")
      
    } catch {
      case e: Exception =>
        println(s"❌ Erreur: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      if (connection != null) connection.close()
      producer.close()
      println("Producteur fermé")
    }
  }
}
