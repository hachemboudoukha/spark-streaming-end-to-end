package consumer

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.streaming.Trigger
import java.util.Properties

object Consumer {
  def main(args: Array[String]): Unit = {
    println("=== [Consumer] Starting ===")

    val spark = SparkSession.builder()
      .appName("Consumer")
      .master("local[*]")
      .config("spark.sql.adaptive.enabled", "true")
      .config("spark.sql.shuffle.partitions", "4")
      .config("spark.streaming.stopGracefullyOnShutdown", "true")
      .getOrCreate()

    import spark.implicits._

    // Configuration
    val topic = sys.env.getOrElse("KAFKA_TOPIC", "spark-streaming-topic")
    val bootstrapServers = sys.env.getOrElse("BOOTSTRAP_SERVERS", "localhost:9092")
    val checkpointLocation = sys.env.getOrElse("CHECKPOINT_DIR", "checkpoint/consumer")
    
    // Configuration PostgreSQL
    val jdbcUrl = sys.env.getOrElse("JDBC_URL", "jdbc:postgresql://localhost:5432/teen_addiction_db")
    val jdbcUser = sys.env.getOrElse("JDBC_USER", "postgres")
    val jdbcPassword = sys.env.getOrElse("JDBC_PASSWORD", "postgrespw")
    val jdbcTable = sys.env.getOrElse("JDBC_TABLE", "teen_phone_data")

    println(s"=== [Consumer] Reading from Kafka: topic=$topic, servers=$bootstrapServers ===")

    // Schéma des données
    val schema = StructType(Array(
      StructField("ID", IntegerType, nullable = true),
      StructField("Name", StringType, nullable = true),
      StructField("Age", IntegerType, nullable = true),
      StructField("Gender", StringType, nullable = true),
      StructField("Location", StringType, nullable = true),
      StructField("School_Grade", StringType, nullable = true),
      StructField("Daily_Usage_Hours", DoubleType, nullable = true),
      StructField("Sleep_Hours", DoubleType, nullable = true),
      StructField("Academic_Performance", StringType, nullable = true),
      StructField("Social_Interactions", StringType, nullable = true),
      StructField("Exercise_Hours", DoubleType, nullable = true),
      StructField("Anxiety_Level", StringType, nullable = true),
      StructField("Depression_Level", StringType, nullable = true),
      StructField("Self_Esteem", StringType, nullable = true),
      StructField("Parental_Control", StringType, nullable = true),
      StructField("Screen_Time_Before_Bed", DoubleType, nullable = true),
      StructField("Phone_Checks_Per_Day", IntegerType, nullable = true),
      StructField("Apps_Used_Daily", IntegerType, nullable = true),
      StructField("Time_on_Social_Media", DoubleType, nullable = true),
      StructField("Time_on_Gaming", DoubleType, nullable = true),
      StructField("Time_on_Education", DoubleType, nullable = true),
      StructField("Phone_Usage_Purpose", StringType, nullable = true),
      StructField("Family_Communication", StringType, nullable = true),
      StructField("Weekend_Usage_Hours", DoubleType, nullable = true),
      StructField("Addiction_Level", StringType, nullable = true)
    ))

    try {
      // Lecture depuis Kafka
      val kafkaDF = spark.readStream
        .format("kafka")
        .option("kafka.bootstrap.servers", bootstrapServers)
        .option("subscribe", topic)
        .option("startingOffsets", "earliest")
        .option("maxOffsetsPerTrigger", "1000") // Contrôle du débit
        .load()

      println("=== [Consumer] Kafka stream connected ===")

      // Parsing et nettoyage des données
      val parsedDF = kafkaDF
        .selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)", "timestamp")
        .select(
          col("key"),
          from_csv(col("value"), schema, Map.empty[String, String]).as("data"),
          col("timestamp").as("kafka_timestamp")
        )
        .select("key", "data.*", "kafka_timestamp")

      // Nettoyage et enrichissement des données
      val cleanedDF = parsedDF
        .filter(col("ID").isNotNull && col("Age").isNotNull) // Filtrer les nulls critiques
        .withColumn("processing_time", current_timestamp())
        .withColumn("Age", when(col("Age") < 10 || col("Age") > 19, null).otherwise(col("Age"))) // Validation âge
        .withColumn("Daily_Usage_Hours", 
          when(col("Daily_Usage_Hours") < 0 || col("Daily_Usage_Hours") > 24, null)
            .otherwise(col("Daily_Usage_Hours")))
        .withColumn("Sleep_Hours", 
          when(col("Sleep_Hours") < 0 || col("Sleep_Hours") > 24, null)
            .otherwise(col("Sleep_Hours")))
        // Normalisation des catégories
        .withColumn("Gender", upper(trim(col("Gender"))))
        .withColumn("Academic_Performance", upper(trim(col("Academic_Performance"))))
        .withColumn("Anxiety_Level", upper(trim(col("Anxiety_Level"))))
        .withColumn("Depression_Level", upper(trim(col("Depression_Level"))))
        .withColumn("Addiction_Level", upper(trim(col("Addiction_Level"))))
        // Calculs dérivés pour l'analyse
        .withColumn("Total_Screen_Time", 
          col("Time_on_Social_Media") + col("Time_on_Gaming") + col("Time_on_Education"))
        .withColumn("Sleep_Deficit", 
          when(col("Age").between(13, 18), lit(9.0) - col("Sleep_Hours"))
            .otherwise(lit(8.0) - col("Sleep_Hours")))
        .withColumn("Risk_Score", 
          (col("Daily_Usage_Hours") * 2 + 
           col("Phone_Checks_Per_Day") / 10 + 
           col("Screen_Time_Before_Bed") * 1.5 - 
           col("Exercise_Hours") * 2) / 5)
        .withColumn("Health_Category",
          when(col("Risk_Score") < 2, "LOW_RISK")
            .when(col("Risk_Score") < 4, "MODERATE_RISK")
            .otherwise("HIGH_RISK"))

      println("=== [Consumer] Data cleaning and enrichment completed ===")

      // Fonction pour écrire dans PostgreSQL
      def writeToPostgres(batchDF: org.apache.spark.sql.DataFrame, batchId: Long): Unit = {
        if (!batchDF.isEmpty) {
          println(s"=== [Consumer] Writing batch $batchId with ${batchDF.count()} rows to PostgreSQL ===")
          
          val connectionProperties = new Properties()
          connectionProperties.put("user", jdbcUser)
          connectionProperties.put("password", jdbcPassword)
          connectionProperties.put("driver", "org.postgresql.Driver")

          batchDF.write
            .mode("append")
            .jdbc(jdbcUrl, jdbcTable, connectionProperties)
          
          println(s"=== [Consumer] Batch $batchId successfully written ===")
        } else {
          println(s"=== [Consumer] Batch $batchId is empty, skipping write ===")
        }
      }

      // Démarrage du streaming avec écriture en micro-batches
      val query = cleanedDF.writeStream
        .foreachBatch(writeToPostgres _)
        .outputMode("append")
        .option("checkpointLocation", checkpointLocation)
        .trigger(Trigger.ProcessingTime("5 seconds")) // Traite toutes les 5 secondes
        .start()

      println("=== [Consumer] Streaming query started ===")
      println(s"=== [Consumer] Checkpoint location: $checkpointLocation ===")
      println(s"=== [Consumer] Writing to PostgreSQL: $jdbcUrl/$jdbcTable ===")
      println("=== [Consumer] Press Ctrl+C to stop ===")
      
      // Attendre l'arrêt
      query.awaitTermination()

    } catch {
      case e: Exception =>
        println(s"=== [Consumer] ERROR: ${e.getMessage} ===")
        e.printStackTrace()
        sys.exit(1)
    } finally {
      spark.stop()
      println("=== [Consumer] Finished ===")
    }
  }
} 