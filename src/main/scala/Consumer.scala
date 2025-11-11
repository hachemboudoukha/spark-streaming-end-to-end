import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka010._
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe

object Consumer {
  def main(args: Array[String]): Unit = {
    // Créer la session Spark
    val spark = SparkSession.builder()
      .appName("SparkStreamingConsumer")
      .master("local[*]")
      .getOrCreate()
    
    // Créer le contexte de streaming (batch toutes les 5 secondes)
    val ssc = new StreamingContext(spark.sparkContext, Seconds(5))
    
    // Configuration Kafka
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "localhost:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "spark-streaming-group",
      "auto.offset.reset" -> "latest"
    )
    
    val topic = Array("spark-streaming-topic")
    
    println("=== CONSUMER DÉMARRÉ ===")
    println("Connexion à Kafka...")
    
    // Créer le stream depuis Kafka
    val stream = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topic, kafkaParams)
    )
    
    // Traiter chaque batch de données
    stream.foreachRDD { rdd =>
      if (!rdd.isEmpty()) {
        val sparkSession = SparkSession.builder().config(rdd.sparkContext.getConf).getOrCreate()
        import sparkSession.implicits._
        
        // Convertir les messages en DataFrame simple
        val lines = rdd.map(_.value())
        val df = lines.toDF("data")
        
        val count = df.count()
        println(s"\n📦 Batch reçu: $count messages")
        
        // Afficher un échantillon
        if (count > 0) {
          println("Échantillon des données:")
          df.show(5, truncate = false)
          
          // Sauvegarder les données reçues
          df.write.mode("append").option("header", "true").csv("output/processed_data")
          println("✅ Données sauvegardées")
        }
      }
    }
    
    // Démarrer le streaming
    ssc.start()
    println("✅ Consumer prêt. Appuyez sur Ctrl+C pour arrêter.")
    ssc.awaitTermination()
  }
}
