# Spark Streaming Project

Ce projet met en place un pipeline de données en temps réel utilisant **Kafka** et **Spark Streaming**.

## 🏗 Architecture

![Architecture](first%20architechture.png)

Le flux de données est simple :
1.  **Producer** : Lit un fichier CSV et envoie les données vers Kafka.
2.  **Kafka** : Reçoit et stocke les messages.
3.  **Consumer** : Lit les messages depuis Kafka et les sauvegarde dans le dossier `output/`.

**Technologies** : Scala, Apache Spark, Apache Kafka, Docker.

## 🚀 Comment exécuter

### Via Docker (Recommandé)
Tout est automatisé avec Docker Compose.

1.  **Démarrer :**
    ```bash
    docker-compose up --build
    ```
2.  **Arrêter :**
    ```bash
    docker-compose down
    ```

### En local (Développement)
Si vous préférez lancer les scripts Scala manuellement :

1.  **Lancer Kafka :** `docker-compose up -d zookeeper kafka`
2.  **Lancer Consumer :** `sbt "runMain Consumer"`
3.  **Lancer Producer :** `sbt "runMain Producer"`
