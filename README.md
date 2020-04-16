
# The Graal Native Image Builder Feature 


Hi, Spring fans! Welcome to another installment of _Spring Tips_. In this installment were going to look at the new support just released for building Sprin gboot applications with Graalvm .

graalvm is  several things. its a C1 JIY replacemnt. You can lsiten to this episode of my podcast, _A Bootiful Podcast_, with [GraalVM contributor and Twitter engineer Chris Thalinger](https://mcdn.podbean.com/mf/web/mkmzne/8182415b-d8d6-4238-9141-83d845d98498.mp3), for more details on this. It lets you run regualr Sring appliations faster in certain conditions and so its worth exploration for htat rason alone. 

but were not going to talk about that in this video. instead, were goin to olook at a particular ocmponetn inside Graal vsqlled substrate vm. SUbstrate vm lets you build native images out your java application.  The native image builder is an exercise in compromise. if you give it enough information to copletely isolate and control everything about your application -   dynmaically linked libraries,  refelction,   proxies, etc. - then it can turn you java applicatin into a statically linkned bianry, sort of like a C or Go-lang application. the process is, being honest hre, painful. BUT, once you do that then the tool can generate native code for you that is _blazingly_ fast. the resulting aplication takes wa less ram, and rstarts up in waaaay below a second. Pretty tantalizing eh? it sure is! 

Keep i mind thogh that there are other costs to be aware of when you run the palication. graal native images are not java pplications. thye dont even run on a tradition jvm. graal vm is developed by oracle labs, and so theres some level of cooperation between the java and graalvm teams, but i would not call it java. the resulting bimnary is not cross platform. and when it runs, it wont run on the jvm, itll run on another runtime called substrate vm. 

So the tradeoffs are many but still, i think thres a lot of potential vaue in using this  tool to build applications. Especially those desitned for prodution in a cloud envronemtn where scale and efficiency is fo paramount concern. 

* downlaod grall vm or use SDK manager on mac: `sdk install java 20.0.0.r8-grl` 
* make sure upi set tje emvorpmemt variable JAVA_HOME to point to the current version of GraalVm - the tools require that in some places, it seems.
* make sure ur using graalvm and java 8 
* install `gu install native-iamge`
* 