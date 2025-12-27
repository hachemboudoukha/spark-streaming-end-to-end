package consumer

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.streaming.Trigger
import java.util.Properties

object Consumer {

  /**
   * Modèle de données pour le streaming
   */
  val dataSchema: StructType = StructType(Array(
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

  def main(args: Array[String]): Unit = {
    println("=== [Consumer] Starting Optimized Pipeline ===")

    val spark = SparkSession.builder()
      .appName("OptimizedConsumer")
      .master("local[*]")
      // Performance Tunings
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .config("spark.sql.adaptive.enabled", "true")
      .config("spark.sql.shuffle.partitions", "4")
      .config("spark.streaming.stopGracefullyOnShutdown", "true")
      .getOrCreate()

    import spark.implicits._

    // Configuration via Env
    val topic = sys.env.getOrElse("KAFKA_TOPIC", "spark-streaming-topic")
    val bootstrapServers = sys.env.getOrElse("BOOTSTRAP_SERVERS", "localhost:9092")
    val checkpointLocation = sys.env.getOrElse("CHECKPOINT_DIR", "checkpoint/consumer")

    try {
      // 1. Lecture
      val kafkaRawDF = spark.readStream
        .format("kafka")
        .option("kafka.bootstrap.servers", bootstrapServers)
        .option("subscribe", topic)
        .option("startingOffsets", "latest")
        .option("maxOffsetsPerTrigger", "1000")
        .option("failOnDataLoss", "false")
        .load()

      // 2. Traitement Modulaire
      val parsedDF = parseKafkaData(kafkaRawDF, dataSchema)
      val cleanedDF = cleanData(parsedDF)
      val enrichedDF = enrichData(cleanedDF)

      // 3. Écriture (PostgreSQL + CSV Backup)
      val query = enrichedDF.writeStream
        .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
          processBatch(batchDF, batchId)
        }
        .outputMode("append")
        .option("checkpointLocation", checkpointLocation)
        .trigger(Trigger.ProcessingTime("5 seconds"))
        .start()

      query.awaitTermination()

    } catch {
      case e: Exception =>
        println(s"=== [Consumer] FATAL ERROR: ${e.getMessage} ===")
        e.printStackTrace()
        sys.exit(1)
    } finally {
      spark.stop()
    }
  }

  /**
   * Désérialise les données Kafka
   */
  def parseKafkaData(df: DataFrame, schema: StructType): DataFrame = {
    df.selectExpr("CAST(value AS STRING)", "timestamp AS kafka_timestamp")
      .select(
        from_csv(col("value"), schema, Map("header" -> "false")).as("data"),
        col("kafka_timestamp")
      )
      .select("data.*", "kafka_timestamp")
  }

  /**
   * Nettoyage et validation
   */
  def cleanData(df: DataFrame): DataFrame = {
    df.filter(col("ID").isNotNull && col("Age").isNotNull)
      .withColumn("processing_time", current_timestamp())
      // Normalisation string
      .withColumn("Gender", upper(trim(col("Gender"))))
      .withColumn("Addiction_Level", upper(trim(col("Addiction_Level"))))
      // Bornage des valeurs aberrantes
      .withColumn("Age", when(col("Age").between(10, 100), col("Age")).otherwise(lit(null)))
  }

  /**
   * Enrichissement métier
   */
  def enrichData(df: DataFrame): DataFrame = {
    df.withColumn("Total_Screen_Time", 
        col("Time_on_Social_Media") + col("Time_on_Gaming") + col("Time_on_Education"))
      .withColumn("Risk_Score", 
        (col("Daily_Usage_Hours") * 1.5 + col("Phone_Checks_Per_Day") / 20.0).cast(DoubleType))
      .withColumn("Health_Category",
        when(col("Risk_Score") < 3, "LOW")
          .when(col("Risk_Score") < 7, "MEDIUM")
          .otherwise("HIGH"))
  }

  /**
   * Sink Multi-destination : PostgreSQL via JDBC + CSV
   */
  def processBatch(batchDF: DataFrame, batchId: Long): Unit = {
    if (!batchDF.isEmpty) {
      val count = batchDF.count()
      println(s"\n" + "=" * 60)
      println(s"=== [Consumer] Batch: $batchId | Rows: $count")
      println("-" * 60)

      // Affichage d'un aperçu des colonnes clés
      batchDF.select("ID", "Age", "Daily_Usage_Hours", "Risk_Score", "Health_Category")
        .show(5, truncate = false)

      // Résumé statistique du batch
      batchDF.groupBy("Health_Category").count().show()

      // Configuration JDBC
      val jdbcUrl = sys.env.getOrElse("JDBC_URL", "jdbc:postgresql://postgres:5432/teen_addiction_db")
      val connectionProperties = new Properties()
      connectionProperties.setProperty("user", sys.env.getOrElse("JDBC_USER", "postgres"))
      connectionProperties.setProperty("password", sys.env.getOrElse("JDBC_PASSWORD", "postgrespw"))
      connectionProperties.setProperty("driver", "org.postgresql.Driver")

      try {
        // Enregistrement PostgreSQL
        batchDF.write
          .mode("append")
          .jdbc(jdbcUrl, "teen_phone_data", connectionProperties)
        
        // Backup CSV local pour audit
        batchDF.write
          .mode("overwrite")
          .option("header", "true")
          .csv(s"output/batch_$batchId")

        println(s"=== [Consumer] Status: SUCCESS (DB & CSV synchronized)")
      } catch {
        case e: Exception =>
          println(s"=== [Consumer] Status: FAILED | Error: ${e.getMessage}")
      }
      println("=" * 60 + "\n")
    }
  }
}
 