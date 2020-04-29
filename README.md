# prices-quarkus-kafka-example
Kafka example with quarkus and kafka (amq streams
clea)
*This uses JDK 8 but probably needs to be updated to use 11 since 8 is deprecated with later versions of Quarkus*


## Local development

### Deploy kafka locally using docker-compose
```
docker-compose -f kafka/docker-compose.yaml up -d 
#to view the docker services
docker-compose -f kafka/docker-compose.yaml ps
# this should return two entries
```

To stop the running kafka
```sh
docker-compose -f kafka/docker-compose.yaml down
```

### Run the Price Generator
live-coding
```sh
cd price-generator
mvn clean compile quarkus:dev
```

### Run the Price Converter
live-coding
```sh
cd price-converter
mvn clean compile quarkus:dev
```

**Note**: If running both microservices concurrently, you may want to pass *-Ddebug=false* to avoid collision on debug ports

### Test
navigate to http://localhost:8080/prices.html
Every 5 seconds you should see the page refresh with a new price

**Note:** if you choose to deploy Kafka elsewhere or maybe you are developing in CodeReady Workspaces (CRW), you can pass the appropriate location of the kafka bootstrap to the local deployment using
```sh
-Dmp.messaging.outgoing.generated-price.bootstrap.servers=<bootstrap location>
```
For example
```sh
#kakfa_bootstrap=prices-cluster-kafka-bootstrap-prices-kafka.apps.cluster-60c1.60c1.example.opentlc.com:80
kakfa_bootstrap=prices-cluster-kafka-bootstrap.prices-kafka:9092 
mvn clean compile quarkus:dev -Dmp.messaging.outgoing.generated-price.bootstrap.servers=$kakfa_bootstrap
```


### Clean up local development
Remember to shutdown the kafka services once down with local development
```sh
docker-compose -f kafka/docker-compose.yaml down
```

## Deploy AMQ Streams
Set the OpenShi name
```sh
PRICES_PROJECT=prices-kafka
```
**Note**: The following commands assume that you are logged into and OpenShift cluster and that the AMQ Streams Operator has been installed on the cluster and is watching the namespace

```sh
oc new-project $PRICES_PROJECT
```

### Create the Kafka Cluster
You can cut and paste the contents of kafka/PriceKafkaCluster.yaml using the OpenShift Operator UI or just run the folllowiing
```sh
oc apply -f kafka/PriceKafkaCluster.yaml -n $PRICES_PROJECT
oc get Kafka
```
Wait till the Kafka cluster and zookeeper statefulsets, and prices-cluster-entity-operator deployment are running

### Create the Topic
You can cut and paste the contents of kafka/PriceKafkaTopic.yaml using the OpenShift Operator UI or just run the folllowiing
```sh
oc apply -f kafka/PriceKafkaTopic.yaml -n $PRICES_PROJECT
oc get KakfaTopic
```
### if you want, you can test topics using oc client
```sh
oc run kafka-producer -ti --image=registry.redhat.io/amq7/amq-streams-kafka-23:1.3.0 --rm=true --restart=Never -- bin/kafka-console-producer.sh --broker-list prices-cluster-kafka-bootstrap:9092 --topic prices

oc run kafka-consumer -ti --image=registry.redhat.io/amq7/amq-streams-kafka-23:1.3.0 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server prices-cluster-kafka-bootstrap:9092 --topic prices --from-beginning

# clean up after testing
oc delete pod kafka-producer
```

## Deploy microservices on openshift

**Note**: This assumes that the Kafka Cluster has been deployed on OpenShift and in the sa
```sh
oc project $PRICES_PROJECT
```
Quarkus has an extension to faciliate deployment to OpenShift. 
```
./mvnw 
quarkus:add-extension -Dextensions="openshift"
```
The two microserivces already include the openshift extension so there is no need to run that command

S2I is also an option, but not used in this example

**Note**: If you want to build the image without deploying, the following command can be run (it performs an S2I build). This will create a BuildConfig, build the image, and push to an ImageStream
```sh
./mvnw clean package -Dquarkus.container-image.build=true
```

### Deploy Price Generator on OpenShift
```sh
cd price-generator
./mvnw clean package -Dquarkus.kubernetes.deploy=true
oc label dc/price-generator  app.kubernetes.io/part-of=prices --overwrite
oc label dc/price-generator app.openshift.io/runtime=java --overwrite 
```

**Note**: if the Image gets built but the deployment fails (and may with JDK8), you can deploy the microservice with
```sh
oc new-app price-generator:1.0-SNAPSHOT
oc expose service price-generator
```

### Deploy Price Converter on OpenShift
```sh
cd price-converter
./mvnw clean package -Dquarkus.kubernetes.deploy=true
oc label dc/price-converter  app.kubernetes.io/part-of=prices --overwrite
oc label dc/price-converter app.openshift.io/runtime=java --overwrite 
```
**Note**: if the Image gets built but the deployment fails(and may with JDK8), you can deploy the microservice with
```sh
oc new-app price-converter:1.0-SNAPSHOT
oc expose service price-converter
```

### Test
```
export URL="http://$(oc get route price-converter -o jsonpath='{.spec.host}')"
echo "Naviate to  URL: $URL/prices.html to view updated prices"
```
