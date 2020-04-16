
# The Graal Native Image Builder Feature 


Hi, Spring fans! Welcome to another installment of _Spring Tips_. In this installment were going to look at the new support just released for building Sprin gboot applications with Graalvm . weve looked at graalvm anf natie images in another Spring Tips when we looked at Spring Fu.

graalvm is  several things. its a C1 JIY replacemnt. You can lsiten to this episode of my podcast, _A Bootiful Podcast_, with [GraalVM contributor and Twitter engineer Chris Thalinger](https://mcdn.podbean.com/mf/web/mkmzne/8182415b-d8d6-4238-9141-83d845d98498.mp3), for more details on this. It lets you run regualr Sring appliations faster in certain conditions and so its worth exploration for htat rason alone. 

but were not going to talk about that in this video. instead, were goin to olook at a particular ocmponetn inside Graal vsqlled substrate vm. SUbstrate vm lets you build native images out your java application.  The native image builder is an exercise in compromise. if you give it enough information to copletely isolate and control everything about your application -   dynmaically linked libraries,  refelction,   proxies, etc. - then it can turn you java applicatin into a statically linkned bianry, sort of like a C or Go-lang application. the process is, being honest hre, painful. BUT, once you do that then the tool can generate native code for you that is _blazingly_ fast. the resulting aplication takes wa less ram, and rstarts up in waaaay below a second. Pretty tantalizing eh? it sure is! 

Keep i mind thogh that there are other costs to be aware of when you run the palication. graal native images are not java pplications. thye dont even run on a tradition jvm. graal vm is developed by oracle labs, and so theres some level of cooperation between the java and graalvm teams, but i would not call it java. the resulting bimnary is not cross platform. and when it runs, it wont run on the jvm, itll run on another runtime called substrate vm. 

So the tradeoffs are many but still, i think thres a lot of potential vaue in using this  tool to build applications. Especially those desitned for prodution in a cloud envronemtn where scale and efficiency is fo paramount concern. 

Lets get started. youre going to need to install GraalVM. You could download [it here](https://www.graalvm.org/) or u could download it using [SDKmanager](http://sdkman.io). I liek to use SDKmanage ro install tmy java distributions.  GrralVm tensd sto bw a little ehidn the mainline version of jva. currently it supports java 8 and java 11. Not, notably, java 14 or 15 or whatever the current versio of java is when you read and watch this. 

Do this to install graalvm for Java 8: `sdk install java 20.0.0.r8-grl`. Id recommend java 8, insead of java 11, as there are some subtle bugs I cant quite figure out yet with the java 11 variant. 

Once youve done that ou also need to install the antive image builder component separately. Run this: `gu install native-image`.  `gu` is a utiluty that you get in a graalvm. Finaly, make sure you have `JAVA_HOME` is setup to point to graalvm. on my machine, a macintosh wuth SDKMAN, my Java_HOME looks like this: 

```bash
export JAVA_HOME=$HOME/.sdkman/candidates/java/current/
``` 

Ok, now that youve got that all setup,lets lok at our application. first thing, go [to the Spring Initializr ](http://start.Spring.io) and generate a new project usign `Lombok`, `R2DBC`, `PostgreSQL`, and `Reactive Web`. 

You've seen this kind of code a million times, so I wont review it excetp tot reprint it here. 

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

The only notable thing in this sapplcation is that we're using SPring Noto's proxyBeanMethods attribute to make sure that we avodi the use of CGLIb and any other non JDK proxies in the appkication. Graal _hates_ non-JDK proxies, and even with JDK proxies there's work to be done to make Graal aware of it. this attribute is new in spring framework 5.2 and is designed, in part, to support graalvm applications. 


So lets talk about that. I alluded earlier that we need o teach GraalVm about te tricky things we might do in our application at runtime that it might not appreciate if we do it in a native image. Things like revlection, proxies, etc. There are a few ways to do this. You can hand craft some configuration and include it in your build. GRaal will automatically inluce tht. You can also run your program under the watch of an java agent an that java agent will note the tricky things that your application does adn - once the applicatinos conlcided - write all that stuff dow in config files which can then be fed to the graal compiler. Another thing you can do wen you run the aplcation is run a _feature_.  A Graal feature. is sort of lie a java agent. it ahas the abiluty to feed information into the graal compiler based on whatever analysis it does. our eature knows and understands how Spring applications work. it knows when Spring beans are proxies. It knows how classes are constructed dynamically at runtiem. It knowws how SPring works and it knows wht Graal wants,, nost of the time. (this is a n early release, after all!)

Wr also need to customzr the build. Heres mu `pom.xml`.

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

Tne notable thing here is that weve added the `native-image-maven-plugin` plugin to the build. The plugin also takes some command line confguaions that help it understand what it should do. That long list of command ine arguments in the `buildArgs` elemtns represent the command line witches required to make this application eun. (I owe a _huge_  thanks to Spring Graal Featuee lead Andy Clement for figuring all this out!) 

```xml
<dependency>
    <groupId>org.springframework.experimental</groupId>
    <artifactId>spring-graal-native</artifactId>
    <version>0.6.0.RELEASE</version>
</dependency>
```

We want to exploit as many vehicoles to contribute information to the graal compielr about how tht epaplicationm shoujld run. We're going to levetag eht ejava agent approach. Were going to leverage the Graal feature approach. Wer alos going to leverage command line configuration. All this ifnormation, taken together, gives graal enough information to successfully turn out appkicatino ito a statically compiled native image. The goal, in the long tterm, is for Spring projects and the Spring Graal feature fo everuthing to support htis proces.

Now that wee got this all convifugeed lets build the application. Heres the basic workflow.

* Compilee te java applicati, as normal 
* runt he java application with the java agnt to collect iforatino. We need to make sure toe xercise the application at this point. Exercise every pathway possible! This is exactly the sort of use case for CI and testing, by the way! They always ay that you should make your application work (which you can do by testing it) and _then_ make it fast. Now, with Graal, you can do both! 
* then rebuild the application again, this time wth the `graal` profie active to compile the native image using the infroramtion gathered from the first run. 

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
And if all that worked, theh you should be able to see the reusllting application in the `target` diectory. Run it, like this.

```bash 
./target/com.example.reactive.reactiveapplication 
```

Your application should zpin up, evident from the output of the applkcation like this. 

```
2020-04-15 23:25:08.826  INFO 7692 --- [           main] c.example.reactive.ReactiveApplication   : Started ReactiveApplication in 0.099 seconds (JVM running for 0.103)
```

Pretty cool, eh? The greaal nativ eimage builder is a nice fit when paired with a cloud platform like cloud goundry or kberentes. You can easily containerize the application and get it working in a cloud paltform with a minimal footprint. Enjoy! And as always, we'd love to hear from you. Does this technology sut your use case? Qustions? Comments? Feedback and sound off to [us on Twitter](http://twitter.com/Springcentral).