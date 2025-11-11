# Projet Spark Streaming - (PostgreSQL + Kafka)

Projet de streaming avec **Apache Spark**, **Kafka**, **PostgreSQL** en **Scala** qui analyse un dataset d’addiction au téléphone chez les adolescents.
first verison in local
---

## Architecture

```
┌─────────────────┐
│   CSV File      │
│  (Dataset)      │
└────────┬────────┘
         │
         ▼
┌──────────────────────────────┐
│  Script Python (CSV → SQL)   │  ← Génère un .sql
└────────┬─────────────────────┘
         │
         ▼
┌─────────────────┐
│  PostgreSQL     │  ← Charge le .sql
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Producer      │  ← Lit PostgreSQL → Kafka
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│     Kafka       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Consumer      │  ← Lit Kafka → écrit `output/processed_data`
└─────────────────┘
```

---

## Prérequis

- Java 11+ et sbt
- Python 3.8+
- PostgreSQL 
- Kafka 3.x + Zookeeper 
- IntelliJ IDEA (plugin Scala) recommandé

---

## Linux - Étapes

1) Services

```bash
sudo systemctl status postgresql
sudo systemctl status zookeeper
sudo systemctl status kafka
# si besoin
sudo systemctl start postgresql
sudo systemctl start zookeeper
sudo systemctl start kafka
```

2) Base PostgreSQL (port 5433 dans cet exemple)

```bash
psql -U postgres -h localhost -p 5433 -c "CREATE DATABASE spark_streaming;" || true
psql -U postgres -h localhost -p 5433 -d spark_streaming -c "\dt" || true
```

3) Générer le SQL depuis le CSV

```bash
python3 traitement/csv_to_postgresql.py \
  --csv data/teen_phone_addiction_dataset.csv \
  --table teen_phone_addiction \
  --output data/teen_phone_addiction.sql
```

4) Charger le SQL dans PostgreSQL

```bash
psql -U postgres -h localhost -p 5433 -d spark_streaming -f data/teen_phone_addiction.sql
```

5) Lancer l’application

- Terminal A (Consumer):

```bash
sbt "runMain Consumer"
```

- Terminal B (Producer):

```bash
sbt "runMain Producer"
```

Les résultats sont écrits dans `output/processed_data/`.

---

## Windows - Étapes

1) Prérequis
- Installer Java 11+, sbt, Python 3.x, PostgreSQL (par défaut port 5432), Kafka + Zookeeper
- Ajouter `psql.exe` (PostgreSQL) et `sbt` au PATH
- Décompresser Kafka (par ex. `C:\kafka`) et utiliser les scripts `.bat`

2) Démarrer Zookeeper et Kafka

PowerShell (deux fenêtres):

```powershell
# Fenêtre 1 - Zookeeper
cd C:\kafka
bin\windows\zookeeper-server-start.bat config\zookeeper.properties

# Fenêtre 2 - Kafka
cd C:\kafka
bin\windows\kafka-server-start.bat config\server.properties
```

(Optionnel) Créer le topic si besoin:

```powershell
cd C:\kafka
bin\windows\kafka-topics.bat --create --topic spark-streaming-topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

3) Créer la base PostgreSQL

```powershell
psql -U postgres -h localhost -p 5432 -c "CREATE DATABASE spark_streaming;"
psql -U postgres -h localhost -p 5432 -d spark_streaming -c "\dt"
```

4) Générer le SQL depuis le CSV

```powershell
python traitement\csv_to_postgresql.py --csv data\teen_phone_addiction_dataset.csv --table teen_phone_addiction --output data\teen_phone_addiction.sql
```

5) Charger le SQL dans PostgreSQL

```powershell
psql -U postgres -h localhost -p 5432 -d spark_streaming -f data\teen_phone_addiction.sql
```

6) Lancer l’application

PowerShell ou CMD (deux fenêtres):

```powershell
# Fenêtre A
sbt "runMain Consumer"

# Fenêtre B
sbt "runMain Producer"
```

Les résultats sont écrits dans `output\processed_data\`.

---

## Structure du projet

```
spark_streaming/
│
├── data/
│   ├── teen_phone_addiction_dataset.csv
│   └── teen_phone_addiction.sql            # généré par le script
│
├── src/main/scala/
│   ├── Producer.scala
│   └── Consumer.scala
│
├── traitement/
│   └── csv_to_postgresql.py                # convertit CSV → SQL (TEXT)
│
├── output/
│   └── processed_data/                     # créé à l’exécution
│
├── build.sbt
└── README.md
```

---

## Notes

- Le convertisseur CSV→SQL génère une table avec toutes les colonnes en TEXT (robuste).
- Utiliser le bon port PostgreSQL (exemples: Linux 5433, Windows défaut 5432).
- Lancer le Consumer avant le Producer.