# Projet Spark Streaming - Analyse de Données en Temps Réel

Projet dans le cadre du cours spark de streaming a l'esgi avec **Apache Spark**, **Kafka**, **PostgreSQL** avec **Scala** pour analyser un dataset d'addiction au téléphone chez les adolescents.

---

## Architecture

```
┌─────────────────┐
│   CSV File      │
│  (Dataset)      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Python Script  │  ← Crée les tables et importe CSV
│ csv_to_postgres │     vers PostgreSQL
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  PostgreSQL     │  ← Base de données
│   (Database)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Producer      │  ← Lit depuis PostgreSQL
│   (Scala)       │     et envoie à Kafka
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│     Kafka       │  ← Message Broker
│   (Topic)       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Consumer      │  ← Reçoit les données
│ Spark Streaming │     et les sauvegarde
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Output CSV    │  ← Résultats sauvegardés
│  (Processed)    │
└─────────────────┘
```

### Composants

1. **Python Script** : Crée les tables PostgreSQL et importe le CSV
2. **PostgreSQL** : Base de données qui stocke le dataset .sql
3. **Producer** : Lit depuis PostgreSQL et envoie chaque ligne à Kafka
4. **Kafka** : Message broker qui stocke temporairement les messages
5. **Consumer** : Reçoit les données de Kafka et les sauvegarde
6. **Output** : Résultats sauvegardés dans `output/processed_data/`

---

## 📋 Prérequis

Avant de commencer, installez :

