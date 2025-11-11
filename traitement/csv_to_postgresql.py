
"""
Script très simple: convertir un CSV en fichier SQL (CREATE TABLE + INSERT)
Sortie: data/<table>.sql
Règles:
- trim des champs texte
- champs vides -> NULL
- toutes les colonnes en TEXT pour éviter les erreurs de type
"""
import sys
import os
import csv


def q_ident(name: str) -> str:
    return '"' + name.replace('"', '""') + '"'


def q_literal(val: str):
    if val is None or val == "":
        return "NULL"
    # nombre simple? (optionnel) mais on reste TEXT pour robustesse, donc on quote
    return "'" + val.replace("'", "''") + "'"


def convert(csv_path: str, table: str, output_sql_path: str):
    print(f"📖 Lecture: {csv_path}")
    with open(csv_path, "r", encoding="utf-8") as f:
        reader = csv.reader(f)
        rows = list(reader)
    if not rows:
        raise RuntimeError("CSV vide")

    headers = [h.strip() for h in rows[0]]
    data_rows = rows[1:]

    # Nettoyage simple: trim, vides -> "" (que l'on traduira en NULL au moment des INSERT)
    cleaned = []
    for r in data_rows:
        cleaned.append([(c.strip() if isinstance(c, str) else c) for c in r])

    os.makedirs(os.path.dirname(output_sql_path), exist_ok=True)
    print(f"📝 Génération SQL: {output_sql_path}")
    with open(output_sql_path, "w", encoding="utf-8") as out:
        out.write("-- Simple SQL generated from CSV\n")
        out.write(f"DROP TABLE IF EXISTS {q_ident(table)};\n")
        out.write(f"CREATE TABLE {q_ident(table)} (\n")
        out.write("  " + ",\n  ".join(f"{q_ident(h)} TEXT" for h in headers) + "\n")
        out.write(");\n")
        for r in cleaned:
            # aligner longueur
            row_vals = r + [""] * (len(headers) - len(r))
            vals_sql = ", ".join(q_literal(v) for v in row_vals)
            out.write(f"INSERT INTO {q_ident(table)} (" + ", ".join(q_ident(h) for h in headers) + f") VALUES ({vals_sql});\n")
    print("✅ Terminé")


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="CSV -> SQL (CREATE TABLE + INSERT), TEXT only")
    parser.add_argument("--csv", required=True, help="Chemin du CSV source")
    parser.add_argument("--table", default="teen_phone_addiction", help="Nom de la table")
    parser.add_argument("--output", help="Chemin fichier SQL de sortie (défaut: data/<table>.sql)")
    args = parser.parse_args()

    output = args.output or os.path.join("data", f"{args.table}.sql")
    try:
        convert(args.csv, args.table, output)
    except Exception as e:
        print(f"❌ Erreur: {e}")
        sys.exit(1)