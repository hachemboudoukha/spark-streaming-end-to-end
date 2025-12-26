-- Création de la base de données (à exécuter manuellement si nécessaire)
-- CREATE DATABASE teen_addiction_db;

-- Connexion à la base de données (à exécuter manuellement)
-- \c teen_addiction_db;

-- Suppression de la table si elle existe (reset)
DROP TABLE IF EXISTS teen_phone_data CASCADE;

-- Table principale
CREATE TABLE teen_phone_data (
    record_id SERIAL PRIMARY KEY,
    key VARCHAR(50),
    ID INTEGER,
    Name VARCHAR(255),
    Age INTEGER,
    Gender VARCHAR(20),
    Location VARCHAR(255),
    School_Grade VARCHAR(20),
    Daily_Usage_Hours DOUBLE PRECISION,
    Sleep_Hours DOUBLE PRECISION,
    Academic_Performance VARCHAR(50),
    Social_Interactions VARCHAR(50),
    Exercise_Hours DOUBLE PRECISION,
    Anxiety_Level VARCHAR(50),
    Depression_Level VARCHAR(50),
    Self_Esteem VARCHAR(50),
    Parental_Control VARCHAR(50),
    Screen_Time_Before_Bed DOUBLE PRECISION,
    Phone_Checks_Per_Day INTEGER,
    Apps_Used_Daily INTEGER,
    Time_on_Social_Media DOUBLE PRECISION,
    Time_on_Gaming DOUBLE PRECISION,
    Time_on_Education DOUBLE PRECISION,
    Phone_Usage_Purpose VARCHAR(100),
    Family_Communication VARCHAR(50),
    Weekend_Usage_Hours DOUBLE PRECISION,
    Addiction_Level VARCHAR(50),
    kafka_timestamp TIMESTAMP,
    processing_time TIMESTAMP,
    Total_Screen_Time DOUBLE PRECISION,
    Sleep_Deficit DOUBLE PRECISION,
    Risk_Score DOUBLE PRECISION,
    Health_Category VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index pour performances
CREATE INDEX idx_processing_time ON teen_phone_data(processing_time);
CREATE INDEX idx_addiction_level ON teen_phone_data(Addiction_Level);
CREATE INDEX idx_health_category ON teen_phone_data(Health_Category);
CREATE INDEX idx_age ON teen_phone_data(Age);
CREATE INDEX idx_gender ON teen_phone_data(Gender);
CREATE INDEX idx_risk_score ON teen_phone_data(Risk_Score);

-- Vue matérialisée pour Power BI
DROP MATERIALIZED VIEW IF EXISTS teen_addiction_summary;

CREATE MATERIALIZED VIEW teen_addiction_summary AS
SELECT 
    DATE_TRUNC('minute', processing_time) as time_bucket,
    Gender,
    Addiction_Level,
    Health_Category,
    COUNT(*) as total_records,
    ROUND(AVG(Daily_Usage_Hours)::numeric, 2) as avg_daily_usage,
    ROUND(AVG(Sleep_Hours)::numeric, 2) as avg_sleep_hours,
    ROUND(AVG(Risk_Score)::numeric, 2) as avg_risk_score,
    ROUND(AVG(Exercise_Hours)::numeric, 2) as avg_exercise_hours,
    ROUND(AVG(Phone_Checks_Per_Day)::numeric, 0) as avg_phone_checks
FROM teen_phone_data
GROUP BY 
    DATE_TRUNC('minute', processing_time),
    Gender,
    Addiction_Level,
    Health_Category;

CREATE INDEX idx_summary_time ON teen_addiction_summary(time_bucket);

-- Fonction de rafraîchissement
CREATE OR REPLACE FUNCTION refresh_summary_view()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY teen_addiction_summary;
END;
$$ LANGUAGE plpgsql;

-- Statistiques
SELECT 'Database initialized successfully' as status;