#/bin/bash
mount -t cifs -o username=cifs,password=cifs,vers=2.0 //192.168.20.74/Share /mnt/walkfolder &&
java -Djava.security.egd=file:/dev/./urandom org.springframework.boot.loader.JarLauncher
