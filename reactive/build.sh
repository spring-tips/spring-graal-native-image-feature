mvn -DskipTests=true clean package
export MI=src/main/resources/META-INF
mkdir -p $MI
java -agentlib:native-image-agent=config-output-dir=${MI}/native-image -jar target/reactive.jar
tree $MI
mvn -Pgraal clean package
