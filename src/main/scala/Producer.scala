import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, Callback, RecordMetadata}
import org.apache.spark.{SparkConf, SparkContext}
import java.util.Properties
import scala.util.{Try, Success, Failure}

object Producer {
  // Constantes
  val Topic = "spark-streaming-topic"
  val CsvPath = "data/teen_phone_addiction_dataset.csv"
  val BatchSize = 200
  val DelayMs = 3000

  def main(args: Array[String]): Unit = {
    // Configuration Kafka
    val props = new Properties()
    props.put("bootstrap.servers", sys.env.getOrElse("BOOTSTRAP_SERVERS", "localhost:9092"))
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("enable.idempotence", "true") // Idempotence
    props.put("delivery.timeout.ms", "120000") // Timeout

    // Configuration Spark
    val conf = new SparkConf()
      .setAppName("CSVProducer")
      .setMaster("local[*]")
    val sc = new SparkContext(conf)
    sc.setLogLevel("WARN")

    println("=== PRODUCER DÉMARRÉ ===")
    println(s"Topic Kafka: $Topic")

    // Lecture du CSV
    val lines = sc.textFile(CsvPath)
    val header = lines.first()
    println(s"Header détecté : $header")

    val data = lines.filter(_ != header).repartition(1) 

    // Traitement par partition
    data.foreachPartition { partition =>
      val producer = new KafkaProducer[String, String](props)
      try {
        var batch = List[String]()
        var batchId = 1
        partition.foreach { line =>
          batch = batch :+ line
          if (batch.size >= BatchSize) {
            batch.foreach { record =>
              val producerRecord = new ProducerRecord[String, String](Topic, record)
              producer.send(producerRecord, new Callback {
                //si toute est correct et ne pas interrompe la fonction 
                override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
                  if (exception != null) {
                    println(s"Erreur lors de l'envoi du message: ${exception.getMessage}")
                  }
                }
              })
            }
            println(s"[Partition] Batch $batchId envoyé (${batch.size} messages)")
            batchId += 1
            batch = List()
            Thread.sleep(DelayMs)
          }
        }
        // Envoi des messages restants dans le batch(le dernier pour feermer
        if (batch.nonEmpty) {
          batch.foreach { record =>
            producer.send(new ProducerRecord[String, String](Topic, record))
          }
          println(s"[Partition] Dernier batch envoyé (${batch.size} messages)")
        }
      } finally {
        producer.close() 
      }
    }
    sc.stop()
  }
}