- ✅ **Java 11+** (pour Spark)
- ✅ **Docker Desktop** (pour Kafka et PostgreSQL)
- ✅ **IntelliJ IDEA** (avec plugin Scala)
- ✅ **Python 3.8+** (pour le script d'import)
- ✅ **sbt** (gestionnaire de dépendances Scala)

---

## 🚀 Installation et Configuration

### Étape 1 : Vérifier les prérequis

```bash
# Vérifier Java
java -version

# Vérifier Docker
docker --version

# Vérifier Python
python --version
```

### Étape 2 : Démarrer les services (Kafka, Zookeeper, PostgreSQL)

Ouvrez un terminal dans le dossier du projet :

```bash
# Démarrer tous les services
docker-compose up -d
```

**Attendez 20-30 secondes** que tous les services démarrent.

Vérifier que tout est prêt :
```bash
docker-compose ps
```

Vous devriez voir :
- `spark_streaming-zookeeper-1` → Up
- `spark_streaming-kafka-1` → Up
- `spark_streaming-postgres-1` → Up

### Étape 3 : Importer le CSV vers PostgreSQL

Installer les dépendances Python :
```bash
pip install -r traitement/requirements.txt
```

Importer le CSV :
```bash
python traitement/csv_to_postgresql.py \
  --csv data/teen_phone_addiction_dataset.csv \
  --password postgres
```

**Options disponibles** :
- `--host` : Host PostgreSQL (défaut: localhost)
- `--port` : Port PostgreSQL (défaut: 5432)
- `--database` : Nom de la base (défaut: spark_streaming)
- `--user` : Utilisateur (défaut: postgres)
- `--password` : Mot de passe (requis)

Le script va :
1. ✅ Lire le fichier CSV
2. ✅ Créer la table `teen_phone_addiction` dans PostgreSQL
3. ✅ Importer toutes les données

---

## ▶️ Exécution de l'Application

### Étape 1 : Lancer le Consumer (d'abord !)

Dans **IntelliJ IDEA** :

1. Ouvrez le fichier : `src/main/scala/Consumer.scala`
2. Clic droit sur `object Consumer`
3. Sélectionnez **Run 'Consumer'**

Vous devriez voir :
```
=== CONSUMER DÉMARRÉ ===
Connexion à Kafka...
✅ Consumer prêt. Appuyez sur Ctrl+C pour arrêter.
```

**⚠️ Important** : Laissez le Consumer en cours d'exécution (ne fermez pas la console).

### Étape 2 : Lancer le Producer

Dans **IntelliJ IDEA** (nouvelle fenêtre ou onglet) :

1. Ouvrez le fichier : `src/main/scala/Producer.scala`
2. Clic droit sur `object Producer`
3. Sélectionnez **Run 'Producer'**

Vous verrez :
```
=== PRODUCER DÉMARRÉ ===
Base de données: jdbc:postgresql://localhost:5432/spark_streaming
Topic Kafka: spark-streaming-topic
✅ Connecté à PostgreSQL
Lecture des données...
Envoyé: 100 lignes
Envoyé: 200 lignes
...
✅ Total envoyé: 3000 lignes
```

### Étape 3 : Observer les Résultats

Dans la console du **Consumer**, toutes les 5 secondes, vous verrez :

```
📦 Batch reçu: 50 messages
Échantillon des données:
+-----------------------------------+
|data                               |
+-----------------------------------+
|1,John Doe,16,Male,New York,10th...|
...
✅ Données sauvegardées
```

Les données sont sauvegardées dans `output/processed_data/`

---

## 📁 Structure des Fichiers

```
spark_streaming/
│
├── data/                                    # Données d'entrée
│   └── teen_phone_addiction_dataset.csv     # Dataset CSV
│
├── src/main/scala/                         # Code source
│   ├── Producer.scala                      # Lit PostgreSQL → Envoie à Kafka
│   └── Consumer.scala                     # Reçoit Kafka → Sauvegarde
│
├── traitement/                             # Scripts Python
│   ├── csv_to_postgresql.py                # Import CSV → PostgreSQL
│   └── requirements.txt                    # Dépendances Python
│
├── output/                                 # Résultats (créé automatiquement)
│   └── processed_data/                     # Données traitées
│
├── build.sbt                               # Dépendances du projet
├── docker-compose.yml                      # Configuration services
└── README.md                               # Ce fichier
```

---

## 🔍 Comprendre le Code

### Producer.scala

**Rôle** : Lire depuis PostgreSQL et envoyer à Kafka

```scala
// 1. Connexion à PostgreSQL
val connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)

// 2. Lire les données
val resultSet = statement.executeQuery("SELECT * FROM teen_phone_addiction")

// 3. Pour chaque ligne, construire CSV et envoyer à Kafka
while (resultSet.next()) {
  val csvLine = values.mkString(",")
  val record = new ProducerRecord[String, String](topic, csvLine)
  producer.send(record)
}
```

### Consumer.scala

**Rôle** : Recevoir de Kafka et sauvegarder

```scala
// 1. Créer le stream Kafka
val stream = KafkaUtils.createDirectStream(...)

// 2. Traiter chaque batch (toutes les 5 secondes)
stream.foreachRDD { rdd =>
  // Convertir en DataFrame
  val df = lines.toDF("data")
  
  // Afficher et sauvegarder
  df.show(5)
  df.write.csv("output/processed_data")
}
```

### csv_to_postgresql.py

**Rôle** : Créer les tables et importer le CSV

```python
# 1. Lire le CSV
df = pd.read_csv(csv_file)

# 2. Connexion à PostgreSQL
conn = psycopg2.connect(...)

# 3. Créer la table
cursor.execute("CREATE TABLE IF NOT EXISTS ...")

# 4. Importer les données
for row in df.iterrows():
    cursor.execute("INSERT INTO ... VALUES ...")
```

---

## 🛠️ Commandes Utiles

### Gérer les services Docker

```bash
# Démarrer tous les services
docker-compose up -d

# Vérifier l'état
docker-compose ps

# Voir les logs PostgreSQL
docker-compose logs postgres

# Voir les logs Kafka
docker-compose logs kafka

# Arrêter tous les services
docker-compose down
```

### Vérifier PostgreSQL

```bash
# Se connecter à PostgreSQL
docker exec -it spark_streaming-postgres-1 psql -U postgres -d spark_streaming

# Dans psql, vérifier les données
SELECT COUNT(*) FROM teen_phone_addiction;
SELECT * FROM teen_phone_addiction LIMIT 5;
\q
```

### Dans IntelliJ

- **Run** : Clic droit → Run
- **Stop** : Bouton rouge dans la console
- **Voir les logs** : Console en bas de l'écran

---

## ❓ Dépannage

### Erreur : "Connection refused" (PostgreSQL)

**Cause** : PostgreSQL n'est pas démarré

**Solution** :
```bash
docker-compose up -d postgres
# Attendre 10 secondes
```

### Erreur : "Table does not exist"

**Cause** : Le CSV n'a pas été importé

**Solution** :
```bash
python traitement/csv_to_postgresql.py \
  --csv data/teen_phone_addiction_dataset.csv \
  --password postgres
```

### Erreur : "Connection refused" (Kafka)

**Cause** : Kafka n'est pas démarré

**Solution** :
```bash
docker-compose up -d kafka zookeeper
# Attendre 15 secondes
```

### Le Consumer ne reçoit rien

**Cause** : Le Producer n'a pas été lancé ou a fini

**Solution** : 
1. Vérifiez que le Producer est en cours d'exécution
2. Relancez le Producer

### Erreur de compilation dans IntelliJ

**Solution** :
1. **File** → **Invalidate Caches / Restart**
2. **View** → **Tool Windows** → **sbt** → **Reload sbt project**

---

## 📊 Dataset

Le dataset `teen_phone_addiction_dataset.csv` contient :
- **3000+ enregistrements** d'adolescents
- **25 colonnes** : ID, Name, Age, Gender, Daily Usage Hours, Addiction Level, etc.

**Colonnes importantes** :
- `Daily_Usage_Hours` : Heures d'utilisation quotidienne
- `Addiction_Level` : Niveau d'addiction (0-10)
- `Academic_Performance` : Performance académique (0-100)
- `Gender` : Genre (Male, Female, Other)

---

## 📝 Notes Importantes

- ⚠️ **Lancer le Consumer AVANT le Producer**
- ⚠️ **Tous les services Docker doivent être démarrés avant de lancer l'application**
- ⚠️ **Le CSV doit être importé dans PostgreSQL avant de lancer le Producer**
- ⚠️ **Le Consumer traite les données par batch de 5 secondes**
- ✅ Les résultats sont sauvegardés automatiquement dans `output/`

---

## 🚀 Prochaines Étapes

Pour aller plus loin :

1. **Modifier le Consumer** : Ajouter des analyses dans `Consumer.scala`
2. **Changer l'intervalle de batch** : `Seconds(5)` → `Seconds(10)`
3. **Ajouter des filtres** : Filtrer certaines données avant sauvegarde
4. **Exporter vers d'autres bases** : Modifier le Consumer pour écrire dans PostgreSQL

---

## 📞 Support

Si vous rencontrez des problèmes :

1. Vérifiez que tous les services sont démarrés : `docker-compose ps`
2. Vérifiez les logs : `docker-compose logs`
3. Vérifiez que le CSV a été importé : Se connecter à PostgreSQL et vérifier

---

**Bon streaming ! 🎉**
