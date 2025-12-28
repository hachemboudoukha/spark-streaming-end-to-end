# Teen Phone Addiction - Spark Streaming Pipeline

![Spark](https://img.shields.io/badge/Apache_Spark-Streaming-E25A1C?style=for-the-badge&logo=apachespark)
![Kafka](https://img.shields.io/badge/Apache_Kafka-Event_Streaming-231F20?style=for-the-badge&logo=apachekafka)
![Scala](https://img.shields.io/badge/Scala-2.13-DC322F?style=for-the-badge&logo=scala)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-336791?style=for-the-badge&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Containerization-2496ED?style=for-the-badge&logo=docker)

A real-time data pipeline analyzing teen phone usage patterns and addiction risks. This project ingests CSV data, streams it through Kafka, processes/enriches it with Apache Spark Structured Streaming, and stores the insights in PostgreSQL.
### // les variables d'environements sont dans le repo pour le prof de spark 

## Architecture

The pipeline consists of the following components orchestrated via Docker Compose:

1.  **Zookeeper**: Manages the Kafka cluster state.
2.  **Kafka**: Serves as the message broker for real-time data.
3.  **Spark Producer**: Reads raw CSV data and publishes messages to Kafka.
4.  **Spark Consumer**: 
5.  **PostgreSQL**: Persistent storage for the processed analytics data.

##  Getting Started

### Prerequisites

- [Docker](https://www.docker.com/) and [Docker Compose](https://docs.docker.com/compose/) installed on your machine.

### Installation & Run

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    cd spark-streaming-end-to-end
    ```

2.  **Start the services:**
    This command will build the Spark applications and start all containers in the background.
    ```bash
    docker-compose up --build -d
    ```

3.  **Check the logs:**
    Monitor the producer and consumer to see the pipeline in action.
    ```bash
    # View Producer logs (sending data)
    docker-compose logs -f spark-producer

    # View Consumer logs (processing and saving data)
    docker-compose logs -f spark-consumer
    ```

4.  **Verify Data in PostgreSQL:**
    You can connect to the Postgres database to query the results.
    ```bash
    docker exec -it postgres psql -U postgres -d teen_addiction_db
    
    # Inside psql shell:
    SELECT * FROM teen_phone_data LIMIT 10;
    ```

5.  **Stop the application:**
    ```bash
    docker-compose down
    ```

##  Project Structure

```
.
├── config/              # (Deprecated/Internal) Config files
├── data/
│   └── *.csv           # Source dataset
├── sql/
│   └── schema.sql      # Database initialization script
├── src/main/scala/
│   ├── producer/       # Producer application code
│   └── consumer/       # Consumer application code
├── build.sbt           # Scala Build Tool configuration
├── Dockerfile          # Multi-stage Docker build for Spark apps
└── docker-compose.yaml # Orchestration of all services
```

## Data & Processing

The pipeline calculates a **Risk Score** and **Health Category** (Low/Moderate/High) based on:
- Daily usage hours
- Sleep hours (calculating sleep deficit)
- Physical exercise
- Bedtime screen usage

##  Visualization (Power BI)

To visualize the real-time insights processed by the pipeline, you can connect Power BI to the PostgreSQL database.

### 1. Connection Settings
Connect Power BI Desktop to PostgreSQL using the following credentials:
- **Server:** `localhost` (if running locally)
- **Port:** `5432`
- **Database:** `teen_addiction_db`
- **Authentication:** Database
- **User:** `postgres`
- **Password:** `postgrespw` (or check your `.env`)

### 2. Recommended Data Source
We have prepared a **Materialized View** optimized for visualization:
- **Table/View:** `teen_addiction_summary`
- **Connectivity Mode:** 
    - **DirectQuery:** For real-time updates (recommended).
    - **Import:** For better performance with static snapshots.

### 3. Key Metrics & Visuals
- **Addiction Trends:** Line chart using `time_bucket` (X-axis) and `avg_risk_score` (Y-axis).
- **Demographics:** Pie chart for `Gender` distribution.
- **Risk Analysis:** Stacked bar chart for `Health_Category` by `Age`.
- **KPI Cards:** Displaying `Avg Daily Usage` and `Avg Sleep Hours`.

> [!TIP]
> Use the `teen_addiction_summary` view to significantly improve dashboard performance, as it contains pre-aggregated data.

