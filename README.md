
# The Graal Native Image Builder Feature 


Hi, Spring fans! Welcome to another installment of _Spring Tips_. In this installment were going to look at the new support just released for building Sprin gboot applications with Graalvm .

graalvm is  several things. its a C1 JIY replacemnt. You can lsiten to this episode of my podcast, _A Bootiful Podcast_, with [GraalVM contributor and Twitter engineer Chris Thalinger](https://mcdn.podbean.com/mf/web/mkmzne/8182415b-d8d6-4238-9141-83d845d98498.mp3), for more details on this. It lets you run regualr Sring appliations faster in certain conditions and so its worth exploration for htat rason alone. 

but were not going to talk about that in this video. instead, were goin to olook at a particular ocmponetn inside Graal vsqlled substrate vm. SUbstrate vm lets you build native images out your java application.  

* downlaod grall vm or use SDK manager on mac: `sdk install java 20.0.0.r8-grl` 
* make sure upi set tje emvorpmemt variable JAVA_HOME to point to the current version of GraalVm - the tools require that in some places, it seems.
* make sure ur using graalvm and java 8 
* install `gu install native-iamge`