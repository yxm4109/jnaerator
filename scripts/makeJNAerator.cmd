MAVEN_OPTS=-Xmx500m

mvn -Dmaven.jar.sign.skip=false -Dstorepass=$KEYSTORE_PASS clean deploy

#mvn -f jnaerator-runtime/pom.xml clean deploy install && mvn "-Dstorepass=$KEYSTORE_PASS" -f jnaerator/pom.xml clean deploy site-deploy
