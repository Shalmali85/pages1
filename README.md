## Instructions for the logging and monitoring

### Creating and Updating the log-pv.yaml and log-pvc.yaml
- Create two files **log-pv.yaml** and **log-pvc.yaml** under deployment directory
- For log-pv.yaml fill all the sections
  * Assign name as log-persistent-volume under metadata section
  * Assign volumeMode to FileSystem under spec
  * Assign storageClassName to slow under spec
  * Capacity storage would be 500Mi
  * accessModes would be ReadWriteOnce
  * hostPath would be "/mnt/logs"
```yaml
kind: PersistentVolume
apiVersion: v1
metadata:
  name: log-persistent-volume
  namespace: <your-name>
  labels:
    type: local
spec:
  volumeMode: Filesystem
  storageClassName: manual
  capacity:
    storage: 500Mi
  accessModes:
  - ReadWriteMany
  hostPath:
    path: "/mnt/logs"
```
- For log-pvc.yaml fill all the sections
  * name would be log-persistent-claim
  * volumeMode would be  FileSystem under spec
  * storageClassName would be slow under spec
  * resources/requests/storage would be 500Mi
  * accessModes would be ReadWriteOnce
 ```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: log-persistent-claim
  namespace: <your-name>
spec:
  volumeMode: Filesystem
  storageClassName: manual
  accessModes:
  - ReadWriteMany
  resources:
    requests:
      storage: 500Mi
```
- Add below properties in application.properties in both test and source
```properties
logging.file.name=/var/tmp/pages-app.log
debug=true
logging.level.org.springframework.web=debug
logging.level.root=debug
```
- Code change in HomeController
  * Add a Logger from slf4j api
  * Add  debug,warn,trace,info and error messages in getPage() method
```java
package org.dell.kube.pages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class HomeController {
    Logger logger = LoggerFactory.getLogger(HomeController.class);
    private String pageContent;

    public HomeController(@Value("${page.content}") String pageContent){
        this.pageContent=pageContent;

    }
    @GetMapping
    public String getPage(){
        logger.debug("Welcome Page Accessed");
        logger.info("Welcome Page Accessed");
        logger.trace("Welcome Page Accessed");
        logger.warn("Welcome Page Accessed");
        logger.error("Welcome Page Accessed");
        return "Hello from page : "+pageContent+" ";
    }


}
```
- Few logging properties are added in application.properties files of test and source in above step. Run your application and verify the logs
- Comment the log properties from application.properties  of both test and source.
- Add logback.xml under resources folders of both test and source with basic logging configuration for FILE and STDOUT appender
```xml
  <?xml version = "1.0" encoding = "UTF-8"?>
  <configuration>
      <include resource="org/springframework/boot/logging/logback/base.xml"/>
      <logger name="org.springframework.web" level="DEBUG"/>
      <appender name = "STDOUT" class = "ch.qos.logback.core.ConsoleAppender">
          <encoder>
              <pattern>[%d{yyyy-MM-dd'T'HH:mm:ss.sss'Z'}] [%C] [%t] [%L] [%-5p] %m%n</pattern>
          </encoder>
      </appender>
  
      <appender name = "FILE" class = "ch.qos.logback.core.FileAppender">
          <File>/var/tmp/pages-app.log</File>
          <encoder>
              <pattern>[%d{yyyy-MM-dd'T'HH:mm:ss.sss'Z'}] [%C] [%t] [%L] [%-5p] %m%n</pattern>
          </encoder>
      </appender>
  
      <root level = "DEBUG">
          <appender-ref ref = "FILE"/>
          <appender-ref ref = "STDOUT"/>
      </root>
  </configuration>
```
- Add log volume details in pages-deployment.yaml
  
- Add liveness and readiness probe information in pages-deployment.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: pages
    servicefor: pages
  name: pages
  namespace: <your-name>
spec:
  replicas: 1
  selector:
    matchLabels:
      app: pages
      servicefor: pages
  strategy: {}
  template:
    metadata:
      labels:
        app: pages
        servicefor: pages
    spec:
      volumes:
      - name: log-volume
        persistentVolumeClaim:
          claimName: log-persistent-claim
      containers:
      - image: adityapratapbhuyan/pages:logging
        imagePullPolicy: Always
        name: pages
        ports:
          - containerPort: 8080
        env:
        - name: PAGE_CONTENT
          valueFrom:
              configMapKeyRef:
                name: pages-config-map
                key: PAGE_CONTENT
        volumeMounts:
        - name: log-volume
          mountPath: "/var/tmp/"
        readinessProbe:
          tcpSocket:
           port: 8080
          initialDelaySeconds: 150
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 150
        resources: {}
status: {}
```
- Build the application with 
```sh
./gradlew clean build
```
- Docker build and push the application with tag **logging**
- Change the tag to *logging* in pages-deployment.yaml
- Use the following commands to deploy the application in kubernetes
```shell script
kubectl apply -f deployment/pages-namespace.yaml
kubectl apply -f deployment/log-pv.yaml
kubectl apply -f deployment/log-pvc.yaml
kubectl apply -f deployment/pages-config.yaml
kubectl apply -f deployment/pages-service.yaml
kubectl apply -f deployment/pages-deployment.yaml
```
- Change the value of **tags** in *pipeline.yaml* to *logging* 
- Put below instructions in pipeline.yaml  to create pv and pvc, just below the statement "kubectl apply -f deployment/pages-namespace.yaml"
```yaml
kubectl apply -f deployment/log-pv.yaml
kubectl apply -f deployment/log-pvc.yaml
```
- Push the code to github repository to start the pipeline
- In PKS cluster the application ready time would be delayed. The application would be ready after 150 seconds as the readiness probe would start after 150 seconds
- Keep on checking the status of the pod which is part of the pages deployment
- After sometime though the status might be **Running**, it might be showing **Not Ready**