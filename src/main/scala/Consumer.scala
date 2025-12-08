import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger
import org.apache.log4j.{Level, Logger}
object Consumer {
  def main(args: Array[String]): Unit = {
    val Topic = "spark-streaming-topic"
    val BootstrapServers = sys.env.getOrElse("BOOTSTRAP_SERVERS", "localhost:9092")
    val OutputPath = "output/processed_data"
    val CheckpointPath = "output/checkpoint"
    val ConsoleCheckpointPath = "output/console_checkpoint"

    Logger.getLogger("org").setLevel(Level.WARN)

    // Spark Session
    val spark = SparkSession.builder()
      .appName("SimpleKafkaConsumer")
      .master("local[*]")
      .getOrCreate()

    // Lecture stream flux Kafka
    val df = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", BootstrapServers)
      .option("subscribe", Topic)
      .option("startingOffsets", "earliest") // Traiter tous les messages depuis le début
      .load()

    // Extraction des messages
    val messages = df.selectExpr("CAST(value AS STRING) AS line")

    // Comptage des messages (pour la console)
    val countDF = messages.groupBy().count()

    // Écriture en console
    val consoleQuery = countDF.writeStream
      .format("console")
      .outputMode("complete")
      .trigger(Trigger.ProcessingTime("5 seconds"))
      .option("truncate", "false")
      .option("checkpointLocation", ConsoleCheckpointPath) 
      .start()

    // Écriture dans un fichier CSV
    val outputQuery = messages.writeStream
      .format("csv")
      .outputMode("append")
      .option("path", OutputPath)
      .option("checkpointLocation", CheckpointPath)
      .trigger(Trigger.ProcessingTime("5 seconds"))
      .start()

    // Attente de la terminaison (avec gestion d'erreur)
    try {
      consoleQuery.awaitTermination()
      outputQuery.awaitTermination()
    } catch {
      case e: Exception =>
        println(s"Erreur lors de l'exécution du flux: ${e.getMessage}")
        consoleQuery.stop()
        outputQuery.stop()
    } finally {
      spark.stop()
    }
  }
}
