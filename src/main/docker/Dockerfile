FROM openjdk:11-jre-slim as builder
WORKDIR application
ADD maven/${project.build.finalName}.jar ${project.build.finalName}.jar
RUN java -Djarmode=layertools -jar ${project.build.finalName}.jar extract

FROM openjdk:11-jre-slim
LABEL PROJECT_NAME=${project.artifactId} \
      PROJECT=${project.id}
RUN apt-get update && apt-get install -y cifs-utils

EXPOSE 8080

WORKDIR application
#COPY smbcreds /application/
COPY entrypoint.sh /application/
RUN mkdir /mnt/walkfolder
RUN chmod +x /application/entrypoint.sh
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
# ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "org.springframework.boot.loader.JarLauncher"]
# ENTRYPOINT ["/bin/sh", "-c", "./entrypoint.sh"]
# ENTRYPOINT mount -t cifs -o username=cifs,password=cifs,vers=2.0 //192.168.20.74/Share /mnt/walkfolder
# ENTRYPOINT ["/bin/sh", "-c", "echo one && echo two"]
ENTRYPOINT ["/bin/bash", "-c", "(sleep 10 ; mount -a) & java -Djava.security.egd=file:/dev/./urandom org.springframework.boot.loader.JarLauncher"]
