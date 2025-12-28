# Explication du Pipeline Spark Streaming

Ce document détaille le rôle des composants, la trace d'exécution et les avantages de l'architecture choisie.

## 1. Rôle du Producer et du Consumer

L'architecture sépare clairement la **génération** des données de leur **traitement**.

### Le Producer (`Producer.scala`)
Il simule une source de données temps réel à partir d'un fichier statique.
*   **Lecture :** Charge le fichier `data/teen_phone_addiction_dataset.csv`.
*   **Préparation ("Batching") :** Découpe les données en paquets (batches) de 100 lignes (configurable via `BATCH_SIZE`).
*   **Simulation Temps Réel :** Envoie ces paquets vers **Kafka** (topic `spark-streaming-topic`) avec une pause de **5 secondes** entre chaque envoi.
*   **Objectif :** Imiter des utilisateurs qui envoient leurs statistiques d'utilisation en continu.

### Le Consumer (`Consumer.scala`)
C'est le moteur ETL (Extract, Transform, Load).
*   **Lecture (Extract) :** Écoute le topic Kafka en continu.
*   **Transformation :**
    *   **Parsing :** Convertit la chaîne CSV brute en colonnes typées.
    *   **Nettoyage (`cleanData`) :** Supprime les données incomplètes et normalise les textes.
    *   **Enrichissement (`enrichData`) :** Ajoute de la valeur métier :
        *   `Total_Screen_Time` : Somme des temps d'écran.
        *   `Risk_Score` : Formule basée sur l'utilisation et le comportement.
        *   `Health_Category` : Classement (LOW, MEDIUM, HIGH).
*   **Chargement (Load) :** Sauvegarde les résultats :
    1.  **PostgreSQL :** Pour la visualisation (PowerBI).
    2.  **Fichiers CSV (local) :** Pour sauvegarde/backup.

## 2. Trace d'exécution du Pipeline (End-to-End)

Chemin parcouru par une donnée, de la source à la base de données :

1.  **Source (CSV)** : Fichier sur disque.
2.  **Spark Producer (JVM 1)** :
    *   Charge le CSV en DataFrame.
    *   Ajoute ID unique et numéro de batch.
    *   **Action :** Envoie les données vers le broker Kafka.
3.  **Kafka (Cluster)** :
    *   Reçoit et stocke le message dans le topic `spark-streaming-topic`.
4.  **Spark Consumer (JVM 2)** :
    *   **Trigger (5s)** : Récupère les nouveaux messages.
    *   **Processing** : Parsing -> Cleaning -> Risk Calculation.
    *   **Action (`foreachBatch`)** :
        *   INSERT dans PostgreSQL (table `teen_phone_data`).
        *   Écriture fichier dans `output/batch_xyz.csv`.
5.  **Analytics (PostgreSQL / PowerBI)** :
    *   Données disponibles pour les tableaux de bord.

## 3. Pourquoi utiliser Spark ?

Avantages critiques pour un système de production :

1.  **Traitement Unifié (Batch & Streaming) :** Même code et logique pour l'historique et le temps réel (**Internal Structured Streaming**).
2.  **Tolérance aux Pannes :** Utilisation de **Checkpoints** pour garantir qu'aucune donnée n'est perdue ou dupliquée ("Exactly-once") même en cas de crash.
3.  **Scalabilité :** Architecture distribuable sur des centaines de nœuds pour gérer des volumes massifs.
4.  **Écosystème :** Intégration native et optimisée avec Kafka, JDBC (Postgres), et formats Big Data.
