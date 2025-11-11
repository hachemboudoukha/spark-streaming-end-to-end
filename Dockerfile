# Dockerfile pour l'application Spark Streaming
FROM openjdk:11-jdk-slim

# Installer Scala et sbt
RUN apt-get update && \
    apt-get install -y curl && \
    curl -s "https://get.sdkman.io" | bash && \
    bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && \
             sdk install scala 2.13.12 && \
             sdk install sbt 1.11.7"

# Installer Python et pip pour les scripts de traitement
RUN apt-get install -y python3 python3-pip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Définir le répertoire de travail
WORKDIR /app

# Copier les fichiers du projet
COPY build.sbt ./
COPY project ./project
COPY src ./src
COPY traitement ./traitement

# Installer les dépendances Python
RUN pip3 install -r traitement/requirements.txt

# Compiler le projet Scala
RUN bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && sbt compile"

# Exposer les ports nécessaires
EXPOSE 9092 2181

# Commande par défaut
CMD ["bash"]

