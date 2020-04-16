
# The Graal Native Image Builder Feature 

Hi, Spring fans! Welcome to another installment of _Spring Tips_. In this installment, we're going to look at the new support just released for building Spring Boot applications with GraalVM. We've looked at GraalVM and native images in another Spring Tips when we looked at Spring Fu.

GraalVM is several things. It's a C1 replacement for a standard OpenJDK install. You can listen to this episode of my podcast, [_A Bootiful Podcast_](http://bootifulpdcast.fm), with [GraalVM contributor and Twitter engineer Chris Thalinger](https://spring.io/blog/2019/05/24/a-bootiful-podcast-twitter-s-chris-thalinger-on-java-graal-jvms-jits-and-more), for more details on this use of GraalVM. It lets you run regular Sring applications faster in certain conditions, and so it's worth exploring for that reason alone. 

We're not going to talk about that in this video. Instead, we're going to look at a particular component inside Graal VM called the native image builder and SubstrateVM. SubstrateVM lets you build native images out of your Java application. Incidentally, I _also_ did a podcast with [Oracle Labs' Oleg Shelajev](https://spring.io/blog/2020/02/07/oleg-elajev-on-zeroturnaround-graalvm-the-vjug-and-so-much-more) on this and other uses of GraalVM.  The native image builder is an exercise in compromise. If you give GraalVM enough information about your application's runtime behavior  -  dynamically linked libraries,  reflection,   proxies, etc. - then it can turn your Java application into a statically linked binary, sort of like a C or Go-lang application. The process is, being honest here, sometimes... _painful_. BUT, once you do that, then the tool can generate native code for you that is _blazingly_ fast. The resulting application takes _way_ less RAM and starts up in below a second. _Waaay_ below a second. Pretty tantalizing, eh? It sure is! 

Keep in mind though that there are other costs to be aware of when you run the application. Graal native images are not Java applications. They don't even run on a traditional JVM. Oracle Labs develop GraalVM, and so there's some level of cooperation between the Java and GraalVM teams, but I would not call it Java. The resulting binary is not cross-platform.  When the application runs, it won't run on the JVM; it'll run on another runtime called SubstrateVM. 

So the tradeoffs are many, but still, I think there's a lot of potential value in using this tool to build applications—especially those destined for production in a cloud environment where scale and efficiency are fo paramount concern. 

Let's get started. You're going to need to install GraalVM. You could download [it here](https://www.graalvm.org/), or u could download it using [SDKmanager](http://sdkman.io). I like to use SDKManager to install my Java distributions.  GraalVm tends to be a little behind the mainline version of Java. Currently, it supports java 8 and java 11. Not, notably, java 14 or 15 or whatever the current version of Java is when you read and watch this. 

Do this to install graalvm for Java 8: `sdk install java 20.0.0.r8-grl`. I'd recommend Java 8, instead of java 11, as there are some subtle bugs I can't quite figure out yet with the java 11 variant. 

Once you've done that, you also need to install the native image builder component separately. Run this: `gu install native-image`.  `gu` is a utility that you get in a graalvm. Finally, make sure you have `JAVA_HOME` is set up to point to graalvm. on my machine, a Macintosh with SDKMAN, my Java_HOME looks like this: 

```bash
export JAVA_HOME=$HOME/.sdkman/candidates/java/current/
``` 

Ok, now that you've got that all set up, let's look at our application. First thing, go [to the Spring Initializr ](http://start.Spring.io) and generate a new project using `Lombok`, `R2DBC`, `PostgreSQL`, and `Reactive Web`. 

You've seen this kind of code a million times, so I won't review it beyond reprinting it here. 

```java
package com.example.reactive;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Log4j2
@SpringBootApplication(proxyBeanMethods = false)
public class ReactiveApplication {

    @Bean
    RouterFunction<ServerResponse> routes(ReservationRepository rr) {
        return route()
            .GET("/reservations", r -> ok().body(rr.findAll(), Reservation.class))
            .build();
    }

    @Bean
    ApplicationRunner runner(DatabaseClient databaseClient, ReservationRepository reservationRepository) {
        return args -> {

            Flux<Reservation> names = Flux
                .just("Andy", "Sebastien")
                .map(name -> new Reservation(null, name))
                .flatMap(reservationRepository::save);

            databaseClient
                .execute("create table reservation(id serial primary key, name varchar(255) not null )")
                .fetch()
                .rowsUpdated()
                .thenMany(names)
                .thenMany(reservationRepository.findAll())
                .subscribe(log::info);
        };
    }


    public static void main(String[] args) {
        SpringApplication.run(ReactiveApplication.class, args);
    }
}

interface ReservationRepository extends ReactiveCrudRepository<Reservation, Integer> {
}


@Data
@AllArgsConstructor
@NoArgsConstructor
class Reservation {

    @Id
    private Integer id;
    private String name;
}

```

The only notable thing in this application is that we're using SPring Noto's proxyBeanMethods attribute to make sure that we avoid the use of CGLIb and any other non-JDK proxies in the app. Graal _hates_ non-JDK proxies, and even with JDK proxies, there's work to be done to make Graal aware of it. This attribute is new in spring framework 5.2 and is designed, in part, to support graalvm applications. 


So let's talk about that. I alluded earlier that we need to teach GraalVm about te tricky things we might do in our application at runtime that it might not appreciate if we do it in a native image. Things like reflection, proxies, etc. There are a few ways to do this. You can handcraft some configuration and include it in your build. Graal will automatically add that. You can also run your program under the watch of a Java agent that will note the tricky things that your application does and - once the application's concluded - write all that stuff down in config files, which can then be fed to the Graal compiler.

Another thing you can do when you run the application is run a _feature_.  A Graal feature is sort of like a Java agent. it can feed information into the Graal compiler based on whatever analysis it does. Our feature knows and understands how Spring applications work. It knows when Spring beans are proxies. It knows how classes are constructed dynamically at runtime. It knows how SPring works, and it knows what Graal wants, most of the time. (this is an early release, after all!)

We also need to customize the build. Heres my `pom.xml`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.3.0.M4</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.example</groupId>
    <artifactId>reactive</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <properties>
        <start-class>
            com.example.reactive.ReactiveApplication
        </start-class>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.experimental</groupId>
            <artifactId>spring-graal-native</artifactId>
            <version>0.6.0.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context-indexer</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-r2dbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>

        <dependency>
            <groupId>io.r2dbc</groupId>
            <artifactId>r2dbc-h2</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.junit.vintage</groupId>
                    <artifactId>junit-vintage-engine</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
    </dependencies>

    <build>
        <finalName>
            ${project.artifactId}
        </finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

    <repositories>
        <repository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
        </repository>
    </repositories>
    <pluginRepositories>
        <pluginRepository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
        </pluginRepository>
    </pluginRepositories>


    <profiles>
        <profile>
            <id>graal</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.graalvm.nativeimage</groupId>
                        <artifactId>native-image-maven-plugin</artifactId>
                        <version>20.0.0</version>
                        <configuration>
                            <buildArgs>
-Dspring.graal.mode=initialization-only -Dspring.graal.dump-config=/tmp/computed-reflect-config.json -Dspring.graal.verbose=true -Dspring.graal.skip-logback=true --initialize-at-run-time=org.springframework.data.r2dbc.connectionfactory.ConnectionFactoryUtils --initialize-at-build-time=io.r2dbc.spi.IsolationLevel,io.r2dbc.spi --initialize-at-build-time=io.r2dbc.spi.ConstantPool,io.r2dbc.spi.Assert,io.r2dbc.spi.ValidationDepth --initialize-at-build-time=org.springframework.data.r2dbc.connectionfactory -H:+TraceClassInitialization --no-fallback --allow-incomplete-classpath --report-unsupported-elements-at-runtime -H:+ReportExceptionStackTraces --no-server --initialize-at-build-time=org.reactivestreams.Publisher --initialize-at-build-time=com.example.reactive.ReservationRepository --initialize-at-run-time=io.netty.channel.unix.Socket --initialize-at-run-time=io.netty.channel.unix.IovArray --initialize-at-run-time=io.netty.channel.epoll.EpollEventLoop --initialize-at-run-time=io.netty.channel.unix.Errors
                            </buildArgs>
                        </configuration>
                        <executions>
                            <execution>
                                <goals>
                                    <goal>native-image</goal>
                                </goals>
                                <phase>package</phase>
                            </execution>
                        </executions>
                    </plugin>
                    <plugin>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-maven-plugin</artifactId>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>

</project>

```

Tne notable thing here is that we've added the `native-image-maven-plugin` plugin to the build. The plugin also takes some command line configurations that help it understand what it should do. That long list of command-line arguments in the `buildArgs` elements represent the command line witches required to make this application run. (I owe a _huge_  thanks to Spring Graal Feature lead Andy Clement for figuring all this out!) 

```xml
<dependency>
    <groupId>org.springframework.experimental</groupId>
    <artifactId>spring-graal-native</artifactId>
    <version>0.6.0.RELEASE</version>
</dependency>
```

We want to exploit as many vehicles to contribute information to the GraalVM compiler about how the application should run. We're going to leverage the Java agent approach. Were going to leverage the Graal feature approach. We're also going to leverage command-line configuration. All this information, taken together, gives graal enough information to turn out the application to a statically compiled native image successfully. The goal, in the long term, is for Spring projects and the Spring Graal feature fo everything to support this process.

Now that we got this all configured, let's build the application. Here's the basic workflow.

* Compile the Java application, as normal 
* runt he java application with the java agent to collect information. We need to make sure to exercise the application at this point. Exercise every pathway possible! This is precisely the sort of use case for CI and testing, by the way! They always say that you should make your application work (which you can do by testing it) and _then_ make it fast. Now, with Graal, you can do both! 
* then rebuild the application again, this time with the `graal` profile active to compile the native image using the information gathered from the first run. 

```bash 
mvn -DskipTests=true clean package
export MI=src/main/resources/META-INF
mkdir -p $MI 
java -agentlib:native-image-agent=config-output-dir=${MI}/native-image -jar target/reactive.jar

## it's at this point that you need to exercise the application: http://localhost:8080/reservations 
## then hit CTRL + C to stop the running application.

tree $MI
mvn -Pgraal clean package
```
And if all that worked, then you should be able to see the resulting application in the `target` directory. Run it like this.

```bash 
./target/com.example.reactive.reactiveapplication 
```

Your application should spin up, evident from the output of the application like this. 

```
2020-04-15 23:25:08.826  INFO 7692 --- [           main] c.example.reactive.ReactiveApplication   : Started ReactiveApplication in 0.099 seconds (JVM running for 0.103)
```

Pretty cool, eh? The GraalVM native image builder is an excellent fit when paired with a cloud platform like CloudFoundry or Kubernetes. You can easily containerize the application and get it working on a cloud platform with a minimal footprint. Enjoy! And as always, we'd love to hear from you. Does this technology suit your use case? Questions? Comments? Feedback and sound off to [us on Twitter](http://twitter.com/Springcentral).