package producer
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.storage.StorageLevel

object Producer {
  def main(args: Array[String]): Unit = {
    var spark: SparkSession = null
    try {
      println("=== [Producer] Starting ===")
      
      spark = SparkSession.builder()
        .appName("Producer")
        .master("local[*]")
        .config("spark.sql.adaptive.enabled", "true")  
        .config("spark.sql.shuffle.partitions", "2")  
        .config("spark.eventLog.enabled", "false")
        .getOrCreate()

      val topic = sys.env.getOrElse("KAFKA_TOPIC", "spark-streaming-topic")
      val csvPath = sys.env.getOrElse("CSV_PATH", "data/teen_phone_addiction_dataset.csv")
      val bootstrapServers = sys.env.getOrElse("BOOTSTRAP_SERVERS", "localhost:9092")
      val batchSize = sys.env.getOrElse("BATCH_SIZE", "100").toInt

      println(s"=== [Producer] Reading CSV: $csvPath ===")
      
      //  Lecture du CSV , header  pour obtenir les colonnes
      val dfRaw = spark.read
        .option("header", "true")
        .csv(csvPath)
      
      //  Récupération des noms de colonnes
      val columnNames = dfRaw.columns
      
      //  Préparation complète du DataFrame avec CACHE
      val dfPrepared = dfRaw
        .withColumn("rowId", monotonically_increasing_id())
        .withColumn("value", concat_ws(",", columnNames.map(col): _*)) 
        .withColumn("key", col("rowId").cast("string"))
        .withColumn("batchNum", (col("rowId") / lit(batchSize)).cast("int"))
        .select("key", "value", "batchNum")
        .persist(StorageLevel.MEMORY_AND_DISK)  //  CACHE pour éviter recalculs

      //  Une seule action pour compter le nombre de lignes 
      val rowCount = dfPrepared.count()
      println(s"=== [Producer] Read $rowCount rows ===")

      if (rowCount == 0) {
        println("=== [Producer] WARNING: No rows to send ===")
        dfPrepared.unpersist()
        return
      }

      val totalBatches = ((rowCount - 1) / batchSize + 1).toInt
      println(s"=== [Producer] Sending to Kafka: topic=$topic, servers=$bootstrapServers ===")
      println(s"=== [Producer] Batch size: $batchSize, Total batches: $totalBatches ===")

      //  Récupération des numéros de batch uniques (depuis le cache)
      val batchNumbers = dfPrepared
        .select("batchNum")
        .distinct()
        .collect()
        .map(_.getInt(0))
        .sorted

      //  Envoi par batch SANS recalcul (utilise le cache)
      batchNumbers.foreach { batchNum =>
        println(s"=== [Producer] Sending batch ${batchNum + 1}/$totalBatches ===")
        
        val batchDf = dfPrepared
          .filter(col("batchNum") === batchNum)
          .drop("batchNum")
        
        batchDf.write
          .format("kafka")
          .option("kafka.bootstrap.servers", bootstrapServers)
          .option("topic", topic)
          .mode("append")
          .save()

        println(s"=== [Producer] Batch ${batchNum + 1}/$totalBatches completed ===")
        Thread.sleep(5000)
      }

      // Libérer le cache
      dfPrepared.unpersist()

      println(s"=== [Producer] Successfully sent $rowCount messages to Kafka in $totalBatches batches ===")

    } catch {
      case e: Exception =>
        println(s"=== [Producer] ERROR: ${e.getMessage} ===")
        e.printStackTrace()
        sys.exit(1)
    } finally {
      if (spark != null) {
        try {
          spark.stop()
        } catch {
          case e: Exception =>
            println(s"=== [Producer] Warning during shutdown: ${e.getMessage} ===")
        }
      }
      println("=== [Producer] Finished ===")
    }
  }
}