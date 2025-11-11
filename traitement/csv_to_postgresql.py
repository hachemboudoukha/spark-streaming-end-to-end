
"""
Script pour créer les tables PostgreSQL et importer le CSV
"""
import pandas as pd
import psycopg2
from psycopg2 import sql
import sys
import os

def create_table_and_import(csv_file, db_config):
    """
    Crée la table PostgreSQL et importe le CSV
    
    Args:
        csv_file: Chemin vers le fichier CSV
        db_config: Dictionnaire avec les infos de connexion DB
    """
    try:
        # Lire le CSV
        print(f"📖 Lecture du fichier CSV: {csv_file}")
        df = pd.read_csv(csv_file)
        print(f"✅ {len(df)} lignes lues")
        
        # Connexion à PostgreSQL
        print("\n🔌 Connexion à PostgreSQL...")
        conn = psycopg2.connect(
            host=db_config['host'],
            port=db_config['port'],
            database=db_config['database'],
            user=db_config['user'],
            password=db_config['password']
        )
        cursor = conn.cursor()
        print("✅ Connecté à PostgreSQL")
        
        # Créer la table
        print("\n📋 Création de la table...")
        create_table_sql = 
        CREATE TABLE IF NOT EXISTS teen_phone_addiction (
            ID INTEGER PRIMARY KEY,
            Name VARCHAR(255),
            Age INTEGER,
            Gender VARCHAR(50),
            Location VARCHAR(255),
            School_Grade VARCHAR(50),
            Daily_Usage_Hours DOUBLE PRECISION,
            Sleep_Hours DOUBLE PRECISION,
            Academic_Performance INTEGER,
            Social_Interactions INTEGER,
            Exercise_Hours DOUBLE PRECISION,
            Anxiety_Level INTEGER,
            Depression_Level INTEGER,
            Self_Esteem INTEGER,
            Parental_Control INTEGER,
            Screen_Time_Before_Bed DOUBLE PRECISION,
            Phone_Checks_Per_Day INTEGER,
            Apps_Used_Daily INTEGER,
            Time_on_Social_Media DOUBLE PRECISION,
            Time_on_Gaming DOUBLE PRECISION,
            Time_on_Education DOUBLE PRECISION,
            Phone_Usage_Purpose VARCHAR(100),
            Family_Communication INTEGER,
            Weekend_Usage_Hours DOUBLE PRECISION,
            Addiction_Level DOUBLE PRECISION
        );
        
        cursor.execute(create_table_sql)
        conn.commit()
        print("✅ Table créée")
        
        # Vider la table si elle existe déjà
        print("\n🗑️  Nettoyage des anciennes données...")
        cursor.execute("TRUNCATE TABLE teen_phone_addiction;")
        conn.commit()
        
        # Importer les données
        print("\n📥 Import des données...")
        for index, row in df.iterrows():
            insert_sql = """
            INSERT INTO teen_phone_addiction VALUES (
                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s,
                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
            )
            """
            cursor.execute(insert_sql, tuple(row))
            
            if (index + 1) % 500 == 0:
                print(f"  Importé: {index + 1} lignes")
        
        conn.commit()
        print(f"✅ {len(df)} lignes importées")
        
        # Vérifier
        cursor.execute("SELECT COUNT(*) FROM teen_phone_addiction;")
        count = cursor.fetchone()[0]
        print(f"\n✅ Total dans la base: {count} enregistrements")
        
        cursor.close()
        conn.close()
        print("\n✅ Import terminé avec succès!")
        
    except Exception as e:
        print(f"❌ Erreur: {str(e)}")
        sys.exit(1)


if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="Importer CSV vers PostgreSQL")
    parser.add_argument("--csv", required=True, help="Chemin vers le fichier CSV")
    parser.add_argument("--host", default="localhost", help="Host PostgreSQL")
    parser.add_argument("--port", default=5432, type=int, help="Port PostgreSQL")
    parser.add_argument("--database", default="spark_streaming", help="Nom de la base")
    parser.add_argument("--user", default="postgres", help="Utilisateur")
    parser.add_argument("--password", required=True, help="Mot de passe")
    
    args = parser.parse_args()
    
    db_config = {
        'host': args.host,
        'port': args.port,
        'database': args.database,
        'user': args.user,
        'password': args.password
    }
    
    create_table_and_import(args.csv, db_config)

